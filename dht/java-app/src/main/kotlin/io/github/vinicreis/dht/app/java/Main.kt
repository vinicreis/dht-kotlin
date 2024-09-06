package io.github.vinicreis.dht.app.java

import io.github.vinicreis.dht.core.grpc.domain.service.DHTServiceGrpc
import io.github.vinicreis.dht.core.grpc.infra.strategy.HashStrategyMD5
import io.github.vinicreis.dht.model.service.Address
import io.github.vinicreis.dht.model.service.Port
import kotlinx.coroutines.Dispatchers

data class Arguments(val id: Long, val port: Int) {
    constructor(args: Array<String>) : this(args[0].toLong(), args[1].toInt())
}

fun main(args: Array<String>) {
    val arguments = Arguments(args)

    DHTServiceGrpc(
        id = arguments.id,
        address = Address("localhost"),
        port = Port(arguments.port),
        hashStrategy = HashStrategyMD5(4L),
        coroutineContext = Dispatchers.IO,
    ).run {
        start()
        blockUntilShutdown()
    }
}
