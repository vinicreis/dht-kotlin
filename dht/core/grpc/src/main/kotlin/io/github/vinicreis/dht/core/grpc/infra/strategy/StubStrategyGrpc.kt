package io.github.vinicreis.dht.core.grpc.infra.strategy

import io.github.vinicreis.dht.core.grpc.domain.strategy.StubStrategy
import io.github.vinicreis.dht.model.service.Node
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.kotlin.AbstractCoroutineStub

abstract class StubStrategyGrpc<S : AbstractCoroutineStub<S>> : StubStrategy<S> {
    protected val Node.channel: ManagedChannel
        get() = ManagedChannelBuilder
            .forAddress(address.value, port.value)
            .usePlaintext()
            .build()

    protected suspend fun <T> ManagedChannel.use(block: suspend (ManagedChannel) -> T): T =
        try {
            block(this)
        } finally {
            shutdown()
        }
}
