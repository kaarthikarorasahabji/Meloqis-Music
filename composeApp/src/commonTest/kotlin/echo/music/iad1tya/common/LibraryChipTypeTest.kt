package echo.music.iad1tya.common

import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryChipTypeTest {
    @Test
    fun unavailablePersistedTabsFallBackToLibraryHome() {
        assertEquals(LibraryChipType.YOUR_LIBRARY, LibraryChipType.CHART.availableLibraryTab(isLoggedIn = true))
        assertEquals(LibraryChipType.YOUR_LIBRARY, LibraryChipType.YOUTUBE_MIX_FOR_YOU.availableLibraryTab(isLoggedIn = true))
        assertEquals(LibraryChipType.YOUR_LIBRARY, LibraryChipType.YOUTUBE_MUSIC_PLAYLIST.availableLibraryTab(isLoggedIn = false))
    }

    @Test
    fun availableTabsRemainSelected() {
        assertEquals(LibraryChipType.LOCAL_PLAYLIST, LibraryChipType.LOCAL_PLAYLIST.availableLibraryTab(isLoggedIn = false))
        assertEquals(LibraryChipType.YOUTUBE_MUSIC_PLAYLIST, LibraryChipType.YOUTUBE_MUSIC_PLAYLIST.availableLibraryTab(isLoggedIn = true))
    }
}
