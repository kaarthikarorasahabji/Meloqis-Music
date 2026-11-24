package echo.music.iad1tya.viewModel

import androidx.lifecycle.viewModelScope
import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.domain.data.model.home.HomeItem
import echo.music.iad1tya.domain.manager.DataStoreManager
import echo.music.iad1tya.domain.repository.HomeRepository
import echo.music.iad1tya.domain.repository.SongRepository
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.viewModel.base.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.song
import echomusic.composeapp.generated.resources.view_count

sealed interface YouTubeAccountContent<out T> {
    data object SignedOut : YouTubeAccountContent<Nothing>
    data object Loading : YouTubeAccountContent<Nothing>
    data object Unavailable : YouTubeAccountContent<Nothing>
    data class Loaded<T>(val items: List<T>) : YouTubeAccountContent<T>
}

class HomeViewModel(
    private val dataStoreManager: DataStoreManager,
    private val homeRepository: HomeRepository,
    private val songRepository: SongRepository,
) : BaseViewModel() {
    private val historyRefresh = MutableStateFlow(0)
    private val playbackMoodContext = MutableStateFlow<Pair<PlaybackMood, Set<String>>?>(null)
    val explicitContentEnabled = dataStoreManager.explicitContentEnabled

    internal fun observePlaybackMood(videoId: String): Flow<PlaybackMood?> =
        combine(dataStoreManager.getString("listening_mood_$videoId"), playbackMoodContext) { savedMood, context ->
            PlaybackMood.entries.firstOrNull { it.name == savedMood }
                ?: context?.first?.takeIf { videoId in context.second }
        }.distinctUntilChanged()
            .catch { emit(null) }

    internal fun rememberPlaybackMood(tracks: List<Track>, endpoint: String?) {
        playbackMoodContext.value = PlaybackMood.entries.firstOrNull { it.endpoint == endpoint }
            ?.let { it to tracks.map(Track::videoId).toSet() }
    }

    internal fun setPlaybackMood(videoId: String, mood: PlaybackMood?) {
        viewModelScope.launch {
            try {
                dataStoreManager.putString("listening_mood_$videoId", mood?.name.orEmpty())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Logger.e("HomeViewModel", "Saving song mood failed: ${error.message}")
            }
        }
    }

    private val accountRequests = combine(dataStoreManager.cookie, dataStoreManager.pageId, historyRefresh) { cookie, pageId, _ ->
        cookie to pageId
    }

    val youtubeListeningHistory = accountContent(homeRepository::getYouTubeListeningHistory)
    val youtubePersonalMixes = accountContent(homeRepository::getYouTubePersonalMixes)
    val youtubeConnected = dataStoreManager.cookie.map(String::isNotBlank).distinctUntilChanged()
    val spotifyConnected = dataStoreManager.spdc.map(String::isNotBlank).distinctUntilChanged()

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun <T> accountContent(load: suspend (String, String?) -> List<T>): StateFlow<YouTubeAccountContent<T>> =
        accountRequests.flatMapLatest { (cookie, pageId) ->
            if (cookie.isBlank()) return@flatMapLatest flowOf(YouTubeAccountContent.SignedOut)
            flow<YouTubeAccountContent<T>> {
                emit(YouTubeAccountContent.Loading)
                val result = try {
                    withTimeoutOrNull(10_000) { load(cookie, pageId.takeIf { it.isNotBlank() }) }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    null
                }
                emit(result?.let { YouTubeAccountContent.Loaded(it) } ?: YouTubeAccountContent.Unavailable)
            }
        }.catch { emit(YouTubeAccountContent.Unavailable) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), YouTubeAccountContent.Loading)

    fun refreshYouTubeHome() {
        historyRefresh.update { it + 1 }
    }

    val recentlyPlayed = songRepository.observeRecentlyPlayedSongs(limit = 12)
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(replayExpirationMillis = 0), emptyList())

    suspend fun claimWeeklySupportPrompt(): Boolean {
        val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
        val key = "weekly_support_prompt_at"
        val lastShown = dataStoreManager.getString(key).first()?.toLongOrNull()
        if (!isWeeklySupportPromptDue(lastShown, now)) return false
        dataStoreManager.putString(key, now.toString())
        return true
    }

    private val _homeItemList: MutableStateFlow<List<HomeItem>> =
        MutableStateFlow(arrayListOf())
    val homeItemList: StateFlow<List<HomeItem>> = _homeItemList

    private val _homeListState = MutableStateFlow(ListState.PAGINATION_EXHAUST)
    val homeListState: StateFlow<ListState> = _homeListState
    private val _continuation = MutableStateFlow<String?>(null)
    val continuation: StateFlow<String?> = _continuation
    private val _accountInfo = MutableStateFlow<Pair<String?, String?>?>(null)
    val accountInfo: StateFlow<Pair<String?, String?>?> = _accountInfo
    private val _params = MutableStateFlow<String?>(null)
    val params: StateFlow<String?> = _params
    val loading = MutableStateFlow(false)
    private var homeJob: Job? = null
    private var paginationJob: Job? = null

    init {
        viewModelScope.launch {
            combine(
                dataStoreManager.cookie,
                dataStoreManager.pageId,
                dataStoreManager.location,
                dataStoreManager.language,
            ) { cookie, pageId, region, language -> listOf(cookie, pageId, region, language) }
                .distinctUntilChanged()
                .collectLatest { (cookie) ->
                    _accountInfo.value = if (cookie.isBlank()) null else Pair(
                        dataStoreManager.getString("AccountName").first(),
                        dataStoreManager.getString("AccountThumbUrl").first(),
                    )
                    loadMoodFeed(params.value)
                }
        }
    }

    fun loadMoodFeed(endpoint: String?) {
        homeJob?.cancel()
        paginationJob?.cancel()
        _homeItemList.value = emptyList()
        _continuation.value = null
        loading.value = endpoint != null
        _homeListState.value = if (endpoint == null) ListState.PAGINATION_EXHAUST else ListState.LOADING
        if (endpoint == null) return
        homeJob = viewModelScope.launch {
            val page = homeRepository.getHomeData(
                endpoint,
                getString(Res.string.view_count),
                getString(Res.string.song),
            ).catch { Logger.w("HomeViewModel", "Mood feed could not be loaded") }.firstOrNull()?.data
            _homeItemList.value = page?.second.orEmpty().moodShelves()
            _continuation.value = page?.first
            _homeListState.value = if (page?.first.isNullOrBlank()) ListState.PAGINATION_EXHAUST else ListState.IDLE
            loading.value = false
        }
    }

    fun getContinueHomeItem(continuation: String?) {
        if (continuation.isNullOrEmpty() || _homeListState.value != ListState.IDLE) return
        _homeListState.value = ListState.PAGINATING
        paginationJob = viewModelScope.launch {
            val page = homeRepository.getHomeDataContinue(
                continuation,
                getString(Res.string.view_count),
                getString(Res.string.song),
            ).catch { Logger.w("HomeViewModel", "More mood suggestions could not be loaded") }.firstOrNull()?.data
            _homeItemList.update { it + page?.second.orEmpty().moodShelves() }
            _continuation.value = page?.first
            _homeListState.value = if (page?.first.isNullOrBlank()) ListState.PAGINATION_EXHAUST else ListState.IDLE
        }
    }

    private fun List<HomeItem>.moodShelves(): List<HomeItem> =
        filterNot { it.title.trim().equals("Trending community playlists", ignoreCase = true) }

    fun setParams(params: String?) {
        if (_params.value != params) {
            _params.value = params
            loadMoodFeed(params)
        }
    }

    override fun onCleared() {
        super.onCleared()
        homeJob?.cancel()
    }

    companion object {
        // Home params
        const val HOME_PARAMS_RELAX = "ggM8SgQIBxADSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SLEEP = "ggM8SgQIBxABSgQIBRADSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ENERGIZE = "ggM8SgQIBxABSgQIBRABSgQICRADSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_SAD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChADSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_ROMANCE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRADSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_FEEL_GOOD = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBADSgQIBBABSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_WORKOUT = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBADSgQIDhABSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_PARTY = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhADSgQIAxABSgQIBhAB"
        const val HOME_PARAMS_COMMUTE = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxADSgQIBhAB"
        const val HOME_PARAMS_FOCUS = "ggM8SgQIBxABSgQIBRABSgQICRABSgQIChABSgQIDRABSgQICBABSgQIBBABSgQIDhABSgQIAxABSgQIBhAD"
    }
}
