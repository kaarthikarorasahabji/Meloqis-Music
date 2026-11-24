package echo.music.iad1tya.ui.component

import echo.music.iad1tya.domain.data.model.browse.album.Track
import kotlin.test.Test
import kotlin.test.assertEquals

class ListeningHistoryMergeTest {
    @Test
    fun includesBothSourcesAndDeduplicatesTracks() {
        val local = listOf(track("local-first"), track("shared"), track("local-last"))
        val youtube = listOf(track("youtube-first"), track("shared"))

        val merged = mergeListeningHistory(local, youtube)

        assertEquals(listOf("local-first", "shared", "local-last", "youtube-first"), merged.map(Track::videoId))
        assertEquals(local, mergeListeningHistory(local, emptyList()))
        assertEquals(youtube, mergeListeningHistory(emptyList(), youtube))
    }

    private fun track(id: String) = Track(
        album = null,
        artists = null,
        duration = null,
        durationSeconds = null,
        isAvailable = true,
        isExplicit = false,
        likeStatus = null,
        thumbnails = null,
        title = id,
        videoId = id,
        videoType = null,
        category = null,
        feedbackTokens = null,
        resultType = null,
    )
}
