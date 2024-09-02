package io.github.vinicreis.dht.core.service.domain.model

import kotlin.math.pow
import kotlin.random.Random

private val MAX_HASH get() = (2.0.pow(2)).toLong()

val ByteArray.hash: Long get() = Random.nextLong(MAX_HASH)

val String.hash: Long get() = toByteArray().hash
