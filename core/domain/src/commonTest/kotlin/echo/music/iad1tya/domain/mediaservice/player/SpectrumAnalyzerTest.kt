package echo.music.iad1tya.domain.mediaservice.player

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.*

class SpectrumAnalyzerTest {
    private fun tone(hz: Double): List<Float> {
        val analyzer = SpectrumAnalyzer()
        var result: List<Float>? = null
        repeat(512) { i -> result = analyzer.push(sin(2 * PI * hz * i / 44100), 44100) ?: result }
        return assertNotNull(result)
    }
    @Test fun silenceDoesNotInventAudio() {
        val analyzer = SpectrumAnalyzer()
        var frame: List<Float>? = null
        repeat(512) { frame = analyzer.push(0.0, 44100) ?: frame }
        assertTrue(assertNotNull(frame).all { it == 0f })
    }
    @Test fun higherToneLightsHigherFrequencyBands() {
        fun strongest(frame: List<Float>) = frame.indices.maxBy { frame[it] }
        assertTrue(strongest(tone(8000.0)) > strongest(tone(1000.0)))
    }
    @Test fun outputIsFiniteAndNormalized() {
        assertTrue(tone(1000.0).all { it.isFinite() && it in 0f..1f })
    }
}
