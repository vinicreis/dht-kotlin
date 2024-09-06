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

fun main() {
    val logger: Logger = Logger.getLogger("SampleMainLogger")
    val clientPort = input("Enter your port")?.toIntOrNull()?.let { Port(it) } ?: return
    val coroutineExceptionHandler = CoroutineExceptionHandler { _, t ->
        logger.severe(t.message)
    }

    val service = VaultServiceImpl(
        server = Node(
            id = 0L,
            address = Address("localhost"),
            port = Port(10090),
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

    while (true) {
        when(selectOption()) {
            Option.GET -> get(service)
            Option.SET -> set(service)
            Option.REMOVE -> remove(service)
            Option.EXIT -> exitProcess(0)
        }
    }
}

private fun input(message: String): String? {
    print("$message: ")
    return readlnOrNull()
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
