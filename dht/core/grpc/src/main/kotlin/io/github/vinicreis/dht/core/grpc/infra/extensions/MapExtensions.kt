package io.github.vinicreis.dht.core.grpc.infra.extensions

import io.github.vinicreis.dht.core.grpc.domain.strategy.HashStrategy
import io.github.vinicreis.dht.model.service.Node

context(HashStrategy)
internal infix fun Map<String, ByteArray>.from(node: Node) = filter { entry -> invoke(entry.key) == node.id }
