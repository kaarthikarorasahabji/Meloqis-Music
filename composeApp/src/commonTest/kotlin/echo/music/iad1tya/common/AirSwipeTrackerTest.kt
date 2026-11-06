package echo.music.iad1tya.common

import echo.music.iad1tya.ui.component.AirSwipe
import echo.music.iad1tya.ui.component.AirSwipeTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AirSwipeTrackerTest {
    @Test fun recognizesEveryDirection() {
        for ((end, expected) in listOf(
            (0.85f to 0.5f) to AirSwipe.RIGHT, (0.15f to 0.5f) to AirSwipe.LEFT,
            (0.5f to 0.15f) to AirSwipe.UP, (0.5f to 0.85f) to AirSwipe.DOWN,
        )) {
            val tracker = AirSwipeTracker()
            assertNull(tracker.observe(0.5f, 0.5f, 1000))
            assertNull(tracker.observe((0.5f + end.first) / 2, (0.5f + end.second) / 2, 1125))
            assertEquals(expected, tracker.observe(end.first, end.second, 1250))
            assertNull(tracker.observe(0.1f, 0.9f, 1400), "Cooldown prevents duplicate actions")
        }
    }

    @Test fun rejectsJitterDiagonalsAndMissingFrames() {
        val tracker = AirSwipeTracker()
        repeat(10) { assertNull(tracker.observe(0.5f + (it % 2) * 0.03f, 0.5f, 1000L + it * 125)) }
        tracker.reset()
        tracker.observe(0.1f, 0.1f, 1000)
        tracker.observe(0.3f, 0.3f, 1125)
        assertNull(tracker.observe(0.7f, 0.7f, 1250))
        tracker.reset()
        tracker.observe(0.1f, 0.5f, 1000)
        tracker.observe(null, null, 1400)
        assertNull(tracker.observe(0.9f, 0.5f, 1500), "A stale hand position cannot skip a track")
    }
}
