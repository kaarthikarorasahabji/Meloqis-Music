package echo.music.iad1tya.domain.data.model.update

data class UpdateData(
    val tagName: String,
    val releaseTime: String?,
    val body: String,
    // Direct URL to the release APK asset, used for in-app download & install.
    // Null when unavailable (e.g. F-Droid update channel).
    val downloadUrl: String? = null,
)
