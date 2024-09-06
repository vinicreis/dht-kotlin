package io.github.vinicreis.dht.core.grpc.domain.service

interface DHTClient {
    suspend fun get(key: String): ByteArray?
    suspend fun set(key: String, value: ByteArray)
//    suspend fun remove(key: String): ByteArray?
}