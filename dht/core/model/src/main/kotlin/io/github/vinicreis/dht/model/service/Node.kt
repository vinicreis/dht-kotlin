package io.github.vinicreis.dht.model.service

data class Node(
    val id: Long,
    val address: Address,
    val port: Port,
) {
    override fun toString(): String = "Node $id ($address:$port)"
}
