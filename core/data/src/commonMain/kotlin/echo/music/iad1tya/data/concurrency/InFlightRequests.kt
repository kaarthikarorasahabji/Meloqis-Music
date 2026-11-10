package echo.music.iad1tya.data.concurrency

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Share an active lookup without tying it to one caller's cancellation or caching its failures. */
class InFlightRequests<K, V>(private val scope: CoroutineScope) {
    private val lock = Mutex()
    private val requests = mutableMapOf<K, Deferred<V>>()

    suspend fun get(key: K, resolve: suspend () -> V): V {
        val request = lock.withLock {
            requests[key]?.takeIf { !it.isCompleted } ?: scope.async(start = CoroutineStart.LAZY) { resolve() }.also { created ->
                requests[key] = created
                created.invokeOnCompletion {
                    scope.launch { lock.withLock { if (requests[key] === created) requests.remove(key) } }
                }
            }
        }
        request.start()
        return request.await()
    }
}
