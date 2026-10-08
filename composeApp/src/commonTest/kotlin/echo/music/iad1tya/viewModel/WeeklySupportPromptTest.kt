package echo.music.iad1tya.viewModel

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WeeklySupportPromptTest {
    @Test
    fun firstLaunchIsDueAndRestartIsNot() {
        assertTrue(isWeeklySupportPromptDue(null, 1000L))
        assertFalse(isWeeklySupportPromptDue(1000L, 1000L))
    }

    @Test
    fun exactlyOneWeekIsDue() {
        val week = 7L * 24 * 60 * 60 * 1000
        assertFalse(isWeeklySupportPromptDue(1000L, 1000L + week - 1))
        assertTrue(isWeeklySupportPromptDue(1000L, 1000L + week))
    }

    @Test
    fun ClockMovingBackDoesNotRepeatPrompt() {
        assertFalse(isWeeklySupportPromptDue(2000L, 1000L))
    }
}
