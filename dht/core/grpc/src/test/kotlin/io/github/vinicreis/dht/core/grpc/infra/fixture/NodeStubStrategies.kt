package io.github.vinicreis.dht.core.grpc.infra.fixture

import io.github.vinicreis.dht.core.grpc.domain.strategy.NodeStubStrategy
import io.github.vinicreis.dht.core.service.DHTServiceClientGrpcKt.DHTServiceClientCoroutineStub
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.model.service.Node
import io.mockk.mockk

//internal class NodeStrategyStubSingleMock(
//    private val serverMock: DHTServiceCoroutineStub,
//    private val clientMock: DHTServiceClientCoroutineStub = mockk(),
//) : NodeStubStrategy {
//    override fun Node.withStub(): DHTServiceCoroutineStub = serverMock
//    override fun Node.ClientStub(): DHTServiceClientCoroutineStub = clientMock
//}
//
//internal class NodeStrategyStubManyMock(
//    vararg stubs: DHTServiceCoroutineStub
//) : NodeStubStrategy {
//    private var returned = 0
//    private val stubs = stubs.toList()
//    private val next: DHTServiceCoroutineStub get() = stubs.getOrNull(returned++) ?: error("No more stubs to return")
//
//    override fun Node.withStub(): DHTServiceCoroutineStub = next
//    override fun Node.ClientStub(): DHTServiceClientCoroutineStub = mockk()
//}
//
//internal class NodeStrategyStubLambdaMock(
//    private val mock: () -> DHTServiceCoroutineStub
//) : NodeStubStrategy {
//    override fun Node.withStub(): DHTServiceCoroutineStub = mock()
//    override fun Node.ClientStub(): DHTServiceClientCoroutineStub = mockk()
//}
