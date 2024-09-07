package io.github.vinicreis.dht.client.infra

import com.google.protobuf.ByteString
import io.github.vinicreis.dht.core.grpc.domain.strategy.ServerStubStrategy
import io.github.vinicreis.dht.core.grpc.domain.extensions.asByteString
import io.github.vinicreis.dht.core.grpc.infra.mapper.asGrpc
import io.github.vinicreis.dht.core.grpc.infra.strategy.ServerStubStrategyGrpc
import io.github.vinicreis.dht.core.model.DataTypeOuterClass
import io.github.vinicreis.dht.core.model.ResultOuterClass
import io.github.vinicreis.dht.core.model.request.FoundRequestOuterClass.FoundRequest
import io.github.vinicreis.dht.core.model.request.NotFoundRequestOuterClass.NotFoundRequest
import io.github.vinicreis.dht.core.model.request.getRequest
import io.github.vinicreis.dht.core.model.request.removeRequest
import io.github.vinicreis.dht.core.model.request.setRequest
import io.github.vinicreis.dht.core.model.response.FoundResponseOuterClass.FoundResponse
import io.github.vinicreis.dht.core.model.response.NotFoundResponseOuterClass.NotFoundResponse
import io.github.vinicreis.dht.core.model.response.foundResponse
import io.github.vinicreis.dht.core.model.response.notFoundResponse
import io.github.vinicreis.dht.core.service.DHTServiceClientGrpcKt.DHTServiceClientCoroutineImplBase
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.client.domain.DHT
import io.github.vinicreis.dht.model.service.Node
import io.grpc.Grpc
import io.grpc.InsecureServerCredentials
import kotlinx.coroutines.channels.Channel
import java.net.ConnectException
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal class DHTClient(
    private val info: Node,
    private val servers: List<Node>,
    private val logger: Logger,
    coroutineContext: CoroutineContext,
) :
    DHT,
    DHTServiceClientCoroutineImplBase(coroutineContext),
    ServerStubStrategy by ServerStubStrategyGrpc()
{
    private val messages = Channel<ByteArray?>(Channel.UNLIMITED)
    private val server = Grpc.newServerBuilderForPort(info.port.value, InsecureServerCredentials.create())
        .addService(this)
        .build()

    fun start() {
        server.start()
    }

    fun shutdown() {
        server.shutdown()
    }

    override suspend fun get(key: String): ByteArray? {
        return withFirstAvailableServer {
            get(getRequest {
                node = info.asGrpc
                this.key = key.asByteString
            }).takeIf { it.result == ResultOuterClass.Result.SUCCESS }?.let {
                messages.receive()
            }
        }
    }

    override suspend fun set(key: String, value: ByteArray) {
        withFirstAvailableServer {
            set(
                setRequest {
                    node = info.asGrpc
                    this.key = key.asByteString
                    data = io.github.vinicreis.dht.core.model.data {
                        type = DataTypeOuterClass.DataType.BYTE
                        content = ByteString.copyFrom(value)
                    }
                }
            )
        }
    }

    override suspend fun remove(key: String): ByteArray? {
        return withFirstAvailableServer {
            remove(
                removeRequest {
                    node = info.asGrpc
                    this.key = key.asByteString
                }
            )
        }.takeIf { it.result == ResultOuterClass.Result.SUCCESS }?.let {
            messages.receive()
        }
    }

    override suspend fun found(request: FoundRequest): FoundResponse {
        messages.send(request.data.content.toByteArray())

        return foundResponse { result = ResultOuterClass.Result.SUCCESS }
    }

    override suspend fun notFound(request: NotFoundRequest): NotFoundResponse {
        messages.send(null)

        return notFoundResponse { result = ResultOuterClass.Result.SUCCESS }
    }

    private suspend fun <T> withFirstAvailableServer(block: suspend DHTServiceCoroutineStub.() -> T): T {
        for (server in servers) {
            try {
                return server.withStub(block)
            } catch (e: ConnectException) {
                logger.info("Server $server is not available yet")
            }
        }

        throw IllegalStateException("No available DHT server found")
    }
}
