package io.github.vinicreis.dht.core.grpc.infra.service

import com.google.protobuf.ByteString
import io.github.vinicreis.dht.core.grpc.infra.mapper.asGrpc
import io.github.vinicreis.dht.core.model.DataTypeOuterClass
import io.github.vinicreis.dht.core.model.request.foundRequest
import io.github.vinicreis.dht.core.model.request.getRequest
import io.github.vinicreis.dht.core.model.request.joinOkRequest
import io.github.vinicreis.dht.core.model.request.joinRequest
import io.github.vinicreis.dht.core.model.request.newNodeRequest
import io.github.vinicreis.dht.core.model.request.nodeGoneRequest
import io.github.vinicreis.dht.core.model.request.notFoundRequest
import io.github.vinicreis.dht.core.model.request.setRequest
import io.github.vinicreis.dht.core.model.request.transferRequest
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.core.service.domain.DHTClient
import io.github.vinicreis.dht.core.service.domain.model.Node
import io.github.vinicreis.dht.core.grpc.infra.extensions.asByteString
import io.github.vinicreis.dht.core.model.ResultOuterClass
import io.github.vinicreis.dht.core.model.request.leaveRequest
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

internal class DHTServiceGrpcClientImpl(
    private val coroutineContext: CoroutineContext,
) : DHTClient {
    override suspend fun Node.join(info: Node): Result<Boolean> {
        return withContext(coroutineContext) {
            Stub().join(joinRequest { this.node = info.asGrpc }).let {
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

    override suspend fun Node.joinOk(next: Node, previous: Node?) {
        withContext(coroutineContext) {
            Stub().joinOk(
                joinOkRequest {
                    this.next = next.asGrpc
                    previous?.let { this.previous = it.asGrpc }
                }
            )
        }
    }

    override suspend fun Node.leave(previous: Node?) {
        withContext(coroutineContext) {
            Stub().leave(
                leaveRequest {
                    previous?.let { this.previous = it.asGrpc }
                }
            )
        }
    }

    override suspend fun Node.newNode(next: Node) {
        withContext(coroutineContext) {
            Stub().newNode(
                newNodeRequest { this.node = next.asGrpc }
            )
        }
    }

    override suspend fun Node.nodeGone(next: Node?) {
        withContext(coroutineContext) {
            Stub().nodeGone(
                nodeGoneRequest {
                    next?.asGrpc?.also { this.next = it }
                }
            )
        }
    }

    override suspend fun Node.set(node: Node, key: String, value: ByteArray) {
        withContext(coroutineContext) {
            Stub().set(
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

    override suspend fun Node.get(node: Node, key: String) {
        withContext(coroutineContext) {
            Stub().get(
                getRequest {
                    this.node = node.asGrpc
                    this.key = key.asByteString
                }
            )
        }
    }

    override suspend fun Node.found(key: String, data: ByteArray) {
        withContext(coroutineContext) {
            Stub().found(
                foundRequest {
                    this.key = key.asByteString
                    this.data = io.github.vinicreis.dht.core.model.data {
                        type = DataTypeOuterClass.DataType.BYTE
                        content = ByteString.copyFrom(data)
                    }
                }
            )
        }
    }

    override suspend fun Node.notFound(key: String) {
        withContext(coroutineContext) {
            Stub().notFound(
                notFoundRequest {
                    this.key = key.asByteString
                }
            )
        }
    }

    override suspend fun Node.transfer(info: Node, data: Map<String, ByteArray>) {
        withContext(coroutineContext) {
            Stub().transfer(
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

    companion object {
        private fun Node.Stub(): DHTServiceCoroutineStub {
            val channel = ManagedChannelBuilder
                .forAddress(address.value, port.value)
                .usePlaintext()
                .build()

            return DHTServiceCoroutineStub(channel)
        }
    }
}