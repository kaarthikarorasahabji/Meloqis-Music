package echo.music.iad1tya.kotlinytmusicscraper.extractor

import echo.music.iad1tya.kotlinytmusicscraper.models.SongItem
import echo.music.iad1tya.kotlinytmusicscraper.models.response.DownloadProgress

expect class Extractor() {
    fun init()

    fun logIn(cookie: String?)

    fun mergeAudioVideoDownload(filePath: String): DownloadProgress

    fun saveAudioWithThumbnail(
        filePath: String,
        track: SongItem,
    ): DownloadProgress

    fun newPipePlayer(videoId: String): List<Pair<Int, String>>
}