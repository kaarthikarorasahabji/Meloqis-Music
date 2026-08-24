package echo.music.iad1tya.kotlinytmusicscraper.pages

import echo.music.iad1tya.kotlinytmusicscraper.models.AlbumItem
import echo.music.iad1tya.kotlinytmusicscraper.models.VideoItem

data class ExplorePage(
    val released: List<AlbumItem>,
    val musicVideo: List<VideoItem>,
)