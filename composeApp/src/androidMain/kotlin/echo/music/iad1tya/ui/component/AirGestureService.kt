package echo.music.iad1tya.ui.component

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import echo.music.iad1tya.domain.mediaservice.handler.MediaPlayerHandler
import echo.music.iad1tya.domain.mediaservice.handler.PlayerEvent
import kotlinx.coroutines.*
import org.koin.core.context.GlobalContext

/** Explicit, non-sticky camera session. Never started from boot or a background receiver. */
class AirGestureService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var camera: HandSwipeCamera? = null
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Hand gesture controls", NotificationManager.IMPORTANCE_LOW))
        scope.launch { AirSession.stop.collect { stopSelf() } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) { stopSelf(); return START_NOT_STICKY }
        if (intent?.action == PLAY_PAUSE) { play(PlayerEvent.PlayPause); return START_NOT_STICKY }
        if (camera != null) return START_NOT_STICKY
        try {
            ServiceCompat.startForeground(this, NOTIFICATION, notification("Preparing on-device hand recognition…"),
                if (Build.VERSION.SDK_INT >= 30) ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA else 0)
            AirSession.active.value = true
            camera = HandSwipeCamera(this,
                onError = { AirSession.error.value = it; stopSelf() },
                onHandPresence = { AirSession.handVisible.value = it },
                onReady = {
                    AirSession.ready.value = true
                    getSystemService(NotificationManager::class.java).notify(NOTIFICATION,
                        notification("Front camera active · Hand swipes control music"))
                },
                onSwipe = gesture@{ swipe ->
                    AirSession.lastGesture.value = swipe
                    if (AirSession.controlsPaused.value) return@gesture
                    Log.d("MeloqisAir", "Recognized hand swipe: $swipe")
                    when (swipe) {
                        AirSwipe.RIGHT -> play(PlayerEvent.Next)
                        AirSwipe.LEFT -> play(PlayerEvent.SkipToPrevious)
                        AirSwipe.UP, AirSwipe.DOWN -> {
                            if (AirSession.scrollMode.value && AirSession.visible.value && !AirScroll.playerOpen.value) {
                                AirScroll.events.tryEmit(if (swipe == AirSwipe.UP) 1 else -1)
                            } else try {
                                (getSystemService(AUDIO_SERVICE) as AudioManager).adjustStreamVolume(AudioManager.STREAM_MUSIC,
                                    if (swipe == AirSwipe.UP) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
                                    if (AirSession.visible.value) AudioManager.FLAG_SHOW_UI else 0)
                            } catch (_: SecurityException) {
                                AirSession.error.value = "This device restricts volume changes. Use its volume buttons."
                            }
                        }
                    }
                },
            ).also { it.start() }
        } catch (_: Exception) {
            AirSession.error.value = "Camera controls could not start. Check permission and try while Meloqis is open."
            stopSelf()
        } catch (_: OutOfMemoryError) {
            AirSession.error.value = "Memory is low. Hand gestures stopped; touch controls are still available."
            stopSelf()
        } catch (error: LinkageError) {
            Log.e("MeloqisAir", "Hand SDK service startup failed", error)
            AirSession.error.value = "The hand detector could not load. Touch controls remain available; please report this error with your app version."
            stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun play(event: PlayerEvent) {
        scope.launch {
            try { GlobalContext.get().get<MediaPlayerHandler>().onPlayerEvent(event) }
            catch (_: Exception) { AirSession.error.value = "Playback is unavailable. Start a song in Meloqis and try again." }
        }
    }

    private fun notification(text: String): android.app.Notification {
        fun action(action: String, request: Int) = PendingIntent.getService(this, request,
            Intent(this, AirGestureService::class.java).setAction(action), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val open = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 3, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Meloqis · Hand gestures")
            .setContentText(text).setContentIntent(open).setOngoing(true).setSilent(true)
            .addAction(0, "Play / pause", action(PLAY_PAUSE, 1))
            .addAction(0, "Stop camera", action(STOP, 2)).build()
    }

    override fun onTaskRemoved(rootIntent: Intent?) { stopSelf() }
    override fun onDestroy() {
        camera?.close(); camera = null
        AirSession.active.value = false; AirSession.ready.value = false
        AirSession.handVisible.value = false; AirSession.lastGesture.value = null
        scope.cancel()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    companion object {
        private const val CHANNEL = "meloqis_air_controls"
        private const val NOTIFICATION = 16002
        private const val STOP = "echo.music.air.STOP"
        private const val PLAY_PAUSE = "echo.music.air.PLAY_PAUSE"
    }
}
