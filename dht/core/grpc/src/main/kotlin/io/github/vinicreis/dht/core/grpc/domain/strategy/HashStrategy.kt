package io.github.vinicreis.dht.core.grpc.domain.strategy

interface HashStrategy {
    operator fun invoke(key: String): Long
}
