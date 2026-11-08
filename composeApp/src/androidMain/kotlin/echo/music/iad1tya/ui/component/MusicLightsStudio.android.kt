package echo.music.iad1tya.ui.component

import android.content.Intent
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import echo.music.iad1tya.domain.mediaservice.player.AudioSpectrum
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.roundToInt

@Composable
actual fun EdgeLightingControls() {
    val context = LocalContext.current
    Card(onClick = { context.startActivity(Intent(context, MusicLightsSettingsActivity::class.java)) }, modifier=Modifier.fillMaxWidth().padding(16.dp),
        colors=CardDefaults.cardColors(containerColor=Color(0xFF171326)), shape=RoundedCornerShape(26.dp)) {
        Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF242039),Color(0xFF101A26)))).padding(24.dp), verticalArrangement=Arrangement.spacedBy(9.dp)) {
            Text("YOUR IN-APP GLOW",color=Color(0xFFBCACFA),style=MaterialTheme.typography.labelMedium)
            Text("Edge lighting",color=Color.White,style=MaterialTheme.typography.headlineSmall)
            Text("Colourful borders while you explore Meloqis.",color=Color(0xFFC7C1D5))
            Text("Make it yours  →",color=Color(0xFF8FE8D5),style=MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun MusicLightsStudio(onClose: () -> Unit) {
    val context=LocalContext.current
    val prefs=remember { LightsPreferences.prefs(context) }
    var settings by remember { mutableStateOf(LightsPreferences.read(prefs)) }
    var tab by remember { mutableIntStateOf(0) }
    var demo by remember { mutableStateOf(true) }
    var guide by remember { mutableStateOf(!prefs.getBoolean("guide_seen_in_app_v3",false)) }
    val running by InAppEdgeLighting.running.collectAsStateWithLifecycle()
    var hasAudio by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while(isActive) {
            val frame=AudioSpectrum.frames.value
            hasAudio=frame.capturedAt>0 && android.os.SystemClock.elapsedRealtime()-frame.capturedAt<500
            delay(500)
        }
    }
    fun update(value: LightsSettings) { settings=value; LightsPreferences.save(prefs,value) }
    Surface(Modifier.fillMaxSize(),color=Color(0xFF0C0D14),contentColor=Color(0xFFECE8F7)) {
        Column(Modifier.safeDrawingPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("MELOQIS EDGE STUDIO",color=Color(0xFFBCACFA),style=MaterialTheme.typography.labelSmall)
                    Text("A little glow. All yours.",style=MaterialTheme.typography.titleLarge)
                }
                TextButton(onClick={guide=true}) { Text("Guide",color=Color(0xFF8FE8D5)) }
                TextButton(onClick=onClose) { Text("Done",color=Color(0xFFBCACFA)) }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal=20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF191A29)),shape=RoundedCornerShape(22.dp)) {
                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("Edge lighting",style=MaterialTheme.typography.titleMedium)
                                Text(if(settings.enabled && running) "On in Meloqis" else if(settings.enabled) "Ready when Meloqis is open" else "Off",color=Color(0xFF8FE8D5),style=MaterialTheme.typography.bodySmall)
                            }
                            Switch(settings.enabled,{update(settings.copy(enabled=it))})
                        }
                        Text("Starts automatically inside the app. Stops when you leave Meloqis. Your choice is remembered.",color=Color(0xFFC7C1D5),style=MaterialTheme.typography.bodySmall)
                    }
                }
                Box(Modifier.fillMaxWidth().height(236.dp),contentAlignment=Alignment.Center) {
                    AndroidView(factory={MusicLightsView(it,transparent=false,preview=true)},update={it.demo=demo;it.invalidate()},
                        modifier=Modifier.width(134.dp).fillMaxHeight().clip(RoundedCornerShape(24.dp)))
                }
                Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                    Text(if(!settings.enabled) "PREVIEW • LIGHTING OFF" else if(hasAudio) "LIVE MUSIC RESPONSE" else if(demo) "STYLE PREVIEW" else "WAITING FOR MUSIC",color=Color(0xFF8FE8D5),style=MaterialTheme.typography.labelSmall,modifier=Modifier.weight(1f))
                    Text("Demo",style=MaterialTheme.typography.bodySmall);Switch(demo,{demo=it})
                }
                Text("Demo animates this preview only. In the app, your music drives the response; gentle idle motion is optional.",color=Color(0xFFA9A3B9),style=MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    listOf("Design","Placement","Behavior").forEachIndexed { index,name -> FilterChip(tab==index,{tab=index},label={Text(name)}) }
                }
                when(tab) {
                    0 -> {
                        StudioSection("One tap, a whole mood")
                        StudioChoices(LightsPreferences.presets,"",2) { update(LightsPreferences.preset(it,settings)) }
                        StudioSection("Your effect")
                        StudioChoices(LightsPreferences.modes,settings.mode,2) { update(settings.copy(mode=it)) }
                        Text(when(settings.mode) {
                            "Comet" -> "A soft trail travels around the border."
                            "Twin trails" -> "Two luminous trails follow opposite sides of the edge."
                            "Breath" -> "A calm border that fades in and out, with a little lift from your music."
                            "Bars" -> "Small spectrum bars stay close to the edges."
                            "Wave" -> "A flowing spectrum traces the border."
                            "Sparkles" -> "Fine points of colour shimmer along the edges."
                            else -> "A smooth colour ribbon follows your screen."
                        },style=MaterialTheme.typography.bodySmall,color=Color(0xFFC7C1D5))
                        StudioSection("Colour story")
                        StudioPalettes(settings) { update(settings.copy(palette=it)) }
                        if(settings.palette=="Album art") Text("Colours follow the current cover, with Aurora as a fallback.",style=MaterialTheme.typography.bodySmall)
                        if(settings.palette=="Custom") {
                            StudioSlider("First hue",settings.hue,0f..360f,"${settings.hue.roundToInt()}°") { update(settings.copy(hue=it)) }
                            StudioSlider("Second hue",settings.secondHue,0f..360f,"${settings.secondHue.roundToInt()}°") { update(settings.copy(secondHue=it)) }
                        }
                        StudioSlider("Light width",settings.thickness,1f..6f,"${settings.thickness.roundToInt()} dp") { update(settings.copy(thickness=it)) }
                        StudioSlider("Soft glow",settings.glow,0f..1f) { update(settings.copy(glow=it)) }
                        StudioSlider("Light intensity",settings.brightness,.1f..1f) { update(settings.copy(brightness=it)) }
                        StudioSlider("Motion speed",settings.speed,.2f..2f,"${(settings.speed*100).roundToInt()}%") { update(settings.copy(speed=it)) }
                        StudioSlider("Audio response",settings.sensitivity,.5f..2f) { update(settings.copy(sensitivity=it)) }
                        StudioSlider("Spectrum reach",settings.size,.1f.. .5f) { update(settings.copy(size=it)) }
                    }
                    1 -> {
                        StudioSection("Keep it on the edges")
                        StudioChoices(LightsPreferences.positions,settings.position,2) { update(settings.copy(position=it)) }
                        StudioSlider("Corner radius",settings.radius,0f..48f,"${settings.radius.roundToInt()} dp") { update(settings.copy(radius=it)) }
                        StudioSlider("Distance from edge",settings.inset,2f..20f,"${settings.inset.roundToInt()} dp") { update(settings.copy(inset=it)) }
                        StudioSlider("Notch gap",settings.notch,0f..180f,if(settings.notch<1) "Auto" else "${settings.notch.roundToInt()} dp") { update(settings.copy(notch=it)) }
                        Text("Lights follow Meloqis content and leave your controls touchable. Spectrum effects stay close to the border.",style=MaterialTheme.typography.bodySmall,color=Color(0xFFC7C1D5))
                    }
                    else -> {
                        StudioSwitch("Animate borders",settings.motion) { update(settings.copy(motion=it)) }
                        StudioSwitch("Gentle glow between songs",settings.ambient) { update(settings.copy(ambient=it)) }
                        StudioSwitch("Mirror the spectrum",settings.mirror) { update(settings.copy(mirror=it)) }
                        StudioSwitch("Reduce motion in battery saver",settings.powerSaver) { update(settings.copy(powerSaver=it)) }
                        Text("Device animation preferences are respected. Lighting pauses when Meloqis is in the background and never keeps your screen awake.",style=MaterialTheme.typography.bodySmall,color=Color(0xFFC7C1D5))
                        Text("Music response uses Meloqis playback on this device. Turn Demo off to check the real response.",style=MaterialTheme.typography.bodySmall,color=Color(0xFFC7C1D5))
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
    fun closeGuide() { guide=false;prefs.edit().putBoolean("guide_seen_in_app_v3",true).apply() }
    if(guide) AlertDialog(onDismissRequest={closeGuide()},title={Text("Your glow, inside Meloqis")},text={
        Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)) {
            Text("1. Already on\nEdge lighting starts automatically when you use Meloqis. The switch above turns it off, and remembers your choice.")
            Text("2. Choose a mood\nPick an effect and a colour story, or start with a preset. Demo lets you try the look before playing music.")
            Text("3. Find your edges\nUse Frame, Top, Bottom, Left or Right. Adjust the width, corners and glow until it feels right.")
            Text("4. Make it comfortable\nUse a quiet glow or music response, and turn animation off if you prefer. Lighting stops when you leave the app.")
        }
    },confirmButton={TextButton(onClick={closeGuide()}) {Text("Got it")}})
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
