package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import echo.music.iad1tya.ui.theme.LocalBatterySaver
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.developed_credit
import org.jetbrains.compose.resources.stringResource

/**
 * App footer: "Developed with ❤️ BY Kaarthik Dass Arora Sahab Ji", rendered centered with the credit
 * sweeping slowly through the brand palette. Tapping it opens the developer site via [onClick].
 * Battery Saver pins the sweep to a static gradient (no infinite transition is created).
 */
@Composable
fun MeloqisFooter(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val credit = stringResource(Res.string.developed_credit)
    val cs = MaterialTheme.colorScheme
    val brandColors = listOf(cs.primary, cs.tertiary, cs.secondary, cs.primary)

    val sweep: Float =
        if (LocalBatterySaver.current) {
            0f
        } else {
            val transition = rememberInfiniteTransition(label = "footerSweep")
            transition
                .animateFloat(
                    initialValue = 0f,
                    targetValue = 1f,
                    animationSpec =
                        infiniteRepeatable(
                            animation = tween(4200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                    label = "footerSweepPhase",
                ).value
        }

    // Slide the gradient window across the text so the brand colours travel through the credit.
    val brush =
        Brush.linearGradient(
            colors = brandColors,
            start = Offset(sweep * 700f - 350f, 0f),
            end = Offset(sweep * 700f + 350f, 0f),
        )

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 24.dp, vertical = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = credit,
            style =
                TextStyle(
                    brush = brush,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                ),
        )
        Text(
            text = "axenoraai.in",
            style =
                MaterialTheme.typography.labelSmall.copy(
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                ),
        )
    }
}
