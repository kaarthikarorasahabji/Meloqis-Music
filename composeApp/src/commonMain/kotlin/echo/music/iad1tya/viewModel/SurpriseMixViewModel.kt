package echo.music.iad1tya.viewModel

import androidx.lifecycle.viewModelScope
import echo.music.iad1tya.common.Config
import echo.music.iad1tya.domain.manager.DataStoreManager
import echo.music.iad1tya.domain.mediaservice.handler.PlaylistType
import echo.music.iad1tya.domain.mediaservice.handler.QueueData
import echo.music.iad1tya.domain.repository.AnalyticsRepository
import echo.music.iad1tya.domain.repository.SongRepository
import echo.music.iad1tya.domain.utils.toTrack
import echo.music.iad1tya.viewModel.base.BaseViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class SurpriseState(
    val mode: SurpriseMode = SurpriseMode.BALANCED,
    val loading: Boolean = false,
    val downloadsOnly: Boolean = false,
    val mixMode: SurpriseMode = SurpriseMode.BALANCED,
    val picks: List<SurprisePick> = emptyList(),
    val message: String = "",
)

class SurpriseMixViewModel(
    private val songs: SongRepository,
    private val analytics: AnalyticsRepository,
    private val preferences: DataStoreManager,
) : BaseViewModel() {
    private val mutableState = MutableStateFlow(SurpriseState())
    val state = mutableState.asStateFlow()
    private var job: Job? = null
    private var generation = 0
    private var started = false
    private var previousIds = emptySet<String>()

    fun start() { if (!started) { started = true; generate() } }
    fun mode(mode: SurpriseMode) { mutableState.update { it.copy(mode = mode) } }

    fun downloadsOnly(enabled: Boolean) {
        mutableState.update { it.copy(downloadsOnly = enabled) }
    }
    fun cancel() {
        generation++
        job?.cancel()
        mutableState.update { if (it.loading) it.copy(loading = false, message = "Mix cancelled.") else it }
    }

    fun generate() {
        cancel()
        val token = generation
        val mode = state.value.mode
        val offline = state.value.downloadsOnly
        mutableState.update { it.copy(loading = true, message = "Finding your next favourites…") }
        job = viewModelScope.launch {
            try {
                val tracking = preferences.localTrackingEnabled.first() == DataStoreManager.TRUE
                val explicit = preferences.explicitContentEnabled.first() == DataStoreManager.TRUE
                val current = mediaPlayerHandler.nowPlayingState.value.songEntity
                val liked = if (offline) emptyList() else songs.getLikedSongs().first()
                val downloaded = songs.getDownloadedSongs().first().orEmpty()
                val played = if (tracking && !offline) songs.getMostPlayedSongs().first() else emptyList()
                val recent = if (tracking) {
                    analytics.getPlaybackEventsByOffset(0, 80).first().map { it.videoId }.toSet()
                } else {
                    emptySet()
                }
                val familiar = (liked + played + downloaded).distinctBy { it.videoId }.map { it.toTrack() }
                val seeds = (familiar.shuffled() + listOfNotNull(current?.toTrack()))
                    .filter { it.isAvailable && (explicit || !it.isExplicit) }
                    .distinctBy { it.artists?.firstOrNull()?.name ?: it.videoId }.take(2)
                if (!offline && seeds.isEmpty()) {
                    mutableState.update { it.copy(loading = false, message = "Play or like a song first, then come back for a mix made for you.") }
                    return@launch
                }
                val discoveries = if (offline) emptyList() else coroutineScope {
                    seeds.map { seed -> async {
                        withTimeoutOrNull(8_000) {
                            try { songs.getRelatedData(seed.videoId).first().data?.first.orEmpty() }
                            catch (e: CancellationException) { throw e }
                            catch (_: Exception) { emptyList() }
                        }.orEmpty()
                    } }.awaitAll().flatten()
                }
                ensureActive()
                if (token != generation) return@launch
                val known = familiar.map { it.videoId }.toSet() + recent + listOfNotNull(current?.videoId)
                val picks = surpriseMix(familiar, discoveries, known, recent + previousIds,
                    current?.videoId, explicit, mode)
                ensureActive()
                if (token != generation) return@launch
                val freshCount = picks.count { !it.familiar }
                val message = when {
                    offline && picks.isEmpty() -> "No eligible downloads found. Download another song or turn Downloads only off."
                    offline -> "${picks.size} downloaded songs"
                    picks.isEmpty() -> "No playable songs found. Try another song or check your connection."
                    freshCount == 0 -> "Favourites mix ready. Fresh discoveries are unavailable right now."
                    else -> "${picks.count { it.familiar }} familiar · $freshCount discoveries · ${picks.size} songs"
                }
                mutableState.update { it.copy(loading = false, picks = picks, mixMode = mode, message = message) }
                previousIds = picks.map { it.track.videoId }.toSet()
                if (picks.isNotEmpty()) play(0)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) {
                if (token == generation) mutableState.update { it.copy(loading = false, message = "Couldn't build your mix. Please try again.") }
            } finally {
                if (token == generation) mutableState.update { it.copy(loading = false) }
            }
        }
    }

    fun play(index: Int) {
        val tracks = state.value.picks.map { it.track }
        val track = tracks.getOrNull(index) ?: return
        setQueueData(QueueData.Data(listTracks = tracks, firstPlayedTrack = track,
            playlistName = "Surprise Me · ${state.value.mixMode.label}", playlistType = PlaylistType.LOCAL_PLAYLIST))
        loadMediaItem(track, Config.PLAYLIST_CLICK, index)
    }
}
