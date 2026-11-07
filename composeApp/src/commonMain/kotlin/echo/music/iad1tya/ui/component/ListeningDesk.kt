package echo.music.iad1tya.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight

/** Shortcuts stay visible beside discovery, without hiding tools in the audio equalizer. */
@Composable
fun ListeningDesk(onSearch: () -> Unit, onLibrary: () -> Unit) {
    val lights = rememberEdgeLightingLauncher()
    val colors = MaterialTheme.colorScheme
    Column(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onSearch, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Find a song") }
            OutlinedButton(onClick = onLibrary, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Your collection") }
        }
        Surface(onClick = lights, shape = RoundedCornerShape(24.dp), color = colors.surfaceContainerHigh) {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(colors.primary.copy(alpha = .14f), colors.tertiary.copy(alpha = .08f))))
                    .border(1.dp, colors.primary.copy(alpha = .18f), RoundedCornerShape(24.dp))
                    .padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("LIGHT, IN YOUR RHYTHM", color = colors.primary, fontSize = 11.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold)
                Text("Give your music an edge.", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
                Text("Explore edge lighting, colors & clock displays", style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                Text("Open your lighting studio  →", style = MaterialTheme.typography.labelLarge, color = colors.primary, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable expect fun rememberEdgeLightingLauncher(): () -> Unit
