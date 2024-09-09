package io.github.vinicreis.dht.sample.vault.app

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.vinicreis.dht.model.service.Address
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.model.service.Port
import io.github.vinicreis.dht.sample.vault.app.model.Option
import io.github.vinicreis.dht.sample.vault.domain.service.VaultService
import io.github.vinicreis.dht.sample.vault.model.Secret
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.net.ConnectException
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.exitProcess

private const val DEFAULT_PORT = 10200

fun main(args: Array<String>) {
    val debug = args.contains("--debug")
    val clientPortValue = input("Enter your port", DEFAULT_PORT.toString())?.toIntOrNull() ?: return
    val clientPort = Port(clientPortValue)
    val logger: Logger = Logger.getLogger("SampleMainLogger").apply {
        if(debug) level = Level.FINEST
    }
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        logger.severe(t.message)
    }

    val service = VaultService(
        servers = getHosts(),
        client = Node(
            id = 1L,
            address = Address("localhost"),
            port = clientPort,
        ),
        logger = logger,
        coroutineContext = Dispatchers.IO + coroutineExceptionHandler,
    )

    while (true) {
        try {
            when(selectOption()) {
                Option.GET -> get(service)
                Option.SET -> set(service)
                Option.REMOVE -> remove(service)
                Option.EXIT -> exitProcess(0)
            }
        } catch (e: ConnectException) {
            println("Could not connect to the server. ${e.message.orEmpty()}")
        }
    }
}

private const val HOSTS_FILE = "hosts.json"

private val hostsFile: InputStream
    get() = {}.javaClass.classLoader.getResourceAsStream(HOSTS_FILE)
        ?: error("Hosts file not found")

private fun getHosts(): List<Node> {
    return Gson().fromJson<Array<Node>?>(hostsFile.reader(), TypeToken.getArray(Node::class.java).type).toList()
}

private fun input(message: String, default: String? = null): String? {
    val defaultText = default?.let { " [$it]" }.orEmpty()

    print("$message$defaultText: ")

    return readlnOrNull()?.takeIf { it.isNotBlank() } ?: default
}

private fun selectOption(): Option {
    println("Select an option:")

    Option.entries.forEachIndexed { index, option ->
        println("${index + 1} - $option")
    }

    return readlnOrNull()?.toIntOrNull()?.let { Option.fromOrdinal(it - 1) } ?: selectOption()
}

private fun get(service: VaultService) {
    runBlocking {
        val key = input("Enter the key") ?: return@runBlocking

        service.get(key)?.also { secret ->
            println("Secret found! ${secret.key} = ${secret.value}")
        } ?: println("No secrey found for key $key")
    }
}

private fun set(service: VaultService) {
    runBlocking {
        val key = input("Enter the key") ?: return@runBlocking
        val secret = input("Enter the secret value") ?: return@runBlocking

        service.set(Secret(key, secret))

        println("Secret $key set!")
    }
}

private fun remove(service: VaultService) {
    runBlocking {
        val key = input("Enter the key to be removed") ?: return@runBlocking

        service.remove(key)?.also {
            println("Secret removed! ${it.key} = ${it.value}")
        } ?: println("No secret found for key $key to be removed!")
    }
}
