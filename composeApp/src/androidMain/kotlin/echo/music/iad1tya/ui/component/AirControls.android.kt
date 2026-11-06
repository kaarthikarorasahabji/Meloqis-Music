package echo.music.iad1tya.ui.component

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.withResumed
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
actual fun AirControls() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val active by AirSession.active.collectAsStateWithLifecycle()
    val ready by AirSession.ready.collectAsStateWithLifecycle()
    val scrollMode by AirSession.scrollMode.collectAsStateWithLifecycle()
    val background by AirSession.background.collectAsStateWithLifecycle()
    val error by AirSession.error.collectAsStateWithLifecycle()
    val handVisible by AirSession.handVisible.collectAsStateWithLifecycle()
    val lastGesture by AirSession.lastGesture.collectAsStateWithLifecycle()
    var showSetup by remember { mutableStateOf(false) }
    var guidePage by remember { mutableIntStateOf(0) }
    var pendingStart by remember { mutableStateOf(false) }
    val showingSetup by rememberUpdatedState(showSetup)
    SideEffect { AirSession.controlsPaused.value = showSetup && AirSession.visible.value }
    fun permissionsNeeded() = buildList {
        add(Manifest.permission.CAMERA)
        if (background && Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS)
    }
    fun start() {
        if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) return
        AirSession.error.value = null
        try {
            ContextCompat.startForegroundService(context, Intent(context, AirGestureService::class.java))
            showSetup = false
        } catch (_: Exception) {
            AirSession.error.value = "Android could not start camera controls. Keep the app open and try again."
        }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (permissionsNeeded().all { required -> ContextCompat.checkSelfPermission(context, required) == PackageManager.PERMISSION_GRANTED }) pendingStart = true
        else AirSession.error.value = "Hand gestures need camera permission. Background mode also needs notifications so you can stop the camera there."
    }
    LaunchedEffect(pendingStart) {
        if (pendingStart) lifecycle.withResumed { start(); pendingStart = false }
    }
    LaunchedEffect(error) { if (error != null) showSetup = true }
    DisposableEffect(lifecycle) {
        AirSession.visible.value = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                AirSession.visible.value = true
                AirSession.controlsPaused.value = showingSetup
            }
            if (event == Lifecycle.Event.ON_PAUSE) {
                AirSession.visible.value = false
                AirSession.controlsPaused.value = false
                if (!AirSession.background.value) context.stopService(Intent(context, AirGestureService::class.java))
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            AirSession.controlsPaused.value = false
            if (!AirSession.background.value) context.stopService(Intent(context, AirGestureService::class.java))
        }
    }
    FilledTonalButton(onClick = { guidePage = 0; showSetup = true }, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
        Text(when { active && ready -> "● Air on"; active -> "Preparing Air…"; else -> "Air controls" })
    }
    if (showSetup) AlertDialog(
        onDismissRequest = { showSetup = false },
        title = { Text(if (guidePage == 0) "Hand gestures · Beta" else "How to use Air Controls") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (guidePage == 0) {
                    Text("Only your front camera is used. Controls respond to a detected hand, not general movement.")
                    Text("Vertical hand swipes control:", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = scrollMode, onClick = { AirSession.scrollMode.value = true }, label = { Text("Scrolling") })
                        FilterChip(selected = !scrollMode, onClick = { AirSession.scrollMode.value = false }, label = { Text("Volume") })
                    }
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Checkbox(checked = background, onCheckedChange = {
                            if (it && active && Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context,
                                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                AirSession.error.value = "Stop camera and enable again to allow background notifications."
                            } else AirSession.background.value = it
                        })
                        Text("Keep gestures active in the background", modifier = Modifier.weight(1f))
                    }
                    Text("Background mode keeps the front camera running with an ongoing notification. Stop there at any time. Background vertical swipes adjust volume; scrolling works inside Meloqis.", style = MaterialTheme.typography.bodySmall)
                    Text("First use downloads a 7.5 MB hand detector from Google. Recognition happens on your phone; frames are never saved or uploaded.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { guidePage = 1 }) { Text("Show gesture guide →") }
                } else {
                    Text("1. Place your phone upright and still, in good light. Wait until the button says Air on.")
                    Text("2. Show one open hand briefly to the front camera, around 30–70 cm away. Keep your whole hand visible.")
                    Text("3. Move your hand right for next track or left for previous track.")
                    Text("4. Move your hand up to scroll down / raise volume. Move it down to scroll up / lower volume.")
                    Text("5. Pause for a second between swipes. Stop using Air on → Stop camera, Stop Air in the player, or Stop in the notification.")
                    if (active) {
                        HorizontalDivider()
                        Text(if (handVisible) "Hand detected ✓" else "Show your hand to practice", color = MaterialTheme.colorScheme.primary)
                        Text("Recognized: ${lastGesture?.name?.lowercase() ?: "waiting for a swipe"}")
                        Text("Practice safely here: music and volume stay unchanged while this guide is open.", style = MaterialTheme.typography.bodySmall)
                    }
                    TextButton(onClick = { guidePage = 0 }) { Text("← Settings") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (active) { context.stopService(Intent(context, AirGestureService::class.java)); showSetup = false }
                else if (permissionsNeeded().all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) start()
                else permission.launch(permissionsNeeded().toTypedArray())
            }) { Text(if (active) "Stop camera" else "Enable hand gestures") }
        },
        dismissButton = { TextButton(onClick = { showSetup = false }) { Text(if (active) "Continue" else "Later") } },
    )
}
