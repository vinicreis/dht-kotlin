package io.github.vinicreis.dht.core.grpc.infra.service

import com.google.protobuf.ByteString
import io.github.vinicreis.dht.core.grpc.domain.strategy.NodeStubStrategy
import io.github.vinicreis.dht.core.grpc.infra.extensions.asByteString
import io.github.vinicreis.dht.core.grpc.infra.mapper.asGrpc
import io.github.vinicreis.dht.core.model.DataTypeOuterClass
import io.github.vinicreis.dht.core.model.ResultOuterClass
import io.github.vinicreis.dht.core.model.request.getRequest
import io.github.vinicreis.dht.core.model.request.joinOkRequest
import io.github.vinicreis.dht.core.model.request.joinRequest
import io.github.vinicreis.dht.core.model.request.leaveRequest
import io.github.vinicreis.dht.core.model.request.newNodeRequest
import io.github.vinicreis.dht.core.model.request.nodeGoneRequest
import io.github.vinicreis.dht.core.model.request.removeRequest
import io.github.vinicreis.dht.core.model.request.setRequest
import io.github.vinicreis.dht.core.model.request.transferRequest
import io.github.vinicreis.dht.core.service.domain.DHTServiceServerStub
import io.github.vinicreis.dht.model.service.Node
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal class DHTServiceServerStubImpl(
    private val coroutineContext: CoroutineContext,
    private val nodeStubStrategy: NodeStubStrategy,
    private val logger: Logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName)
) :
    DHTServiceServerStub,
    NodeStubStrategy by nodeStubStrategy {
    override suspend fun Node.join(info: Node): Result<Boolean> {
        logger.info("Sending JOIN to $this")

        return withContext(coroutineContext) {
            withServerStub {
                join(
                    joinRequest { this.node = info.asGrpc }
                ).let {
                    when (it.result) {
                        null -> Result.success(false)
                        ResultOuterClass.Result.UNRECOGNIZED -> Result.success(false)
                        ResultOuterClass.Result.FAIL -> Result.success(false)
                        ResultOuterClass.Result.EXCEPTION -> Result.success(false)
                        ResultOuterClass.Result.SUCCESS -> Result.success(true)
                    }
                }
            }
        }
    }

    override suspend fun Node.joinOk(next: Node, previous: Node?) {
        logger.info("Sending JOINOK to $this")

        withContext(coroutineContext) {
            withServerStub {
                joinOk(
                    joinOkRequest {
                        this.next = next.asGrpc
                        previous?.also { this.previous = it.asGrpc }
                    }
                )
            }
        }
    }

    override suspend fun Node.leave(info: Node, previous: Node?) {
        logger.info("Sending LEAVE to $this")

        withContext(coroutineContext) {
            withServerStub {
                leave(
                    leaveRequest {
                        node = info.asGrpc
                        previous?.also { this.previous = it.asGrpc }
                    }
                )
            }
        }
    }

    override suspend fun Node.newNode(next: Node) {
        logger.info("Sending NEW_NODE to $this")

        withContext(coroutineContext) {
            withServerStub {
                newNode(
                    newNodeRequest { this.node = next.asGrpc }
                )
            }
        }
    }

    override suspend fun Node.nodeGone(next: Node?) {
        logger.info("Sending NODE_GONE to $this")

        withContext(coroutineContext) {
            withServerStub {
                nodeGone(
                    nodeGoneRequest {
                        next?.asGrpc?.also { this.next = it }
                    }
                )
            }
        }
    }

    override suspend fun Node.set(node: Node, key: String, value: ByteArray) {
        logger.info("Sending SET to $this")

        withContext(coroutineContext) {
            withServerStub {
                set(
                    setRequest {
                        this.node = node.asGrpc
                        this.key = key.asByteString
                        this.data = io.github.vinicreis.dht.core.model.data {
                            type = DataTypeOuterClass.DataType.BYTE
                            content = ByteString.copyFrom(value)
                        }
                    }
                )
            }
        }
    }

    override suspend fun Node.get(node: Node, key: String) {
        logger.info("Sending GET to $this")

        withContext(coroutineContext) {
            withServerStub {
                get(
                    getRequest {
                        this.node = node.asGrpc
                        this.key = key.asByteString
                    }
                )
            }
        }
    }

    override suspend fun Node.remove(node: Node, key: String) {
        logger.info("Sending REMOVE to $this")

        withContext(coroutineContext) {
            withServerStub {
                remove(
                    removeRequest {
                        this.node = node.asGrpc
                        this.key = key.asByteString
                        this.key = key.asByteString
                    }
                )
            }
        }
    }

    override suspend fun Node.transfer(info: Node, data: Map<String, ByteArray>) {
        logger.info("Sending TRANSFER to $this")

        withContext(coroutineContext) {
            withServerStub {
                transfer(
                    // TODO: Refactor to move to an extension property
                    flow {
                        data.iterator().forEach { data ->
                            emit(
                                transferRequest {
                                    this.node = info.asGrpc
                                    key = data.key.asByteString
                                    this.data = io.github.vinicreis.dht.core.model.data {
                                        type = DataTypeOuterClass.DataType.BYTE
                                        content = ByteString.copyFrom(data.value)
                                    }
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}
