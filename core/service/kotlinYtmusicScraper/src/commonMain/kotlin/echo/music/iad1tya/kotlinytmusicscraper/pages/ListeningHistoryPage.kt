package echo.music.iad1tya.kotlinytmusicscraper.pages

import echo.music.iad1tya.kotlinytmusicscraper.models.SongItem
import echo.music.iad1tya.kotlinytmusicscraper.models.response.BrowseResponse

/** History has one song shelf per date. The server sends the latest shelf first. */
internal fun BrowseResponse.listeningHistory(limit: Int): List<SongItem> {
    val body = checkNotNull(contents) { "YouTube Music returned no history page" }
    val sections = body.singleColumnBrowseResultsRenderer?.tabs
        ?.firstNotNullOfOrNull { it.tabRenderer?.content?.sectionListRenderer?.contents }
        ?: body.twoColumnBrowseResultsRenderer?.secondaryContents?.sectionListRenderer?.contents
        ?: body.sectionListRenderer?.contents
        ?: error("YouTube Music returned an unsupported history page")

    return sections.asSequence()
        .flatMap { (it.musicShelfRenderer?.contents ?: it.musicPlaylistShelfRenderer?.contents).orEmpty().asSequence() }
        .mapNotNull { PlaylistPage.fromMusicResponsiveListItemRenderer(it.musicResponsiveListItemRenderer) }
        .filter { it.id.isNotBlank() && it.title.isNotBlank() }
        .distinctBy { it.id }
        .take(limit)
        .toList()
}
