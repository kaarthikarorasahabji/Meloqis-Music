package echo.music.iad1tya.domain.data.model.searchResult

import echo.music.iad1tya.domain.data.type.SearchResultType

data class SearchSuggestions(
    val queries: List<String>,
    val recommendedItems: List<SearchResultType>,
)