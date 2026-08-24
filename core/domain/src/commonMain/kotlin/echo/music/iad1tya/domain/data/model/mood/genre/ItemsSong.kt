package echo.music.iad1tya.domain.data.model.mood.genre

import echo.music.iad1tya.domain.data.model.searchResult.songs.Artist

data class ItemsSong(
    val title: String,
    val artist: List<Artist>?,
    val videoId: String,
)