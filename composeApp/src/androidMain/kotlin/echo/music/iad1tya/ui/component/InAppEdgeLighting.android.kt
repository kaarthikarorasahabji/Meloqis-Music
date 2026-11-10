package echo.music.iad1tya.ui.component

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.os.PowerManager
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Collections
import java.util.WeakHashMap

/** A non-interactive ViewGroupOverlay belongs to an Activity, never to WindowManager. */
object InAppEdgeLighting : Application.ActivityLifecycleCallbacks {
    private data class Layer(val root: ViewGroup, val view: MusicLightsView, val layout: View.OnLayoutChangeListener)
    private val layers = WeakHashMap<Activity, Layer>()
    private val resumed: MutableSet<Activity> = Collections.newSetFromMap(WeakHashMap<Activity, Boolean>())
    private data class FocusWatch(val root: View, val listener: ViewTreeObserver.OnWindowFocusChangeListener)
    private val focusWatches = WeakHashMap<Activity, FocusWatch>()
    private val state = MutableStateFlow(false)
    val running = state.asStateFlow()
    private lateinit var preferences: SharedPreferences
    private lateinit var application: Application
    private fun refresh() {
        val activities = resumed.toList()
        activities.forEach { detach(it) }
        activities.forEach { attach(it) }
    }
    private val screenEvents = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) layers.keys.toList().forEach { detach(it) }
            else refresh()
        }
    }
    private val changes = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled") {
            val sync = Runnable { refresh() }
            if (Looper.myLooper() == Looper.getMainLooper()) sync.run()
            else android.os.Handler(Looper.getMainLooper()).post(sync)
        }
    }
    fun install(application: Application) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (::preferences.isInitialized) return
        this.application = application
        preferences = LightsPreferences.prefs(application)
        preferences.registerOnSharedPreferenceChangeListener(changes)
        // Old floating components disappear on upgrade; remove their stale notification too.
        application.getSystemService(NotificationManager::class.java).apply {
            cancel(903)
            deleteNotificationChannel("music_lights")
        }
        application.registerActivityLifecycleCallbacks(this)
        ContextCompat.registerReceiver(application, screenEvents, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF); addAction(Intent.ACTION_SCREEN_ON); addAction(Intent.ACTION_USER_PRESENT)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    private fun attach(activity: Activity) {
        if (!edgeLightingVisible(LightsPreferences.read(preferences).enabled, resumed.contains(activity),
                application.getSystemService(PowerManager::class.java).isInteractive,
                application.getSystemService(KeyguardManager::class.java).let { it.isDeviceLocked || it.isKeyguardLocked }, activity.hasWindowFocus()) ||
            activity.isFinishing || activity.isDestroyed ||
            activity.javaClass.name.endsWith(".MandatoryUpdateActivity") || layers.containsKey(activity)) return
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val view = MusicLightsView(activity, transparent = true).apply {
            isClickable = false; isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        val layout = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> view.layout(0, 0, root.width, root.height) }
        root.addOnLayoutChangeListener(layout)
        view.layout(0, 0, root.width, root.height)
        root.overlay.add(view)
        layers[activity] = Layer(root, view, layout)
        state.value = true
    }
    private fun detach(activity: Activity) {
        layers.remove(activity)?.let { layer ->
            layer.view.stop(); layer.root.removeOnLayoutChangeListener(layer.layout); layer.root.overlay.remove(layer.view)
        }
        state.value = layers.isNotEmpty()
    }
    private fun stopWatching(activity: Activity) {
        focusWatches.remove(activity)?.let { watch -> if (watch.root.viewTreeObserver.isAlive) watch.root.viewTreeObserver.removeOnWindowFocusChangeListener(watch.listener) }
        resumed.remove(activity); detach(activity)
    }
    override fun onActivityResumed(activity: Activity) {
        resumed.add(activity)
        if (!focusWatches.containsKey(activity)) {
            val root = activity.window.decorView
            val listener = ViewTreeObserver.OnWindowFocusChangeListener { focused -> if (focused) attach(activity) else detach(activity) }
            root.viewTreeObserver.addOnWindowFocusChangeListener(listener)
            focusWatches[activity] = FocusWatch(root, listener)
        }
        attach(activity)
    }
    override fun onActivityPaused(activity: Activity) { stopWatching(activity) }
    override fun onActivityDestroyed(activity: Activity) { stopWatching(activity) }
    override fun onActivityCreated(activity: Activity, state: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) { stopWatching(activity) }
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
}
