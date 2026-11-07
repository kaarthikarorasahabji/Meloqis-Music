package echo.music.iad1tya.ui.component

import android.app.*
import android.content.*
import android.content.pm.ServiceInfo
import android.os.*
import android.provider.Settings
import android.service.dreams.DreamService
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import echo.music.iad1tya.domain.mediaservice.player.AudioSpectrum
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object MusicLightsOverlayState {
    private val current = MutableStateFlow(false)
    val running = current.asStateFlow()
    fun set(value: Boolean) { current.value = value }
}

class MusicLightsSettingsActivity : ComponentActivity() {
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        setContent { MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFFBCACFA), secondary = Color(0xFF8FE8D5), surface = Color(0xFF0C0D14))) { MusicLightsStudio(onClose = { finish() }) } }
    }
}

class MusicLightsActivity : ComponentActivity() {
    private lateinit var lights: MusicLightsView
    private val handler = Handler(Looper.getMainLooper())
    private val timeout = Runnable { finish() }
    private val screenOff = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { finish() } }
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val settings = LightsPreferences.read(LightsPreferences.prefs(this))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.attributes = window.attributes.apply { screenBrightness = settings.brightness }
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.insetsController?.hide(WindowInsets.Type.systemBars())
            window.insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
        }
        if (Build.VERSION.SDK_INT >= 28) window.attributes = window.attributes.apply { layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES }
        lights = MusicLightsView(this, immersive = true)
        val frame = FrameLayout(this)
        frame.addView(lights, FrameLayout.LayoutParams(-1, -1))
        val close = TextView(this).apply {
            text = "Back to Edge lighting"; setTextColor(android.graphics.Color.LTGRAY); textSize = 13f
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding * 2, padding, padding)
            setOnClickListener { finish() }; contentDescription = "Close clock display"
            postDelayed({ animate().alpha(.25f).setDuration(1000).start() }, 5000)
        }
        frame.addView(close, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        setContentView(frame)
        ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
        handler.postDelayed(timeout, settings.sessionMinutes * 60_000L)
    }
    override fun onResume() { super.onResume(); if (::lights.isInitialized) lights.start() }
    override fun onPause() { if (::lights.isInitialized) lights.stop(); super.onPause() }
    override fun onDestroy() { handler.removeCallbacks(timeout); runCatching { unregisterReceiver(screenOff) }; super.onDestroy() }
}

/** Runs only after the user selects Meloqis in Android screensaver settings. */
class MusicLightsDreamService : DreamService() {
    private var lights: MusicLightsView? = null
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false; isFullscreen = true; isScreenBright = false
        window?.let { dreamWindow -> dreamWindow.attributes = dreamWindow.attributes.apply {
            screenBrightness = LightsPreferences.read(LightsPreferences.prefs(this@MusicLightsDreamService)).brightness.coerceAtMost(.3f)
        } }
        lights = MusicLightsView(this, immersive = true).also { setContentView(it) }
    }
    override fun onDreamingStarted() { super.onDreamingStarted(); lights?.start() }
    override fun onDreamingStopped() { lights?.stop(); super.onDreamingStopped() }
    override fun onDetachedFromWindow() { lights?.stop(); lights = null; super.onDetachedFromWindow() }
    override fun onDestroy() { lights?.stop(); lights = null; super.onDestroy() }
}

/** Non-touchable visual layer. Does not observe another app's content. */
class MusicLightsOverlayService : Service() {
    private var view: MusicLightsView? = null
    private val handler = Handler(Looper.getMainLooper())
    private var started = 0L
    private var lastAudio = 0L
    private val screenOff = object : BroadcastReceiver() { override fun onReceive(c: Context?, i: Intent?) { stopSelf() } }
    private val monitor = object : Runnable {
        override fun run() {
            val now = SystemClock.elapsedRealtime()
            val settings = LightsPreferences.read(LightsPreferences.prefs(this@MusicLightsOverlayService))
            if (now - AudioSpectrum.frames.value.capturedAt < 1000) lastAudio = now
            val saver = getSystemService(PowerManager::class.java).isPowerSaveMode
            if (!Settings.canDrawOverlays(this@MusicLightsOverlayService) || now - started > settings.sessionMinutes * 60_000L ||
                (!settings.ambient && now - lastAudio > 60_000) || (settings.powerSaver && saver)) stopSelf()
            else handler.postDelayed(this, 2000)
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP") { stopSelf(); return START_NOT_STICKY }
        if (view != null) return START_NOT_STICKY
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY }
        val settings = LightsPreferences.read(LightsPreferences.prefs(this))
        if (settings.powerSaver && getSystemService(PowerManager::class.java).isPowerSaveMode) { stopSelf(); return START_NOT_STICKY }
        try {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(NotificationChannel("music_lights", "Floating music lights", NotificationManager.IMPORTANCE_LOW))
            val stop = PendingIntent.getService(this, 903, Intent(this, MusicLightsOverlayService::class.java).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE)
            val open = PendingIntent.getActivity(this, 904, Intent(this, MusicLightsSettingsActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
            val notification = NotificationCompat.Builder(this, "music_lights").setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Meloqis Edge lighting").setContentText("Floating lights are on. Tap to customize.")
                .setContentIntent(open).setOngoing(true).addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stop).build()
            ServiceCompat.startForeground(this, 903, notification, if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
            val params = WindowManager.LayoutParams(-1, -1, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN, android.graphics.PixelFormat.TRANSLUCENT)
            params.alpha = .65f; params.gravity = Gravity.TOP or Gravity.START
            if (Build.VERSION.SDK_INT >= 28) params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            view = MusicLightsView(this, immersive = false, overlay = true).also { getSystemService(WindowManager::class.java).addView(it, params) }
            ContextCompat.registerReceiver(this, screenOff, IntentFilter(Intent.ACTION_SCREEN_OFF), ContextCompat.RECEIVER_NOT_EXPORTED)
            started = SystemClock.elapsedRealtime(); lastAudio = started
            MusicLightsOverlayState.set(true); handler.postDelayed(monitor, 2000)
        } catch (_: Exception) { stopSelf() }
        return START_NOT_STICKY
    }
    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf() }
    override fun onDestroy() {
        handler.removeCallbacks(monitor); runCatching { unregisterReceiver(screenOff) }
        view?.let { it.stop(); runCatching { getSystemService(WindowManager::class.java).removeView(it) } }; view = null
        MusicLightsOverlayState.set(false); stopForeground(STOP_FOREGROUND_REMOVE); super.onDestroy()
    }
}
