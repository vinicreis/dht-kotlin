package io.github.vinicreis.dht.core.service.domain.model

data class Node(
    val id: Long,
    val address: Address,
    val port: Port,
)
