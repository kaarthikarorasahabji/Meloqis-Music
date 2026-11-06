package echo.music.iad1tya.domain.mediaservice.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.*

data class SpectrumFrame(val bands: List<Float>, val capturedAt: Long)

/** Only app-decoded PCM is inspected, and only while a visible display subscribes. */
object AudioSpectrum {
    private val consumers = MutableStateFlow(0)
    private val current = MutableStateFlow(SpectrumFrame(List(24) { 0f }, 0))
    val frames = current.asStateFlow()
    val enabled get() = consumers.value > 0
    fun acquire() { consumers.update { it + 1 } }
    fun release() { consumers.update { (it - 1).coerceAtLeast(0) } }
    fun publish(bands: List<Float>, at: Long) { current.value = SpectrumFrame(bands, at) }
}

/** Reusable 512-point FFT with Hann window and logarithmic frequency buckets. */
class SpectrumAnalyzer {
    private val samples = DoubleArray(512)
    private val real = DoubleArray(512)
    private val imaginary = DoubleArray(512)
    private var count = 0
    fun push(sample: Double, sampleRate: Int): List<Float>? {
        samples[count++] = sample
        if (count < samples.size) return null
        count = 0
        for (i in samples.indices) {
            real[i] = samples[i] * (0.5 - 0.5 * cos(2 * PI * i / 511))
            imaginary[i] = 0.0
        }
        var j = 0
        for (i in 1 until 512) {
            var bit = 256
            while (j and bit != 0) { j = j xor bit; bit = bit shr 1 }
            j = j xor bit
            if (i < j) { val r = real[i]; real[i] = real[j]; real[j] = r }
        }
        var length = 2
        while (length <= 512) {
            for (start in 0 until 512 step length) {
                for (k in 0 until length / 2) {
                    val angle = -2 * PI * k / length
                    val a = start + k
                    val b = a + length / 2
                    val r = real[b] * cos(angle) - imaginary[b] * sin(angle)
                    val im = real[b] * sin(angle) + imaginary[b] * cos(angle)
                    real[b] = real[a] - r; imaginary[b] = imaginary[a] - im
                    real[a] += r; imaginary[a] += im
                }
            }
            length *= 2
        }
        return List(24) { band ->
            val low = (60.0 * (16000.0 / 60).pow(band / 24.0) * 512 / sampleRate).toInt().coerceIn(1, 255)
            val high = (60.0 * (16000.0 / 60).pow((band + 1) / 24.0) * 512 / sampleRate).toInt().coerceIn(low + 1, 256)
            var peak = 0.0
            for (bin in low until high) peak = max(peak, hypot(real[bin], imaginary[bin]) / 128)
            ((20 * log10(max(peak, 0.0001)) + 65) / 65).toFloat().coerceIn(0f, 1f)
        }
    }
}
