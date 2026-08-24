package echo.music.iad1tya.expect

expect fun openUrl(url: String)

expect fun shareUrl(
    title: String,
    url: String,
)

/**
 * Downloads the given APK URL and launches the system package installer so the
 * user can update the app from within the app itself (in-app update).
 */
expect fun downloadAndInstallApk(url: String)