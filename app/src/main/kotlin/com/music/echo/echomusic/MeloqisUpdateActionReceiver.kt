package iad1tya.echo.music.echomusic

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import iad1tya.echo.music.echomusic.updater.downloadmanager.UpdateDownloadWorker

/**
 * Starts only official Meloqis update downloads selected from the signed-in-app
 * release notification. Android still owns the final package-install prompt.
 */
class MeloqisUpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_DOWNLOAD_UPDATE) return

        val version = intent.getStringExtra(EXTRA_VERSION).orEmpty().trim()
        val downloadUrl = intent.getStringExtra(EXTRA_DOWNLOAD_URL).orEmpty().trim()
        val sha256 = intent.getStringExtra(EXTRA_SHA256).orEmpty().trim().lowercase()
        if (
            version.isBlank() ||
            !sha256.matches(Regex("[0-9a-f]{64}")) ||
            !isOfficialDownloadUrl(downloadUrl)
        ) {
            return
        }

        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .setInputData(
                workDataOf(
                    "apk_url" to downloadUrl,
                    "version" to version,
                    "sha256" to sha256,
                ),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            UPDATE_DOWNLOAD_WORK,
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    private fun isOfficialDownloadUrl(value: String): Boolean {
        val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return false
        return uri.scheme == "https" &&
            uri.host == OFFICIAL_R2_HOST &&
            uri.path.orEmpty().startsWith("/previews/") &&
            uri.path.orEmpty().endsWith(".apk")
    }

    companion object {
        const val ACTION_DOWNLOAD_UPDATE = "in.axenoraai.meloqis.action.DOWNLOAD_UPDATE"
        const val EXTRA_VERSION = "version"
        const val EXTRA_DOWNLOAD_URL = "download_url"
        const val EXTRA_SHA256 = "sha256"

        private const val OFFICIAL_R2_HOST = "pub-7cad9af12a364d3f928f96a083db320f.r2.dev"
        const val UPDATE_DOWNLOAD_WORK = "meloqis_verified_update_download"
    }
}
