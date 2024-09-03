package io.github.vinicreis.dht.app.java

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.vinicreis.dht.core.grpc.domain.service.DHTServiceGrpc
import io.github.vinicreis.dht.core.grpc.infra.strategy.HashStrategyMD5
import io.github.vinicreis.dht.core.service.domain.model.Address
import io.github.vinicreis.dht.core.service.domain.model.Node
import io.github.vinicreis.dht.core.service.domain.model.Port
import kotlinx.coroutines.Dispatchers
import java.io.InputStream

data class Arguments(val id: Long, val port: Int) {
    constructor(args: Array<String>) : this(args[0].toLong(), args[1].toInt())
}

fun main(args: Array<String>) {
    val arguments = Arguments(args)

    DHTServiceGrpc(
        id = arguments.id,
        address = Address("localhost"),
        port = Port(arguments.port),
        knownNodes = getHosts(),
        hashStrategy = HashStrategyMD5(4L),
        coroutineContext = Dispatchers.IO,
    ).run {
        start()
        blockUntilShutdown()
    }
}

private const val HOSTS_FILE = "hosts.json"

private val hostsFile: InputStream
    get() = {}.javaClass.classLoader.getResourceAsStream(HOSTS_FILE)
        ?: error("Hosts file not found")

private fun getHosts(): List<Node> {
    return Gson().fromJson<Array<Node>?>(hostsFile.reader(), TypeToken.getArray(Node::class.java).type).toList()
}
