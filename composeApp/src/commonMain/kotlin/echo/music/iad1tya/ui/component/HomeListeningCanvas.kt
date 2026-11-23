package echo.music.iad1tya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import echo.music.iad1tya.domain.data.entities.SongEntity
import echo.music.iad1tya.domain.mediaservice.handler.NowPlayingTrackState
import echo.music.iad1tya.expect.ui.MediaPlayerView
import echo.music.iad1tya.ui.icon.Pause
import echo.music.iad1tya.ui.icon.PlayArrow
import echo.music.iad1tya.ui.icon.Search
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.viewModel.NowPlayingScreenData
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.player_pause
import echomusic.composeapp.generated.resources.player_play
import echomusic.composeapp.generated.resources.search
import echomusic.composeapp.generated.resources.your_recently_played
import org.jetbrains.compose.resources.stringResource

/** The top of Home works with the playback session and the on-device library. */
@Composable
fun HomeListeningCanvas(
    currentTrack: NowPlayingTrackState?,
    playerArtwork: NowPlayingScreenData,
    isPlaying: Boolean,
    motionAllowed: Boolean,
    recentSongs: List<SongEntity>,
    onOpenPlayer: () -> Unit,
    onPlayPause: () -> Unit,
    onPlaySong: (SongEntity) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val current = currentTrack?.songEntity
    val last = recentSongs.firstOrNull()
    val featured = current ?: last
    Column(modifier.fillMaxWidth().padding(top = 8.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (featured != null) {
            val canvas = playerArtwork.canvasData.takeIf { current?.title == playerArtwork.nowPlayingTitle }
            val featuredClick = if (current != null) onOpenPlayer else { { onPlaySong(featured) } }
            val shape = RoundedCornerShape(26.dp)
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(max = 340.dp).aspectRatio(1.6f)
                    .clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClickLabel = if (current != null) "Open player" else "Play ${featured.title}", onClick = featuredClick),
                contentAlignment = Alignment.BottomCenter,
            ) {
                val artwork = canvas?.takeUnless { it.isVideo }?.url
                    ?: current?.canvasThumbUrl
                    ?: featured.thumbnails
                AsyncImage(
                    model = artwork,
                    contentDescription = null,
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (motionAllowed && isPlaying && canvas?.isVideo == true) {
                    MediaPlayerView(url = canvas.url, modifier = Modifier.fillMaxSize(), cropToBounds = true)
                }
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.48f), Color.Black.copy(alpha = 0.84f))),
                    ),
                )
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xD924202C),
                    contentColor = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                ) {
                    Row(
                        modifier = Modifier.padding(start = 14.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(if (current != null) "Now playing" else "Last played", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD8CFEB))
                            Text(featured.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            featured.artistName.orEmpty().joinToString(", ").takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodySmall, color = Color(0xFFD8CFEB), maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFE7D8FF), contentColor = Color(0xFF291D3A)) {
                            IconButton(onClick = if (current != null) onPlayPause else featuredClick) {
                                Icon(
                                    if (current != null && isPlaying) echoIcons.Pause else echoIcons.PlayArrow,
                                    contentDescription = stringResource(if (current != null && isPlaying) Res.string.player_pause else Res.string.player_play),
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                onClick = onSearch,
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(echoIcons.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Find something to play", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        val shelf = recentSongs.filter { it.videoId != featured?.videoId }.take(10)
        if (shelf.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(stringResource(Res.string.your_recently_played), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(shelf, key = { it.videoId }) { song ->
                        Column(
                            Modifier.width(118.dp).clip(RoundedCornerShape(14.dp))
                                .clickable(onClickLabel = "Play ${song.title}") { onPlaySong(song) },
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            AsyncImage(
                                model = song.canvasThumbUrl ?: song.thumbnails,
                                contentDescription = null,
                                placeholder = rememberHolderPainter(),
                                error = rememberHolderPainter(),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(118.dp).clip(RoundedCornerShape(14.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainer),
                            )
                            Text(song.title, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
