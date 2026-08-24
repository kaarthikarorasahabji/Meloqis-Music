package echo.music.iad1tya.kotlinytmusicscraper.models

import kotlinx.serialization.Serializable

@Serializable
data class MusicPlaylistShelfRenderer(
    val playlistId: String?,
    val contents: List<MusicShelfRenderer.Content>? = null,
    val collapsedItemCount: Int,
    val continuations: List<Continuation>?,
)