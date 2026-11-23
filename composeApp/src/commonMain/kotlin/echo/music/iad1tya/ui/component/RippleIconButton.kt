package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.ui.icon.Pause
import echo.music.iad1tya.ui.icon.PlayArrow
import echo.music.iad1tya.ui.icon.echoIcons
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.player_pause
import echomusic.composeapp.generated.resources.player_play
import org.jetbrains.compose.resources.stringResource

@Composable
fun RippleIconButton(
    imageVector: ImageVector,
    modifier: Modifier = Modifier,
    fillMaxSize: Boolean = false,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Icon(
            imageVector,
            null,
            tint = tint,
            modifier = if (fillMaxSize) Modifier.fillMaxSize().padding(4.dp) else Modifier,
        )
    }
}

@Composable
fun PlayPauseButton(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = if (isPlaying) echoIcons.Pause else echoIcons.PlayArrow,
            contentDescription = stringResource(if (isPlaying) Res.string.player_pause else Res.string.player_play),
            tint = tint,
        )
    }
}
