package io.github.vinicreis.dht.core.grpc.infra.fixture

import io.github.vinicreis.dht.core.grpc.domain.strategy.NodeStubStrategy
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.core.service.domain.model.Node

internal class NodeStrategyStubSingleMock(
    private val mock: DHTServiceCoroutineStub
) : NodeStubStrategy {
    override fun Node.Stub(): DHTServiceCoroutineStub = mock
}

internal class NodeStrategyStubManyMock(
    vararg stubs: DHTServiceCoroutineStub
) : NodeStubStrategy {
    private var returned = 0
    private val stubs = stubs.toList()
    private val next: DHTServiceCoroutineStub get() = stubs.getOrNull(returned++) ?: error("No more stubs to return")

    override fun Node.Stub(): DHTServiceCoroutineStub = next
}

internal class NodeStrategyStubLambdaMock(
    private val mock: () -> DHTServiceCoroutineStub
) : NodeStubStrategy {
    override fun Node.Stub(): DHTServiceCoroutineStub = mock()
}
