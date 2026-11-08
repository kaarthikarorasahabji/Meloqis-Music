package echo.music.iad1tya.ui.component

import android.app.Activity
import android.app.Application
import android.app.NotificationManager
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.WeakHashMap

/** A non-interactive ViewGroupOverlay belongs to an Activity, never to WindowManager. */
object InAppEdgeLighting : Application.ActivityLifecycleCallbacks {
    private data class Layer(val root: ViewGroup, val view: MusicLightsView, val layout: View.OnLayoutChangeListener)
    private val layers = WeakHashMap<Activity, Layer>()
    private var current = WeakReference<Activity>(null)
    private val state = MutableStateFlow(false)
    val running = state.asStateFlow()
    private lateinit var preferences: SharedPreferences
    private val changes = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "enabled") current.get()?.let { activity ->
            activity.runOnUiThread { detach(activity); attach(activity) }
        }
    }
    fun install(application: Application) {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (::preferences.isInitialized) return
        preferences = LightsPreferences.prefs(application)
        preferences.registerOnSharedPreferenceChangeListener(changes)
        // Old floating components disappear on upgrade; remove their stale notification too.
        application.getSystemService(NotificationManager::class.java).apply {
            cancel(903)
            deleteNotificationChannel("music_lights")
        }
        application.registerActivityLifecycleCallbacks(this)
    }
    private fun attach(activity: Activity) {
        if (!LightsPreferences.read(preferences).enabled || activity.isFinishing || activity.isDestroyed ||
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
    override fun onActivityResumed(activity: Activity) { current = WeakReference(activity); attach(activity) }
    override fun onActivityPaused(activity: Activity) { detach(activity); if (current.get() === activity) current.clear() }
    override fun onActivityDestroyed(activity: Activity) { detach(activity); if (current.get() === activity) current.clear() }
    override fun onActivityCreated(activity: Activity, state: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) { detach(activity) }
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
}
