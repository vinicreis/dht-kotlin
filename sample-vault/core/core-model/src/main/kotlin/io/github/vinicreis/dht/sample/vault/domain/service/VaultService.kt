package io.github.vinicreis.dht.sample.vault.domain.service

import io.github.vinicreis.dht.sample.vault.model.Secret

interface VaultService {
    fun start()
    suspend fun get(key: String): Secret?
    suspend fun set(secret: Secret)
    suspend fun remove(key: String): Secret?
    fun shutdown()
}
