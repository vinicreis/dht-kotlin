package io.github.vinicreis.dht.core.grpc.infra.strategy

import io.github.vinicreis.dht.core.grpc.domain.strategy.HashStrategy
import java.security.MessageDigest

class HashStrategyMD5(private val maxValue: Long) : HashStrategy {
    private val digest = MessageDigest.getInstance(ALGO)

    override fun invoke(key: String): Long = digest
        .digest(key.toByteArray())
        .sumOf { it.toUInt() }
        .toLong() % maxValue

    companion object {
        private const val ALGO = "MD5"
    }
}
