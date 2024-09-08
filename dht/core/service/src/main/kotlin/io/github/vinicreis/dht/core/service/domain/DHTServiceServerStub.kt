package io.github.vinicreis.dht.core.service.domain

import io.github.vinicreis.dht.model.service.Node

interface DHTServiceServerStub {
    suspend fun Node.join(info: Node): Result<Boolean>
    suspend fun Node.joinOk(next: Node, previous: Node?)
    suspend fun Node.leave(info: Node, previous: Node?)
    suspend fun Node.newNode(next: Node)
    suspend fun Node.nodeGone(next: Node?)
    suspend fun Node.transfer(info: Node, data: Map<Node, Map<String, ByteArray>>)
    suspend fun Node.get(node: Node, key: String)
    suspend fun Node.set(node: Node, key: String, value: ByteArray)
    suspend fun Node.remove(node: Node, key: String)
}