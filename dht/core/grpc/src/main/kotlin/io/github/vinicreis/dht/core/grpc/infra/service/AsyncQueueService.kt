package io.github.vinicreis.dht.core.grpc.infra.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

class AsyncQueueService(
    coroutineContext: CoroutineContext
) {
    private val coroutineScope = CoroutineScope(coroutineContext + SupervisorJob())
    private val queue = Channel<suspend CoroutineScope.() -> Unit>(capacity = Channel.UNLIMITED)
    private val mutex = Mutex()

    operator fun invoke(block: suspend CoroutineScope.() -> Unit) {
        queue.trySend(block)
        launch()
    }

    private fun launch() {
        coroutineScope.launch {
            mutex.withLock {
                ensureActive()
                queue.receive().invoke(this)
            }
        }
    }
}
