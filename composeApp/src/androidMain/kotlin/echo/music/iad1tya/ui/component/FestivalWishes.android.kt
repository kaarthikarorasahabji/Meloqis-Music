package echo.music.iad1tya.ui.component

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import java.time.*
import java.util.concurrent.TimeUnit
import kotlin.math.*

data class FestivalCampaign(val id: String, val date: LocalDate, val title: String, val message: String) {
    val scheduledAt: Long get() = date.atTime(4, 0).atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
    val expiresAt: Long get() = date.plusDays(1).atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli()
}

/** Dated campaigns are explicit, never repeated on the same Gregorian date next year. */
object FestivalEngine {
    const val TOPIC = "meloqis-festivals-v1"
    private const val CHANNEL = "festival_wishes"
    val dialog = MutableStateFlow<FestivalCampaign?>(null)
    val enabled = MutableStateFlow(false)
    // Android application supplies the Firebase topic subscription implementation.
    var onSubscriptionChanged: ((Boolean) -> Unit)? = null
    private fun prefs(c: Context) = c.getSharedPreferences("festival_wishes", Context.MODE_PRIVATE)
    private val defaults = listOf(
        FestivalCampaign("diwali-2026", LocalDate.of(2026, 11, 8), "Happy Diwali", "May your home glow with happiness, your heart with hope, and your days with beautiful music. With love, Meloqis."),
        FestivalCampaign("vishwakarma-2026", LocalDate.of(2026, 11, 9), "Happy Vishwakarma Day", "Celebrating the hands that create and the dreams that inspire. May your work bring joy, prosperity and new beginnings."),
        FestivalCampaign("bhai-dooj-2026", LocalDate.of(2026, 11, 10), "Happy Bhai Dooj", "To shared songs, childhood memories and a bond that keeps growing. Wishing you and your family love and happiness."),
    )
    fun initialize(context: Context) {
        enabled.value = prefs(context).getBoolean("enabled", false)
        // Delivery belongs to Studio: a cancelled campaign must not survive as a local alarm.
        WorkManager.getInstance(context).cancelAllWorkByTag("festival-wish")
        WorkManager.getInstance(context).cancelUniqueWork("festival-feed-refresh")
        WorkManager.getInstance(context).cancelUniqueWork("festival-feed-now")
        onSubscriptionChanged?.invoke(enabled.value)
    }
    fun setEnabled(context: Context, value: Boolean) {
        prefs(context).edit().putBoolean("enabled", value).apply(); enabled.value = value
        if (value) {
            initialize(context)
        } else {
            onSubscriptionChanged?.invoke(false)
            WorkManager.getInstance(context).cancelAllWorkByTag("festival-wish")
            WorkManager.getInstance(context).cancelUniqueWork("festival-feed-refresh")
            WorkManager.getInstance(context).cancelUniqueWork("festival-feed-now")
            NotificationManagerCompat.from(context).cancelAllFestivalNotifications(context)
            dialog.value = null
        }
    }
    private fun NotificationManagerCompat.cancelAllFestivalNotifications(c: Context) {
        campaigns(c).forEach { cancel("festival", it.id.hashCode()) }
    }
    fun parse(json: String): List<FestivalCampaign> {
        val root = JSONObject(json)
        require(root.getInt("schemaVersion") == 1)
        val entries = root.getJSONArray("campaigns")
        require(entries.length() <= 100)
        return (0 until entries.length()).map { i ->
            val item = entries.getJSONObject(i)
            val id = item.getString("id"); val title = item.getString("title"); val message = item.getString("message")
            val date = LocalDate.parse(item.getString("date"))
            require(id.matches(Regex("[a-z0-9-]{1,64}")) && title.length in 1..80 && message.length in 1..300 && date.year in 2026..2035)
            FestivalCampaign(id, date, title, message)
        }.also { require(it.map { campaign -> campaign.id }.distinct().size == it.size) }
    }
    fun campaigns(context: Context): List<FestivalCampaign> = runCatching {
        prefs(context).getString("feed", null)?.let(::parse) ?: defaults
    }.getOrDefault(defaults)
    @Synchronized fun notify(context: Context, campaign: FestivalCampaign) {
        if (!prefs(context).getBoolean("enabled", false)) return
        val now = System.currentTimeMillis()
        if (now < campaign.scheduledAt || now >= campaign.expiresAt || prefs(context).getBoolean("sent-${campaign.id}", false)) return
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL, "Festival wishes", NotificationManager.IMPORTANCE_DEFAULT))
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return
        launch.putExtra("festival_id", campaign.id).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val content = PendingIntent.getActivity(context, campaign.id.hashCode(), launch, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        manager.notify("festival", campaign.id.hashCode(), NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.btn_star_big_on).setContentTitle(campaign.title).setContentText(campaign.message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(campaign.message)).setContentIntent(content)
            .setAutoCancel(true).setCategory(NotificationCompat.CATEGORY_SOCIAL).setOnlyAlertOnce(true).build())
        prefs(context).edit().putBoolean("sent-${campaign.id}", true).apply()
    }
    fun checkToday(context: Context, requestedId: String? = null) {
        if (!prefs(context).getBoolean("enabled", false)) return
        val now = System.currentTimeMillis()
        val campaign = campaigns(context).firstOrNull { prefs(context).getBoolean("received-${it.id}", false) && now >= it.scheduledAt && now < it.expiresAt && (requestedId == null || it.id == requestedId) } ?: return
        if (requestedId != null || !prefs(context).getBoolean("seen-${campaign.id}", false)) dialog.value = campaign
    }
    fun dismiss(context: Context, campaign: FestivalCampaign) {
        prefs(context).edit().putBoolean("seen-${campaign.id}", true).apply(); dialog.value = null
    }
    fun receive(context: Context, data: Map<String, String>) {
        if (!prefs(context).getBoolean("enabled", false)) return
        val id = data["campaign_id"] ?: return
        // Only dated greeting text is accepted: no URLs, executable actions or arbitrary intents.
        data["campaign"]?.let { payload ->
            runCatching {
                require(payload.length <= 2048)
                val parsed = parse(JSONObject().put("schemaVersion", 1).put("campaigns", JSONArray().put(JSONObject(payload))).toString()).single()
                require(parsed.id == id)
                val all = campaigns(context).filterNot { it.id == id } + parsed
                val json = JSONObject().put("schemaVersion", 1).put("campaigns", JSONArray().apply {
                    all.takeLast(100).forEach { put(JSONObject().put("id", it.id).put("date", it.date.toString()).put("title", it.title).put("message", it.message)) }
                }).toString()
                prefs(context).edit().putString("feed", json).putBoolean("received-$id", true).apply()
            }
        }
        campaigns(context).firstOrNull { it.id == id }?.let { notify(context, it) }
        checkToday(context, id)
    }
}

@Composable
actual fun FestivalWishes() {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var invite by remember { mutableStateOf(false) }
    val preferences = remember { context.getSharedPreferences("festival_wishes", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) FestivalEngine.setEnabled(context, true)
    }
    LaunchedEffect(Unit) {
        delay(3500)
        if (!preferences.getBoolean("invited", false)) invite = true
    }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) FestivalEngine.checkToday(context) }
        lifecycle.addObserver(observer); FestivalEngine.checkToday(context)
        onDispose { lifecycle.removeObserver(observer) }
    }
    if (invite) AlertDialog(onDismissRequest = { preferences.edit().putBoolean("invited", true).apply(); invite = false },
        title = { Text("A little celebration from Meloqis") },
        text = { Text("Receive festival wishes and a colorful greeting when you open the app. Wishes are scheduled for 4:00 AM India time. Optional. You can manage notification channels in your phone’s app notification settings.") },
        confirmButton = { TextButton(onClick = {
            preferences.edit().putBoolean("invited", true).apply(); invite = false
            if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS) else FestivalEngine.setEnabled(context, true)
        }) { Text("Count me in") } }, dismissButton = { TextButton(onClick = { preferences.edit().putBoolean("invited", true).apply(); invite = false }) { Text("Maybe later") } })
    val campaign by FestivalEngine.dialog.collectAsStateWithLifecycle()
    campaign?.let { wish ->
        val transition = rememberInfiniteTransition(label = "festival")
        val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing)), label = "sparkles")
        AlertDialog(onDismissRequest = { FestivalEngine.dismiss(context, wish) },
            containerColor = Color(0xFF171226),
            title = { Text(wish.title, color = Color(0xFFFFD997), style = MaterialTheme.typography.headlineMedium) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Canvas(Modifier.fillMaxWidth().height(150.dp).background(Brush.radialGradient(listOf(Color(0xFF65403B), Color(0xFF171226))), RoundedCornerShape(24.dp))) {
                    repeat(36) { i ->
                        val t = (progress + i / 36f) % 1
                        val x = size.width * ((i * .6180339f) % 1)
                        val y = size.height * (1 - t)
                        drawCircle(if (i % 2 == 0) Color(0xFFFFD997) else Color(0xFFCFB4FF), (1f + sin(t * PI).toFloat() * 2.5f), Offset(x, y), alpha = sin(t * PI).toFloat().coerceIn(0f, 1f))
                    }
                    val center = Offset(size.width / 2, size.height * .67f)
                    drawArc(Color(0xFFEC9B58), 0f, 180f, true, topLeft = Offset(center.x - 42, center.y - 8), size = androidx.compose.ui.geometry.Size(84f, 42f))
                    drawOval(Color(0xFFFFD997), Offset(center.x - 7, center.y - 42 - 4 * sin(progress * 2 * PI).toFloat()), androidx.compose.ui.geometry.Size(14f, 38f))
                }
                Text(wish.message, color = Color(0xFFEDE2F5), style = MaterialTheme.typography.bodyLarge)
                Text("WITH LOVE, MELOQIS MUSIC", color = Color(0xFFBD9DDF), style = MaterialTheme.typography.labelSmall)
            } }, confirmButton = { TextButton(onClick = { FestivalEngine.dismiss(context, wish) }) { Text("Celebrate with music", color = Color(0xFFFFD997)) } })
    }
}
