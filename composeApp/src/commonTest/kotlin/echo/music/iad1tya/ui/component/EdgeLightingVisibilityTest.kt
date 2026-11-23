package echo.music.iad1tya.ui.component

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EdgeLightingVisibilityTest {
    @Test fun chargingWakeBehindLockScreenIsHidden() {
        assertFalse(edgeLightingVisible(true,true,true,true,false,false))
        assertFalse(edgeLightingVisible(true,true,true,true,true,false))
    }
    @Test fun screenOffBackgroundAndLostFocusAreHidden() {
        assertFalse(edgeLightingVisible(true,true,false,false,true,false))
        assertFalse(edgeLightingVisible(true,false,true,false,true,false))
        assertFalse(edgeLightingVisible(true,true,true,false,false,false))
        assertFalse(edgeLightingVisible(false,true,true,false,true,false))
    }
    @Test fun pluggedInPausesLightingEvenInTheForeground() {
        assertFalse(edgeLightingVisible(true, true, true, false, true, true))
    }
    @Test fun unlockedFocusedAppCanShowLights() {
        assertTrue(edgeLightingVisible(true,true,true,false,true,false))
    }
}
