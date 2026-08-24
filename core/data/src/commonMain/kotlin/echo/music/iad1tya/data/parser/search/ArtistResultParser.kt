package echo.music.iad1tya.data.parser.search

import echo.music.iad1tya.domain.data.model.searchResult.artists.ArtistsResult
import echo.music.iad1tya.domain.data.model.searchResult.songs.Thumbnail
import echo.music.iad1tya.kotlinytmusicscraper.models.ArtistItem
import echo.music.iad1tya.kotlinytmusicscraper.pages.SearchResult

internal fun parseSearchArtist(result: SearchResult): ArrayList<ArtistsResult> {
    val artistsResult: ArrayList<ArtistsResult> = arrayListOf()
    result.items.forEach {
        val artist = it as ArtistItem
        artistsResult.add(
            ArtistsResult(
                artist = artist.title,
                browseId = artist.id,
                category = "Artist",
                radioId = artist.radioEndpoint?.playlistId ?: "",
                resultType = "Artist",
                shuffleId = artist.shuffleEndpoint?.playlistId ?: "",
                thumbnails = listOf(Thumbnail(544, Regex("([wh])120").replace(artist.thumbnail, "$1544"), 544)),
            ),
        )
    }
    return artistsResult
}