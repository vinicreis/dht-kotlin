package io.github.vinicreis.dht.core.grpc.infra.strategy

import io.github.vinicreis.dht.core.grpc.domain.strategy.NodeStubStrategy
import io.github.vinicreis.dht.core.service.DHTServiceClientGrpcKt.DHTServiceClientCoroutineStub
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.model.service.Node
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

class NodeStubStrategyGrpc : NodeStubStrategy {
    private val Node.channel: ManagedChannel
        get() = ManagedChannelBuilder
            .forAddress(address.value, port.value)
            .usePlaintext()
            .build()

    override suspend fun <T> Node.withServerStub(block: suspend DHTServiceCoroutineStub.() -> T): T {
        return channel.use { block(DHTServiceCoroutineStub(it)) }
    }

    override suspend fun <T> Node.withClientStub(block: suspend DHTServiceClientCoroutineStub.() -> T): T {
        return channel.use { block(DHTServiceClientCoroutineStub(it)) }
    }

    private suspend fun <T> ManagedChannel.use(block: suspend (ManagedChannel) -> T): T {
        val result = block(this)

        shutdown()

        return result
    }
}
