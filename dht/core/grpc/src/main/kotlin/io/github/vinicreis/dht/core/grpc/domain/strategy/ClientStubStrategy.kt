package io.github.vinicreis.dht.core.grpc.domain.strategy

import io.github.vinicreis.dht.core.service.DHTServiceClientGrpcKt.DHTServiceClientCoroutineStub
import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub
import io.github.vinicreis.dht.model.service.Node

interface ClientStubStrategy : StubStrategy<DHTServiceClientCoroutineStub>
