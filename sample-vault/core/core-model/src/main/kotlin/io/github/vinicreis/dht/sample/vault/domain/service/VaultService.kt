package io.github.vinicreis.dht.sample.vault.domain.service

import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.sample.vault.infra.service.VaultServiceImpl
import io.github.vinicreis.dht.sample.vault.model.Secret
import java.util.logging.Logger
import kotlin.coroutines.CoroutineContext

fun VaultService(
    servers: List<Node>,
    client: Node,
    logger: Logger,
    coroutineContext: CoroutineContext,
) : VaultService = VaultServiceImpl(
    servers = servers,
    client = client,
    logger = logger,
    coroutineContext = coroutineContext,
)

interface VaultService {
    suspend fun get(key: String): Secret?
    suspend fun set(secret: Secret)
    suspend fun remove(key: String): Secret?
}
