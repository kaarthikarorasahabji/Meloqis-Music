package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import echo.music.iad1tya.ui.theme.LocalBatterySaver

/** Three native bars: animation state is read only during drawing, never by the song list. */
@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 18.dp,
) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    val animate = !LocalBatterySaver.current && lifecycleState.isAtLeast(Lifecycle.State.STARTED)
    val phase = if (animate) {
        rememberInfiniteTransition(label = "playingBars").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600, easing = LinearEasing), RepeatMode.Reverse),
            label = "barHeight",
        )
    } else {
        rememberUpdatedState(0.5f)
    }
    Canvas(modifier = modifier.size(size).semantics { contentDescription = "Playing" }) {
        val gap = this.size.width * 0.16f
        val barWidth = (this.size.width - gap * 2) / 3
        val progress = phase.value
        repeat(3) { index ->
            val fraction = when (index) {
                0 -> 0.3f + 0.65f * progress
                1 -> 0.95f - 0.55f * progress
                else -> 0.4f + 0.4f * progress
            }
            val barHeight = this.size.height * fraction
            drawRoundRect(
                color = color,
                topLeft = Offset(index * (barWidth + gap), this.size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
