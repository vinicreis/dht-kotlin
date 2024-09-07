package io.github.vinicreis.dht.core.grpc.domain.strategy

import io.github.vinicreis.dht.core.service.DHTServiceGrpcKt.DHTServiceCoroutineStub

interface ServerStubStrategy : StubStrategy<DHTServiceCoroutineStub>
