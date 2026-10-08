package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import echo.music.iad1tya.ui.icon.Download
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.theme.LocalBatterySaver

/** Native download feedback without parsing an animation file for every screen. */
@Composable
fun DownloadActivityIndicator(size: Dp = 28.dp) {
    val state by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    if (LocalBatterySaver.current || !state.isAtLeast(Lifecycle.State.STARTED)) {
        Icon(echoIcons.Download, contentDescription = "Downloading", tint = Color.White, modifier = Modifier.size(size))
    } else {
        CircularProgressIndicator(modifier = Modifier.size(size), color = Color.White, strokeWidth = 2.dp)
    }
}
