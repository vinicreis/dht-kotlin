package io.github.vinicreis.dht.core.grpc.infra.fixture

import io.github.vinicreis.dht.core.grpc.domain.strategy.ClientStubStrategy
import io.github.vinicreis.dht.core.grpc.domain.strategy.ServerStubStrategy
import io.github.vinicreis.dht.core.service.DHTServiceClientGrpcKt
import io.github.vinicreis.dht.core.service.DHTServiceClientGrpcKt.DHTServiceClientCoroutineStub
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.model.service.Node

internal class ClientStrategyStubSingleMock(
    private val serverMock: DHTServiceClientCoroutineStub,
) : ClientStubStrategy {
    override suspend fun <T> Node.withStub(block: suspend DHTServiceClientCoroutineStub.() -> T): T {
        return block(serverMock)
    }
}

internal class ClientStrategyStubManyMock(
    vararg stubs: DHTServiceClientCoroutineStub
) : ClientStubStrategy {
    private var returned = 0
    private val stubs = stubs.toList()
    private val next: DHTServiceClientCoroutineStub get() = stubs.getOrNull(returned++) ?: error("No more stubs to return")
    override suspend fun <T> Node.withStub(block: suspend DHTServiceClientCoroutineStub.() -> T): T {
        return block(next)
    }
}

internal class ClientStrategyStubLambdaMock(
    private val mock: () -> DHTServiceClientCoroutineStub
) : ClientStubStrategy {
    override suspend fun <T> Node.withStub(block: suspend DHTServiceClientCoroutineStub.() -> T): T {
        return block(mock())
    }
}
