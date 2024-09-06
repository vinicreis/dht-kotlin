package io.github.vinicreis.dht.core.grpc.infra.strategy

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.random.Random

@OptIn(ExperimentalEncodingApi::class)
class HashStrategyMD5Tests {
    private val maxValue = Random.nextLong(1_000)
    private val sut = HashStrategyMD5(maxValue)

    @Test
    fun `Should return a positive value lower than max`() {
        repeat(10_000) {
            val key = Base64.encode(Random.nextBytes(64))
            val result = sut(key)

            assertTrue(result >= 0)
            assertTrue(result < maxValue)
        }
    }
}