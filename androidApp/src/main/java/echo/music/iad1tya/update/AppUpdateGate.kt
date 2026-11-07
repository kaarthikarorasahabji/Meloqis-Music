package echo.music.iad1tya.update

import android.app.Activity
import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import echo.music.iad1tya.ui.component.compareReleaseVersions
import java.lang.ref.WeakReference
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

/** Checks on foreground entry; a cached confirmed policy also works offline. */
object AppUpdateGate : Application.ActivityLifecycleCallbacks {
    private const val POLICY = "https://github.com/kaarthikarorasahabji/Meloqis-Music/releases/latest/download/update-policy.json"
    const val RELEASES = "https://github.com/kaarthikarorasahabji/Meloqis-Music/releases/latest"
    private lateinit var app: Application
    private val main = Handler(Looper.getMainLooper())
    private var foreground = WeakReference<Activity>(null)
    private val checking = AtomicBoolean(false)
    private var lastCheck = 0L
    private val prefs get() = app.getSharedPreferences("required_updates", 0)
    val installed: String get() = app.packageManager.getPackageInfo(app.packageName, 0).versionName ?: "0.0.0"
    val minimum: String get() = prefs.getString("minimum", "") ?: ""
    fun required(): Boolean = prefs.getBoolean("required", false) && (compareReleaseVersions(installed.substringBefore('-'), minimum)?.let { it < 0 } == true)
    fun install(application: Application) { app = application; app.registerActivityLifecycleCallbacks(this) }

    fun refresh(force: Boolean = false, done: (() -> Unit)? = null) {
        if ((!force && System.currentTimeMillis() - lastCheck < 900_000) || !checking.compareAndSet(false, true)) { done?.invoke(); return }
        Thread({
            try {
                val connection = URL(POLICY).openConnection() as HttpsURLConnection
                connection.connectTimeout = 6_000; connection.readTimeout = 6_000
                connection.setRequestProperty("Accept", "application/json")
                val policy = try {
                    check(connection.responseCode == 200)
                    connection.inputStream.bufferedReader().use { reader ->
                        val buffer = CharArray(8193); var count = 0
                        while (count < buffer.size) { val n = reader.read(buffer, count, buffer.size-count); if (n < 0) break; count += n }
                        check(count <= 8192); JSONObject(String(buffer, 0, count))
                    }
                } finally { connection.disconnect() }
                val floor = if (echo.music.iad1tya.BuildKonfig.playStore == 1) policy.getString("minimumPlayVersion") else policy.getString("minimumVersion")
                check(compareReleaseVersions(floor, floor) != null)
                val required = policy.getBoolean("required")
                prefs.edit().putString("minimum", floor).putBoolean("required", required).apply()
            } catch (_: Exception) { /* Keep last confirmed policy; new offline installs remain usable. */ }
            finally { lastCheck = System.currentTimeMillis(); checking.set(false); main.post { enforce(); done?.invoke() } }
        }, "Meloqis-release-check").start()
    }

    private fun enforce() {
        val activity = foreground.get() ?: return
        if (required() && activity !is MandatoryUpdateActivity && !activity.isFinishing && !activity.isDestroyed) {
            activity.startActivity(Intent(activity, MandatoryUpdateActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        }
    }
    override fun onActivityResumed(activity: Activity) { foreground = WeakReference(activity); enforce(); refresh() }
    override fun onActivityPaused(activity: Activity) { if (foreground.get() === activity) foreground.clear() }
    override fun onActivityCreated(activity: Activity, state: Bundle?) {}
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { if (foreground.get() === activity) foreground.clear() }
}
