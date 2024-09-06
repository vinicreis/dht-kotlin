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
import io.github.vinicreis.dht.core.model.request.setRequest
import io.github.vinicreis.dht.core.model.request.transferRequest
import io.github.vinicreis.dht.core.service.domain.DHTServiceServerStub
import io.github.vinicreis.dht.model.service.Node
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal class DHTServiceServerStubImpl(
    private val coroutineContext: CoroutineContext,
    private val nodeStubStrategy: NodeStubStrategy,
    private val logger: Logger = Logger.getLogger(DHTServiceGrpcServerImpl::class.java.simpleName)
) :
    DHTServiceServerStub,
    NodeStubStrategy by nodeStubStrategy
{
    override suspend fun Node.join(info: Node): Result<Boolean> {
        return withContext(coroutineContext) {
            logger.info("Sending JOIN to $id")

            withServerStub {
                join(joinRequest { this.node = info.asGrpc }).let {
                    when(it.result) {
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
        withContext(coroutineContext) {
            logger.info("Sending JOINOK to $id")

            withServerStub {
                joinOk(
                    joinOkRequest {
                        this.next = next.asGrpc
                        previous?.let { this.previous = it.asGrpc }
                    }
                )
            }
        }
    }

    override suspend fun Node.leave(previous: Node?) {
        withContext(coroutineContext) {
            logger.info("Sending LEAVE to $id")

            withServerStub {
                leave(
                    leaveRequest {
                        previous?.let { this.previous = it.asGrpc }
                    }
                )
            }
        }
    }

    override suspend fun Node.newNode(next: Node) {
        withContext(coroutineContext) {
            logger.info("Sending NEW_NODE to $id")

            withServerStub {
                newNode(
                    newNodeRequest { this.node = next.asGrpc }
                )
            }
        }
    }

    override suspend fun Node.nodeGone(next: Node?) {
        withContext(coroutineContext) {
            logger.info("Sending NODE_GONE to $id")

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
        withContext(coroutineContext) {
            logger.info("Sending SET to $id")

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
        withContext(coroutineContext) {
            logger.info("Sending GET to $id")

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

    override suspend fun Node.transfer(info: Node, data: Map<String, ByteArray>) {
        withContext(coroutineContext) {
            logger.info("Sending TRANSFER to $id")

            withServerStub {
                transfer(
                    channelFlow {
                        data.iterator().forEach { data ->
                            send(
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

                        close()
                    }
                )
            }
        }
    }
}
