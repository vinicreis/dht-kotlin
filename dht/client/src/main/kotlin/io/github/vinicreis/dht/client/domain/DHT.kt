package io.github.vinicreis.dht.client.domain

import io.github.vinicreis.dht.client.infra.DHTClient
import io.github.vinicreis.dht.model.service.Node
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.coroutines.CoroutineContext

fun DHT(
    info: Node,
    servers: List<Node>,
    logger: Logger,
    coroutineContext: CoroutineContext,
) : DHT = DHTClient(
    info = info,
    servers = servers,
    logger = logger,
    coroutineContext = coroutineContext,
).apply {
    start()

    Runtime.getRuntime().addShutdownHook(
        thread(start = false, name = "Shutdown-Thread") { shutdown() }
    )
}

interface DHT {
    suspend fun get(key: String): ByteArray?
    suspend fun set(key: String, value: ByteArray)
    suspend fun remove(key: String): ByteArray?
}
