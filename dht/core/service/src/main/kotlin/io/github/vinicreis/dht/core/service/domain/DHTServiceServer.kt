package io.github.vinicreis.dht.core.service.domain

import io.github.vinicreis.dht.model.service.Node

interface DHTServiceServer {
    val info: Node
    var next: Node?
    var previous: Node?
    val data: MutableMap<Node, MutableMap<String, ByteArray>>

    fun start()
    fun blockUntilShutdown()
    fun shutdown()
    suspend fun join(nodes: List<Node>)
    suspend fun leave()
}
