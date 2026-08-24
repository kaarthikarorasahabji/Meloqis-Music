package echo.music.iad1tya.domain.data.model.home.chart

import echo.music.iad1tya.domain.data.model.browse.artist.ResultPlaylist

data class ChartItemPlaylist(
    val title: String,
    val playlists: List<ResultPlaylist>,
)