package io.github.vinicreis.dht.core.grpc.infra.strategy

import io.github.vinicreis.dht.core.grpc.domain.strategy.NodeStubStrategy
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.core.service.domain.model.Node
import io.grpc.ManagedChannelBuilder

class NodeStubStrategyGrpc : NodeStubStrategy {
    override fun Node.Stub(): DHTServiceCoroutineStub {
        val channel = ManagedChannelBuilder
            .forAddress(address.value, port.value)
            .usePlaintext()
            .build()

        return DHTServiceCoroutineStub(channel)
    }
}
