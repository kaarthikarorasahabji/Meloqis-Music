package echo.music.iad1tya.expect

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import org.koin.mp.KoinPlatform.getKoin

actual fun openUrl(url: String) {
    val context: AppCompatActivity = getKoin().get()
    val browserIntent =
        Intent(
            Intent.ACTION_VIEW,
            url.toUri(),
        )
    browserIntent.setFlags(FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(browserIntent)
}

actual fun shareUrl(
    title: String,
    url: String,
) {
    val context: AppCompatActivity = getKoin().get()
    val shareIntent = Intent(Intent.ACTION_SEND)
    shareIntent.type = "text/plain"
    shareIntent.putExtra(Intent.EXTRA_TEXT, url)
    shareIntent.setFlags(FLAG_ACTIVITY_NEW_TASK)
    val chooserIntent =
        Intent.createChooser(shareIntent, title)
    context.startActivity(chooserIntent)
}

actual fun downloadAndInstallApk(url: String) {
    val activity: AppCompatActivity = getKoin().get()
    val appContext = activity.applicationContext
    val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    val fileName = "Meloqis-Music-update.apk"

    // App-specific external files dir — no storage permission required.
    val request =
        DownloadManager
            .Request(url.toUri())
            .setTitle("Meloqis Music")
            .setDescription("Downloading the latest update…")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED,
            ).setDestinationInExternalFilesDir(
                appContext,
                Environment.DIRECTORY_DOWNLOADS,
                fileName,
            )
    val downloadId = downloadManager.enqueue(request)

    val receiver =
        object : BroadcastReceiver() {
            override fun onReceive(
                ctx: Context,
                intent: Intent,
            ) {
                val completedId =
                    intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (completedId != downloadId) return
                runCatching { appContext.unregisterReceiver(this) }
                // DownloadManager exposes an installable content:// URI for the file.
                val contentUri = downloadManager.getUriForDownloadedFile(downloadId) ?: return
                val installIntent =
                    Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(
                            contentUri,
                            "application/vnd.android.package-archive",
                        )
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION or FLAG_ACTIVITY_NEW_TASK,
                        )
                    }
                runCatching { activity.startActivity(installIntent) }
            }
        }

    val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        appContext.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
    } else {
        @Suppress("UnspecifiedRegisterReceiverFlag")
        appContext.registerReceiver(receiver, filter)
    }

    android.widget.Toast
        .makeText(
            appContext,
            "Downloading update… you'll be prompted to install when it's ready.",
            android.widget.Toast.LENGTH_LONG,
        ).show()
}