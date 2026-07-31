

package iad1tya.echo.music.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.innertube.YouTube
import com.music.innertube.models.WatchEndpoint
import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.utils.YouTubeUrlParser
import iad1tya.echo.music.constants.HideExplicitKey
import iad1tya.echo.music.constants.HideVideoSongsKey
import iad1tya.echo.music.db.MusicDatabase
import iad1tya.echo.music.db.entities.SearchHistory
import iad1tya.echo.music.utils.dataStore
import iad1tya.echo.music.utils.get
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class OnlineSearchSuggestionViewModel
@Inject
constructor(
    @ApplicationContext val context: Context,
    database: MusicDatabase,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val _viewState = MutableStateFlow(SearchSuggestionViewState())
    val viewState = _viewState.asStateFlow()

    fun updateQuery(value: String) {
        val normalizedValue = value.trim()
        if (query.value.trim() != normalizedValue) {
            _viewState.value = SearchSuggestionViewState(
                query = normalizedValue,
                isLoading = normalizedValue.isNotEmpty(),
            )
        }
        query.value = value
    }

    init {
        viewModelScope.launch {
            query
                .map(String::trim)
                .distinctUntilChanged()
                .debounce { value ->
                    if (value.isEmpty() || YouTubeUrlParser.isYouTubeUrl(value)) 0L else 220L
                }
                .flatMapLatest { query ->
                    if (query.isEmpty()) {
                        database.searchHistory().map { history ->
                            SearchSuggestionViewState(
                                history = history.take(8),
                            )
                        }
                    } else {
                        flow {
                            emit(
                                SearchSuggestionViewState(
                                    query = query,
                                    isLoading = true,
                                )
                            )

                            val parsedUrl = YouTubeUrlParser.parse(query)
                            val parsedItem = if (parsedUrl != null) fetchParsedUrlItem(parsedUrl) else null
                            val suggestionResult =
                                if (parsedUrl == null) YouTube.searchSuggestions(query) else null
                            val result = suggestionResult?.getOrNull()
                            val hideExplicit = context.dataStore.get(HideExplicitKey, false)
                            val hideVideoSongs = context.dataStore.get(HideVideoSongsKey, false)
                            val recommendedItems = result
                                ?.recommendedItems
                                ?.distinctBy { it.id }
                                ?.filter { it.id != parsedItem?.id }
                                ?.filterExplicit(hideExplicit)
                                ?.filterVideoSongs(hideVideoSongs)
                                .orEmpty()
                            val remoteSuggestions = result
                                ?.queries
                                ?.filterNot { it.equals(query, ignoreCase = true) }
                                ?.take(8)
                                .orEmpty()

                            emitAll(
                                database.searchHistory(query).map { matchingHistory ->
                                    val history = matchingHistory.take(4)
                                    val itemTitleSuggestions =
                                        if (remoteSuggestions.isEmpty()) {
                                            recommendedItems
                                                .map { it.title }
                                                .filterNot { it.equals(query, ignoreCase = true) }
                                                .distinctBy { it.lowercase() }
                                                .take(6)
                                        } else {
                                            emptyList()
                                        }

                                    SearchSuggestionViewState(
                                        query = query,
                                        history = history,
                                        suggestions = (remoteSuggestions + itemTitleSuggestions)
                                            .filter { suggestionQuery ->
                                                history.none {
                                                    it.query.equals(suggestionQuery, ignoreCase = true)
                                                }
                                            }
                                            .distinctBy { it.lowercase() },
                                        items = listOfNotNull(parsedItem) + recommendedItems,
                                        isFromLink = parsedUrl != null,
                                        suggestionsUnavailable =
                                            parsedUrl == null && suggestionResult?.isFailure == true,
                                    )
                                }
                            )
                        }
                    }
                }.collect {
                    _viewState.value = it
                }
        }
    }

    private suspend fun fetchParsedUrlItem(parsedUrl: YouTubeUrlParser.ParsedUrl): YTItem? {
        return try {
            when (parsedUrl) {
                is YouTubeUrlParser.ParsedUrl.Video -> {
                    YouTube.queue(listOf(parsedUrl.id)).getOrNull()?.firstOrNull()
                }

                is YouTubeUrlParser.ParsedUrl.Artist -> {
                    YouTube.artist(parsedUrl.id).getOrNull()?.artist
                }
            }
        } catch (e: Exception) {
            null
        }
    }
}

data class SearchSuggestionViewState(
    val query: String = "",
    val history: List<SearchHistory> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val items: List<YTItem> = emptyList(),
    val isFromLink: Boolean = false,
    val isLoading: Boolean = false,
    val suggestionsUnavailable: Boolean = false,
)
