package echo.music.iad1tya.ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import echo.music.iad1tya.extension.artworkScrimBrush
import echo.music.iad1tya.ui.theme.LocalBatterySaver
import echo.music.iad1tya.ui.theme.typo
import kotlinx.coroutines.delay
import kotlin.math.absoluteValue

/** One card in the [HeroCarousel]. Tap wiring is supplied by the caller via [onClick]. */
data class HeroCarouselItem(
    val id: String,
    val title: String,
    val subtitle: String?,
    val thumbnailUrl: String?,
    val onClick: () -> Unit,
)

/**
 * A running, edge-peeking hero carousel of large artwork cards for the top of Home. The focused
 * card is full-size; neighbours shrink slightly for depth. It auto-advances (~every 4.5s) and loops.
 *
 * Battery Saver: the auto-advance ticker is a continuous clock, so it is NOT launched under Battery
 * Saver — the carousel stays fully swipeable, just doesn't advance on its own (same gating rule the
 * rest of the UI follows via [LocalBatterySaver]).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    items: List<HeroCarouselItem>,
    modifier: Modifier = Modifier,
    accentColor: Color = Color.White,
) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })
    val batterySaver = LocalBatterySaver.current

    if (!batterySaver && items.size > 1) {
        LaunchedEffect(pagerState, items.size) {
            while (true) {
                delay(4500)
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Column(modifier) {
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 8.dp),
            pageSpacing = 14.dp,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(204.dp),
        ) { page ->
            val item = items[page]
            // Depth peek: the focused page renders at full scale, neighbours ease down to 0.9.
            val offset =
                ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                    .absoluteValue
                    .coerceIn(0f, 1f)
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val scale = lerp(0.9f, 1f, 1f - offset)
                            scaleX = scale
                            scaleY = scale
                        }.clip(RoundedCornerShape(24.dp))
                        .clickable { item.onClick() },
            ) {
                AsyncImage(
                    model =
                        ImageRequest
                            .Builder(LocalPlatformContext.current)
                            .data(upscaleThumbUrl(item.thumbnailUrl))
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(item.thumbnailUrl)
                            .crossfade(550)
                            .build(),
                    contentDescription = item.title,
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                // Bottom scrim so the title stays legible on any artwork.
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .align(Alignment.BottomCenter)
                            .background(artworkScrimBrush(Color.Black)),
                )
                Column(
                    modifier =
                        Modifier
                            .align(Alignment.BottomStart)
                            .padding(16.dp),
                ) {
                    if (!item.subtitle.isNullOrBlank()) {
                        Text(
                            text = item.subtitle,
                            style = typo().bodySmall,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = item.title,
                        style = typo().titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        // Page indicator dots.
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(items.size) { i ->
                val selected = pagerState.currentPage == i
                Box(
                    modifier =
                        Modifier
                            .padding(horizontal = 3.dp)
                            .size(if (selected) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) accentColor else accentColor.copy(alpha = 0.3f),
                            ),
                )
            }
        }
    }
}

/**
 * YouTube-Music thumbnails come back small (e.g. w120/w226). For a full-bleed hero card we ask for a
 * larger square. Best-effort string rewrite — falls back to the original URL when the shape is
 * unfamiliar.
 */
private fun upscaleThumbUrl(url: String?): String? {
    if (url.isNullOrBlank()) return url
    return when {
        Regex("[wh]\\d{2,4}").containsMatchIn(url) -> Regex("([wh])\\d{2,4}").replace(url, "$1544")
        Regex("=w\\d+-h\\d+").containsMatchIn(url) -> Regex("=w\\d+-h\\d+").replace(url, "=w544-h544")
        else -> url
    }
}
