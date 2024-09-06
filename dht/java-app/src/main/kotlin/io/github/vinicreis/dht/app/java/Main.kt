package io.github.vinicreis.dht.app.java

import io.github.vinicreis.dht.core.grpc.domain.service.DHTServiceGrpc
import io.github.vinicreis.dht.core.grpc.infra.strategy.HashStrategyMD5
import io.github.vinicreis.dht.model.service.Address
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.model.service.Port
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger
import kotlin.concurrent.thread

data class Arguments(val id: Long, val port: Int) {
    constructor(args: Array<String>) : this(args[0].toLong(), args[1].toInt())
}

fun main(args: Array<String>) {
    val arguments = Arguments(args)
    val logger = Logger.getLogger("DHTServer")
    val service = DHTServiceGrpc(
        info = Node(
            id = arguments.id,
            address = Address("localhost"),
            port = Port(arguments.port),
        ),
        hashStrategy = HashStrategyMD5(4L),
        coroutineContext = Dispatchers.IO,
        logger = logger,
    )

    Runtime.getRuntime().addShutdownHook(
        thread(start = false, name = "Leave-Thread") {
            runBlocking {
                try {
                    service.leave()
                    Logger.getLogger("Main").info("Node left gracefully!")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    )

    service.start()
    println("Press ENTER to leave...")
    readlnOrNull()
}
