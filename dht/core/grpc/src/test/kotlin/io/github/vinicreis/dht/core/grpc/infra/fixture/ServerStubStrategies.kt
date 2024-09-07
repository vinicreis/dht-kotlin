package io.github.vinicreis.dht.core.grpc.infra.fixture

import io.github.vinicreis.dht.core.grpc.domain.strategy.ServerStubStrategy
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.model.service.Node

internal class ServerStrategyStubSingleMock(
    private val serverMock: DHTServiceCoroutineStub,
) : ServerStubStrategy {
    override suspend fun <T> Node.withStub(block: suspend DHTServiceCoroutineStub.() -> T): T {
        return block(serverMock)
    }
}

internal class ServerStrategyStubManyMock(
    vararg stubs: DHTServiceCoroutineStub,
    val then: DHTServiceCoroutineStub? = null,
) : ServerStubStrategy {
    private var returned = 0
    private val stubs = stubs.toList()
    private val next: DHTServiceCoroutineStub get() = stubs.getOrNull(returned++) ?: then ?: error("No more stubs to return")
    override suspend fun <T> Node.withStub(block: suspend DHTServiceCoroutineStub.() -> T): T {
        return block(next)
    }
}

internal class ServerStrategyStubLambdaMock(
    private val mock: () -> DHTServiceCoroutineStub
) : ServerStubStrategy {
    override suspend fun <T> Node.withStub(block: suspend DHTServiceCoroutineStub.() -> T): T {
        return block(mock())
    }
}
