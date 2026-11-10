package echo.music.iad1tya.media3.di

import androidx.media3.common.util.UnstableApi
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

    fun loadControl(dataSaver: Boolean = false): DefaultLoadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(if (dataSaver) 5_000 else 10_000, if (dataSaver) 15_000 else 30_000, 500, 1_000)
        .setPrioritizeTimeOverSizeThresholds(true)
        .build()
}
