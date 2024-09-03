package io.github.vinicreis.dht.core.service.domain

import io.github.vinicreis.dht.core.service.domain.model.Node
import kotlinx.coroutines.flow.Flow

interface DHTService : DHTServer, DHTClient {
    sealed interface Event {
        data object JoinStarted : Event
        data object Joined : Event
        data object Transferring : Event
        data object Ready : Event
        data object WaitingResult: Event
        sealed interface ResultReceived: Event
        data class Found(val key: String, val data: ByteArray): ResultReceived
        data class NotFound(val key: String): ResultReceived
        data object Leaving : Event
    }

    val events: Flow<Event>
}

interface DHTServer {
    val info: Node
    var next: Node?
    var previous: Node?
    val data: MutableMap<String, ByteArray>

    fun start()
    fun blockUntilShutdown()
    fun shutdown()
    suspend fun join(nodes: List<Node>)
    suspend fun leave()
    suspend fun get(key: String): ByteArray?
    suspend fun set(key: String, value: ByteArray)
}

interface DHTClient {
    suspend fun Node.join(info: Node): Result<Boolean>
    suspend fun Node.joinOk(next: Node, previous: Node?)
    suspend fun Node.leave(previous: Node?)
    suspend fun Node.newNode(next: Node)
    suspend fun Node.nodeGone(next: Node?)
    suspend fun Node.found(key: String, data: ByteArray)
    suspend fun Node.notFound(key: String)
    suspend fun Node.transfer(info: Node, data: Map<String, ByteArray>)
    suspend fun Node.get(node: Node, key: String)
    suspend fun Node.set(node: Node, key: String, value: ByteArray)
}
