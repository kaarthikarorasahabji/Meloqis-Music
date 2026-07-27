package iad1tya.echo.music.echomusic

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import iad1tya.echo.music.R
import iad1tya.echo.music.BuildConfig

object UpdateNotificationHelper {
    private const val CHANNEL_ID = "updates"
    private const val NOTIFICATION_ID = 1001

    fun showUpdateNotification(context: Context, versionName: String): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.app_updates_title),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            nm.createNotificationChannel(channel)
        }

        val permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val channelEnabled = Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            nm.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE

        if (!permissionGranted || !notificationsEnabled || !channelEnabled) {
            return false
        }

        
        val intent = Intent(Intent.ACTION_VIEW, "${BuildConfig.BRAND_WEBSITE}/download/".toUri())

        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(context, NOTIFICATION_ID, intent, flags)

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_nobg)
            .setContentTitle(context.getString(R.string.update_available_title))
            .setContentText("Meloqis $versionName is ready to download")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Meloqis $versionName is available from the official Axenora release channel. Tap Update now to download the verified APK.")
            )
            .setContentIntent(pending)
            .addAction(R.drawable.updated, "Update now", pending)
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notif)
            true
        }.getOrDefault(false)
    }
}
