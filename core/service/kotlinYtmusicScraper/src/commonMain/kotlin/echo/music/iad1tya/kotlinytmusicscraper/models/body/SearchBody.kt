package echo.music.iad1tya.kotlinytmusicscraper.models.body

import echo.music.iad1tya.kotlinytmusicscraper.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class SearchBody(
    val context: Context,
    val query: String?,
    val params: String?,
)