package echo.music.iad1tya.domain.data.model.home.chart

import echo.music.iad1tya.domain.data.model.searchResult.songs.Thumbnail

data class ItemArtist(
    val browseId: String,
    val rank: String,
    val subscribers: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
    val trend: String,
)