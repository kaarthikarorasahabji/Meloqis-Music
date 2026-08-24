package echo.music.iad1tya.domain.data.player

/**
 * Generic tracks information
 */
data class GenericTracks(
    val groups: List<GenericTrackGroup>,
) {
    data class GenericTrackGroup(
        val trackCount: Int,
    )
}