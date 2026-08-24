package echo.music.iad1tya.data.mapping

import echo.music.iad1tya.data.parser.toListThumbnail
import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.domain.data.model.canvas.CanvasResult
import echo.music.iad1tya.domain.data.model.mediaService.SponsorSkipSegments
import echo.music.iad1tya.domain.data.model.metadata.Line
import echo.music.iad1tya.domain.data.model.metadata.Lyrics
import echo.music.iad1tya.domain.data.model.searchResult.albums.AlbumsResult
import echo.music.iad1tya.domain.data.model.searchResult.artists.ArtistsResult
import echo.music.iad1tya.domain.data.model.searchResult.playlists.PlaylistsResult
import echo.music.iad1tya.domain.data.model.searchResult.songs.Album
import echo.music.iad1tya.domain.data.model.searchResult.songs.Artist
import echo.music.iad1tya.domain.data.model.searchResult.songs.SongsResult
import echo.music.iad1tya.domain.data.model.searchResult.songs.Thumbnail
import echo.music.iad1tya.domain.data.model.searchResult.videos.VideosResult
import echo.music.iad1tya.domain.data.model.streams.YouTubeWatchEndpoint
import echo.music.iad1tya.kotlinytmusicscraper.models.AccountInfo
import echo.music.iad1tya.kotlinytmusicscraper.models.AlbumItem
import echo.music.iad1tya.kotlinytmusicscraper.models.ArtistItem
import echo.music.iad1tya.kotlinytmusicscraper.models.PlaylistItem
import echo.music.iad1tya.kotlinytmusicscraper.models.SearchSuggestions
import echo.music.iad1tya.kotlinytmusicscraper.models.SongItem
import echo.music.iad1tya.kotlinytmusicscraper.models.VideoItem
import echo.music.iad1tya.kotlinytmusicscraper.models.WatchEndpoint
import echo.music.iad1tya.kotlinytmusicscraper.models.response.PipedResponse
import echo.music.iad1tya.kotlinytmusicscraper.models.sponsorblock.SkipSegments
import echo.music.iad1tya.kotlinytmusicscraper.models.youtube.Transcript
import echo.music.iad1tya.kotlinytmusicscraper.models.youtube.YouTubeInitialPage
import echo.music.iad1tya.spotify.model.response.spotify.CanvasResponse
import echo.music.iad1tya.spotify.model.response.spotify.SpotifyLyricsResponse
import echo.music.iad1tya.lyrics.models.response.LyricsResponse
import echo.music.iad1tya.lyrics.models.response.TranslatedLyricsResponse
import echo.music.iad1tya.lyrics.parser.parseRichSyncLyrics
import echo.music.iad1tya.lyrics.parser.parseSyncedLyrics
import echo.music.iad1tya.lyrics.parser.parseUnsyncedLyrics
import kotlin.jvm.JvmName

internal fun SongItem.toTrack(): Track =
    Track(
        album = this.album.let { Album(it?.id ?: "", it?.name ?: "") },
        artists = this.artists.map { artist -> Artist(id = artist.id ?: "", name = artist.name) },
        duration = this.duration.toString(),
        durationSeconds = this.duration,
        isAvailable = false,
        isExplicit = this.explicit,
        likeStatus = null,
        thumbnails = this.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
        title = this.title,
        videoId = this.id,
        videoType = this.musicVideoType,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = null,
    )

internal fun VideoItem.toTrack(): Track =
    Track(
        album = this.album.let { Album(it?.id ?: "", it?.name ?: "") },
        artists = this.artists.map { artist -> Artist(id = artist.id ?: "", name = artist.name) },
        duration = this.duration.toString(),
        durationSeconds = this.duration,
        isAvailable = false,
        isExplicit = false,
        likeStatus = null,
        thumbnails = this.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
        title = this.title,
        videoId = this.id,
        videoType = this.musicVideoType,
        category = null,
        feedbackTokens = null,
        resultType = null,
        year = null,
    )

@JvmName("SongItemtoTrack")
internal fun List<SongItem>?.toListTrack(): ArrayList<Track> {
    val listTrack = arrayListOf<Track>()
    if (this != null) {
        for (item in this) {
            listTrack.add(item.toTrack())
        }
    }
    return listTrack
}

internal fun Track.toSongItemForDownload(): SongItem =
    SongItem(
        id = this.videoId,
        title = this.title,
        artists =
            this.artists?.map {
                echo.music.iad1tya.kotlinytmusicscraper.models.Artist(
                    id = it.id ?: "",
                    name = it.name,
                )
            } ?: emptyList(),
        album =
            echo.music.iad1tya.kotlinytmusicscraper.models.Album(
                id = this.album?.id ?: "",
                name = this.album?.name ?: "",
            ),
        duration = this.durationSeconds,
        thumbnail = this.thumbnails?.lastOrNull()?.url ?: "",
        explicit = this.isExplicit,
    )

internal fun echo.music.iad1tya.lyrics.domain.Lyrics.toLyrics(): Lyrics {
    val lines: ArrayList<Line> = arrayListOf()
    if (this.lyrics != null) {
        this.lyrics?.lines?.forEach {
            lines.add(
                Line(
                    endTimeMs = it.endTimeMs,
                    startTimeMs = it.startTimeMs,
                    syllables = it.syllables ?: listOf(),
                    words = it.words,
                ),
            )
        }
        return Lyrics(
            error = false,
            lines = lines,
            syncType = this.lyrics!!.syncType,
        )
    } else {
        return Lyrics(
            error = true,
            lines = null,
            syncType = null,
        )
    }
}

internal fun Lyrics.toLibraryLyrics(): echo.music.iad1tya.lyrics.domain.Lyrics =
    echo.music.iad1tya.lyrics.domain.Lyrics(
        lyrics =
            echo.music.iad1tya.lyrics.domain.Lyrics.LyricsX(
                lines =
                    this.lines?.map {
                        echo.music.iad1tya.lyrics.domain.Lyrics.LyricsX.Line(
                            endTimeMs = it.endTimeMs,
                            startTimeMs = it.startTimeMs,
                            syllables = listOf(),
                            words = it.words,
                        )
                    },
                syncType = this.syncType,
            ),
    )

internal fun SpotifyLyricsResponse.toLyrics(): Lyrics {
    val lines: ArrayList<Line> = arrayListOf()
    this.lyrics.lines.forEach {
        lines.add(
            Line(
                endTimeMs = it.endTimeMs,
                startTimeMs = it.startTimeMs,
                syllables = listOf(),
                words = it.words,
            ),
        )
    }
    return Lyrics(
        error = false,
        lines = lines,
        syncType = this.lyrics.syncType,
    )
}

internal fun PipedResponse.toTrack(videoId: String): Track =
    Track(
        album = null,
        artists =
            listOf(
                Artist(
                    this.uploaderUrl?.replace("/channel/", ""),
                    this.uploader.toString(),
                ),
            ),
        duration = "",
        durationSeconds = 0,
        isAvailable = false,
        isExplicit = false,
        likeStatus = "INDIFFERENT",
        thumbnails =
            listOf(
                Thumbnail(
                    720,
                    this.thumbnailUrl ?: "https://i.ytimg.com/vi/$videoId/maxresdefault.jpg",
                    1080,
                ),
            ),
        title = this.title ?: " ",
        videoId = videoId,
        // Piped is not YouTube Music and never reports musicVideoType.
        videoType = null,
        category = "",
        feedbackTokens = null,
        resultType = null,
        year = "",
    )

internal fun YouTubeInitialPage.toTrack(): Track {
    val initialPage = this

    return Track(
        album = null,
        artists =
            listOf(
                Artist(
                    name = initialPage.videoDetails?.author ?: "",
                    id = initialPage.videoDetails?.channelId,
                ),
            ),
        duration = initialPage.videoDetails?.lengthSeconds,
        durationSeconds = initialPage.videoDetails?.lengthSeconds?.toInt() ?: 0,
        isAvailable = false,
        isExplicit = false,
        likeStatus = null,
        thumbnails =
            initialPage.videoDetails
                ?.thumbnail
                ?.thumbnails
                ?.toListThumbnail() ?: listOf(),
        title = initialPage.videoDetails?.title ?: "",
        videoId = initialPage.videoDetails?.videoId ?: "",
        // The plain-YouTube player response has no musicVideoType; only the YouTube *Music* one
        // (PlayerResponse.VideoDetails) carries it.
        videoType = null,
        category = "",
        feedbackTokens = null,
        resultType = "",
        year = "",
    )
}

internal fun Transcript.toLyrics(): Lyrics {
    val lines =
        this.text.map {
            Line(
                endTimeMs = "0",
                startTimeMs = (it.start.toFloat() * 1000).toInt().toString(),
                syllables = listOf(),
                words = it.content.replace(Regex("<[^>]*>"), ""),
            )
        }
    val sortedLine = lines.sortedBy { it.startTimeMs.toInt() }
    return Lyrics(
        error = false,
        lines = sortedLine,
        syncType = "LINE_SYNCED",
    )
}

internal fun AlbumItem.toAlbumsResult(): AlbumsResult =
    AlbumsResult(
        artists =
            this.artists?.map {
                Artist(
                    id = it.id ?: "",
                    name = it.name,
                )
            } ?: emptyList(),
        browseId = this.id,
        category = this.title,
        duration = "",
        isExplicit = this.explicit,
        resultType = "ALBUM",
        thumbnails =
            listOf(
                Thumbnail(
                    width = 720,
                    url = this.thumbnail,
                    height = 720,
                ),
            ),
        title = this.title,
        type = if (isSingle) "SINGLE" else "ALBUM",
        year = this.year?.toString() ?: "",
    )

// SimpMusic Lyrics Extension
internal fun LyricsResponse.toLyrics(): Lyrics? =
    (
        richSyncLyrics?.takeIf { it.isNotEmpty() }?.let {
            parseRichSyncLyrics(it)
        }
            ?: syncedLyrics?.let { if (it.isNotEmpty() && it.isNotBlank()) parseSyncedLyrics(it) else null }
            ?: (
                if (plainLyric.isNotEmpty() && plainLyric.isNotBlank()) {
                    parseUnsyncedLyrics(plainLyric)
                } else {
                    null
                }
            )
    )?.toLyrics()

internal fun TranslatedLyricsResponse.toLyrics(): Lyrics = parseSyncedLyrics(this.translatedLyric).toLyrics()

internal fun SearchSuggestions.toDomainSearchSuggestions(): echo.music.iad1tya.domain.data.model.searchResult.SearchSuggestions =
    echo.music.iad1tya.domain.data.model.searchResult.SearchSuggestions(
        queries = this.queries,
        recommendedItems =
            this.recommendedItems.map {
                when (it) {
                    is SongItem -> {
                        SongsResult(
                            album =
                                Album(
                                    id = it.album?.id ?: "",
                                    name = it.album?.name ?: "",
                                ),
                            artists =
                                it.artists.map { artist ->
                                    Artist(
                                        id = artist.id ?: "",
                                        name = artist.name,
                                    )
                                },
                            category = "",
                            duration = it.duration.toString(),
                            durationSeconds = it.duration,
                            feedbackTokens = null,
                            isExplicit = it.explicit,
                            resultType = "Song",
                            thumbnails = it.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
                            title = it.title,
                            videoId = it.id,
                            videoType = it.musicVideoType,
                            year = "",
                        )
                    }

                    is AlbumItem -> {
                        AlbumsResult(
                            artists =
                                it.artists?.map {
                                    Artist(
                                        id = it.id ?: "",
                                        name = it.name,
                                    )
                                } ?: emptyList(),
                            browseId = it.browseId,
                            category = "",
                            duration = "",
                            isExplicit = it.explicit,
                            resultType = "ALBUM",
                            thumbnails =
                                listOf(
                                    Thumbnail(
                                        width = 720,
                                        url = it.thumbnail,
                                        height = 720,
                                    ),
                                ),
                            title = it.title,
                            type = if (it.isSingle) "SINGLE" else "ALBUM",
                            year = it.year?.toString() ?: "",
                        )
                    }

                    is ArtistItem -> {
                        ArtistsResult(
                            artist = it.title,
                            browseId = it.id,
                            category = "",
                            radioId = it.radioEndpoint?.playlistId ?: "",
                            resultType = "ARTIST",
                            shuffleId = it.shuffleEndpoint?.playlistId ?: "",
                            thumbnails =
                                listOf(
                                    Thumbnail(
                                        width = 720,
                                        url = it.thumbnail,
                                        height = 720,
                                    ),
                                ),
                        )
                    }

                    is PlaylistItem -> {
                        PlaylistsResult(
                            author = it.author?.name ?: "YouTube Music",
                            browseId = it.id,
                            category = "",
                            itemCount = "0",
                            resultType = "PLAYLIST",
                            thumbnails =
                                listOf(
                                    Thumbnail(
                                        width = 720,
                                        url = it.thumbnail,
                                        height = 720,
                                    ),
                                ),
                            title = it.title,
                        )
                    }

                    is VideoItem -> {
                        VideosResult(
                            artists =
                                it.artists.map { artist ->
                                    Artist(
                                        id = artist.id ?: "",
                                        name = artist.name,
                                    )
                                },
                            category = null,
                            duration = it.duration?.toString(),
                            durationSeconds = it.duration,
                            resultType = "VIDEO",
                            thumbnails = it.thumbnails?.thumbnails?.toListThumbnail() ?: listOf(),
                            title = it.title,
                            videoId = it.id,
                            videoType = it.musicVideoType,
                            views = it.view,
                            year = "",
                        )
                    }
                }
            },
    )

internal fun CanvasResponse.toCanvasResult(): CanvasResult? {
    val canvasUrl = this.canvases.firstOrNull()?.canvas_url ?: return null
    val canvasThumbs = this.canvases.firstOrNull()?.thumbsOfCanva
    val thumbUrl =
        if (!canvasThumbs.isNullOrEmpty()) {
            (
                canvasThumbs.let { thumb ->
                    thumb
                        .maxByOrNull {
                            (it.height ?: 0) + (it.width ?: 0)
                        }?.url
                } ?: canvasThumbs.first().url
            )
        } else {
            null
        }
    return CanvasResult(
        isVideo = canvasUrl.contains(".mp4"),
        canvasUrl = canvasUrl,
        canvasThumbUrl = thumbUrl,
    )
}

internal fun YouTubeWatchEndpoint.toWatchEndpoint(): WatchEndpoint =
    WatchEndpoint(
        videoId = this.videoId,
        playlistId = this.playlistId,
        playlistSetVideoId = this.playlistSetVideoId,
        params = this.params,
        index = this.index,
        watchEndpointMusicSupportedConfigs =
            this.watchEndpointMusicSupportedConfigs?.let { supportedConfig ->
                WatchEndpoint.WatchEndpointMusicSupportedConfigs(
                    watchEndpointMusicConfig =
                        WatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig(
                            musicVideoType =
                                supportedConfig.watchEndpointMusicConfig.musicVideoType,
                        ),
                )
            },
    )

internal fun WatchEndpoint.toYouTubeWatchEndpoint(): YouTubeWatchEndpoint =
    YouTubeWatchEndpoint(
        videoId = this.videoId,
        playlistId = this.playlistId,
        playlistSetVideoId = this.playlistSetVideoId,
        params = this.params,
        index = this.index,
        watchEndpointMusicSupportedConfigs =
            this.watchEndpointMusicSupportedConfigs?.let { supportedConfig ->
                YouTubeWatchEndpoint.WatchEndpointMusicSupportedConfigs(
                    watchEndpointMusicConfig =
                        YouTubeWatchEndpoint.WatchEndpointMusicSupportedConfigs.WatchEndpointMusicConfig(
                            musicVideoType =
                                supportedConfig.watchEndpointMusicConfig.musicVideoType,
                        ),
                )
            },
    )

internal fun SkipSegments.toSponsorSkipSegments(): SponsorSkipSegments =
    SponsorSkipSegments(
        actionType = this.actionType,
        category = this.category,
        description = this.description,
        locked = this.locked,
        segment = this.segment,
        uUID = this.uUID,
        videoDuration = this.videoDuration,
        votes = this.votes,
    )

internal fun AccountInfo.toDomainAccountInfo(): echo.music.iad1tya.domain.data.model.account.AccountInfo =
    echo.music.iad1tya.domain.data.model.account.AccountInfo(
        name = this.name,
        email = this.email,
        pageId = this.pageId,
        thumbnails = thumbnails.toListThumbnail(),
    )