package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import echo.music.iad1tya.domain.data.entities.SongEntity
import echo.music.iad1tya.viewModel.PlaybackMood
import org.jetbrains.compose.resources.stringResource

internal val PlaybackMood.accent: Color
    get() = when (this) {
        PlaybackMood.Workout -> Color(0xFFF97562)
        PlaybackMood.FeelGood -> Color(0xFFF8B65A)
        PlaybackMood.Energize -> Color(0xFFF19143)
        PlaybackMood.Relax -> Color(0xFF51BBA5)
        PlaybackMood.Focus -> Color(0xFF71A9ED)
        PlaybackMood.Sleep -> Color(0xFFA69AF0)
        PlaybackMood.Sad -> Color(0xFF8F9DB8)
        PlaybackMood.Romance -> Color(0xFFEA8DAD)
        PlaybackMood.Party -> Color(0xFFD283EA)
        PlaybackMood.Commute -> Color(0xFF71B5D5)
    }

@Composable
internal fun PlaybackMoodRow(
    mood: PlaybackMood?,
    onChoose: () -> Unit,
    onBrowse: (PlaybackMood) -> Unit,
) {
    val tint = mood?.accent ?: MaterialTheme.colorScheme.primary
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = tint.copy(alpha = 0.13f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Song mood", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
            AssistChip(
                onClick = onChoose,
                label = { Text(mood?.let { stringResource(it.label) } ?: "Set mood") },
            )
            if (mood != null) TextButton(onClick = { onBrowse(mood) }) { Text("Browse mix") }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChoosePlaybackMoodDialog(
    activeMood: PlaybackMood?,
    onDismiss: () -> Unit,
    onSelect: (PlaybackMood?) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set song mood") },
        text = {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PlaybackMood.entries.forEach { mood ->
                    FilterChip(
                        selected = mood == activeMood,
                        onClick = { onSelect(mood) },
                        label = { Text(stringResource(mood.label)) },
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSelect(null) }) { Text("Follow source") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
internal fun CreateHomePlaylistDialog(
    currentSong: SongEntity?,
    onDismiss: () -> Unit,
    onCreate: (String, SongEntity?, (Boolean) -> Unit) -> Unit,
) {
    var title by rememberSaveable { mutableStateOf("") }
    var includeSong by rememberSaveable { mutableStateOf(currentSong != null) }
    var saving by remember { mutableStateOf(false) }
    var creationFailed by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Create playlist") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !saving,
                )
                if (currentSong != null) {
                    Row(
                        Modifier.fillMaxWidth().toggleable(
                            value = includeSong,
                            enabled = !saving,
                            role = Role.Checkbox,
                            onValueChange = { includeSong = it },
                        ).padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = includeSong, onCheckedChange = null)
                        Text("Add ${currentSong.title}", maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (creationFailed) Text("Couldn’t create playlist. Try again.", color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(
                enabled = title.isNotBlank() && !saving,
                onClick = {
                    saving = true
                    creationFailed = false
                    onCreate(title.trim(), currentSong.takeIf { includeSong }) { created ->
                        saving = false
                        creationFailed = !created
                        if (created) onDismiss()
                    }
                },
            ) { Text(if (saving) "Creating…" else "Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}
