package echo.music.iad1tya.ui.component

import java.time.LocalDate
import java.time.LocalTime
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class CampaignTimeTest {
    @Test fun indiaTimeAndMidnightDeliveryWindow() {
        val early = FestivalCampaign("test", LocalDate.of(2026,11,8), "Hello", "World", LocalTime.of(0,15))
        assertEquals("2026-11-07T18:45:00Z", Instant.ofEpochMilli(early.scheduledAt).toString())
        val late = early.copy(time = LocalTime.of(23,55))
        assertEquals(3_600_000L, late.expiresAt-late.scheduledAt)
    }
}
