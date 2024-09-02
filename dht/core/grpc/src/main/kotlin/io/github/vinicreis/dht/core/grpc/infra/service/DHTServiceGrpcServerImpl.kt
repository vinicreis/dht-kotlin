package io.github.vinicreis.dht.core.grpc.infra.service

import io.github.vinicreis.dht.core.grpc.domain.service.DHTServiceGrpc
import io.github.vinicreis.dht.core.grpc.infra.mapper.asDomain
import io.github.vinicreis.dht.core.model.ResultOuterClass
import io.github.vinicreis.dht.core.model.ResultOuterClass.Result
import io.github.vinicreis.dht.core.model.request.FoundRequestOuterClass.FoundRequest
import io.github.vinicreis.dht.core.model.request.GetRequestOuterClass.GetRequest
import io.github.vinicreis.dht.core.model.request.JoinOkRequestOuterClass.JoinOkRequest
import io.github.vinicreis.dht.core.model.request.JoinRequestOuterClass.JoinRequest
import io.github.vinicreis.dht.core.model.request.LeaveRequestOuterClass.LeaveRequest
import io.github.vinicreis.dht.core.model.request.NewNodeRequestOuterClass.NewNodeRequest
import io.github.vinicreis.dht.core.model.request.NodeGoneRequestOuterClass.NodeGoneRequest
import io.github.vinicreis.dht.core.model.request.NotFoundRequestOuterClass.NotFoundRequest
import io.github.vinicreis.dht.core.model.request.SetRequestOuterClass.SetRequest
import io.github.vinicreis.dht.core.model.request.TransferRequestOuterClass.TransferRequest
import io.github.vinicreis.dht.core.model.request.nextOrNull
import io.github.vinicreis.dht.core.model.response.FoundResponseOuterClass.FoundResponse
import io.github.vinicreis.dht.core.model.response.GetResponseOuterClass.GetResponse
import io.github.vinicreis.dht.core.model.response.JoinOkResponseOuterClass.JoinOkResponse
import io.github.vinicreis.dht.core.model.response.JoinResponseOuterClass.JoinResponse
import io.github.vinicreis.dht.core.model.response.LeaveResponseOuterClass.LeaveResponse
import io.github.vinicreis.dht.core.model.response.NewNodeResponseOuterClass.NewNodeResponse
import io.github.vinicreis.dht.core.model.response.NodeGoneResponseOuterClass.NodeGoneResponse
import io.github.vinicreis.dht.core.model.response.NotFoundResponseOuterClass.NotFoundResponse
import io.github.vinicreis.dht.core.model.response.SetResponseOuterClass.SetResponse
import io.github.vinicreis.dht.core.model.response.TransferResponseOuterClass.TransferResponse
import io.github.vinicreis.dht.core.model.response.foundResponse
import io.github.vinicreis.dht.core.model.response.getResponse
import io.github.vinicreis.dht.core.model.response.joinOkResponse
import io.github.vinicreis.dht.core.model.response.joinResponse
import io.github.vinicreis.dht.core.model.response.leaveResponse
import io.github.vinicreis.dht.core.model.response.newNodeResponse
import io.github.vinicreis.dht.core.model.response.nodeGoneResponse
import io.github.vinicreis.dht.core.model.response.notFoundResponse
import io.github.vinicreis.dht.core.model.response.setResponse
import io.github.vinicreis.dht.core.model.response.transferResponse
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineImplBase
import io.github.vinicreis.dht.core.service.domain.DHTClient
import io.github.vinicreis.dht.core.service.domain.DHTService
import io.github.vinicreis.dht.core.service.domain.model.Node
import io.github.vinicreis.dht.core.service.domain.model.hash
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal class DHTServiceGrpcServerImpl(
    override val info: Node,
    private val knownNodes: List<Node>,
    private val logger: Logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.simpleName),
    coroutineContext: CoroutineContext,
) :
    DHTService, DHTServiceGrpc,
    DHTClient by DHTServiceGrpcClientImpl(coroutineContext),
    DHTServiceCoroutineImplBase(coroutineContext)
{
    private val responsibleFor = knownNodes.toMutableSet()
    private val queue = AsyncQueueService(coroutineContext)
    override var next: Node? = null
    override var previous: Node? = null
    override val data: MutableMap<String, ByteArray> = mutableMapOf()
    private val mutableEvents = MutableSharedFlow<DHTService.Event>()

    fun start() {
        queue { join(knownNodes) }
    }

    override suspend fun join(nodes: List<Node>) {
        mutableEvents.emit(DHTService.Event.JoinStarted)

        for (node in nodes) {
            try {
                val result = node.join(info)

                if (result.isSuccess && result.getOrElse { false }) break
            } catch (e: Exception) {
                logger.warning("Failed to join node $node")
            }
        }

        mutableEvents.emit(DHTService.Event.Joined)
    }

    override suspend fun leave() {
        queue { next?.leave(previous) }
        queue { next?.transfer(info, data) }
    }

    override suspend fun get(key: String): ByteArray? {
        return if (isResponsableFor(key)) {
            data[key]
        } else {
            next?.get(info, key)

            return mutableEvents
                .filterIsInstance<DHTService.Event.ResultReceived>()
                .first()
                .let {
                    when (it) {
                        is DHTService.Event.Found -> it.data
                        is DHTService.Event.NotFound -> null
                    }
                }
        }
    }

    override suspend fun set(key: String, value: ByteArray) {
        if (isResponsableFor(key)) {
            data[key] = value
        } else {
            next?.set(info, key, value)
        }
    }

    override suspend fun join(request: JoinRequest): JoinResponse {
        return joinResponse {
            result = let {
                try {
                    val newNode = request.node.asDomain

                    when {
                        responsibleFor.contains(newNode) -> {
                            next = newNode
                            responsibleFor.remove(newNode)
                        }

                        next != null -> queue { newNode.joinOk(info, previous) }
                    }

                    Result.SUCCESS
                } catch (e: Exception) {
                    Result.FAIL
                }
            }
        }
    }

    override suspend fun joinOk(request: JoinOkRequest): JoinOkResponse {
        return joinOkResponse {
            result = request.nextOrNull?.asDomain?.let { node ->
                try {
                    next = node
                    previous = request.takeIf { it.hasPrevious() }?.previous?.asDomain

                    queue { node.joinOk(info, previous) }
                    queue { node.transfer(info, data) }

                    Result.SUCCESS
                } catch (e: Exception) {
                    Result.FAIL
                }
            } ?: Result.FAIL
        }
    }

    override suspend fun newNode(request: NewNodeRequest): NewNodeResponse {
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
        queue {
            val key = request.key.toStringUtf8()

            when {
                isResponsableFor(key) -> data[key] = request.data.content.toByteArray()
                next != null -> next!!.set(request.node.asDomain, key, request.data.content.toByteArray())
            }
        }

        return setResponse { result = ResultOuterClass.Result.SUCCESS }
    }

    override suspend fun found(request: FoundRequest): FoundResponse {
        queue {
            mutableEvents.emit(
                DHTService.Event.Found(request.key.toStringUtf8(), request.data.content.toByteArray())
            )
        }

        return foundResponse { result = Result.SUCCESS }
    }

    override suspend fun notFound(request: NotFoundRequest): NotFoundResponse {
        queue {
            mutableEvents.emit(
                DHTService.Event.NotFound(request.key.toStringUtf8())
            )
        }

        return notFoundResponse { result = Result.SUCCESS }
    }

    override suspend fun transfer(requests: Flow<TransferRequest>): TransferResponse {
        queue {
            requests.collect { entry ->
                data[entry.key.toStringUtf8()] = entry.data.content.toByteArray()
            }
        }

        return transferResponse { result = Result.SUCCESS }
    }

    private fun isResponsableFor(key: String): Boolean {
        return responsibleFor.any { it.id == key.hash }
    }
}

