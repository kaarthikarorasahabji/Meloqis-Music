package echo.music.iad1tya.ui.component

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.domain.mediaservice.handler.ControlState
import echo.music.iad1tya.domain.mediaservice.handler.RepeatState
import echo.music.iad1tya.ui.icon.Pause
import echo.music.iad1tya.ui.icon.PauseCircle
import echo.music.iad1tya.ui.icon.PlayArrow
import echo.music.iad1tya.ui.icon.PlayCircle
import echo.music.iad1tya.ui.icon.Repeat
import echo.music.iad1tya.ui.icon.RepeatOne
import echo.music.iad1tya.ui.icon.Shuffle
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.icon.SkipNext
import echo.music.iad1tya.ui.icon.SkipPrevious
import echo.music.iad1tya.ui.theme.seed
import echo.music.iad1tya.ui.theme.LocalBatterySaver
import echo.music.iad1tya.viewModel.UIEvent
import org.jetbrains.compose.resources.stringResource
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.shuffle
import echomusic.composeapp.generated.resources.repeat_off
import echomusic.composeapp.generated.resources.repeat_all
import echomusic.composeapp.generated.resources.repeat_one
import echomusic.composeapp.generated.resources.player_play
import echomusic.composeapp.generated.resources.player_pause
import echomusic.composeapp.generated.resources.player_previous
import echomusic.composeapp.generated.resources.player_next
import echomusic.composeapp.generated.resources.player_seek_back
import echomusic.composeapp.generated.resources.player_seek_forward

@Composable
fun PlayerControlLayout(
    controllerState: ControlState,
    isSmallSize: Boolean = false,
    // Bare ▶ / ⏸ glyphs instead of the disc-enclosed PlayCircle/PauseCircle pair.
    // The desktop capsule asks for these; Now Playing keeps the discs.
    plainPlayPause: Boolean = false,
    // The capsule already pads its own edges; stacking this 20dp on top of that
    // read as a hole at both ends of the transport cluster.
    horizontalPadding: Dp = 20.dp,
    // Tint for the ACTIVE shuffle/repeat state. The default keeps the raw seed (#8ECAE6) every
    // existing call site had; the capsule passes a theme-aware colour because pastel seed on a
    // light glass surface is nearly invisible.
    activeColor: Color = seed,
    contentColor: Color = Color.White,
    onUIEvent: (UIEvent) -> Unit,
) {
    val seekBackLabel = stringResource(Res.string.player_seek_back)
    val seekForwardLabel = stringResource(Res.string.player_seek_forward)
    val height = if (isSmallSize) 48.dp else 96.dp
    val smallIcon = if (isSmallSize) 20.dp to 28.dp else 32.dp to 42.dp
    val mediumIcon = if (isSmallSize) 28.dp to 38.dp else 42.dp to 52.dp
    val bigIcon = if (isSmallSize) 38.dp to 48.dp else 72.dp to 96.dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(height)
                .padding(horizontal = horizontalPadding),
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(smallIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .transportClick(onClick = {
                            onUIEvent(UIEvent.Shuffle)
                        }),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = controllerState.isShuffle, label = "Shuffle Button") { isShuffle ->
                    if (!isShuffle) {
                        Icon(
                            imageVector = echoIcons.Shuffle,
                            tint = contentColor,
                            contentDescription = stringResource(Res.string.shuffle),
                            modifier = Modifier.size(smallIcon.first),
                        )
                    } else {
                        Icon(
                            imageVector = echoIcons.Shuffle,
                            tint = activeColor,
                            contentDescription = stringResource(Res.string.shuffle),
                            modifier = Modifier.size(smallIcon.first),
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(mediumIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .transportClick(
                            onClick = {
                                if (controllerState.isPreviousAvailable) onUIEvent(UIEvent.Previous)
                            },
                            onLongClickLabel = seekBackLabel,
                            onLongClick = { onUIEvent(UIEvent.Backward) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = echoIcons.SkipPrevious,
                    tint = if (controllerState.isPreviousAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                    contentDescription = stringResource(Res.string.player_previous),
                    modifier = Modifier.size(mediumIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(bigIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .transportClick(onClick = {
                            onUIEvent(UIEvent.PlayPause)
                        }),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = controllerState.isPlaying) { isPlaying ->
                    if (!isPlaying) {
                        Icon(
                            imageVector = if (plainPlayPause) echoIcons.PlayArrow else echoIcons.PlayCircle,
                            tint = contentColor,
                            contentDescription = stringResource(Res.string.player_play),
                            modifier = Modifier.size(bigIcon.first),
                        )
                    } else {
                        Icon(
                            imageVector = if (plainPlayPause) echoIcons.Pause else echoIcons.PauseCircle,
                            tint = contentColor,
                            contentDescription = stringResource(Res.string.player_pause),
                            modifier = Modifier.size(bigIcon.first),
                        )
                    }
                }
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .background(Color.Transparent)
                        .size(mediumIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .transportClick(
                            onClick = {
                                if (controllerState.isNextAvailable) onUIEvent(UIEvent.Next)
                            },
                            onLongClickLabel = seekForwardLabel,
                            onLongClick = { onUIEvent(UIEvent.Forward) },
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = echoIcons.SkipNext,
                    tint = if (controllerState.isNextAvailable) contentColor else contentColor.copy(alpha = 0.4f),
                    contentDescription = stringResource(Res.string.player_next),
                    modifier = Modifier.size(mediumIcon.first),
                )
            }
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Box(
                modifier =
                    Modifier
                        .size(smallIcon.second)
                        .aspectRatio(1f)
                        .clip(
                            CircleShape,
                        )
                        .transportClick(onClick = {
                            onUIEvent(UIEvent.Repeat)
                        }),
                contentAlignment = Alignment.Center,
            ) {
                Crossfade(targetState = controllerState.repeatState) { rs ->
                    when (rs) {
                        is RepeatState.None -> {
                            Icon(
                                imageVector = echoIcons.Repeat,
                                tint = contentColor,
                                contentDescription = stringResource(Res.string.repeat_off),
                                modifier = Modifier.size(smallIcon.first),
                            )
                        }

                        RepeatState.All -> {
                            Icon(
                                imageVector = echoIcons.Repeat,
                                tint = activeColor,
                                contentDescription = stringResource(Res.string.repeat_all),
                                modifier = Modifier.size(smallIcon.first),
                            )
                        }

                        RepeatState.One -> {
                            Icon(
                                imageVector = echoIcons.RepeatOne,
                                tint = activeColor,
                                contentDescription = stringResource(Res.string.repeat_one),
                                modifier = Modifier.size(smallIcon.first),
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Brief press feedback; the scale updates a graphics layer without relaying out the controls. */
@Composable
private fun Modifier.transportClick(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null,
): Modifier {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale = animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = tween(if (LocalBatterySaver.current) 0 else 120),
        label = "transportPress",
    )
    return graphicsLayer {
        scaleX = scale.value
        scaleY = scale.value
    }.combinedClickable(
        interactionSource = interactions,
        indication = LocalIndication.current,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel,
    )
}
