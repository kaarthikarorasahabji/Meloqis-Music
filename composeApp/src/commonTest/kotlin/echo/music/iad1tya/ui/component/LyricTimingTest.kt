package echo.music.iad1tya.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals

class LyricTimingTest {
    private val lines = listOf(
        TimedLineIndex(2, 1_000),
        TimedLineIndex(0, 2_000),
        TimedLineIndex(4, 3_000),
    )

    @Test
    fun emptyAndBeforeFirstLine() {
        assertEquals(-1, emptyList<TimedLineIndex>().activeIndexAt(0))
        assertEquals(-1, lines.activeIndexAt(-1))
        assertEquals(-1, lines.activeIndexAt(999))
    }

    @Test
    fun exactBoundariesAndStickyLastLine() {
        assertEquals(2, lines.activeIndexAt(1_000))
        assertEquals(2, lines.activeIndexAt(1_999))
        assertEquals(0, lines.activeIndexAt(2_000))
        assertEquals(4, lines.activeIndexAt(Long.MAX_VALUE))
    }

    @Test
    fun seekingBackwardsDoesNotKeepOldLine() {
        assertEquals(4, lines.activeIndexAt(3_500))
        assertEquals(2, lines.activeIndexAt(1_500))
        assertEquals(-1, lines.activeIndexAt(0))
    }

    @Test
    fun duplicateTimestampsChooseLastEntry() {
        val duplicates = listOf(TimedLineIndex(1, 0), TimedLineIndex(3, 0))
        assertEquals(3, duplicates.activeIndexAt(0))
    }
}
