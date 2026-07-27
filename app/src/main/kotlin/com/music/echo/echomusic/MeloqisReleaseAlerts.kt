package iad1tya.echo.music.echomusic

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.echomusic.updater.isNewerVersion
import iad1tya.echo.music.echomusic.updater.saveUpdateAvailableState
import org.json.JSONObject
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Checks only Axenora's public Meloqis release manifest.
 *
 * This keeps the FOSS build independent from Firebase and from any upstream
 * repository while still giving users background release notifications.
 */
class MeloqisReleaseAlertWorker(
    appContext: Context,
    workerParams: WorkerParameters,
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        return runCatching {
            val connection = URL(RELEASE_MANIFEST).openConnection().apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", "Meloqis/${BuildConfig.VERSION_NAME}")
            }
            val manifest = connection.getInputStream().bufferedReader().use { JSONObject(it.readText()) }
            val latestVersion = manifest.getString("version").trim()
            if (
                latestVersion.isNotBlank() &&
                isNewerVersion(latestVersion, BuildConfig.VERSION_NAME) &&
                latestVersion != lastNotifiedVersion()
            ) {
                UpdateNotificationHelper.showUpdateNotification(applicationContext, latestVersion)
                rememberNotifiedVersion(latestVersion)
                saveUpdateAvailableState(applicationContext, true)
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    private fun lastNotifiedVersion(): String =
        applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .getString(KEY_LAST_NOTIFIED_VERSION, "")
            .orEmpty()

    private fun rememberNotifiedVersion(version: String) {
        applicationContext
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_NOTIFIED_VERSION, version)
            .apply()
    }

    companion object {
        private const val RELEASE_MANIFEST = "https://meloqis.axenoraai.in/releases/latest.json"
        private const val PREFERENCES = "meloqis_release_alerts"
        private const val KEY_LAST_NOTIFIED_VERSION = "last_notified_version"
    }
}

object MeloqisReleaseAlerts {
    private const val PERIODIC_WORK = "meloqis_official_release_alerts"
    private const val STARTUP_WORK = "meloqis_official_release_startup_check"

    fun schedule(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val periodicRequest = PeriodicWorkRequestBuilder<MeloqisReleaseAlertWorker>(
            12,
            TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()
        val startupRequest = OneTimeWorkRequestBuilder<MeloqisReleaseAlertWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            periodicRequest,
        )
        WorkManager.getInstance(context).enqueueUniqueWork(
            STARTUP_WORK,
            ExistingWorkPolicy.KEEP,
            startupRequest,
        )
    }
}
