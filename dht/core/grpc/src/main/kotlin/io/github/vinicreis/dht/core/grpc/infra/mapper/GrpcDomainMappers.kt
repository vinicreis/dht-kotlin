package io.github.vinicreis.dht.core.grpc.infra.mapper

import io.github.vinicreis.dht.core.model.node
import io.github.vinicreis.dht.core.service.domain.model.Address
import io.github.vinicreis.dht.core.service.domain.model.Node
import io.github.vinicreis.dht.core.service.domain.model.Port
import io.github.vinicreis.dht.core.model.NodeOuterClass.Node as GrpcNode

internal val GrpcNode.asDomain: Node
    get() = Node(
        id = id,
        address = Address(address),
        port = Port(port),
    )

internal val Node.asGrpc: GrpcNode
    get() = node {
        id = this@asGrpc.id
        address = this@asGrpc.address.value
        port = this@asGrpc.port.value
    }
