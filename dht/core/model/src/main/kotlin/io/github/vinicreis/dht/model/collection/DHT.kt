package io.github.vinicreis.dht.model.collection

interface DHT<T> {
    suspend fun set(key: String, value: T)
    suspend fun get(key: String): T?
    suspend fun remove(key: String): T?
}
