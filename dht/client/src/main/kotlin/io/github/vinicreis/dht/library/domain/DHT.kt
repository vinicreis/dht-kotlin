package io.github.vinicreis.dht.library.domain

interface DHT {
    suspend fun get(key: String): ByteArray?
    suspend fun set(key: String, value: ByteArray)
    suspend fun remove(key: String): ByteArray?
}
