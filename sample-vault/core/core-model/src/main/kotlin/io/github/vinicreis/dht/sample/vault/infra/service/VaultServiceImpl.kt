package io.github.vinicreis.dht.sample.vault.infra.service

import io.github.vinicreis.dht.client.domain.DHT
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.sample.vault.domain.service.VaultService
import io.github.vinicreis.dht.sample.vault.model.Secret
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

internal class VaultServiceImpl(
    servers: List<Node>,
    client: Node,
    logger: Logger,
    coroutineContext: CoroutineContext,
) : VaultService {
    private val service = DHT(
        info = client,
        servers = servers,
        logger = logger,
        coroutineContext = coroutineContext,
    )

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
