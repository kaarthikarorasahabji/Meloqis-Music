package echo.music.iad1tya.viewModel

import echo.music.iad1tya.domain.data.model.browse.album.Track
import kotlin.math.roundToInt
import kotlin.random.Random

enum class SurpriseMode(val label: String, val familiarShare: Double) {
    COMFORT("Comfort", .75), BALANCED("Balanced", .5), ADVENTUROUS("Adventurous", .25)
}

data class SurprisePick(val track: Track, val familiar: Boolean)

/** Pure selection: no network, playback changes or listening-history writes. */
internal fun surpriseMix(
    familiar: List<Track>, discoveries: List<Track>, knownIds: Set<String>,
    avoidIds: Set<String>, excludedId: String?, allowExplicit: Boolean,
    mode: SurpriseMode, random: Random = Random.Default, size: Int = 15,
): List<SurprisePick> {
    if (size <= 0) return emptyList()
    fun valid(t: Track) = t.videoId.isNotBlank() && t.title.isNotBlank() &&
        t.isAvailable && (allowExplicit || !t.isExplicit) && t.videoId != excludedId
    fun candidates(tracks: List<Track>) = tracks.filter(::valid).distinctBy { it.videoId }
        .shuffled(random).sortedBy { it.videoId in avoidIds }
    val old = candidates(familiar).toMutableList()
    val oldIds = familiar.map { it.videoId }.toSet() + knownIds
    val fresh = candidates(discoveries).filter { it.videoId !in oldIds }.toMutableList()
    val targetOld = (size * mode.familiarShare).roundToInt()
    val result = mutableListOf<SurprisePick>()
    fun artist(t: Track) = t.artists?.firstOrNull()?.name?.trim()?.lowercase()
        ?.takeIf { it.isNotEmpty() } ?: t.videoId
    repeat(minOf(size, old.size + fresh.size)) { index ->
        val wantOld = ((index + 1) * targetOld / size) > result.count { it.familiar }
        val pool = if ((wantOld && old.isNotEmpty()) || fresh.isEmpty()) old else fresh
        val previousArtist = result.lastOrNull()?.track?.let(::artist)
        // Avoid repeats before artist adjacency; relax only when the pool runs out.
        val preferred = pool.filter { it.videoId !in avoidIds }.ifEmpty { pool }
        val pick = preferred.firstOrNull { artist(it) != previousArtist } ?: preferred.first()
        result += SurprisePick(pick, pool === old)
        pool.remove(pick)
    }
    return result
}
