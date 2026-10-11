package echo.music.iad1tya.domain.mediaservice.player

import kotlin.test.Test
import kotlin.test.assertEquals

class EqualizerSettingsTest {
    @Test
    fun normalizesMalformedAndOutOfRangeBands() {
        assertEquals(
            listOf(0f, -12f, 3f, 0f, 0f, 0f, 0f, 0f, 0f, 0f),
            normalizeEqualizerBands(listOf(Float.NaN, -30f, 3f, Float.POSITIVE_INFINITY)),
        )
    }

    @Test
    fun keepsPreampFiniteAndCutOnly() {
        assertEquals(-15f, normalizeEqualizerPreampDb(-30f))
        assertEquals(0f, normalizeEqualizerPreampDb(6f))
        assertEquals(0f, normalizeEqualizerPreampDb(Float.NaN))
    }
}
