package io.github.vinicreis.dht.core.grpc.infra.mapper

import io.github.vinicreis.dht.core.model.node
import io.github.vinicreis.dht.model.service.Address
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.model.service.Port
import io.github.vinicreis.dht.core.model.NodeOuterClass.Node as GrpcNode

internal val GrpcNode.asDomain: Node
    get() = Node(
        id = id,
        address = Address(address),
        port = Port(port),
    )

val Node.asGrpc: GrpcNode
    get() = node {
        id = this@asGrpc.id
        address = this@asGrpc.address.value
        port = this@asGrpc.port.value
    }
