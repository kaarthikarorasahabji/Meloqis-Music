package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.mono
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

/**
 * Meloqis Music startup animation: a full-screen brand-gradient overlay with a
 * spring-scaled note glyph and wordmark that fades out to reveal the app.
 * Self-contained (own colours) so it works regardless of the resolved theme.
 */
@Composable
fun MeloqisSplashOverlay(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.68f) }
    val contentAlpha = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }

    LaunchedEffectOnce {
        launch {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
            )
        }
        contentAlpha.animateTo(1f, tween(600))
        delay(1000)
        overlayAlpha.animateTo(0f, tween(520))
        onFinished()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = overlayAlpha.value }
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFFFF6A3D),
                            Color(0xFFFA2D48),
                            Color(0xFFFF2D78),
                        ),
                    ),
                ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.mono),
                contentDescription = "Meloqis Music",
                modifier =
                    Modifier
                        .size(128.dp)
                        .graphicsLayer {
                            scaleX = scale.value
                            scaleY = scale.value
                            alpha = contentAlpha.value
                        },
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "Meloqis Music",
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha.value },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "feel every beat",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer { alpha = contentAlpha.value },
            )
        }

        // Developer credit tag, pinned to the bottom during loading.
        Text(
            text = "developed by Kaarthik Dass Arora Sahab Ji",
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 36.dp)
                    .graphicsLayer { alpha = contentAlpha.value },
        )
    }
}

/** Small helper so the animation sequence runs exactly once per mount. */
@Composable
private fun LaunchedEffectOnce(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) { block() }
}
