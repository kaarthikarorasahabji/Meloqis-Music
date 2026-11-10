package echo.music.iad1tya

import echo.music.iad1tya.data.concurrency.InFlightRequests
import kotlinx.coroutines.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InFlightRequestsTest {
    @Test fun cancelledPreparationDoesNotCancelForegroundPlaybackLookup() = runBlocking {
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val requests = InFlightRequests<String, Int>(owner)
            val gate = CompletableDeferred<Unit>()
            var calls = 0
            val preparation = async(start = CoroutineStart.UNDISPATCHED) { requests.get("song") { calls++; gate.await(); 7 } }
            val foreground = async(start = CoroutineStart.UNDISPATCHED) { requests.get("song") { error("Duplicated lookup") } }
            preparation.cancelAndJoin()
            gate.complete(Unit)
            assertEquals(7, foreground.await())
            assertEquals(1, calls)
        } finally { owner.cancel() }
    }

    @Test fun failedLookupCanBeRetried() = runBlocking {
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val requests = InFlightRequests<String, Int>(owner)
            assertFailsWith<IllegalStateException> { requests.get("song") { error("Provider unavailable") } }
            assertEquals(9, requests.get("song") { 9 })
        } finally { owner.cancel() }
    }

    @Test fun anotherSongIsNotBlockedByAnUnfinishedLookup() = runBlocking {
        val owner = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val requests = InFlightRequests<String, Int>(owner)
            val gate = CompletableDeferred<Unit>()
            val slow = async(start = CoroutineStart.UNDISPATCHED) { requests.get("slow") { gate.await(); 1 } }
            assertEquals(2, requests.get("new selection") { 2 })
            gate.complete(Unit)
            assertEquals(1, slow.await())
        } finally { owner.cancel() }
    }
}
