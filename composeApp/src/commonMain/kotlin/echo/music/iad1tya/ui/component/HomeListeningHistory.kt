package echo.music.iad1tya.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.ui.icon.PlayArrow
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.viewModel.YouTubeAccountContent

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeListeningHistory(
    history: YouTubeAccountContent<Track>,
    localTracks: List<Track>,
    currentTrackId: String?,
    onPlay: (Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    onConnect: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val accountTracks = (history as? YouTubeAccountContent.Loaded)?.items.orEmpty()
    val isAccountHistory = accountTracks.isNotEmpty()
    val hasLocalHistory = localTracks.isNotEmpty()
    val tracks = remember(accountTracks, localTracks) { mergeListeningHistory(localTracks, accountTracks) }
    if (history == YouTubeAccountContent.SignedOut && tracks.isEmpty()) {
        Surface(
            modifier = modifier.fillMaxWidth().padding(top = 12.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Make Home yours", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Connect YouTube Music for your listening history and mixes.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onConnect) { Text("Connect YouTube Music") }
            }
        }
        return
    }
    Column(
        modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Listen again", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (tracks.isNotEmpty()) {
                    Text(
                        when {
                            isAccountHistory && hasLocalHistory -> "Meloqis and YouTube Music"
                            isAccountHistory -> "YouTube Music history"
                            else -> "On this device"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (tracks.size > 1) {
                OutlinedButton(onClick = { onPlayAll(tracks) }) { Text("Play all") }
            }
        }

        when (history) {
            YouTubeAccountContent.Loading -> {
                if (tracks.isEmpty()) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            YouTubeAccountContent.SignedOut -> {
                TextButton(onClick = onConnect) { Text("Connect YouTube Music") }
            }
            YouTubeAccountContent.Unavailable -> {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("YouTube Music history is unavailable.", Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = onRetry) { Text("Retry") }
                }
            }
            is YouTubeAccountContent.Loaded -> {
                if (accountTracks.isEmpty()) {
                    Text("No YouTube Music tracks to show yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

        if (tracks.isNotEmpty()) {
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val columns = when {
                    maxWidth >= 600.dp -> 5
                    maxWidth < 260.dp -> 2
                    else -> 3
                }
                key(isAccountHistory, columns) {
                    HistoryGrid(
                        tracks = tracks,
                        columns = columns,
                        rows = if (columns == 5) 2 else 3,
                        currentTrackId = currentTrackId,
                        onPlay = onPlay,
                    )
                }
            }
        }
    }
}

internal fun mergeListeningHistory(localTracks: List<Track>, accountTracks: List<Track>): List<Track> =
    buildList {
        repeat(maxOf(localTracks.size, accountTracks.size)) { index ->
            localTracks.getOrNull(index)?.let { add(it) }
            accountTracks.getOrNull(index)?.let { add(it) }
        }
    }.distinctBy(Track::videoId)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HistoryGrid(
    tracks: List<Track>,
    columns: Int,
    rows: Int,
    currentTrackId: String?,
    onPlay: (Track) -> Unit,
) {
    val pages = remember(tracks, columns, rows) { tracks.chunked(columns * rows) }
    val pager = rememberPagerState(pageCount = { pages.size })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalPager(state = pager, pageSpacing = 14.dp) { page ->
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                pages[page].chunked(columns).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { track ->
                            HistoryCover(track, track.videoId == currentTrackId, Modifier.weight(1f)) { onPlay(track) }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        if (pages.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                repeat(pages.size) { index ->
                    Box(
                        Modifier.size(if (index == pager.currentPage) 8.dp else 5.dp)
                            .clip(CircleShape)
                            .background(
                                if (index == pager.currentPage) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryCover(track: Track, current: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(9.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = if (current) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(1.21f)) {
                AsyncImage(
                    model = track.thumbnails?.lastOrNull()?.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    placeholder = rememberHolderPainter(),
                    error = rememberHolderPainter(),
                    contentScale = ContentScale.Crop,
                )
                if (current) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ) {
                        Icon(echoIcons.PlayArrow, contentDescription = "Current song", modifier = Modifier.padding(5.dp).size(14.dp))
                    }
                }
            }
            Text(
                track.title,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 6.dp),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
