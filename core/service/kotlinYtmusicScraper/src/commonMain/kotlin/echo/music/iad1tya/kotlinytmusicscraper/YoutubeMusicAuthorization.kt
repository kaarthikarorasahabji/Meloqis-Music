package echo.music.iad1tya.kotlinytmusicscraper

import echo.music.iad1tya.kotlinytmusicscraper.utils.parseCookieString
import echo.music.iad1tya.kotlinytmusicscraper.utils.sha1
import kotlin.time.Clock

internal fun youtubeMusicAuthorization(cookie: String, timestampSeconds: Long = Clock.System.now().epochSeconds): String? {
    val values = parseCookieString(cookie)
    val secret = values["SAPISID"]?.takeIf { it.isNotBlank() }
        ?: values["__Secure-3PAPISID"]?.takeIf { it.isNotBlank() }
        ?: return null
    val digest = sha1("$timestampSeconds $secret https://music.youtube.com")
    return "SAPISIDHASH ${timestampSeconds}_$digest"
}
