package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.ui.theme.LocalBatterySaver
import io.github.alexzhirkevich.compottie.Compottie
import io.github.alexzhirkevich.compottie.LottieCompositionSpec
import io.github.alexzhirkevich.compottie.rememberLottieComposition
import io.github.alexzhirkevich.compottie.rememberLottiePainter
import echomusic.composeapp.generated.resources.Res

/**
 * A tiny "now playing" equaliser. Reuses the same Lottie audio-bars asset the full-width rows use
 * ([FullWidthItems]) so the animation is consistent across the app. Meant to sit beside a title
 * (e.g. the "Meloqis Music" header) and be shown only while a track is actually playing.
 *
 * Battery Saver: continuous Lottie clocks are gated app-wide, so under Battery Saver this renders a
 * cheap static 3-bar glyph instead of the animated painter (same rule as the rest of the UI).
 */
@Composable
fun EqualizerBars(
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    size: Dp = 18.dp,
) {
    if (LocalBatterySaver.current) {
        StaticEqualizerGlyph(modifier = modifier.size(size), color = color)
        return
    }

    val composition by rememberLottieComposition {
        LottieCompositionSpec.JsonString(
            Res.readBytes("files/audio_playing_animation.json").decodeToString(),
        )
    }
    Image(
        painter =
            rememberLottiePainter(
                composition = composition,
                iterations = Compottie.IterateForever,
            ),
        contentDescription = "Playing",
        colorFilter = ColorFilter.tint(color),
        modifier = modifier.size(size),
    )
}

/** Static fallback: three rounded bars at fixed heights. No animation clock. */
@Composable
private fun StaticEqualizerGlyph(
    modifier: Modifier,
    color: Color,
) {
    val heights = listOf(0.5f, 1f, 0.68f)
    Canvas(modifier = modifier) {
        val gap = size.width * 0.16f
        val barWidth = (size.width - gap * (heights.size - 1)) / heights.size
        heights.forEachIndexed { i, h ->
            val barHeight = size.height * h
            drawRoundRect(
                color = color,
                topLeft = Offset(i * (barWidth + gap), size.height - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
}
