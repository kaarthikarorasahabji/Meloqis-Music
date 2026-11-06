package echo.music.iad1tya.ui.component

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.graphics.*
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import echo.music.iad1tya.domain.mediaservice.player.AudioSpectrum
import echo.music.iad1tya.domain.mediaservice.handler.MediaPlayerHandler
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

internal object LightsPreferences {
    fun prefs(context: Context) = context.getSharedPreferences("music_lights", Context.MODE_PRIVATE)
    val positions = listOf("Bottom", "Top", "Left", "Right", "Frame", "Center")
    val modes = listOf("Bars", "Wave", "Orbit", "Dots")
    val palettes = listOf("Aurora", "Sunset", "Ocean", "Rainbow")
}

@Composable
actual fun MusicLightsControls() {
    val context = LocalContext.current
    val prefs = remember { LightsPreferences.prefs(context) }
    var setup by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(prefs.getString("position", "Bottom")!!) }
    var mode by remember { mutableStateOf(prefs.getString("mode", "Bars")!!) }
    var palette by remember { mutableStateOf(prefs.getString("palette", "Aurora")!!) }
    var size by remember { mutableFloatStateOf(prefs.getFloat("size", 0.35f)) }
    var brightness by remember { mutableFloatStateOf(prefs.getFloat("brightness", 0.7f)) }
    var sensitivity by remember { mutableFloatStateOf(prefs.getFloat("sensitivity", 1f)) }
    var clock by remember { mutableStateOf(prefs.getBoolean("clock", true)) }
    var error by remember { mutableStateOf<String?>(null) }
    fun save() { prefs.edit().putString("position", position).putString("mode", mode)
        .putString("palette", palette).putFloat("size", size).putFloat("brightness", brightness)
        .putFloat("sensitivity", sensitivity).putBoolean("clock", clock).apply() }
    fun startOverlay() {
        try { ContextCompat.startForegroundService(context, Intent(context, MusicLightsOverlayService::class.java)); error = null }
        catch (_: Exception) { error = "This device could not open the overlay. Keep Meloqis visible and try again." }
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (Settings.canDrawOverlays(context)) startOverlay()
        else error = "Overlay permission was not granted. The full-screen display still works."
    }
    Card(onClick = { setup = true }, modifier = Modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("MUSIC LIGHTS", style = MaterialTheme.typography.labelMedium)
            Text("Give your music a glow", style = MaterialTheme.typography.headlineSmall)
            Text("Live audio · Edge, wave & orbit modes", style = MaterialTheme.typography.bodyMedium)
            Text("Customize display  →", style = MaterialTheme.typography.labelLarge)
        }
    }
    if (setup) AlertDialog(onDismissRequest = { setup = false }, title = { Text("Your music. Your light.") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reacts to audio playing in Meloqis. No microphone, recording or audio uploads.")
                AndroidView(factory = { MusicLightsView(it, false) }, modifier = Modifier.fillMaxWidth().height(120.dp))
                Text("Placement", style = MaterialTheme.typography.titleSmall)
                LightsPreferences.positions.chunked(3).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    row.forEach { value -> FilterChip(position == value, { position = value; save() }, label = { Text(value) }) }
                } }
                Text("Visual mode", style = MaterialTheme.typography.titleSmall)
                LightsPreferences.modes.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { value -> FilterChip(mode == value, { mode = value; save() }, label = { Text(value) }) }
                } }
                Text("Colors", style = MaterialTheme.typography.titleSmall)
                LightsPreferences.palettes.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { value -> FilterChip(palette == value, { palette = value; save() }, label = { Text(value) }) }
                } }
                Text("Light height")
                Slider(size, { size = it; save() }, valueRange = 0.1f..0.8f)
                Text("Brightness")
                Slider(brightness, { brightness = it; save() }, valueRange = 0.08f..1f)
                Text("Response")
                Slider(sensitivity, { sensitivity = it; save() }, valueRange = 0.5f..1.8f)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text("Show clock", Modifier.weight(1f)); Switch(clock, { clock = it; save() })
                }
                Text("The display keeps the screen awake. Content shifts to reduce static pixels; lower brightness and shorter sessions help protect OLED screens. Overlay stops when the screen turns off, after 30 minutes, or after a minute without local audio.", style = MaterialTheme.typography.bodySmall)
                Button(onClick = { save(); context.startActivity(Intent(context, MusicLightsActivity::class.java)) }, modifier = Modifier.fillMaxWidth()) { Text("Open immersive display") }
                OutlinedButton(onClick = { save(); if (Settings.canDrawOverlays(context)) startOverlay()
                    else permission.launch(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri())) }, modifier = Modifier.fillMaxWidth()) { Text("Show over other apps") }
                TextButton(onClick = { context.stopService(Intent(context, MusicLightsOverlayService::class.java)) }) { Text("Stop floating lights") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        }, confirmButton = { TextButton(onClick = { setup = false }) { Text("Done") } })
}

/** User-opened immersive display, not a replacement for a manufacturer's hardware AOD. */
class MusicLightsActivity : ComponentActivity() {
    private lateinit var lights: MusicLightsView
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finish() }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = LightsPreferences.prefs(this@MusicLightsActivity).getFloat("brightness", .7f) }
        if (Build.VERSION.SDK_INT >= 30) {
            window.insetsController?.hide(android.view.WindowInsets.Type.systemBars())
            window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        lights = MusicLightsView(this, true)
        val frame = FrameLayout(this)
        frame.addView(lights, FrameLayout.LayoutParams(-1, -1))
        val close = TextView(this).apply {
            text = "‹  Back to Meloqis"; setTextColor(android.graphics.Color.WHITE); textSize = 16f
            setPadding(32, 40, 32, 24); setOnClickListener { finish() }
            contentDescription = "Close music display"
        }
        frame.addView(close, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        setContentView(frame)
        handler.postDelayed(timeout, 30 * 60_000L)
    }
    override fun onResume() { super.onResume(); if (::lights.isInitialized) lights.start() }
    override fun onPause() { if (::lights.isInitialized) lights.stop(); super.onPause() }
    override fun onDestroy() { handler.removeCallbacks(timeout); super.onDestroy() }
}

internal class MusicLightsView(context: Context, private val immersive: Boolean, private val overlay: Boolean = false) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val hsv = floatArrayOf(270f, .7f, 1f)
    private val smooth = FloatArray(24)
    private val prefs = LightsPreferences.prefs(context)
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val date = Date()
    private var running = false
    fun start() { if (!running) { running = true; AudioSpectrum.acquire(); invalidate() } }
    fun stop() { if (running) { running = false; AudioSpectrum.release() }; removeCallbacks(redraw) }
    private val redraw = Runnable { if (running) invalidate() }
    override fun onAttachedToWindow() { super.onAttachedToWindow(); start() }
    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && isAttachedToWindow) start() else stop()
    }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!overlay) canvas.drawColor(android.graphics.Color.rgb(5, 6, 12))
        val now = SystemClock.elapsedRealtime()
        val frame = AudioSpectrum.frames.value
        val active = now - frame.capturedAt < 500
        val gain = prefs.getFloat("sensitivity", 1f)
        for (i in smooth.indices) {
            val target = if (active) (frame.bands.getOrElse(i) { 0f } * gain).coerceIn(0f, 1f) else 0f
            smooth[i] += (target - smooth[i]) * if (target > smooth[i]) .5f else .16f
        }
        val w = width.toFloat(); val h = height.toFloat(); val margin = 12f * resources.displayMetrics.density
        val level = prefs.getFloat("size", .35f)
        val brightness = prefs.getFloat("brightness", .7f)
        val mode = prefs.getString("mode", "Bars")
        val placement = prefs.getString("position", "Bottom")
        val palette = prefs.getString("palette", "Aurora")
        fun color(i: Int): Int {
            hsv[0] = when (palette) { "Sunset" -> 330f + i * 3f; "Ocean" -> 165f + i * 3f; "Rainbow" -> i * 15f; else -> 230f + i * 5f }
            hsv[1] = .72f; hsv[2] = .35f + brightness * .65f
            return android.graphics.Color.HSVToColor(if (active) 240 else 90, hsv)
        }
        val drift = if (immersive) ((now / 60_000) % 7 - 3).toFloat() * 2 else 0f
        canvas.save(); canvas.translate(drift, drift)
        if (mode == "Orbit") {
            val cx = when (placement) { "Left" -> w * .22f; "Right" -> w * .78f; else -> w / 2 }
            val cy = when (placement) { "Top" -> h * .22f; "Bottom" -> h * .78f; else -> h / 2 }
            val radius = min(w, h) * .14f
            paint.strokeWidth = 5f * resources.displayMetrics.density; paint.strokeCap = Paint.Cap.ROUND
            for (i in smooth.indices) {
                val angle = i * 2 * PI / smooth.size - PI / 2
                val outer = radius + 4f + smooth[i] * min(w, h) * level * .3f
                paint.color = color(i)
                canvas.drawLine(cx + cos(angle).toFloat() * radius, cy + sin(angle).toFloat() * radius,
                    cx + cos(angle).toFloat() * outer, cy + sin(angle).toFloat() * outer, paint)
            }
        } else {
            fun edge(side: String) {
                val vertical = side == "Left" || side == "Right"
                val length = if (vertical) h else w
                val step = (length - 2 * margin) / 24
                val maxHeight = min(w, h) * level * (if (immersive) .5f else .65f)
                path.reset()
                for (i in smooth.indices) {
                    val along = margin + (i + .5f) * step
                    val amount = 3f + smooth[i] * maxHeight
                    val base = when (side) { "Top", "Left" -> margin; "Center" -> h * .65f; else -> if (vertical) w - margin else h - margin }
                    val end = base + if (side == "Top" || side == "Left") amount else -amount
                    val x = if (vertical) end else along; val y = if (vertical) along else end
                    paint.color = color(i); paint.strokeWidth = max(2f, step * .55f); paint.strokeCap = Paint.Cap.ROUND
                    if (mode == "Dots") canvas.drawCircle(x, y, max(2f, step * .17f), paint)
                    else if (mode == "Wave") { if (i == 0) path.moveTo(x, y) else path.lineTo(x, y) }
                    else if (vertical) canvas.drawLine(base, along, end, along, paint)
                    else canvas.drawLine(along, base, along, end, paint)
                }
                if (mode == "Wave") { paint.style = Paint.Style.STROKE; paint.strokeWidth = 4f * resources.displayMetrics.density; paint.color = color(12); canvas.drawPath(path, paint); paint.style = Paint.Style.FILL }
            }
            if (placement == "Frame") listOf("Top", "Bottom", "Left", "Right").forEach { edge(it) }
            else edge(placement ?: "Bottom")
        }
        if (immersive) {
            paint.color = android.graphics.Color.rgb(228, 226, 246); paint.textAlign = Paint.Align.CENTER
            if (prefs.getBoolean("clock", true)) {
                date.time = System.currentTimeMillis(); paint.textSize = min(w * .16f, 140f * resources.displayMetrics.density)
                canvas.drawText(timeFormat.format(date), w / 2, h * .41f, paint)
            }
            val title = runCatching { GlobalContext.get().get<MediaPlayerHandler>().nowPlaying.value?.metadata?.title }.getOrNull()
            paint.textSize = 16f * resources.displayMetrics.scaledDensity
            canvas.drawText((title ?: "Meloqis Music").take(32), w / 2, h * .48f, paint)
            paint.color = android.graphics.Color.GRAY; paint.textSize = 12f * resources.displayMetrics.scaledDensity
            canvas.drawText(if (active) "MUSIC LIGHTS · LIVE AUDIO" else "Play music in Meloqis to light up", w / 2, h * .53f, paint)
        }
        canvas.restore()
        if (running) postDelayed(redraw, if (active) 33 else 150)
    }
}

/** Small non-touchable visual layer. Never intercepts another app's controls. */
class MusicLightsOverlayService : Service() {
    private var view: MusicLightsView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var started = 0L
    private var lastAudio = 0L
    private val screenOff = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { stopSelf() } }
    private val monitor = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            if (now - AudioSpectrum.frames.value.capturedAt < 1000) lastAudio = now
            if (!Settings.canDrawOverlays(this@MusicLightsOverlayService) || now - started > 30 * 60_000 || now - lastAudio > 60_000) stopSelf()
            else handler.postDelayed(this, 5000)
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (view != null) {
            handler.removeCallbacks(monitor)
            runCatching { unregisterReceiver(screenOff) }
            view?.let { getSystemService(WindowManager::class.java).removeView(it) }
            view = null
        }
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("music_lights", "Floating music lights", NotificationManager.IMPORTANCE_LOW))
            val stop = PendingIntent.getService(this, 903, Intent(this, MusicLightsOverlayService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(this, "music_lights").setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Meloqis Music Lights").setContentText("Floating lights are on · Tap Stop to close")
                .setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop).build()
            ServiceCompat.startForeground(this, 903, notification, if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
            val params = WindowManager.LayoutParams(-1, (160 * resources.displayMetrics.density).toInt(), WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, PixelFormat.TRANSLUCENT)
            params.alpha = .65f // Below Android's obscuring-opacity limit; touches pass through.
            val position = LightsPreferences.prefs(this).getString("position", "Bottom")
            if (position == "Left" || position == "Right" || position == "Frame") { params.height = -1; params.width = if (position == "Frame") -1 else (100 * resources.displayMetrics.density).toInt() }
            params.gravity = when (position) { "Top" -> Gravity.TOP; "Left" -> Gravity.LEFT; "Right" -> Gravity.RIGHT; "Center", "Frame" -> Gravity.CENTER; else -> Gravity.BOTTOM }
            view = MusicLightsView(this, false, true).also { getSystemService(WindowManager::class.java).addView(it, params) }
            ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
            started = SystemClock.elapsedRealtime(); lastAudio = started; handler.postDelayed(monitor, 5000)
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf() }
    override fun onDestroy() {
        handler.removeCallbacks(monitor)
        runCatching { unregisterReceiver(screenOff) }
        view?.let { runCatching { getSystemService(WindowManager::class.java).removeView(it) } }; view = null
        stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy()
    }
}
