package io.github.vinicreis.dht.core.grpc.infra.service

import io.github.vinicreis.dht.core.grpc.domain.service.DHTServiceGrpc
import io.github.vinicreis.dht.core.grpc.domain.strategy.HashStrategy
import io.github.vinicreis.dht.core.grpc.domain.strategy.NodeStubStrategy
import io.github.vinicreis.dht.core.grpc.infra.extensions.from
import io.github.vinicreis.dht.core.grpc.infra.mapper.asDomain
import io.github.vinicreis.dht.core.model.ResultOuterClass
import io.github.vinicreis.dht.core.model.ResultOuterClass.Result
import io.github.vinicreis.dht.core.model.request.GetRequestOuterClass.GetRequest
import io.github.vinicreis.dht.core.model.request.JoinOkRequestOuterClass.JoinOkRequest
import io.github.vinicreis.dht.core.model.request.JoinRequestOuterClass.JoinRequest
import io.github.vinicreis.dht.core.model.request.LeaveRequestOuterClass.LeaveRequest
import io.github.vinicreis.dht.core.model.request.NewNodeRequestOuterClass.NewNodeRequest
import io.github.vinicreis.dht.core.model.request.NodeGoneRequestOuterClass.NodeGoneRequest
import io.github.vinicreis.dht.core.model.request.SetRequestOuterClass.SetRequest
import io.github.vinicreis.dht.core.model.request.TransferRequestOuterClass.TransferRequest
import io.github.vinicreis.dht.core.model.request.nextOrNull
import io.github.vinicreis.dht.core.model.response.GetResponseOuterClass.GetResponse
import io.github.vinicreis.dht.core.model.response.JoinOkResponseOuterClass.JoinOkResponse
import io.github.vinicreis.dht.core.model.response.JoinResponseOuterClass.JoinResponse
import io.github.vinicreis.dht.core.model.response.LeaveResponseOuterClass.LeaveResponse
import io.github.vinicreis.dht.core.model.response.NewNodeResponseOuterClass.NewNodeResponse
import io.github.vinicreis.dht.core.model.response.NodeGoneResponseOuterClass.NodeGoneResponse
import io.github.vinicreis.dht.core.model.response.SetResponseOuterClass.SetResponse
import io.github.vinicreis.dht.core.model.response.TransferResponseOuterClass.TransferResponse
import io.github.vinicreis.dht.core.model.response.getResponse
import io.github.vinicreis.dht.core.model.response.joinOkResponse
import io.github.vinicreis.dht.core.model.response.joinResponse
import io.github.vinicreis.dht.core.model.response.leaveResponse
import io.github.vinicreis.dht.core.model.response.newNodeResponse
import io.github.vinicreis.dht.core.model.response.nodeGoneResponse
import io.github.vinicreis.dht.core.model.response.setResponse
import io.github.vinicreis.dht.core.model.response.transferResponse
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineImplBase
import io.github.vinicreis.dht.core.service.domain.DHTService
import io.github.vinicreis.dht.core.service.domain.DHTServiceClientStub
import io.github.vinicreis.dht.core.service.domain.DHTServiceServerStub
import io.github.vinicreis.dht.model.service.Node
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal class DHTServiceGrpcServerImpl(
    override val info: Node,
    private val knownNodes: List<Node>,
    private val nodeStubStrategy: NodeStubStrategy,
    private val logger: Logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.simpleName),
    private val hashStrategy: HashStrategy,
    coroutineContext: CoroutineContext,
) :
    DHTService,
    DHTServiceGrpc,
    DHTServiceServerStub by DHTServiceServerStubImpl(coroutineContext, nodeStubStrategy),
    DHTServiceClientStub by DHTServiceClientStubImpl(coroutineContext, nodeStubStrategy),
    DHTServiceCoroutineImplBase(coroutineContext)
{
    override var next: Node? = null
    override var previous: Node? = null
    override val data: MutableMap<String, ByteArray> = mutableMapOf()
    private val responsibleFor = knownNodes.toMutableSet()
    private val queue = AsyncQueueService(coroutineContext)
    private val mutableEvents = MutableSharedFlow<DHTService.Event>()
    override val events: Flow<DHTService.Event> = mutableEvents.asSharedFlow()
    private val server = Grpc.newServerBuilderForPort(info.port.value, InsecureServerCredentials.create())
        .addService(this)
        .build()

    override fun start() {
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
        mutableEvents.emit(DHTService.Event.JoinStarted)

        for (node in nodes) {
            if(node == info) continue

            try {
                val result = node.join(info)

                if (result.isSuccess && result.getOrElse { false }) break
            } catch (e: Exception) {
                logger.severe("Failed to join node $node: ${e.message}")
            }
        }

        mutableEvents.emit(DHTService.Event.Joined)
    }

    override suspend fun leave() {
        queue {
            next?.leave(previous)
            with(hashStrategy) { next?.transfer(info, data from next!!) }
        }
    }

    override suspend fun join(request: JoinRequest): JoinResponse {
        logger.info("Received JOIN request...")

        return joinResponse {
            result = let {
                try {
                    val newNode = request.node.asDomain

                    if (responsibleFor.contains(newNode)) {
                        next = newNode
                        responsibleFor.remove(newNode)
                    }

                    queue {
                        newNode.joinOk(info, previous)
                        with (hashStrategy) { newNode.transfer(info, data from newNode) }
                    }

                    Result.SUCCESS
                } catch (e: Exception) {
                    Result.FAIL
                }
            }
        }
    }

    override suspend fun joinOk(request: JoinOkRequest): JoinOkResponse {
        logger.info("Received JOIN_OK request...")

        return joinOkResponse {
            result = request.nextOrNull?.asDomain?.let { node ->
                try {
                    next = node
                    previous = request.takeIf { it.hasPrevious() }?.previous?.asDomain

                    queue { previous?.newNode(info) }

                    Result.SUCCESS
                } catch (e: Exception) {
                    Result.FAIL
                }
            } ?: Result.FAIL
        }
    }

    override suspend fun newNode(request: NewNodeRequest): NewNodeResponse {
        logger.info("Received NEW_NODE request...")

        return newNodeResponse {
            result = let {
                try {
                    next = request.node.asDomain

                    Result.SUCCESS
                } catch (e: Exception) {
                    Result.FAIL
                }
            }
        }
    }

    override suspend fun nodeGone(request: NodeGoneRequest): NodeGoneResponse {
        logger.info("Received NODE_GONE request...")

        return nodeGoneResponse {
            result = let {
                try {
                    next = request.next.asDomain

                    Result.SUCCESS
                } catch (e: Exception) {
                    Result.FAIL
                }
            }
        }
    }

    override suspend fun leave(request: LeaveRequest): LeaveResponse {
        logger.info("Received LEAVE request...")

        return leaveResponse {
            result = let {
                try {
                    previous = request.previous.asDomain

                    Result.SUCCESS
                } catch (e: Exception) {
                    Result.FAIL
                }
            }
        }
    }

    override suspend fun get(request: GetRequest): GetResponse {
        logger.info("Received GET request...")

        queue {
            val key = request.key.toStringUtf8()

            when {
                isResponsableFor(key) -> {
                    data[key]?.let { bytes ->
                        request.node.asDomain.found(key, bytes)
                    } ?: request.node.asDomain.notFound(key)
                }

                next != null -> next!!.get(request.node.asDomain, key)
            }
        }

        return getResponse { result = ResultOuterClass.Result.SUCCESS }
    }

    override suspend fun set(request: SetRequest): SetResponse {
        logger.info("Received SET request...")

        queue {
            val key = request.key.toStringUtf8()

            when {
                isResponsableFor(key) -> data[key] = request.data.content.toByteArray()
                next == null -> error("Node should be responsible for this key or have a next node")
                else -> next!!.set(request.node.asDomain, key, request.data.content.toByteArray())
            }
        }

        return setResponse { result = ResultOuterClass.Result.SUCCESS }
    }

    override suspend fun transfer(requests: Flow<TransferRequest>): TransferResponse {
        logger.info("Received TRANSFER request...")

        queue {
            requests.collect { entry ->
                val key = entry.key.toStringUtf8()

                logger.info("Receiving key $key from ${entry.node.asDomain}")

                data[key] = entry.data.content.toByteArray()
            }
        }

        return transferResponse { result = Result.SUCCESS }
    }

    private fun isResponsableFor(key: String): Boolean {
        return responsibleFor.any { it.id == hashStrategy(key) }
    }
}
