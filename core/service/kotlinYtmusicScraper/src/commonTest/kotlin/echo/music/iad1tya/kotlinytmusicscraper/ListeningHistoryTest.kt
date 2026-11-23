package echo.music.iad1tya.kotlinytmusicscraper

import echo.music.iad1tya.kotlinytmusicscraper.models.response.BrowseResponse
import echo.music.iad1tya.kotlinytmusicscraper.pages.listeningHistory
import echo.music.iad1tya.kotlinytmusicscraper.utils.sha1
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@OptIn(ExperimentalSerializationApi::class)
class ListeningHistoryTest {
    private val json = Json { explicitNulls = false; ignoreUnknownKeys = true }

    @Test
    fun parsesDatedShelvesInOrderAndDeduplicatesTracks() {
        val page = decode("""[
            {"musicShelfRenderer":{"contents":[${song("new", "New song")}, {}]}},
            {"musicShelfRenderer":{"contents":[${song("new", "New song")}, ${song("old", "Old song")}]}}
        ]""")
        val songs = page.listeningHistory(10)
        assertEquals(listOf("new", "old"), songs.map { it.id })
        assertEquals("New song", songs[0].title)
        assertEquals(listOf("Artist"), songs[0].artists.map { it.name })
        assertEquals(listOf("new"), page.listeningHistory(1).map { it.id })
    }

    @Test
    fun distinguishesAnEmptyHistoryFromAnUnsupportedPage() {
        assertEquals(emptyList(), decode("[]").listeningHistory(10))
        assertFailsWith<IllegalStateException> {
            json.decodeFromString<BrowseResponse>("""{"responseContext":{},"contents":{}}""").listeningHistory(10)
        }
    }

    @Test
    fun acceptsEitherSessionFieldAndUsesEpochSeconds() {
        val dummyValue = "fixture"
        val expectedDigest = sha1("1700000000 $dummyValue https://music.youtube.com")
        val expected = "SAPISIDHASH 1700000000_$expectedDigest"
        assertEquals(expected, youtubeMusicAuthorization("SAPISID=$dummyValue", 1_700_000_000))
        assertEquals(expected, youtubeMusicAuthorization("__Secure-3PAPISID=$dummyValue", 1_700_000_000))
        assertEquals(expected, youtubeMusicAuthorization("PREF=a=b;ignored;SAPISID=$dummyValue", 1_700_000_000))
        assertNull(youtubeMusicAuthorization("PREF=$dummyValue", 1_700_000_000))
    }

    private fun decode(sections: String) = json.decodeFromString<BrowseResponse>("""
        {"responseContext":{},"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[
            {"tabRenderer":{"content":{"sectionListRenderer":{"contents":$sections}}}}
        ]}}}
    """)

    private fun song(id: String, title: String) = """
        {"musicResponsiveListItemRenderer":{
          "playlistItemData":{"videoId":"$id"},
          "flexColumns":[
            {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"$title"}]}}},
            {"musicResponsiveListItemFlexColumnRenderer":{"text":{"runs":[{"text":"Artist"}]}}}
          ],
          "thumbnail":{"musicThumbnailRenderer":{"thumbnail":{"thumbnails":[{"url":"https://example.com/cover.jpg"}]}}}
        }}
    """
}
