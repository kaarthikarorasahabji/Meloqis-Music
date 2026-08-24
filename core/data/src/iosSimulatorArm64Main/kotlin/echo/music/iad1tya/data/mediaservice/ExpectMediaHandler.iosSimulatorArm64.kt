package echo.music.iad1tya.data.mediaservice

actual fun createMediaServiceHandler(
    dataStoreManager: echo.music.iad1tya.domain.manager.DataStoreManager,
    songRepository: echo.music.iad1tya.domain.repository.SongRepository,
    streamRepository: echo.music.iad1tya.domain.repository.StreamRepository,
    localPlaylistRepository: echo.music.iad1tya.domain.repository.LocalPlaylistRepository,
    analyticsRepository: echo.music.iad1tya.domain.repository.AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): echo.music.iad1tya.domain.mediaservice.handler.MediaPlayerHandler {
    TODO("Not yet implemented")
}