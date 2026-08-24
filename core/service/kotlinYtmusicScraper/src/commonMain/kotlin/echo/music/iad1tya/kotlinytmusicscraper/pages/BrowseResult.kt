package echo.music.iad1tya.kotlinytmusicscraper.pages

import echo.music.iad1tya.kotlinytmusicscraper.models.YTItem

data class BrowseResult(
    val title: String?,
    val items: List<Item>,
) {
    data class Item(
        val title: String?,
        val items: List<YTItem>,
    )
}