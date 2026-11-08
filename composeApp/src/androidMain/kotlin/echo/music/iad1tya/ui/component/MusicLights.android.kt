package echo.music.iad1tya.ui.component

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

class MusicLightsSettingsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary=Color(0xFFBCACFA), secondary=Color(0xFF8FE8D5), surface=Color(0xFF0C0D14))) {
                MusicLightsStudio(onClose = { finish() })
            }
        }
    }
}
