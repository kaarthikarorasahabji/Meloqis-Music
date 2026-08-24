package echo.music.iad1tya.domain.data.model.browse.album

import echo.music.iad1tya.domain.data.model.browse.artist.ResultAlbum
import echo.music.iad1tya.domain.data.model.searchResult.songs.Artist
import echo.music.iad1tya.domain.data.model.searchResult.songs.Thumbnail

data class AlbumBrowse(
    val artists: List<Artist>,
    val audioPlaylistId: String,
    val description: String?,
    val duration: String?,
    val durationSeconds: Int,
    val thumbnails: List<Thumbnail>?,
    val title: String,
    val trackCount: Int,
    val tracks: List<Track>,
    val type: String,
    val year: String?,
    val otherVersion: List<ResultAlbum> = emptyList(),
)