package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ListeningDesk(onSearch: () -> Unit, onLibrary: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom=16.dp),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
        OutlinedButton(onClick=onSearch,modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Text("Find a song") }
        OutlinedButton(onClick=onLibrary,modifier=Modifier.weight(1f).heightIn(min=48.dp)) { Text("Your collection") }
    }
}
@Composable expect fun rememberEdgeLightingLauncher(): () -> Unit
