package echo.music.iad1tya.ui.component

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import echo.music.iad1tya.domain.mediaservice.player.AudioSpectrum
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
actual fun EdgeLightingControls() {
    val context = LocalContext.current
    Card(onClick = { context.startActivity(Intent(context, MusicLightsSettingsActivity::class.java)) }, modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF171326)), shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF242039), Color(0xFF101A26)))).padding(24.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("MELOQIS EDGE STUDIO", color = Color(0xFFBCACFA), style = MaterialTheme.typography.labelMedium)
            Text("Edge lighting", color = Color.White, style = MaterialTheme.typography.headlineSmall)
            Text("Edge effects, music visualizers & your own clock display.", color = Color(0xFFC7C1D5))
            Text("Create your glow  →", color = Color(0xFF8FE8D5), style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun MusicLightsStudio(onClose: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { LightsPreferences.prefs(context) }
    var settings by remember { mutableStateOf(LightsPreferences.read(prefs)) }
    var tab by remember { mutableIntStateOf(0) }
    var demo by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var guide by remember { mutableStateOf(!prefs.getBoolean("guide_seen_v2", false)) }
    val overlayRunning by MusicLightsOverlayState.running.collectAsStateWithLifecycle()
    var hasAudio by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (isActive) {
            val frame = AudioSpectrum.frames.value
            hasAudio = frame.capturedAt > 0 && android.os.SystemClock.elapsedRealtime() - frame.capturedAt < 500
            delay(500)
        }
    }
    fun update(value: LightsSettings) { settings = value; LightsPreferences.save(prefs, value) }
    fun startOverlay() {
        if (settings.powerSaver && context.getSystemService(android.os.PowerManager::class.java).isPowerSaveMode) {
            error = "Battery saver is on. Turn it off, or change the battery-saver limit in Behavior."
            return
        }
        try { ContextCompat.startForegroundService(context, Intent(context, MusicLightsOverlayService::class.java)); error = null }
        catch (_: Exception) { error = "Couldn't start the floating display. Keep Meloqis open and try again." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(context)) startOverlay()
        else error = "Floating lights need display-over-other-apps permission. The clock display works without it."
    }
    Surface(Modifier.fillMaxSize(), color = Color(0xFF0C0D14), contentColor = Color(0xFFECE8F7)) {
        Column(Modifier.safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MUSIC LIGHTS", color = Color(0xFFBCACFA), style = MaterialTheme.typography.labelSmall)
                    Text("Edge lighting", style = MaterialTheme.typography.headlineSmall)
                }
                TextButton(onClick = { guide = true }) { Text("Guide", color = Color(0xFF8FE8D5)) }
                TextButton(onClick = onClose) { Text("Done", color = Color(0xFFBCACFA)) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
                    AndroidView(factory = { MusicLightsView(it, immersive = true, preview = true) },
                        update = { it.demo = demo; it.invalidate() },
                        modifier = Modifier.width(168.dp).fillMaxHeight().clip(RoundedCornerShape(26.dp)))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (hasAudio) "●  LIVE MELOQIS AUDIO" else if (demo) "●  DEMO PREVIEW" else "○  WAITING FOR MUSIC",
                        color = if (hasAudio) Color(0xFF8FE8D5) else Color(0xFFBCACFA), style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                    Text("Demo", style = MaterialTheme.typography.bodySmall)
                    Switch(demo, { demo = it })
                }
                Text("Demo only animates this preview. Real displays respond to Meloqis playback; ambient mode keeps a gentle glow between songs.", color = Color(0xFFA9A3B9), style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Design", "Fit", "Clock", "Behavior").forEachIndexed { index, name ->
                        FilterChip(tab == index, { tab = index }, label = { Text(name) })
                    }
                }
                when (tab) {
                    0 -> {
                        StudioSection("Start with a mood")
                        StudioChoices(LightsPreferences.presets, "", 2) { update(LightsPreferences.preset(it, settings)) }
                        StudioSection("Effect")
                        StudioChoices(LightsPreferences.modes, settings.mode, 3) {
                            update(settings.copy(mode = it, position = if (it == "Orbit") "Center" else settings.position))
                        }
                        StudioSection("Colour story")
                        StudioPalettes(settings) { update(settings.copy(palette = it)) }
                        if (settings.palette == "Album art") Text("Colours follow the current cover. Aurora is used when artwork is unavailable.", style = MaterialTheme.typography.bodySmall)
                        if (settings.palette == "Custom") {
                            StudioSlider("First hue", settings.hue, 0f..360f, "${settings.hue.roundToInt()}°") { update(settings.copy(hue = it)) }
                            StudioSlider("Second hue", settings.secondHue, 0f..360f, "${settings.secondHue.roundToInt()}°") { update(settings.copy(secondHue = it)) }
                        }
                        StudioSlider("Light width", settings.thickness, 1f..12f, "${settings.thickness.roundToInt()} dp") { update(settings.copy(thickness = it)) }
                        StudioSlider("Soft glow", settings.glow, 0f..1f) { update(settings.copy(glow = it)) }
                        StudioSlider("Motion speed", settings.speed, .2f..2f, "${(settings.speed * 100).roundToInt()}%") { update(settings.copy(speed = it)) }
                        StudioSlider("Audio response", settings.sensitivity, .5f..2f) { update(settings.copy(sensitivity = it)) }
                        StudioSlider("Spectrum reach", settings.size, .1f.. .8f) { update(settings.copy(size = it)) }
                        StudioSwitch("Mirror the spectrum", settings.mirror) { update(settings.copy(mirror = it)) }
                    }
                    1 -> {
                        StudioSection("Where the light lives")
                        StudioChoices(LightsPreferences.positions, settings.position, 3) { update(settings.copy(position = it)) }
                        StudioSlider("Corner radius", settings.radius, 0f..80f, "${settings.radius.roundToInt()} dp") { update(settings.copy(radius = it)) }
                        StudioSlider("Distance from edge", settings.inset, 2f..40f, "${settings.inset.roundToInt()} dp") { update(settings.copy(inset = it)) }
                        StudioSlider("Camera / notch gap", settings.notch, 0f..180f, if (settings.notch < 1) "Auto" else "${settings.notch.roundToInt()} dp") { update(settings.copy(notch = it)) }
                        Text("The preview follows your settings. Check the immersive display to fine-tune the fit on your own screen.", style = MaterialTheme.typography.bodySmall)
                    }
                    2 -> {
                        StudioSwitch("Show clock", settings.clock) { update(settings.copy(clock = it)) }
                        if (settings.clock) {
                            StudioChoices(LightsPreferences.clocks, settings.clockStyle, 2) { update(settings.copy(clockStyle = it)) }
                            StudioSlider("Clock position", settings.clockY, .25f.. .7f) { update(settings.copy(clockY = it)) }
                        }
                        StudioSwitch("Show battery", settings.battery) { update(settings.copy(battery = it)) }
                        StudioSwitch("Show song details", settings.details) { update(settings.copy(details = it)) }
                        StudioSlider("Display brightness", settings.brightness, .08f..1f) { update(settings.copy(brightness = it)) }
                        OutlinedButton(onClick = {
                            try { context.startActivity(Intent(Settings.ACTION_DREAM_SETTINGS)) }
                            catch (_: Exception) { error = "This phone doesn't expose Android screensaver settings. Use Open clock display instead." }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Set up Android screensaver") }
                        Text("Select Meloqis Music Lights in your phone's screensaver settings. Android decides when it runs, usually while charging or docked. Screensaver brightness is limited to 30%. This is a screen-on screensaver, not a replacement for the phone's low-power hardware AOD. Tap the screensaver to exit.", style = MaterialTheme.typography.bodySmall)
                    }
                    else -> {
                        StudioSwitch("Ambient glow between songs", settings.ambient) { update(settings.copy(ambient = it)) }
                        StudioSwitch("Stop floating lights in battery saver", settings.powerSaver) { update(settings.copy(powerSaver = it)) }
                        StudioSection("Floating / clock session")
                        StudioChoices(listOf("15 min", "30 min", "60 min", "120 min"), "${settings.sessionMinutes} min", 2) { update(settings.copy(sessionMinutes = it.substringBefore(' ').toInt())) }
                        Text("Floating lights are touch-through and stop when the screen turns off, when you close Meloqis from Recents, or when the session ends. With ambient glow off, they also stop after a minute without Meloqis audio.", style = MaterialTheme.typography.bodySmall)
                        Text("Clock displays use a black background and shifting content. Lower brightness and shorter sessions reduce battery use and static-pixel exposure; they cannot guarantee protection against OLED burn-in.", style = MaterialTheme.typography.bodySmall)
                        Text("Audio source: Meloqis playback on this device. Other players and Cast audio aren't captured. No microphone or notification-reading access is needed.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                error?.let { Text(it, color = Color(0xFFFFB4AB)) }
                Spacer(Modifier.height(12.dp))
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { context.startActivity(Intent(context, MusicLightsActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Open clock display") }
                OutlinedButton(onClick = {
                    if (overlayRunning) context.stopService(Intent(context, MusicLightsOverlayService::class.java))
                    else if (Settings.canDrawOverlays(context)) startOverlay()
                    else permission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()))
                }, modifier = Modifier.fillMaxWidth()) { Text(if (overlayRunning) "Stop floating lights" else "Show lights over other apps") }
            }
        }
    }
    if (guide) AlertDialog(
        onDismissRequest = { guide = false; prefs.edit().putBoolean("guide_seen_v2", true).apply() },
        title = { Text("Make the edges yours") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("1. Choose a look\nStart with a preset, then pick an effect and colours. Demo animates only the preview; play a song in Meloqis to see the real response.")
            Text("2. Fit your screen\nUse Fit to place the lights and adjust corners, edge distance and the camera gap.")
            Text("3. Take it with you\nShow lights over other apps asks Android for overlay permission. The overlay passes touches through. Stop it here or from its notification.")
            Text("4. Build a clock display\nChoose a clock in Clock, then open the display. For charging or docked use, tap Set up Android screensaver and select Meloqis Music Lights. Availability depends on your phone.")
            Text("Screen off closes floating lights. The screensaver is a screen-on display, not a hardware AOD replacement. Keep brightness low and choose a session limit in Behavior.")
            Text("Find this guide anytime at the top. Edge lighting has its own Settings menu; Equalizer stays for sound adjustments.")
        } },
        confirmButton = { TextButton(onClick = { guide = false; prefs.edit().putBoolean("guide_seen_v2", true).apply() }) { Text("Let's create") } },
    )
}

@Composable private fun StudioSection(text: String) { Text(text, color = Color(0xFFD4C7FF), style = MaterialTheme.typography.titleSmall) }
@Composable private fun StudioPalettes(settings: LightsSettings, select: (String) -> Unit) {
    LightsPreferences.palettes.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { name ->
                val colours = when (name) {
                    "Sunset" -> listOf(Color(0xFFFF587B), Color(0xFFFFBD6B), Color(0xFFDA67F5))
                    "Ocean" -> listOf(Color(0xFF60EED3), Color(0xFF4298FF), Color(0xFF8F92FF))
                    "Neon" -> listOf(Color(0xFFFF48C4), Color(0xFF963EFF), Color(0xFF44F9F0))
                    "Rainbow" -> listOf(Color(0xFFFF587B), Color.Yellow, Color.Cyan, Color(0xFF963EFF))
                    "Custom" -> listOf(Color.hsv(settings.hue % 360, .8f, 1f), Color.hsv(settings.secondHue % 360, .8f, 1f))
                    else -> listOf(Color(0xFFB79AFF), Color(0xFF6CF2D2), Color(0xFF658CFF))
                }
                Card(onClick = { select(name) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (settings.palette == name) Color(0xFFBCACFA) else Color(0xFF343040)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF171620))) {
                    Box(Modifier.fillMaxWidth().height(28.dp).background(Brush.horizontalGradient(colours)))
                    Text(name, Modifier.padding(horizontal = 12.dp, vertical = 10.dp), style = MaterialTheme.typography.labelLarge)
                }
            }
            if (row.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}
@Composable private fun StudioChoices(values: List<String>, selected: String, columns: Int, onSelect: (String) -> Unit) {
    values.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { value -> FilterChip(selected == value, { onSelect(value) }, label = { Text(value, style = MaterialTheme.typography.labelMedium) }, modifier = Modifier.weight(1f)) }
            repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
        }
    }
}
@Composable private fun StudioSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, description: String = "${(value * 100).roundToInt()}%", onChange: (Float) -> Unit) {
    Column {
        Row(Modifier.fillMaxWidth()) { Text(label, Modifier.weight(1f)); Text(description, color = Color(0xFFBCACFA), style = MaterialTheme.typography.labelMedium) }
        Slider(value, onChange, valueRange = range)
    }
}
@Composable private fun StudioSwitch(label: String, enabled: Boolean, change: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(enabled, change) }
}
