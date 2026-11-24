package echo.music.iad1tya.media3.service.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import echo.music.iad1tya.media3.R
import echo.music.iad1tya.media3.service.SimpleMediaService
import echo.music.iad1tya.media3.utils.CoilBitmapLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ExecutionException

@UnstableApi
class PlaybackWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        manager.updateAppWidget(ids, render(context))
        onWidgetAdded?.invoke()
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val action = intent.action
        if (action != TOGGLE && action != PREVIOUS && action != NEXT) return
        // The broadcast context forbids binding to the playback service.
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val token = SessionToken(appContext, ComponentName(appContext, SimpleMediaService::class.java))
                val future = MediaController.Builder(appContext, token).buildAsync()
                try {
                    val player = withTimeoutOrNull(5_000) { future.await() } ?: return@launch
                    when (action) {
                        TOGGLE -> if (player.playWhenReady) player.pause() else {
                            if (player.playbackState == Player.STATE_IDLE) player.prepare()
                            player.play()
                        }
                        PREVIOUS -> if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM)) {
                            player.seekToPreviousMediaItem()
                        } else if (player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM)) {
                            player.seekTo(0)
                        }
                        NEXT -> if (player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM)) {
                            player.seekToNextMediaItem()
                        }
                    }
                } finally {
                    MediaController.releaseFuture(future)
                }
            } catch (_: ExecutionException) {
                // The playback service can disappear as the player is closing.
            } catch (_: IllegalStateException) {
                // Playback may end before the controller finishes connecting.
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val TOGGLE = "echo.music.iad1tya.widget.TOGGLE"
        private const val PREVIOUS = "echo.music.iad1tya.widget.PREVIOUS"
        private const val NEXT = "echo.music.iad1tya.widget.NEXT"
        private const val FLAGS = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT

        private var state = WidgetState()
        internal var onWidgetAdded: (() -> Unit)? = null

        internal fun update(context: Context, next: WidgetState) {
            state = next
            AppWidgetManager.getInstance(context).updateAppWidget(ComponentName(context, PlaybackWidget::class.java), render(context))
        }

        private fun render(context: Context): RemoteViews =
            RemoteViews(context.packageName, R.layout.playback_widget).apply {
                val open = context.packageManager.getLaunchIntentForPackage(context.packageName)
                    ?.let { PendingIntent.getActivity(context, 0, it, FLAGS) }
                setOnClickPendingIntent(R.id.widget_cover, open)
                setOnClickPendingIntent(R.id.widget_track_info, open)
                setTextViewText(R.id.widget_title, state.title ?: context.getString(R.string.playback_widget_name))
                setTextViewText(R.id.widget_artist, state.artist ?: if (state.title == null) {
                    context.getString(R.string.playback_widget_empty)
                } else {
                    ""
                })
                val cover = state.cover
                if (cover == null) setImageViewResource(R.id.widget_cover, R.drawable.playback_widget_note)
                else setImageViewBitmap(R.id.widget_cover, cover)
                setImageViewResource(R.id.widget_toggle, if (state.playing) R.drawable.playback_widget_pause else R.drawable.playback_widget_play)
                setContentDescription(R.id.widget_toggle, context.getString(if (state.playing) R.string.playback_widget_pause else R.string.playback_widget_play))
                setOnClickPendingIntent(R.id.widget_toggle, if (state.title == null) open else action(context, TOGGLE))
                setOnClickPendingIntent(R.id.widget_previous, action(context, PREVIOUS))
                setOnClickPendingIntent(R.id.widget_next, action(context, NEXT))
                setFloat(R.id.widget_previous, "setAlpha", if (state.canGoBack) 1f else 0.38f)
                setFloat(R.id.widget_next, "setAlpha", if (state.canGoNext) 1f else 0.38f)
                setBoolean(R.id.widget_previous, "setEnabled", state.canGoBack)
                setBoolean(R.id.widget_next, "setEnabled", state.canGoNext)
            }

        private fun action(context: Context, action: String): PendingIntent =
            PendingIntent.getBroadcast(context, 0, Intent(context, PlaybackWidget::class.java).setAction(action), FLAGS)
    }
}

internal data class WidgetState(
    val title: String? = null,
    val artist: String? = null,
    val cover: Bitmap? = null,
    val playing: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoNext: Boolean = false,
)

@UnstableApi
internal class PlaybackWidgetUpdater(
    private val context: Context,
    private val player: Player,
    private val bitmapLoader: CoilBitmapLoader,
) : Player.Listener {
    private var artwork: Uri? = null
    private var bitmap: Bitmap? = null
    private var artworkRequest: ListenableFuture<Bitmap>? = null

    fun start() {
        player.addListener(this)
        PlaybackWidget.onWidgetAdded = ::refresh
        refresh()
    }

    fun stop() {
        PlaybackWidget.onWidgetAdded = null
        player.removeListener(this)
        artworkRequest?.cancel(false)
        PlaybackWidget.update(context, WidgetState())
    }

    override fun onEvents(player: Player, events: Player.Events) {
        if (events.containsAny(
                Player.EVENT_MEDIA_METADATA_CHANGED,
                Player.EVENT_MEDIA_ITEM_TRANSITION,
                Player.EVENT_PLAY_WHEN_READY_CHANGED,
                Player.EVENT_AVAILABLE_COMMANDS_CHANGED,
            )
        ) refresh()
    }

    private fun refresh() {
        val manager = AppWidgetManager.getInstance(context)
        if (manager.getAppWidgetIds(ComponentName(context, PlaybackWidget::class.java)).isEmpty()) return
        val nextArtwork = player.mediaMetadata.artworkUri
        if (nextArtwork != artwork) {
            artworkRequest?.cancel(false)
            artwork = nextArtwork
            bitmap = null
            nextArtwork?.let { uri ->
                val future = bitmapLoader.loadBitmap(uri)
                artworkRequest = future
                future.addListener({
                    if (artworkRequest === future && !future.isCancelled) {
                        bitmap = runCatching { Bitmap.createScaledBitmap(future.get(), 128, 128, true) }.getOrNull()
                        render()
                    }
                }, ContextCompat.getMainExecutor(context))
            }
        }
        render()
    }

    private fun render() {
        val hasTrack = player.currentMediaItem != null
        PlaybackWidget.update(context, WidgetState(
            title = player.mediaMetadata.title?.toString()?.takeIf { hasTrack },
            artist = player.mediaMetadata.artist?.toString()?.takeIf { hasTrack },
            cover = bitmap.takeIf { hasTrack },
            playing = hasTrack && player.playWhenReady,
            canGoBack = hasTrack && player.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM),
            canGoNext = hasTrack && player.isCommandAvailable(Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM),
        ))
    }
}
