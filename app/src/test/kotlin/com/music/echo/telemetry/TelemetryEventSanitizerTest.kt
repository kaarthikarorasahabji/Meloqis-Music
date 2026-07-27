package iad1tya.echo.music.telemetry

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TelemetryEventSanitizerTest {
    @Test
    fun allowsOnlyDocumentedEvents() {
        assertTrue(TelemetryEventSanitizer.isAllowedEvent("first_open"))
        assertTrue(TelemetryEventSanitizer.isAllowedEvent("playback_failure"))
        assertFalse(TelemetryEventSanitizer.isAllowedEvent("song_title"))
    }

    @Test
    fun removesControlCharactersAndBoundsCodes() {
        assertEquals(
            "HTTP 403 retry",
            TelemetryEventSanitizer.cleanCode(" HTTP 403\nretry "),
        )
        assertEquals(80, TelemetryEventSanitizer.cleanCode("x".repeat(200))?.length)
    }

    @Test
    fun rejectsMalformedVersions() {
        assertEquals("0.1.8", TelemetryEventSanitizer.cleanVersion("0.1.8"))
        assertNull(TelemetryEventSanitizer.cleanVersion("version with spaces"))
    }
}
