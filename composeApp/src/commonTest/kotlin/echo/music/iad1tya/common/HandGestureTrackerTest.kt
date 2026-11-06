package echo.music.iad1tya.common

import echo.music.iad1tya.ui.component.HandGestureTracker
import echo.music.iad1tya.ui.component.AirSwipe
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HandGestureTrackerTest {
    @Test fun requiresDetectedHandBeforeTriggering() {
        val tracker = HandGestureTracker()
        repeat(20) { assertNull(tracker.observe(emptyList(), it * 125L)) }
        assertNull(tracker.observe(List(5) { 0.9f to 0.1f }, 2600))
        assertNull(tracker.observe(List(21) { Float.NaN to 0.1f }, 2700))
        assertNull(tracker.observe(List(21) { 0.2f to 0.5f }, 3000))
        assertNull(tracker.observe(List(21) { 0.35f to 0.5f }, 3125))
        assertEquals(AirSwipe.RIGHT, tracker.observe(List(21) { 0.55f to 0.5f }, 3250))
    }
    @Test fun fingerMovementWithoutPalmMovementDoesNotTrigger() {
        val tracker = HandGestureTracker()
        repeat(10) { index ->
            val hand = List(21) { point ->
                if (point in listOf(0, 5, 9, 13, 17)) 0.5f to 0.5f else (index / 10f) to 0.2f
            }
            assertNull(tracker.observe(hand, 1000 + index * 125L))
        }
    }
}
