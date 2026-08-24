package echo.music.iad1tya.domain.data.model.browse.artist

import echo.music.iad1tya.domain.data.model.searchResult.songs.Thumbnail

data class ResultRelated(
    val browseId: String,
    val subscribers: String,
    val thumbnails: List<Thumbnail>,
    val title: String,
)