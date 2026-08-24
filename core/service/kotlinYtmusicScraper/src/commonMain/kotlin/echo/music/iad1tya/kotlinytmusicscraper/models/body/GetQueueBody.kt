package echo.music.iad1tya.kotlinytmusicscraper.models.body

import echo.music.iad1tya.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class GetQueueBody(
    val context: Context,
    val videoIds: List<String>?,
    val playlistId: String?,
)