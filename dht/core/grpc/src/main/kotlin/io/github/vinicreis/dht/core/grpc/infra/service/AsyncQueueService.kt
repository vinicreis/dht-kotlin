package io.github.vinicreis.dht.core.grpc.infra.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.CoroutineContext

class AsyncQueueService(
    coroutineContext: CoroutineContext
) {
    private val coroutineScope = CoroutineScope(coroutineContext)
    private val queue = Channel<suspend () -> Unit>(capacity = Channel.UNLIMITED)
    private val mutex = Mutex()

    operator fun invoke(block: suspend () -> Unit) {
        queue.trySend(block)
        launch()
    }

    private fun launch() {
        coroutineScope.launch {
            for (block in queue) mutex.withLock { block() }
        }
    }
}