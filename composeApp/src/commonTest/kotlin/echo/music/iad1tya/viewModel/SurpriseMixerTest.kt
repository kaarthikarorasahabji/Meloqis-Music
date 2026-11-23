package echo.music.iad1tya.viewModel

import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.domain.data.model.searchResult.songs.Artist
import kotlin.random.Random
import kotlin.test.*

class SurpriseMixerTest {
    private fun track(id: String, artist: String = id, explicit: Boolean = false, available: Boolean = true) =
        Track(null, listOf(Artist(null, artist)), "3:00", 180, available, explicit, null, null,
            id, id, null, null, null, null)
    private val favourites = (1..30).map { track("f$it") }
    private val discoveries = (1..30).map { track("d$it") }
    private fun mix(mode: SurpriseMode, old: List<Track> = favourites, fresh: List<Track> = discoveries,
        known: Set<String> = emptySet(), avoid: Set<String> = emptySet(), excluded: String? = null) =
        surpriseMix(old, fresh, known, avoid, excluded, false, mode, Random(42))

    @Test fun modesBalanceFamiliarAndFresh() {
        assertEquals(11, mix(SurpriseMode.COMFORT).count { it.familiar })
        assertEquals(8, mix(SurpriseMode.BALANCED).count { it.familiar })
        assertEquals(4, mix(SurpriseMode.ADVENTUROUS).count { it.familiar })
    }
    @Test fun filtersUnavailableExplicitBlankAndCurrentTracks() {
        val picks = mix(SurpriseMode.BALANCED,
            listOf(track("ok"), track("explicit", explicit = true), track("gone", available = false), track(""), track("current")),
            emptyList(), excluded = "current")
        assertEquals(listOf("ok"), picks.map { it.track.videoId })
    }
    @Test fun knownSongsAreNeverLabelledDiscoveriesAndDuplicatesAreRemoved() {
        val picks = mix(SurpriseMode.BALANCED, favourites + favourites,
            discoveries + favourites + discoveries, known = setOf("d1"))
        assertEquals(picks.size, picks.map { it.track.videoId }.toSet().size)
        assertFalse(picks.any { !it.familiar && it.track.videoId == "d1" })
        assertFalse(picks.any { !it.familiar && it.track.videoId.startsWith("f") })
    }
    @Test fun favouritesFillMixWhenDiscoveryIsUnavailable() {
        val picks = mix(SurpriseMode.ADVENTUROUS, fresh = emptyList())
        assertEquals(15, picks.size)
        assertTrue(picks.all { it.familiar })
    }
    @Test fun avoidsRecentTracksWhenAlternativesExistAndRelaxesForSmallLibraries() {
        val avoided = (1..10).map { "f$it" }.toSet()
        assertTrue(mix(SurpriseMode.COMFORT, fresh = emptyList(), avoid = avoided).none { it.track.videoId in avoided })
        assertEquals(2, mix(SurpriseMode.BALANCED, favourites.take(2), emptyList(), avoid = favourites.map { it.videoId }.toSet()).size)
    }
    @Test fun avoidsConsecutiveArtistsWherePoolOffersAlternatives() {
        val tracks = (1..10).map { track("a$it", "A") } + (1..10).map { track("b$it", "B") }
        val picks = mix(SurpriseMode.COMFORT, tracks, emptyList())
        assertTrue(picks.zipWithNext().all { (a, b) -> a.track.artists!!.first().name != b.track.artists!!.first().name })
    }
    @Test fun emptyPoolAndZeroSizeAreSafe() {
        assertTrue(mix(SurpriseMode.BALANCED, emptyList(), emptyList()).isEmpty())
        assertTrue(surpriseMix(favourites, discoveries, emptySet(), emptySet(), null, true, SurpriseMode.BALANCED, size = 0).isEmpty())
    }
}
