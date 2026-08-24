package echo.music.iad1tya.domain.data.model.searchResult.songs

import kotlinx.serialization.Serializable

@Serializable
data class Artist(
    val id: String?,
    val name: String,
)