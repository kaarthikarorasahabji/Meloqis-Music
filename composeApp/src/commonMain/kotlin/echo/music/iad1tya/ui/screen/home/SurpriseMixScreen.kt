package echo.music.iad1tya.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.ui.navigation.destination.search.SearchDestination
import echo.music.iad1tya.ui.component.SongFullWidthItems
import echo.music.iad1tya.viewModel.SurpriseMixViewModel
import echo.music.iad1tya.viewModel.SurpriseMode
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SurpriseMixScreen(navController: NavController, innerPadding: PaddingValues) {
    val viewModel: SurpriseMixViewModel = koinViewModel()
    val state by viewModel.state.collectAsState()
    val playing by viewModel.nowPlayingVideoId.collectAsState()
    LaunchedEffect(viewModel) { viewModel.start() }
    DisposableEffect(viewModel) { onDispose { viewModel.cancel() } }
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = innerPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { TextButton(onClick = { navController.popBackStack() }) { Text("Back") } }
        item { Text("Surprise Me", style = MaterialTheme.typography.headlineLarge) }
        if (!state.downloadsOnly) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SurpriseMode.entries.forEach { mode ->
                    FilterChip(selected = state.mode == mode, enabled = !state.loading,
                        onClick = { viewModel.mode(mode) }, label = { Text(mode.label) })
                }
            }
        }
        if (!state.downloadsOnly) item {
            Text(when (state.mode) {
                SurpriseMode.COMFORT -> "Mostly favourites, with a few discoveries."
                SurpriseMode.BALANCED -> "An even blend of favourites and discoveries."
                SurpriseMode.ADVENTUROUS -> "More discoveries, with familiar songs along the way."
            }, style = MaterialTheme.typography.bodyMedium)
        }
        item {
            Text("Uses your likes, downloads and listening history when enabled. New songs come from related recommendations; availability can change the balance.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()) {
                Text("Downloads only")
                Switch(checked = state.downloadsOnly, enabled = !state.loading, onCheckedChange = viewModel::downloadsOnly,
                    modifier = Modifier.semantics { contentDescription = "Downloads only" })
            }
        }
        if (state.message.isNotBlank()) item { Text(state.message, style = MaterialTheme.typography.bodyMedium) }
        if (!state.loading && state.picks.isEmpty() && !state.downloadsOnly) item {
            TextButton(onClick = { navController.navigate(SearchDestination) { launchSingleTop = true } }) {
                Text("Find a song")
            }
        }
        item {
            if (state.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(onClick = viewModel::cancel) { Text("Cancel") }
            } else {
                Button(onClick = viewModel::generate, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(if (state.picks.isEmpty()) "Surprise me" else "Surprise again")
                }
            }
        }
        itemsIndexed(state.picks, key = { _, pick -> pick.track.videoId }) { index, pick ->
            Column {
                Text(if (pick.familiar) "Familiar" else "Discovery", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
                SongFullWidthItems(track = pick.track, isPlaying = playing == pick.track.videoId,
                    modifier = Modifier.fillMaxWidth(), onClickListener = { if (!state.loading) viewModel.play(index) })
            }
        }
    }
}
