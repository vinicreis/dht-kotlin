package io.github.vinicreis.dht.core.grpc.domain.strategy

import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.core.service.domain.model.Node

interface NodeStubStrategy {
    fun Node.Stub(): DHTServiceCoroutineStub
}
