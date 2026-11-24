package echo.music.iad1tya.media3.service

import echo.music.iad1tya.common.Config
import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.domain.manager.DataStoreManager
import echo.music.iad1tya.domain.mediaservice.handler.MediaPlayerHandler
import echo.music.iad1tya.domain.mediaservice.handler.PlaylistType
import echo.music.iad1tya.domain.mediaservice.handler.QueueData
import echo.music.iad1tya.domain.repository.SearchRepository
import echo.music.iad1tya.domain.repository.SongRepository
import echo.music.iad1tya.domain.utils.Resource
import echo.music.iad1tya.domain.utils.toListTrack
import echo.music.iad1tya.domain.utils.toTrack
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.withTimeoutOrNull

/** Resolves Android media-search requests through the same queue and player as in-app playback. */
class VoicePlayback(
    private val player: MediaPlayerHandler,
    private val search: SearchRepository,
    private val songs: SongRepository,
    private val preferences: DataStoreManager,
) {
    suspend fun play(query: String, title: String? = null, artist: String? = null): Boolean {
        val songName = title.orEmpty().trim().take(160)
        val artistName = artist.orEmpty().trim().take(100)
        val searchText = listOf(songName, artistName).filter(String::isNotEmpty).joinToString(" ")
            .ifBlank { query.trim().take(250) }
        val canPlayExplicit = preferences.explicitContentEnabled.first() != DataStoreManager.FALSE

        if (searchText.isBlank() && player.getCurrentMediaItem() != null) {
            player.player.play()
            return true
        }

        val local = songs.getAllSongs(300).first()
            .filter { canPlayExplicit || !it.isExplicit }
        val track = if (searchText.isBlank()) {
            local.firstOrNull()?.toTrack()
        } else {
            val titleKey = songName.ifBlank { searchText }.lowercase()
            local.firstOrNull { song ->
                song.title.trim().lowercase() == titleKey &&
                    (artistName.isBlank() || song.artistName.orEmpty().any { it.contains(artistName, ignoreCase = true) })
            }?.toTrack() ?: withTimeoutOrNull(8_000) {
                (search.getSearchDataSong(searchText).lastOrNull() as? Resource.Success)
                    ?.data?.toListTrack()?.firstOrNull { canPlayExplicit || !it.isExplicit }
            }
        } ?: return false

        playTrack(track)
        return true
    }

    private suspend fun playTrack(track: Track) {
        player.setQueueData(
            QueueData.Data(
                listTracks = listOf(track),
                firstPlayedTrack = track,
                playlistId = "RDAMVM${track.videoId}",
                playlistName = track.title,
                playlistType = PlaylistType.RADIO,
            ),
        )
        player.loadMediaItem(track, Config.SONG_CLICK, 0)
    }
}
