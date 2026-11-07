package echo.music.iad1tya.ui.component

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReleaseVersionTest {
    @Test fun numericOrdering() {
        assertEquals(-1, compareReleaseVersions("1.9.0", "1.10.0"))
        assertEquals(1, compareReleaseVersions("2.0.0", "1.99.99"))
        assertEquals(0, compareReleaseVersions("v1.9.0", "1.9.0"))
    }
    @Test fun invalidPoliciesCannotBlock() {
        listOf("", "nightly", "1.9", "1.9.0-beta", "999999999.0.0").forEach { assertNull(compareReleaseVersions("1.9.0", it)) }
    }
}
