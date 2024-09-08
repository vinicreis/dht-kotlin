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
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

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
    val debug = args.contains("--debug")
    val arguments = Arguments.build(args) ?: getArguments()
    val consoleHandler = ConsoleHandler().apply {
        level = if (debug) Level.ALL else Level.WARNING
        formatter = object : SimpleFormatter() {
            override fun format(record: java.util.logging.LogRecord): String {
                return "${record.level}: ${record.message}\n"
            }
        }
    }
    val logger = Logger.getLogger("DHTServer").apply {
        addHandler(consoleHandler)
        level = if (debug) Level.ALL else Level.WARNING
    }
    val knownNodes = getHosts()
    val service = DHTServiceGrpc(
        info = Node(
            id = arguments.id,
            address = Address("localhost"),
            port = arguments.port,
        ),
        knownNodes = knownNodes,
        hashStrategy = HashStrategyMD5(knownNodes.size.toLong()),
        coroutineContext = Dispatchers.IO,
        logger = logger,
    )

    service.start()
    println("Press ENTER to leave...")
    readlnOrNull()

    runBlocking {
        service.leave()
        logger.info("Node left gracefully!")
    }
}

private fun input(messsage: String, default: String? = null): String? {
    print("$messsage${default?.let { " [$it]" }.orEmpty()}: ")

    return readlnOrNull()?.takeIf { it.isNotBlank() } ?: default
}

private fun getArguments(): Arguments {
    val hosts = getHosts()

    println("The known nodes for DHT are:")
    hosts.forEachIndexed { i, node -> println("$i - $node") }
    println()

    val nodeIndex = input("Select the index from one of them", "0")?.toInt() ?: return getArguments()

    return hosts.getOrNull(nodeIndex)?.let { Arguments(it.id, it.port) } ?: getArguments()
}

private const val HOSTS_FILE = "hosts.json"

private val hostsFile: InputStream
    get() = {}.javaClass.classLoader.getResourceAsStream(HOSTS_FILE)
        ?: error("Hosts file not found")

private fun getHosts(): List<Node> {
    return Gson().fromJson<Array<Node>?>(hostsFile.reader(), TypeToken.getArray(Node::class.java).type).toList()
}