package io.github.vinicreis.dht.core.grpc.infra.strategy

import io.github.vinicreis.dht.core.grpc.domain.strategy.ClientStubStrategy
import io.github.vinicreis.dht.core.service.DHTServiceClientGrpcKt.DHTServiceClientCoroutineStub
import io.github.vinicreis.dht.model.service.Node

internal class ClientStubStrategyGrpc : ClientStubStrategy, StubStrategyGrpc<DHTServiceClientCoroutineStub>() {
    override suspend fun <T> Node.withStub(block: suspend DHTServiceClientCoroutineStub.() -> T): T {
        return channel.use { block(DHTServiceClientCoroutineStub(it)) }
    }
}
