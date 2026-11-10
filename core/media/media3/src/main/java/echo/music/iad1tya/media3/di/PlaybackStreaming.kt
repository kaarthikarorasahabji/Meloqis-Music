package echo.music.iad1tya.media3.di

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.DefaultLoadControl

/** Streaming reads populate the playback cache without waiting for a full offline download. */
@UnstableApi
object PlaybackStreaming {
    fun cacheFactory(downloads: Cache, playback: Cache, upstream: DataSource.Factory): CacheDataSource.Factory =
        CacheDataSource.Factory().setCache(downloads).setCacheWriteDataSinkFactory(null)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            .setUpstreamDataSourceFactory(CacheDataSource.Factory().setCache(playback)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR).setUpstreamDataSourceFactory(upstream))

    /** Prime at most 128 KiB; fetch an index tail only after the server confirms byte-range support. */
    fun prime(factory: DataSource.Factory, key: String, url: String, length: Long?, checkActive: () -> Unit = {}) {
        val budget = 64 * 1024L
        val headLength = length?.coerceAtMost(budget) ?: budget
        val ranges = mutableListOf(0L to headLength)
        if (length != null && length > headLength) {
            val tail = (length - budget).coerceAtLeast(headLength)
            if (tail < length) ranges += tail to (length - tail)
        }
        val buffer = ByteArray(16 * 1024)
        var rangesConfirmed = false
        for ((position, count) in ranges) {
            if (position > 0 && !rangesConfirmed) break
            checkActive()
            val source = factory.createDataSource()
            try {
                source.open(DataSpec.Builder().setUri(url).setKey(key).setPosition(position).setLength(count).build())
                if (position == 0L) rangesConfirmed = source.responseHeaders.any { (name, values) ->
                    name.equals("Content-Range", ignoreCase = true) && values.any { it.startsWith("bytes ") }
                }
                var remaining = count
                while (remaining > 0) {
                    checkActive()
                    val read = source.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                    if (read < 0) break
                    remaining -= read
                }
            } finally { source.close() }
        }
    }

    fun loadControl(dataSaver: Boolean = false): DefaultLoadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(if (dataSaver) 5_000 else 10_000, if (dataSaver) 15_000 else 30_000, 500, 1_000)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()
}
