package io.github.vinicreis.dht.core.grpc.domain.strategy

import io.github.vinicreis.dht.model.service.Node
import io.grpc.kotlin.AbstractCoroutineStub

interface StubStrategy<S : AbstractCoroutineStub<S>> {
    suspend fun <T> Node.withStub(block: suspend S.() -> T): T
}
