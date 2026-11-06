package echo.music.iad1tya.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

@Composable
expect fun AirControls()

object AirScroll {
    val events = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val playerOpen = MutableStateFlow(false)
}

object AirSession {
    val active = MutableStateFlow(false)
    val ready = MutableStateFlow(false)
    val background = MutableStateFlow(false)
    val visible = MutableStateFlow(true)
    val scrollMode = MutableStateFlow(true)
    val error = MutableStateFlow<String?>(null)
    val handVisible = MutableStateFlow(false)
    val lastGesture = MutableStateFlow<AirSwipe?>(null)
    val controlsPaused = MutableStateFlow(false)
    val stop = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
}

@Composable
fun AirStopButton() {
    val active by AirSession.active.collectAsStateWithLifecycle()
    if (active) androidx.compose.material3.TextButton(onClick = { AirSession.stop.tryEmit(Unit) }) {
        androidx.compose.material3.Text("Stop Air")
    }
}

/** Only the resumed screen responds; gestures never accumulate while navigating. */
@Composable
fun AirScrollEffect(state: LazyListState) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            AirScroll.events.collectLatest { direction ->
                if (!AirScroll.playerOpen.value) state.animateScrollBy(direction * state.layoutInfo.viewportSize.height * 0.65f)
            }
        }
    }
}

enum class AirSwipe { LEFT, RIGHT, UP, DOWN }

/** No detected hand means no command; a palm centroid avoids finger-motion jitter. */
class HandGestureTracker {
    private val swipe = AirSwipeTracker()
    fun reset() = swipe.reset()
    fun observe(landmarks: List<Pair<Float, Float>>, timeMs: Long): AirSwipe? {
        if (landmarks.size != 21 || landmarks.any { !it.first.isFinite() || !it.second.isFinite() }) {
            return swipe.observe(null, null, timeMs)
        }
        val palm = listOf(0, 5, 9, 13, 17).map { landmarks[it] }
        return swipe.observe(palm.sumOf { it.first.toDouble() }.toFloat() / palm.size,
            palm.sumOf { it.second.toDouble() }.toFloat() / palm.size, timeMs)
    }
}

@Composable
fun AirScrollEffect(state: LazyGridState) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(state, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            AirScroll.events.collectLatest { direction ->
                if (!AirScroll.playerOpen.value) state.animateScrollBy(direction * state.layoutInfo.viewportSize.height * 0.65f)
            }
        }
    }
}

/** Small motion tracker. Coordinates are normalized, upright and mirrored for the user. */
class AirSwipeTracker {
    private var startX = 0f
    private var startY = 0f
    private var startedAt = 0L
    private var lastSeen = 0L
    private var samples = 0
    private var cooldownUntil = 0L

    fun reset() { samples = 0; lastSeen = 0L; cooldownUntil = 0L }

    fun observe(x: Float?, y: Float?, timeMs: Long): AirSwipe? {
        if (timeMs < cooldownUntil) return null
        if (x == null || y == null) {
            if (timeMs - lastSeen > 250) samples = 0
            return null
        }
        if (samples == 0 || timeMs - startedAt > 1200 || timeMs - lastSeen > 250) {
            startX = x; startY = y; startedAt = timeMs; samples = 0
        }
        lastSeen = timeMs
        samples++
        if (samples < 3 || timeMs - startedAt < 140) return null
        val dx = x - startX
        val dy = y - startY
        val result = when {
            abs(dx) > 0.27f && abs(dx) > abs(dy) * 1.5f -> if (dx > 0) AirSwipe.RIGHT else AirSwipe.LEFT
            abs(dy) > 0.27f && abs(dy) > abs(dx) * 1.5f -> if (dy > 0) AirSwipe.DOWN else AirSwipe.UP
            else -> return null
        }
        samples = 0
        cooldownUntil = timeMs + 1100
        return result
    }
}
