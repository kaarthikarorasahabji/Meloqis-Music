package echo.music.iad1tya.data.dataStore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import echo.music.iad1tya.common.SETTINGS_FILENAME
import echo.music.iad1tya.data.io.getHomeFolderPath
import createDataStore
import java.io.File

actual fun createDataStoreInstance(): DataStore<Preferences> = createDataStore(
    producePath = {
        val file = File(getHomeFolderPath(listOf(".simpmusic")), "$SETTINGS_FILENAME.preferences_pb")
        file.absolutePath
    }
)