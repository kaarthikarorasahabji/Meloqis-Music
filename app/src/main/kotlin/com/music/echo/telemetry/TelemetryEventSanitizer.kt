package iad1tya.echo.music.telemetry

internal object TelemetryEventSanitizer {
    private val allowedEvents = setOf(
        "first_open",
        "app_active",
        "update_download_started",
        "update_download_completed",
        "update_download_failed",
        "update_verification_failed",
        "update_ready_to_install",
        "update_install_succeeded",
        "crash",
        "playback_failure",
    )

    fun isAllowedEvent(name: String): Boolean = name in allowedEvents

    fun cleanCode(value: String?): String? =
        value
            ?.replace(Regex("[\\u0000-\\u001f\\u007f]"), " ")
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.take(80)
            ?.takeIf(String::isNotBlank)

    fun cleanVersion(value: String?): String? =
        value
            ?.trim()
            ?.take(32)
            ?.takeIf { it.matches(Regex("[0-9A-Za-z][0-9A-Za-z.+_-]{0,31}")) }
}
