package echo.music.iad1tya.ui.component

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

@Composable actual fun rememberEdgeLightingLauncher(): () -> Unit {
    val context = LocalContext.current
    return remember(context) { { context.startActivity(Intent(context, MusicLightsSettingsActivity::class.java)) } }
}
