package echo.music.iad1tya.domain.repository

import echo.music.iad1tya.domain.data.entities.SearchHistory
import echo.music.iad1tya.domain.data.model.searchResult.SearchSuggestions
import echo.music.iad1tya.domain.data.model.searchResult.albums.AlbumsResult
import echo.music.iad1tya.domain.data.model.searchResult.artists.ArtistsResult
import echo.music.iad1tya.domain.data.model.searchResult.playlists.PlaylistsResult
import echo.music.iad1tya.domain.data.model.searchResult.songs.SongsResult
import echo.music.iad1tya.domain.data.model.searchResult.videos.VideosResult
import echo.music.iad1tya.domain.utils.Resource
import kotlinx.coroutines.flow.Flow

interface SearchRepository {
    fun getSearchHistory(): Flow<List<SearchHistory>>

    fun insertSearchHistory(searchHistory: SearchHistory): Flow<Long>

    suspend fun deleteSearchHistory()

    fun getSearchDataSong(query: String): Flow<Resource<ArrayList<SongsResult>>>

    fun getSearchDataVideo(query: String): Flow<Resource<ArrayList<VideosResult>>>

    fun getSearchDataPodcast(query: String): Flow<Resource<ArrayList<PlaylistsResult>>>

    fun getSearchDataFeaturedPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>>

    fun getSearchDataArtist(query: String): Flow<Resource<ArrayList<ArtistsResult>>>

    fun getSearchDataAlbum(query: String): Flow<Resource<ArrayList<AlbumsResult>>>

    fun getSearchDataPlaylist(query: String): Flow<Resource<ArrayList<PlaylistsResult>>>

    fun getSuggestQuery(query: String): Flow<Resource<SearchSuggestions>>
}