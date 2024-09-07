package io.github.vinicreis.dht.core.service.domain

import io.github.vinicreis.dht.model.service.Node

interface DHTServiceClientStub {
    suspend fun Node.found(key: String, data: ByteArray)
    suspend fun Node.notFound(key: String)
}