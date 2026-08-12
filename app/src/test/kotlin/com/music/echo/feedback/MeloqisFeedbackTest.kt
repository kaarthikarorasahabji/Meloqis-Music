package iad1tya.echo.music.feedback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MeloqisFeedbackTest {
    @Test
    fun `prompt becomes eligible after two days`() {
        val firstUse = 1_000L
        assertFalse(MeloqisFeedback.isEligible(firstUse, false, 0L, firstUse + MeloqisFeedback.PROMPT_DELAY_MS - 1))
        assertTrue(MeloqisFeedback.isEligible(firstUse, false, 0L, firstUse + MeloqisFeedback.PROMPT_DELAY_MS))
    }

    @Test
    fun `completed and snoozed prompts stay hidden`() {
        val now = 10L * MeloqisFeedback.PROMPT_DELAY_MS
        assertFalse(MeloqisFeedback.isEligible(0L, true, 0L, now))
        assertFalse(MeloqisFeedback.isEligible(0L, false, now + 1L, now))
    }

    @Test
    fun `missing first use timestamp never prompts`() {
        assertFalse(MeloqisFeedback.isEligible(null, false, 0L, Long.MAX_VALUE))
    }
}
