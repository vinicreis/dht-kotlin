package io.github.vinicreis.dht.sample.vault.app

import io.github.vinicreis.dht.model.service.Address
import io.github.vinicreis.dht.model.service.Node
import io.github.vinicreis.dht.model.service.Port
import io.github.vinicreis.dht.sample.vault.app.model.Option
import io.github.vinicreis.dht.sample.vault.domain.service.VaultService
import io.github.vinicreis.dht.sample.vault.domain.service.VaultServiceImpl
import io.github.vinicreis.dht.sample.vault.model.Secret
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.logging.Logger
import kotlin.concurrent.thread
import kotlin.system.exitProcess

private const val DEFAULT_PORT = 10100

fun main() {
    val logger: Logger = Logger.getLogger("SampleMainLogger")
    val clientPortValue = input("Enter your port", DEFAULT_PORT.toString())?.toIntOrNull() ?: return
    val clientPort = Port(clientPortValue)
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        logger.severe(t.message)
    }

    val service = VaultServiceImpl(
        server = Node(
            id = 2L,
            address = Address("localhost"),
            port = Port(10092),
        ),
        client = Node(
            id = 1L,
            address = Address("localhost"),
            port = clientPort,
        ),
        coroutineContext = Dispatchers.IO + coroutineExceptionHandler,
    ).apply { start() }

    Runtime.getRuntime().addShutdownHook(
        thread(start = false) { service.shutdown() }
    )

    // TODO: Delete after development
    setInitialData(service)

    while (true) {
        when(selectOption()) {
            Option.GET -> get(service)
            Option.SET -> set(service)
            Option.REMOVE -> remove(service)
            Option.EXIT -> exitProcess(0)
        }
    }
}

@Deprecated("Delete after development")
private fun setInitialData(service: VaultService) {
    runBlocking {
        service.set(Secret("1", "ola"))
        service.set(Secret("2", "mundo"))
        service.set(Secret("3", "mundao"))
    }
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

    val option = readlnOrNull()?.toIntOrNull()?.let { Option.entries.getOrNull(it - 1) }
    return option ?: selectOption()
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
