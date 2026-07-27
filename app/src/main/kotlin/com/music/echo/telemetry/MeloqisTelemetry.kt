package iad1tya.echo.music.telemetry

import android.content.Context
import android.os.Build
import iad1tya.echo.music.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.Executors

/**
 * Small, privacy-bounded telemetry client for Axenora-owned infrastructure.
 *
 * The server receives a random app-installation UUID, never Android ID, IMEI,
 * phone number, advertising ID, account data, media IDs, titles, or artists.
 */
object MeloqisTelemetry {
    private const val PREFERENCES = "meloqis_anonymous_insights"
    private const val KEY_INSTALLATION_ID = "installation_id"
    private const val KEY_INSTALL_TOKEN = "install_token"
    private const val KEY_EVENT_QUEUE = "event_queue"
    private const val KEY_FIRST_OPEN_RECORDED = "first_open_recorded"
    private const val KEY_LAST_ACTIVE_AT = "last_active_at"
    private const val KEY_LAST_VERSION = "last_version"
    private const val KEY_PENDING_UPDATE_VERSION = "pending_update_version"
    private const val KEY_ENABLED_CACHE = "enabled_cache"
    private const val MAX_QUEUED_EVENTS = 64
    private const val ACTIVE_INTERVAL_MS = 24L * 60L * 60L * 1000L

    private val lock = Any()
    private val networkExecutor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "MeloqisTelemetry").apply { isDaemon = true }
    }

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var enabled = true

    fun prepare(context: Context) {
        appContext = context.applicationContext
        enabled = preferences().getBoolean(KEY_ENABLED_CACHE, true)
    }

    fun initialize(context: Context, telemetryEnabled: Boolean) {
        prepare(context)
        setEnabledCache(telemetryEnabled)
        if (!telemetryEnabled) return

        val preferences = preferences()
        if (!preferences.getBoolean(KEY_FIRST_OPEN_RECORDED, false)) {
            val packageInfo = runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0)
            }.getOrNull()
            val installKind = when {
                packageInfo == null -> "unknown"
                packageInfo.lastUpdateTime - packageInfo.firstInstallTime > 60_000L -> "upgrade"
                else -> "fresh"
            }
            enqueue("first_open", mapOf("installKind" to installKind))
            preferences.edit().putBoolean(KEY_FIRST_OPEN_RECORDED, true).apply()
        }

        val previousVersion = preferences.getString(KEY_LAST_VERSION, null)
        val pendingVersion = preferences.getString(KEY_PENDING_UPDATE_VERSION, null)
        if (
            previousVersion != null &&
            previousVersion != BuildConfig.VERSION_NAME &&
            pendingVersion == BuildConfig.VERSION_NAME
        ) {
            enqueue(
                "update_install_succeeded",
                mapOf(
                    "fromVersion" to previousVersion,
                    "toVersion" to BuildConfig.VERSION_NAME,
                ),
            )
            preferences.edit().remove(KEY_PENDING_UPDATE_VERSION).apply()
        }
        preferences.edit().putString(KEY_LAST_VERSION, BuildConfig.VERSION_NAME).apply()

        val now = System.currentTimeMillis()
        val lastActiveAt = preferences.getLong(KEY_LAST_ACTIVE_AT, 0L)
        if (now - lastActiveAt >= ACTIVE_INTERVAL_MS) {
            enqueue("app_active")
            preferences.edit().putLong(KEY_LAST_ACTIVE_AT, now).apply()
        }
        flush()
    }

    fun setEnabled(context: Context, telemetryEnabled: Boolean) {
        if (appContext == null) prepare(context)
        setEnabledCache(telemetryEnabled)
        if (telemetryEnabled) {
            initialize(context, true)
        } else {
            synchronized(lock) {
                preferences().edit().remove(KEY_EVENT_QUEUE).apply()
            }
        }
    }

    fun recordCrash(throwable: Throwable) {
        val topFrame = throwable.stackTrace.firstOrNull()
        val code = buildString {
            append(throwable.javaClass.simpleName.ifBlank { "Throwable" })
            topFrame?.let {
                append('@')
                append(it.className.substringAfterLast('.'))
                append('.')
                append(it.methodName)
            }
        }
        enqueue("crash", mapOf("code" to code), flushImmediately = false)
    }

    fun recordPlaybackFailure(errorCode: Int) {
        enqueue("playback_failure", mapOf("code" to "media3_$errorCode"))
    }

    fun recordUpdateEvent(
        name: String,
        code: String? = null,
        fromVersion: String? = null,
        toVersion: String? = null,
    ) {
        enqueue(
            name,
            mapOf(
                "code" to code,
                "fromVersion" to fromVersion,
                "toVersion" to toVersion,
            ),
        )
    }

    fun markPendingUpdate(version: String) {
        val cleaned = TelemetryEventSanitizer.cleanVersion(version) ?: return
        preferences().edit().putString(KEY_PENDING_UPDATE_VERSION, cleaned).apply()
    }

    private fun setEnabledCache(telemetryEnabled: Boolean) {
        enabled = telemetryEnabled
        preferences().edit().putBoolean(KEY_ENABLED_CACHE, telemetryEnabled).apply()
    }

    private fun enqueue(
        name: String,
        details: Map<String, String?> = emptyMap(),
        flushImmediately: Boolean = true,
    ) {
        if (!enabled || !TelemetryEventSanitizer.isAllowedEvent(name) || appContext == null) return
        val event = JSONObject()
            .put("eventId", UUID.randomUUID().toString())
            .put("name", name)
            .put("appVersion", BuildConfig.VERSION_NAME)
            .put("versionCode", BuildConfig.VERSION_CODE)
            .put("androidVersion", Build.VERSION.RELEASE.orEmpty().take(24))
            .put("sdkInt", Build.VERSION.SDK_INT)

        val sanitizedDetails = JSONObject()
        TelemetryEventSanitizer.cleanCode(details["code"])?.let {
            sanitizedDetails.put("code", it)
        }
        TelemetryEventSanitizer.cleanVersion(details["fromVersion"])?.let {
            sanitizedDetails.put("fromVersion", it)
        }
        TelemetryEventSanitizer.cleanVersion(details["toVersion"])?.let {
            sanitizedDetails.put("toVersion", it)
        }
        details["installKind"]?.takeIf { it in setOf("fresh", "upgrade", "unknown") }?.let {
            sanitizedDetails.put("installKind", it)
        }
        if (sanitizedDetails.length() > 0) event.put("details", sanitizedDetails)

        synchronized(lock) {
            val queue = readQueue()
            val start = (queue.length() - MAX_QUEUED_EVENTS + 1).coerceAtLeast(0)
            val bounded = JSONArray()
            for (index in start until queue.length()) bounded.put(queue.optJSONObject(index))
            bounded.put(event)
            preferences().edit().putString(KEY_EVENT_QUEUE, bounded.toString()).apply()
        }
        if (flushImmediately) flush()
    }

    private fun flush() {
        if (!enabled || appContext == null) return
        networkExecutor.execute {
            val installationId: String
            val batch: JSONArray
            synchronized(lock) {
                installationId = installationId()
                val queue = readQueue()
                if (queue.length() == 0) return@execute
                batch = JSONArray()
                for (index in 0 until minOf(queue.length(), 32)) {
                    queue.optJSONObject(index)?.let(batch::put)
                }
            }

            val sentIds = buildSet {
                for (index in 0 until batch.length()) {
                    batch.optJSONObject(index)?.optString("eventId")?.let(::add)
                }
            }
            val accepted = runCatching {
                val installToken = installToken(installationId) ?: return@runCatching false
                val connection = URL(BuildConfig.TELEMETRY_ENDPOINT).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 7_000
                connection.readTimeout = 7_000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("User-Agent", "Meloqis/${BuildConfig.VERSION_NAME}")
                val payload = JSONObject()
                    .put("installationId", installationId)
                    .put("installToken", installToken)
                    .put("events", batch)
                    .toString()
                connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val responseCode = connection.responseCode
                runCatching {
                    val stream = if (responseCode >= 400) connection.errorStream else connection.inputStream
                    stream?.close()
                }
                connection.disconnect()
                if (responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
                    preferences().edit().remove(KEY_INSTALL_TOKEN).apply()
                }
                responseCode in 200..299
            }.onFailure {
                Timber.tag("MeloqisTelemetry").d(it, "Anonymous insight upload deferred")
            }.getOrDefault(false)

            if (accepted) {
                synchronized(lock) {
                    val current = readQueue()
                    val remaining = JSONArray()
                    for (index in 0 until current.length()) {
                        val event = current.optJSONObject(index) ?: continue
                        if (event.optString("eventId") !in sentIds) remaining.put(event)
                    }
                    preferences().edit().putString(KEY_EVENT_QUEUE, remaining.toString()).apply()
                }
            }
        }
    }

    private fun installationId(): String {
        val preferences = preferences()
        return preferences.getString(KEY_INSTALLATION_ID, null)
            ?.takeIf { runCatching { UUID.fromString(it) }.isSuccess }
            ?: UUID.randomUUID().toString().also {
                preferences.edit().putString(KEY_INSTALLATION_ID, it).commit()
            }
    }

    private fun installToken(installationId: String): String? {
        preferences().getString(KEY_INSTALL_TOKEN, null)?.takeIf { it.length in 32..128 }?.let {
            return it
        }

        val connection =
            URL(BuildConfig.TELEMETRY_REGISTER_ENDPOINT).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 7_000
            connection.readTimeout = 7_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("User-Agent", "Meloqis/${BuildConfig.VERSION_NAME}")
            val payload = JSONObject()
                .put("installationId", installationId)
                .toString()
            connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return null
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            JSONObject(response).optString("installToken")
                .takeIf { it.length in 32..128 }
                ?.also { preferences().edit().putString(KEY_INSTALL_TOKEN, it).commit() }
        } finally {
            connection.disconnect()
        }
    }

    private fun readQueue(): JSONArray =
        runCatching {
            JSONArray(preferences().getString(KEY_EVENT_QUEUE, "[]") ?: "[]")
        }.getOrElse { JSONArray() }

    private fun preferences() =
        requireNotNull(appContext) { "MeloqisTelemetry.prepare must be called first" }
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
