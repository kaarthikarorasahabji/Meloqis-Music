package echo.music.iad1tya.ui.component

import kotlin.test.*

class EdgeLightEnvelopeTest {
    @Test fun responseDoesNotDependOnFrameRate() {
        val fast = EdgeLightEnvelope(1); val slow = EdgeLightEnvelope(1)
        repeat(4) { fast.update(floatArrayOf(1f), 16f) }
        slow.update(floatArrayOf(1f), 64f)
        assertEquals(slow.bands[0], fast.bands[0], .00001f)
    }
    @Test fun silenceIsNotInventedAndOldAudioFades() {
        val lights = EdgeLightEnvelope(1)
        lights.update(floatArrayOf(0f), 100f)
        assertEquals(0f, lights.bands[0])
        lights.update(floatArrayOf(1f), 200f)
        assertTrue(lights.bands[0] > .9f)
        repeat(120) { lights.update(floatArrayOf(0f), 16f) }
        assertTrue(lights.bands[0] < .0001f)
    }
    @Test fun invalidInputCannotPoisonTheRenderer() {
        val lights = EdgeLightEnvelope(5)
        lights.update(floatArrayOf(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, -1f, 2f), Float.NaN, Float.NaN)
        assertTrue(lights.bands.all { it.isFinite() && it in 0f..1f })
        assertTrue(lights.bands.take(4).all { it == 0f })
    }
    @Test fun beatsRiseFasterThanTheyFade() {
        val lights = EdgeLightEnvelope(1)
        lights.update(floatArrayOf(1f), 45f)
        val rise = lights.bands[0]
        lights.update(floatArrayOf(0f), 45f)
        val fall = rise - lights.bands[0]
        assertTrue(rise > fall * 2)
    }
}
