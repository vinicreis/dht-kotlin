package io.github.vinicreis.dht.core.grpc.infra.service

import io.github.vinicreis.dht.core.grpc.domain.service.DHTServiceServerGrpc
import io.github.vinicreis.dht.core.grpc.domain.strategy.ClientStubStrategy
import io.github.vinicreis.dht.core.grpc.domain.strategy.HashStrategy
import io.github.vinicreis.dht.core.grpc.domain.strategy.ServerStubStrategy
import io.github.vinicreis.dht.core.grpc.infra.mapper.asDomain
import io.github.vinicreis.dht.core.model.ResultOuterClass
import io.github.vinicreis.dht.core.model.ResultOuterClass.Result
import io.github.vinicreis.dht.core.model.request.GetRequestOuterClass.GetRequest
import io.github.vinicreis.dht.core.model.request.JoinOkRequestOuterClass.JoinOkRequest
import io.github.vinicreis.dht.core.model.request.JoinRequestOuterClass.JoinRequest
import io.github.vinicreis.dht.core.model.request.LeaveRequestOuterClass.LeaveRequest
import io.github.vinicreis.dht.core.model.request.NewNodeRequestOuterClass.NewNodeRequest
import io.github.vinicreis.dht.core.model.request.NodeGoneRequestOuterClass.NodeGoneRequest
import io.github.vinicreis.dht.core.model.request.RemoveRequestOuterClass.RemoveRequest
import io.github.vinicreis.dht.core.model.request.SetRequestOuterClass.SetRequest
import io.github.vinicreis.dht.core.model.request.TransferRequestOuterClass.TransferRequest
import io.github.vinicreis.dht.core.model.request.nextOrNull
import io.github.vinicreis.dht.core.model.request.previousOrNull
import io.github.vinicreis.dht.core.model.response.GetResponseOuterClass.GetResponse
import io.github.vinicreis.dht.core.model.response.JoinOkResponseOuterClass.JoinOkResponse
import io.github.vinicreis.dht.core.model.response.JoinResponseOuterClass.JoinResponse
import io.github.vinicreis.dht.core.model.response.LeaveResponseOuterClass.LeaveResponse
import io.github.vinicreis.dht.core.model.response.NewNodeResponseOuterClass.NewNodeResponse
import io.github.vinicreis.dht.core.model.response.NodeGoneResponseOuterClass.NodeGoneResponse
import io.github.vinicreis.dht.core.model.response.RemoveResponseOuterClass.RemoveResponse
import io.github.vinicreis.dht.core.model.response.SetResponseOuterClass.SetResponse
import io.github.vinicreis.dht.core.model.response.TransferResponseOuterClass.TransferResponse
import io.github.vinicreis.dht.core.model.response.getResponse
import io.github.vinicreis.dht.core.model.response.joinOkResponse
import io.github.vinicreis.dht.core.model.response.joinResponse
import io.github.vinicreis.dht.core.model.response.leaveResponse
import io.github.vinicreis.dht.core.model.response.newNodeResponse
import io.github.vinicreis.dht.core.model.response.nodeGoneResponse
import io.github.vinicreis.dht.core.model.response.removeResponse
import io.github.vinicreis.dht.core.model.response.setResponse
import io.github.vinicreis.dht.core.model.response.transferResponse
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineImplBase
import io.github.vinicreis.dht.core.service.domain.DHTServiceClientStub
import io.github.vinicreis.dht.core.service.domain.DHTServiceServer
import io.github.vinicreis.dht.core.service.domain.DHTServiceServerStub
import io.github.vinicreis.dht.model.service.Node
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import io.grpc.Status
import io.grpc.StatusException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onStart
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext
import kotlin.properties.Delegates.vetoable

internal class DHTServiceServerGrpcImpl(
    override val info: Node,
    private val knownNodes: List<Node>,
    private val serverStubStrategy: ServerStubStrategy,
    private val clientStubStrategy: ClientStubStrategy,
    private val logger: Logger,
    private val hashStrategy: HashStrategy,
    coroutineContext: CoroutineContext,
) :
    DHTServiceServer,
    DHTServiceServerGrpc,
    DHTServiceCoroutineImplBase(coroutineContext),
    DHTServiceServerStub by DHTServiceServerStubImpl(coroutineContext, serverStubStrategy),
    DHTServiceClientStub by DHTServiceClientStubImpl(coroutineContext, clientStubStrategy)
{
    override val data: MutableMap<String, ByteArray> = ConcurrentHashMap()
    override var next: Node? by vetoable(null) { _, _, new ->
        new == null || new != info
    }

    override var previous: Node? by vetoable(null) { _, _, new ->
        new == null || new != info
    }

    override val responsibleForIds = knownNodes.map { it.id }.toMutableSet()
    private val queue = JobQueueService(coroutineContext)
    private val server = Grpc.newServerBuilderForPort(info.port.value, InsecureServerCredentials.create())
        .addService(this)
        .build()

    override fun start() {
        logger.info("Starting $info...")
        server.start()
        queue { join(knownNodes) }
    }

    override fun blockUntilShutdown() {
        server.awaitTermination()
    }

    override fun shutdown() {
        server.shutdown()
    }

    override suspend fun join(nodes: List<Node>) {
        for (node in nodes) {
            if(node == info) continue

            try {
                val result = node.join(info)

                if (result.isSuccess && result.getOrElse { false }) break
            } catch (e: StatusException) {
                when(e.status.code) {
                    null -> logger.severe("Join failed without status code")
                    Status.Code.UNIMPLEMENTED -> logger.severe("Join call is not implemented on server")
                    Status.Code.OK -> logger.severe("Join failed with OK status code. This should not happen")
                    Status.Code.UNAVAILABLE,
                    Status.Code.DEADLINE_EXCEEDED,
                    Status.Code.CANCELLED,
                    Status.Code.ABORTED,
                    Status.Code.NOT_FOUND -> logger.info("$node might not be available yet. Skipping...")
                    Status.Code.PERMISSION_DENIED,
                    Status.Code.UNAUTHENTICATED -> logger.info("Join failed by authentication error")
                    Status.Code.INVALID_ARGUMENT,
                    Status.Code.FAILED_PRECONDITION,
                    Status.Code.OUT_OF_RANGE,
                    Status.Code.ALREADY_EXISTS ->
                        logger.warning("Join failed with code ${e.status.code}. Check your arguments")
                    Status.Code.DATA_LOSS,
                    Status.Code.UNKNOWN,
                    Status.Code.RESOURCE_EXHAUSTED,
                    Status.Code.INTERNAL -> logger.info("Join failed by internal error ${e.status.code}")
                }
            }
        }
    }

    override suspend fun leave() {
        next?.also { nextNode ->
            nextNode.leave(info, previous)
            nextNode.transfer(info, data)
            previous?.nodeGone(nextNode.takeIf { it != previous })
        }
    }

    override suspend fun join(request: JoinRequest): JoinResponse {
        logger.fine("Received JOIN request...")

        val newNode = request.node.asDomain

        queue {
            if (info isNotResponsibleFor newNode) {
                next?.join(newNode) ?: error("Next node should be set if $info is not responsible for $newNode")
            } else {
                newNode.joinOk(info, previous ?: info)
                newNode.transfer(info, data from newNode)
                data removeFrom newNode
                responsibleForIds.remove(newNode.id)
                previous = newNode
            }
        }

        return joinResponse { result = Result.SUCCESS }
    }

    override suspend fun joinOk(request: JoinOkRequest): JoinOkResponse {
        logger.fine("Received JOIN_OK request...")

        require(request.hasNext()) { "A next node is required on JOIN_OK request" }
        val node = request.next.asDomain

        next = node
        previous = request.previousOrNull?.asDomain

        queue { previous?.newNode(info) }
        responsibleForIds.apply {
            clear()
            add(info.id)
        }

        return joinOkResponse { result = Result.SUCCESS }
    }

    override suspend fun newNode(request: NewNodeRequest): NewNodeResponse {
        logger.fine("Received NEW_NODE request...")

        next = request.node.asDomain

        return newNodeResponse { result = Result.SUCCESS }
    }

    override suspend fun nodeGone(request: NodeGoneRequest): NodeGoneResponse {
        logger.fine("Received NODE_GONE request...")

        next = request.nextOrNull?.asDomain?.takeIf { it != info }

        return nodeGoneResponse { result = Result.SUCCESS }
    }

    override suspend fun leave(request: LeaveRequest): LeaveResponse {
        logger.fine("Received LEAVE request...")

        previous = request.previousOrNull?.asDomain?.takeIf { it != info }
        responsibleForIds.add(request.node.asDomain.id)

        return leaveResponse { result = Result.SUCCESS }
    }

    override suspend fun get(request: GetRequest): GetResponse {
        logger.fine("Received GET request...")

        queue {
            val key = request.key.toStringUtf8()

            when {
                info isResponsibleFor key -> {
                    data[key]?.let { bytes ->
                        request.node.asDomain.found(key, bytes)
                    } ?: request.node.asDomain.notFound(key)
                }
                next == null -> error("$info should be responsible for this key or have a next node")
                else -> next!!.get(request.node.asDomain, key)
            }
        }

        return getResponse { result = ResultOuterClass.Result.SUCCESS }
    }

    override suspend fun set(request: SetRequest): SetResponse {
        logger.fine("Received SET request...")

        queue {
            val key = request.key.toStringUtf8()

            when {
                info isResponsibleFor key -> data[key] = request.data.content.toByteArray()
                next == null -> error("Node should be responsible for this key or have a next node")
                else -> next!!.set(request.node.asDomain, key, request.data.content.toByteArray())
            }
        }

        return setResponse { result = ResultOuterClass.Result.SUCCESS }
    }

    override suspend fun remove(request: RemoveRequest): RemoveResponse {
        logger.fine("Received REMOVE request...")

        queue {
            val key = request.key.toStringUtf8()

            when {
                info isResponsibleFor key -> data.remove(key)?.let {
                    request.node.asDomain.found(key, it)
                } ?: request.node.asDomain.notFound(key)
                next == null -> error("$info should be responsible for this key or have a next node")
                else -> next!!.remove(request.node.asDomain, key)
            }
        }

        return removeResponse { result = ResultOuterClass.Result.SUCCESS }
    }

    override suspend fun transfer(requests: Flow<TransferRequest>): TransferResponse {
        logger.fine("Received TRANSFER request...")

        coroutineScope {
            requests
                .onStart { logger.fine("Collecting transfer requests...") }
                .onCompletion { logger.info("Transfer requests collection finished!\nNow responsible for keys ${data.keys}") }
                .collect { request ->
                    data[request.key.toStringUtf8()] = request.data.content.toByteArray()
                }
        }

        return transferResponse { result = Result.SUCCESS }
    }

    private infix fun Map<String, ByteArray>.from(node: Node) = filter { entry -> hashStrategy(entry.key) == node.id }

    private infix fun <V> MutableMap<String, V>.removeFrom(node: Node) {
        keys.asSequence()
            .filter { key -> key belongsTo node }
            .forEach { key -> remove(key)?.let { logger.info("Removed key $key") } }
    }

    private infix fun String.belongsTo(node: Node): Boolean {
        return node.id == hashStrategy(this)
    }

    private infix fun Node.isResponsibleFor(key: String): Boolean {
        return responsibleForIds.any { id -> id == hashStrategy(key) }
    }

    private infix fun Node.isResponsibleFor(node: Node): Boolean {
        return responsibleForIds.any { id -> id == node.id }
    }

    private infix fun Node.isNotResponsibleFor(node: Node): Boolean {
        return (this isResponsibleFor node).not()
    }
}
