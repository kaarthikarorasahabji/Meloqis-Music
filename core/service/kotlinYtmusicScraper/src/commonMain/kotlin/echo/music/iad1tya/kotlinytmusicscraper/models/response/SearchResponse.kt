package echo.music.iad1tya.kotlinytmusicscraper.models.response

import echo.music.iad1tya.kotlinytmusicscraper.models.Continuation
import echo.music.iad1tya.kotlinytmusicscraper.models.MusicResponsiveListItemRenderer
import echo.music.iad1tya.kotlinytmusicscraper.models.MusicShelfRenderer
import echo.music.iad1tya.kotlinytmusicscraper.models.Tabs
import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
    val contents: Contents?,
    val continuationContents: ContinuationContents?,
) {
    @Serializable
    data class Contents(
        val tabbedSearchResultsRenderer: Tabs?,
    )

    @Serializable
    data class ContinuationContents(
        val musicShelfContinuation: MusicShelfContinuation,
    ) {
        @Serializable
        data class MusicShelfContinuation(
            val contents: List<Content>,
            val continuations: List<Continuation>?,
        ) {
            @Serializable
            data class Content(
                val musicResponsiveListItemRenderer: MusicResponsiveListItemRenderer?,
                val musicMultiRowListItemRenderer: MusicShelfRenderer.Content.MusicMultiRowListItemRenderer?,
            )
        }
    }
}