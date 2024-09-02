package io.github.vinicreis.dht.app.java

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.vinicreis.dht.core.service.domain.model.Node
import java.io.InputStream

fun main(args: Array<String>) {
    val hosts = getHosts()

    hosts.forEach { println(it) }
}

private const val HOSTS_FILE = "hosts.json"

private val hostsFile: InputStream
    get() = {}.javaClass.classLoader.getResourceAsStream(HOSTS_FILE)
        ?: error("Hosts file not found")

private fun getHosts(): Array<Node> {
    return Gson().fromJson(hostsFile.reader(), TypeToken.getArray(Node::class.java).type)
}
