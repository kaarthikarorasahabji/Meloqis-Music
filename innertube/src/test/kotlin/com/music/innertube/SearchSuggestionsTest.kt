package com.music.innertube

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchSuggestionsTest {
    @Test
    fun returnsQueriesOrRecommendedItemsForMusicQuery() = runBlocking {
        val result = YouTube.searchSuggestions("arijit")

        assertTrue(
            "Suggestion request failed: ${result.exceptionOrNull()?.message}",
            result.isSuccess,
        )

        val suggestions = result.getOrThrow()
        assertTrue(
            "Suggestion response contained neither queries nor music items",
            suggestions.queries.isNotEmpty() || suggestions.recommendedItems.isNotEmpty(),
        )
    }
}
