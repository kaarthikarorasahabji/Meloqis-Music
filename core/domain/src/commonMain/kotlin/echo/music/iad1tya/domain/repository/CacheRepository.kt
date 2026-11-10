package echo.music.iad1tya.domain.repository

interface CacheRepository {
    suspend fun prepareAudio(videoId: String, url: String, length: Long?) {}

    suspend fun getCacheSize(cacheName: String): Long

    fun clearCache(cacheName: String)

    suspend fun getAllCacheKeys(cacheName: String): List<String>
}