package io.github.vinicreis.dht.server

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.vinicreis.dht.core.grpc.domain.service.DHTServiceGrpc
import io.github.vinicreis.dht.core.grpc.infra.strategy.HashStrategyMD5
import io.github.vinicreis.dht.model.service.Address
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.model.service.Port
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.util.logging.Logger

data class Arguments(val id: Long, val port: Port) {
    companion object {
        fun build(args: Array<String>): Arguments? {
            val id = args.getOrNull(1)?.toLongOrNull()
            val port = args.getOrNull(0)?.toIntOrNull()

            return if (port == null || id == null) {
                return null
            } else {
                Arguments(id, Port(port))
            }
        }
    }
}

fun main(args: Array<String>) {
    val arguments = Arguments.build(args) ?: getArguments()
    val logger = Logger.getLogger("DHTServer")
    val service = DHTServiceGrpc(
        info = Node(
            id = arguments.id,
            address = Address("localhost"),
            port = arguments.port,
        ),
        knownNodes = getHosts(),
        hashStrategy = HashStrategyMD5(4L),
        coroutineContext = Dispatchers.IO,
        logger = logger,
    )

    service.start()
    println("Press ENTER to leave...")
    readlnOrNull()

    runBlocking {
        service.leave()
        Logger.getLogger("Main").info("Node left gracefully!")
    }
}

private fun input(messsage: String, default: String? = null): String? {
    print("$messsage${default?.let { " [$it]" }.orEmpty()}: ")

    return readlnOrNull()?.takeIf { it.isNotBlank() } ?: default
}

private fun getArguments(): Arguments {
    val id = input("Enter the node ID")?.toLong() ?: return getArguments()
    val port = input("Enter the port", "10090")?.toInt() ?: return getArguments()

    return Arguments(id, Port(port))
}

private const val HOSTS_FILE = "hosts.json"

private val hostsFile: InputStream
    get() = {}.javaClass.classLoader.getResourceAsStream(HOSTS_FILE)
        ?: error("Hosts file not found")

private fun getHosts(): List<Node> {
    return Gson().fromJson<Array<Node>?>(hostsFile.reader(), TypeToken.getArray(Node::class.java).type).toList()
}