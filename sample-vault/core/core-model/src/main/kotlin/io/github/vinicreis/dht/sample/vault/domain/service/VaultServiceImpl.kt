package io.github.vinicreis.dht.sample.vault.domain.service

import io.github.vinicreis.dht.library.infra.DHTClient
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.sample.vault.model.Secret
import kotlin.coroutines.CoroutineContext

class VaultServiceImpl(
    server: Node,
    client: Node,
    coroutineContext: CoroutineContext,
) : VaultService {
    private val service = DHTClient(
        info = client,
        dhtServer = server,
        coroutineContext = coroutineContext,
    )

    override fun start() {
        service.start()
    }

    override fun shutdown() {
        service.shutdown()
    }

    override suspend fun get(key: String): Secret? {
        return service.get(key)?.let { bytes ->
            Secret(key = key, value = String(bytes))
        }
    }

    override suspend fun set(secret: Secret) {
        service.set(secret.key, secret.value.toByteArray())
    }

    override suspend fun remove(key: String): Secret? {
        return service.remove(key)?.let { bytes ->
            Secret(key = key, value = String(bytes))
        }
    }
}
