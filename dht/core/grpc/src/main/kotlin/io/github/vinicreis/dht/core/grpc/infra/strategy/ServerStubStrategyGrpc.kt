package io.github.vinicreis.dht.core.grpc.infra.strategy

import io.github.vinicreis.dht.core.grpc.domain.strategy.ServerStubStrategy
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.model.service.Node

class ServerStubStrategyGrpc : ServerStubStrategy, StubStrategyGrpc<DHTServiceCoroutineStub>() {
    override suspend fun <T> Node.withStub(block: suspend DHTServiceCoroutineStub.() -> T): T {
        return channel.use { block(DHTServiceCoroutineStub(it)) }
    }
}
