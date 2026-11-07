package echo.music.iad1tya.ui.component

import kotlin.math.exp

/** UI-owned envelope: quick attack and gentle release, independent of rendering rate. */
internal class EdgeLightEnvelope(count: Int = 24) {
    val bands = FloatArray(count)
    fun update(samples: FloatArray, elapsedMillis: Float, gain: Float = 1f) {
        val elapsed = if (elapsedMillis.isFinite()) elapsedMillis.coerceIn(1f, 2000f) else 33f
        val safeGain = if (gain.isFinite()) gain.coerceIn(.5f, 2f) else 1f
        val attack = 1 - exp(-elapsed / 45f)
        val release = 1 - exp(-elapsed / 180f)
        for (index in bands.indices) {
            val raw = samples.getOrElse(index) { 0f }
            val target = if (raw.isFinite()) (raw * safeGain).coerceIn(0f, 1f) else 0f
            bands[index] += (target - bands[index]) * if (target > bands[index]) attack else release
        }
    }
}
