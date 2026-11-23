package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.domain.data.model.searchResult.playlists.PlaylistsResult
import echo.music.iad1tya.viewModel.YouTubeAccountContent

@Composable
internal fun PersonalYouTubeMixes(
    content: YouTubeAccountContent<PlaylistsResult>,
    onRetry: () -> Unit,
    onOpen: (PlaylistsResult) -> Unit,
) {
    if (content == YouTubeAccountContent.SignedOut) return

    Column(Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            "Mixes for you",
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.headlineSmall,
        )
        when (content) {
            YouTubeAccountContent.SignedOut -> Unit
            YouTubeAccountContent.Loading -> LinearProgressIndicator(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            )
            YouTubeAccountContent.Unavailable -> Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Couldn’t load your YouTube Music mixes.", Modifier.weight(1f))
                TextButton(onClick = onRetry) { Text("Retry") }
            }
            is YouTubeAccountContent.Loaded -> {
                if (content.items.isEmpty()) {
                    Text(
                        "YouTube Music hasn’t made any mixes for this account yet.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyRow(contentPadding = PaddingValues(horizontal = 6.dp)) {
                        items(content.items, key = { it.browseId }) { mix ->
                            HomeItemContentPlaylist(onClick = { onOpen(mix) }, data = mix, thumbSize = 136.dp)
                        }
                    }
                }
            }
        }
    }
}
