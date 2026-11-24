package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeAccountsSheet(
    youtubeConnected: Boolean,
    spotifyConnected: Boolean,
    onDismiss: () -> Unit,
    onConnectYouTube: () -> Unit,
    onConnectSpotify: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Music accounts", style = MaterialTheme.typography.headlineSmall)
            AccountRow(
                title = "YouTube Music",
                subtitle = "Your listening history and mixes",
                connected = youtubeConnected,
                onClick = if (youtubeConnected) onOpenSettings else onConnectYouTube,
            )
            AccountRow(
                title = "Spotify",
                subtitle = "Spotify lyrics and Canvas",
                connected = spotifyConnected,
                onClick = if (spotifyConnected) onOpenSettings else onConnectSpotify,
            )
            HorizontalDivider()
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(onClick = onOpenSettings) { Text("Settings") }
                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

@Composable
private fun AccountRow(title: String, subtitle: String, connected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                if (connected) "Connected" else subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        val action = if (connected) "Manage" else "Connect"
        TextButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = "$action $title" },
        ) { Text(action) }
    }
}
