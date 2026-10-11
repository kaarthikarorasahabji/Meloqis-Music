package echo.music.iad1tya.ui.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import echo.music.iad1tya.ui.icon.PlayArrow
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.theme.LocalBatterySaver
import echo.music.iad1tya.ui.theme.typo
import kotlin.math.min
import echo.music.iad1tya.ui.component.upscaleThumbUrl

/** A featured listen, presented as a manually browsed Meloqis vinyl room. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ListeningRoom(
    items: List<HeroCarouselItem>,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
) {
    if (items.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { items.size })
    val surface = MaterialTheme.colorScheme.surfaceContainerHigh
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateAsState()

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "THE LISTENING ROOM",
                    color = accentColor,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Picked for this moment",
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            Text(
                text = "%02d / %02d".format(pagerState.currentPage + 1, items.size),
                style = typo().labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(0.dp),
            pageSpacing = 10.dp,
            userScrollEnabled = items.size > 1,
            modifier = Modifier.fillMaxWidth().height(188.dp),
        ) { page ->
            val item = items[page]
            val playingHere = page == pagerState.currentPage && item.isPlaying
            val animateVinyl =
                playingHere && !LocalBatterySaver.current && lifecycleState.isAtLeast(Lifecycle.State.STARTED)
            val rotation =
                if (animateVinyl) {
                    rememberInfiniteTransition(label = "listeningRoomVinyl").animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(12_000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart,
                        ),
                        label = "vinylRotation",
                    )
                } else {
                    rememberUpdatedState(0f)
                }

            BoxWithConstraints(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.2f),
                                    surface,
                                    MaterialTheme.colorScheme.surface,
                                ),
                            ),
                        ).border(
                            width = 1.dp,
                            color = accentColor.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(28.dp),
                        ).clickable { item.onClick() }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                val artworkSize = maxHeight.coerceAtMost(maxWidth * 0.43f)
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VinylArtwork(
                        thumbnailUrl = item.thumbnailUrl,
                        title = item.title,
                        accentColor = accentColor,
                        rotation = rotation.value,
                        modifier = Modifier.width(artworkSize).aspectRatio(1f),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text = if (playingHere) "IN YOUR ROTATION" else "FROM YOUR MIXES",
                            color = accentColor,
                            fontSize = 9.sp,
                            letterSpacing = 1.1.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                        )
                        Text(
                            text = item.title,
                            style = typo().titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 5.dp),
                        )
                        if (!item.subtitle.isNullOrBlank()) {
                            Text(
                                text = item.subtitle,
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                        Row(
                            modifier = Modifier.padding(top = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(27.dp).clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.19f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (playingHere) {
                                    EqualizerBars(color = accentColor, size = 14.dp)
                                } else {
                                    Icon(
                                        imageVector = echoIcons.PlayArrow,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(19.dp),
                                    )
                                }
                            }
                            Text(
                                text = if (playingHere) "NOW PLAYING" else "TUNE IN",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 9.sp,
                                letterSpacing = 0.8.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 7.dp),
                            )
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 9.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(items.size) { index ->
                Box(
                    modifier = Modifier.padding(horizontal = 2.dp).width(if (pagerState.currentPage == index) 16.dp else 5.dp)
                        .height(3.dp).clip(CircleShape)
                        .background(if (pagerState.currentPage == index) accentColor else accentColor.copy(alpha = 0.28f)),
                )
            }
        }
    }
}

@Composable
private fun VinylArtwork(
    thumbnailUrl: String?,
    title: String,
    accentColor: Color,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation },
        ) {
            val diameter = min(size.width, size.height)
            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
            listOf(0.96f, 0.84f, 0.72f).forEach { fraction ->
                drawCircle(
                    color = accentColor.copy(alpha = 0.22f),
                    radius = diameter * fraction / 2f,
                    center = center,
                    style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(upscaleThumbUrl(thumbnailUrl))
                .diskCachePolicy(CachePolicy.ENABLED)
                .diskCacheKey(thumbnailUrl)
                .crossfade(300)
                .build(),
            contentDescription = title,
            placeholder = rememberHolderPainter(),
            error = rememberHolderPainter(),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(0.72f).clip(CircleShape)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f), CircleShape),
        )
    }
}
