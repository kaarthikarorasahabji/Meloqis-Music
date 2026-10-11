package echo.music.iad1tya.domain.mediaservice.player

/** Shared limits used when restoring persisted equalizer values on every player backend. */
const val EQUALIZER_BAND_COUNT = 10
const val EQUALIZER_MIN_GAIN_DB = -12f
const val EQUALIZER_MAX_GAIN_DB = 12f
const val EQUALIZER_MIN_PREAMP_DB = -15f

/** Return a finite, bounded ten-band curve so stale or malformed preferences cannot reach a DSP. */
fun normalizeEqualizerBands(bandsDb: List<Float>): List<Float> =
    List(EQUALIZER_BAND_COUNT) { index ->
        bandsDb.getOrNull(index)
            ?.takeIf(Float::isFinite)
            ?.coerceIn(EQUALIZER_MIN_GAIN_DB, EQUALIZER_MAX_GAIN_DB)
            ?: 0f
    }

/** The preamp is a cut-only headroom control. */
fun normalizeEqualizerPreampDb(preampDb: Float): Float =
    preampDb.takeIf(Float::isFinite)?.coerceIn(EQUALIZER_MIN_PREAMP_DB, 0f) ?: 0f
