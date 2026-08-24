package echo.music.iad1tya.data.mediaservice

import echo.music.iad1tya.domain.manager.DataStoreManager
import echo.music.iad1tya.domain.mediaservice.handler.MediaPlayerHandler
import echo.music.iad1tya.domain.repository.AnalyticsRepository
import echo.music.iad1tya.domain.repository.LocalPlaylistRepository
import echo.music.iad1tya.domain.repository.SongRepository
import echo.music.iad1tya.domain.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope

expect fun createMediaServiceHandler(
    dataStoreManager: DataStoreManager,
    songRepository: SongRepository,
    streamRepository: StreamRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: CoroutineScope,
): MediaPlayerHandler