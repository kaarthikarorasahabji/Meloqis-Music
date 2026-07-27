package iad1tya.echo.music.playback

import android.animation.ValueAnimator
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.view.animation.PathInterpolator
import android.view.animation.OvershootInterpolator
import iad1tya.echo.music.models.MediaMetadata
import java.net.URL
import java.util.Locale
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/**
 * Optional, camera-aware Meloqis playback overlay.
 *
 * The capsule reads only the active Meloqis player. It does not use notification
 * access, accessibility services, or inspect any other application.
 */
class NowCapsuleOverlayController(
    context: Context,
    private val onTogglePlayback: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit,
    private val onSeekTo: (Long) -> Unit,
    private val onOpenApp: () -> Unit,
    positionProvider: () -> Long,
    durationProvider: () -> Long,
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val view = NowCapsuleView(
        context = appContext,
        onTogglePlayback = onTogglePlayback,
        onPrevious = onPrevious,
        onNext = onNext,
        onSeekTo = onSeekTo,
        onOpenApp = onOpenApp,
        positionProvider = positionProvider,
        durationProvider = durationProvider,
        onSizeChanged = ::animateSize,
    )
    private var attached = false
    private var widthPx = view.collapsedWidth
    private var heightPx = view.collapsedHeight
    private val layoutParams = WindowManager.LayoutParams(
        widthPx,
        heightPx,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        android.graphics.PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        y = 4.dp
        windowAnimations = 0
    }
    private var sizeAnimator: ValueAnimator? = null

    fun showOrUpdate(metadata: MediaMetadata?, isPlaying: Boolean) {
        if (!Settings.canDrawOverlays(appContext) || metadata == null) {
            dispose()
            return
        }
        view.update(metadata, isPlaying)
        if (!attached) {
            runCatching {
                windowManager.addView(view, layoutParams)
                attached = true
                view.alpha = 0f
                view.scaleX = 0.86f
                view.scaleY = 0.86f
                view.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(420L)
                    .setInterpolator(OvershootInterpolator(1.15f))
                    .start()
            }
        }
    }

    fun dispose() {
        sizeAnimator?.cancel()
        if (attached) {
            runCatching { windowManager.removeViewImmediate(view) }
            attached = false
        }
    }

    private fun animateSize(expanded: Boolean) {
        val targetWidth = if (expanded) view.expandedWidth else view.collapsedWidth
        val targetHeight = if (expanded) view.expandedHeight else view.collapsedHeight
        val endProgress = if (expanded) 1f else 0f
        view.setExpandedTarget(expanded)

        if (!attached) {
            widthPx = targetWidth
            heightPx = targetHeight
            view.setExpansionProgress(endProgress)
            return
        }
        if (widthPx == targetWidth && heightPx == targetHeight) {
            view.setExpansionProgress(endProgress)
            return
        }

        val startWidth = widthPx
        val startHeight = heightPx
        val startProgress = view.expansionProgress
        sizeAnimator?.cancel()
        sizeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 340L
            interpolator = PathInterpolator(0.22f, 1f, 0.36f, 1f)
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                widthPx = (startWidth + (targetWidth - startWidth) * fraction).toInt()
                heightPx = (startHeight + (targetHeight - startHeight) * fraction).toInt()
                layoutParams.width = widthPx
                layoutParams.height = heightPx
                view.setExpansionProgress(startProgress + (endProgress - startProgress) * fraction)
                runCatching { windowManager.updateViewLayout(view, layoutParams) }
            }
            start()
        }
    }

    private val Int.dp: Int
        get() = (this * appContext.resources.displayMetrics.density).toInt()
}

private class NowCapsuleView(
    context: Context,
    private val onTogglePlayback: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit,
    private val onSeekTo: (Long) -> Unit,
    private val onOpenApp: () -> Unit,
    private val positionProvider: () -> Long,
    private val durationProvider: () -> Long,
    private val onSizeChanged: (Boolean) -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

    val collapsedWidth = 190.dp
    val collapsedHeight = 48.dp
    val expandedWidth = minOf(390.dp, resources.displayMetrics.widthPixels - 16.dp)
    val expandedHeight = 152.dp

    private val capsuleRect = RectF()
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.dp.toFloat()
    }
    private val artworkPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val artworkBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val artworkRingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.dp.toFloat()
        color = Color.argb(95, 255, 255, 255)
    }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(157, 113, 255)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3.dp.toFloat()
    }
    private val progressTrackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(52, 255, 255, 255)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3.dp.toFloat()
    }
    private val controlSurfacePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(28, 255, 255, 255)
    }
    private val auraPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val motionBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f * density
    }
    private val artworkOrbitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 2.dp.toFloat()
    }
    private val primaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(173, 169, 183)
    }
    private val labelTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(185, 154, 255)
        textSize = 8.sp
        letterSpacing = 0.13f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2.dp.toFloat()
    }

    private var title = ""
    private var artists = ""
    private var artworkUrl: String? = null
    private var artworkBitmap: Bitmap? = null
    private val artworkExecutor = Executors.newSingleThreadExecutor()
    private var accentColor = Color.rgb(157, 113, 255)
    private var accentColorDeep = Color.rgb(55, 28, 104)
    private var previousAccentColor = accentColor
    private var accentTransition = 1f
    private var accentAnimator: ValueAnimator? = null
    private var pressScale = 1f
    private var pressAnimator: ValueAnimator? = null
    private var swipeOffsetX = 0f
    private var swipeAnimator: ValueAnimator? = null
    private var horizontalGesture = false
    private var isPlaying = false
    private var expandedTarget = false
    var expansionProgress = 0f
        private set
    private var touchDownAt = 0L
    private var touchDownX = 0f
    private var touchDownY = 0f
    private var scrubbing = false
    private var scrubPosition = 0L
    private var lastTapAt = 0L
    private var songMotionSeed = 0

    init {
        isClickable = true
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun update(metadata: MediaMetadata, playing: Boolean) {
        title = metadata.title
        artists = metadata.artists.joinToString(", ") { it.name }
        val songIdentity = "${metadata.id}|${metadata.thumbnailUrl}|$title|$artists"
        songMotionSeed = songIdentity.hashCode()
        val hue = ((songIdentity.hashCode().toLong() and 0x7fffffffL) % 360L).toFloat()
        transitionAccent(
            Color.HSVToColor(floatArrayOf(hue, 0.62f, 1f)),
            Color.HSVToColor(floatArrayOf(hue, 0.78f, 0.35f)),
        )
        loadArtwork(metadata.thumbnailUrl)
        isPlaying = playing
        contentDescription = "${title.ifBlank { "Meloqis Music" }} by ${artists.ifBlank { "Meloqis" }}. " +
            (if (playing) "Playing. " else "Paused. ") +
            "Swipe left or right to change track, tap to expand, double tap to play or pause, and hold to open Meloqis."
        invalidate()
    }

    private fun loadArtwork(url: String?) {
        if (url.isNullOrBlank() || url == artworkUrl) return
        artworkUrl = url
        val requestedUrl = url
        artworkExecutor.execute {
            val decoded = runCatching {
                URL(requestedUrl).openConnection().apply {
                    connectTimeout = 5_000
                    readTimeout = 8_000
                }.getInputStream().use(BitmapFactory::decodeStream)
            }.getOrNull()
            if (decoded != null) {
                val palette = extractArtworkPalette(decoded)
                post {
                    if (artworkUrl == requestedUrl) {
                        artworkBitmap?.takeIf { it !== decoded && !it.isRecycled }?.recycle()
                        artworkBitmap = decoded
                        transitionAccent(palette.first, palette.second)
                        invalidate()
                    } else {
                        decoded.recycle()
                    }
                }
            }
        }
    }

    private fun extractArtworkPalette(bitmap: Bitmap): Pair<Int, Int> {
        var red = 0L
        var green = 0L
        var blue = 0L
        var weightTotal = 0L
        val stepX = max(1, bitmap.width / 12)
        val stepY = max(1, bitmap.height / 12)
        for (x in stepX / 2 until bitmap.width step stepX) {
            for (y in stepY / 2 until bitmap.height step stepY) {
                val pixel = bitmap.getPixel(x, y)
                val maxChannel = max(Color.red(pixel), max(Color.green(pixel), Color.blue(pixel)))
                val minChannel = min(Color.red(pixel), min(Color.green(pixel), Color.blue(pixel)))
                val saturationWeight = 1L + (maxChannel - minChannel).toLong()
                red += Color.red(pixel) * saturationWeight
                green += Color.green(pixel) * saturationWeight
                blue += Color.blue(pixel) * saturationWeight
                weightTotal += saturationWeight
            }
        }
        if (weightTotal == 0L) return accentColor to accentColorDeep
        val base = Color.rgb(
            (red / weightTotal).toInt(),
            (green / weightTotal).toInt(),
            (blue / weightTotal).toInt(),
        )
        val hsv = FloatArray(3)
        Color.colorToHSV(base, hsv)
        hsv[1] = max(0.5f, hsv[1])
        hsv[2] = max(0.82f, hsv[2])
        val bright = Color.HSVToColor(hsv)
        hsv[1] = min(0.9f, hsv[1] + 0.15f)
        hsv[2] = 0.32f
        return bright to Color.HSVToColor(hsv)
    }

    private fun transitionAccent(nextAccent: Int, nextDeep: Int) {
        if (nextAccent == accentColor) return
        previousAccentColor = currentAccent()
        val startDeep = accentColorDeep
        accentAnimator?.cancel()
        accentTransition = 0f
        accentAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 650L
            interpolator = PathInterpolator(0.2f, 0.8f, 0.2f, 1f)
            addUpdateListener {
                accentTransition = it.animatedFraction
                accentColor = blend(previousAccentColor, nextAccent, accentTransition)
                accentColorDeep = blend(startDeep, nextDeep, accentTransition)
                accentPaint.color = accentColor
                labelTextPaint.color = accentColor
                invalidate()
            }
            start()
        }
    }

    private fun currentAccent(): Int = accentColor

    fun setExpandedTarget(expanded: Boolean) {
        expandedTarget = expanded
        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun setExpansionProgress(progress: Float) {
        expansionProgress = progress.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pressSave = canvas.save()
        canvas.scale(pressScale, pressScale, width / 2f, height / 2f)
        val p = expansionProgress
        val radius = lerp(collapsedHeight / 2f, 27.dp.toFloat(), p)
        capsuleRect.set(2.dp.toFloat(), 2.dp.toFloat(), width - 2.dp.toFloat(), height - 2.dp.toFloat())

        backgroundPaint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(Color.rgb(7, 7, 10), blend(Color.rgb(15, 11, 22), accentColorDeep, 0.42f), Color.rgb(5, 5, 8)),
            null,
            Shader.TileMode.CLAMP,
        )
        backgroundPaint.setShadowLayer(16.dp.toFloat(), 0f, 7.dp.toFloat(), Color.argb(165, 0, 0, 0))
        canvas.drawRoundRect(capsuleRect, radius, radius, backgroundPaint)
        backgroundPaint.clearShadowLayer()

        borderPaint.shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            withAlpha(accentColor, 145),
            Color.argb(18, 255, 255, 255),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(capsuleRect, radius, radius, borderPaint)

        drawPlaybackAura(canvas)
        drawMovingBorderHighlight(canvas, radius)

        val swipeFraction = (abs(swipeOffsetX) / (width * 0.42f).coerceAtLeast(1f)).coerceIn(0f, 1f)
        val contentLayer = canvas.saveLayerAlpha(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            (255f * (1f - swipeFraction * 0.48f)).toInt(),
        )
        canvas.translate(swipeOffsetX, 0f)
        if (p < 1f) {
            val save = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), ((1f - p) * 255).toInt())
            drawCollapsed(canvas)
            canvas.restoreToCount(save)
        }
        if (p > 0f) {
            val save = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (p * 255).toInt())
            drawAmbientEqualizer(canvas)
            drawExpanded(canvas)
            canvas.restoreToCount(save)
        }
        canvas.restoreToCount(contentLayer)
        drawSwipeAffordance(canvas, swipeFraction)

        if (isPlaying || swipeAnimator?.isRunning == true) postInvalidateOnAnimation()
        canvas.restoreToCount(pressSave)
    }

    private fun drawPlaybackAura(canvas: Canvas) {
        if (!isPlaying) return
        val time = SystemClock.uptimeMillis() / 900.0
        val pulse = (0.5f + sin(time).toFloat() * 0.5f).coerceIn(0f, 1f)
        val centerX = width * (0.28f + pulse * 0.42f)
        auraPaint.shader = RadialGradient(
            centerX,
            height * 0.56f,
            width * 0.46f,
            intArrayOf(withAlpha(accentColor, 25), withAlpha(accentColorDeep, 10), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(capsuleRect, collapsedHeight / 2f, collapsedHeight / 2f, auraPaint)
    }

    private fun drawMovingBorderHighlight(canvas: Canvas, radius: Float) {
        if (!isPlaying) return
        val travel = ((SystemClock.uptimeMillis() % 2600L) / 2600f)
        val start = -width + travel * width * 2f
        motionBorderPaint.shader = LinearGradient(
            start,
            0f,
            start + width * 0.72f,
            height.toFloat(),
            intArrayOf(Color.TRANSPARENT, withAlpha(accentColor, 175), Color.argb(125, 255, 255, 255), Color.TRANSPARENT),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawRoundRect(capsuleRect, radius, radius, motionBorderPaint)
    }

    private fun drawSwipeAffordance(canvas: Canvas, fraction: Float) {
        if (fraction <= 0.02f) return
        val alpha = (fraction * 220).toInt().coerceIn(0, 220)
        iconPaint.alpha = alpha
        val centerY = height / 2f
        if (swipeOffsetX < 0f) {
            drawNext(canvas, width - 17.dp.toFloat(), centerY)
        } else {
            drawPrevious(canvas, 17.dp.toFloat(), centerY)
        }
        iconPaint.alpha = 255
    }

    private fun drawCollapsed(canvas: Canvas) {
        drawArtwork(canvas, 25.dp.toFloat(), height / 2f, 17.dp.toFloat(), compact = true)

        primaryTextPaint.textSize = 12.sp
        drawEllipsized(
            canvas,
            title.ifBlank { "Meloqis Music" },
            primaryTextPaint,
            50.dp.toFloat(),
            20.dp.toFloat(),
            width - 94.dp.toFloat(),
        )
        secondaryTextPaint.textSize = 9.sp
        drawEllipsized(
            canvas,
            artists.ifBlank { if (isPlaying) "Now playing" else "Paused" },
            secondaryTextPaint,
            50.dp.toFloat(),
            35.dp.toFloat(),
            width - 94.dp.toFloat(),
        )
        drawControlCircle(canvas, width - 25.dp.toFloat(), height / 2f, 16.dp.toFloat())
        drawPlayPause(canvas, width - 25.dp.toFloat(), height / 2f, 7.dp.toFloat())
    }

    private fun drawExpanded(canvas: Canvas) {
        drawArtwork(canvas, 39.dp.toFloat(), 38.dp.toFloat(), 26.dp.toFloat(), compact = false)

        primaryTextPaint.textSize = 15.sp
        drawEllipsized(canvas, title.ifBlank { "Meloqis Music" }, primaryTextPaint, 76.dp.toFloat(), 31.dp.toFloat(), width - 128.dp.toFloat())
        secondaryTextPaint.textSize = 11.sp
        drawEllipsized(canvas, artists.ifBlank { "Meloqis" }, secondaryTextPaint, 76.dp.toFloat(), 50.dp.toFloat(), width - 128.dp.toFloat())

        canvas.drawText("MELOQIS", width - 75.dp.toFloat(), 21.dp.toFloat(), labelTextPaint)
        drawCollapseChevron(canvas, width - 24.dp.toFloat(), 43.dp.toFloat())

        val progressStart = 16.dp.toFloat()
        val progressEnd = width - 16.dp.toFloat()
        val progressY = 79.dp.toFloat()
        canvas.drawLine(progressStart, progressY, progressEnd, progressY, progressTrackPaint)
        val duration = durationProvider().takeIf { it > 0 } ?: 0L
        val position = if (scrubbing) scrubPosition else positionProvider().coerceAtLeast(0L)
        val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
        if (progress > 0f) {
            val progressX = progressStart + (progressEnd - progressStart) * progress
            accentPaint.setShadowLayer(7.dp.toFloat(), 0f, 0f, withAlpha(accentColor, 180))
            canvas.drawLine(progressStart, progressY, progressX, progressY, accentPaint)
            accentPaint.clearShadowLayer()
            canvas.drawCircle(progressX, progressY, if (scrubbing) 5.dp.toFloat() else 3.dp.toFloat(), accentPaint)
        }

        secondaryTextPaint.textSize = 9.sp
        canvas.drawText(formatTime(position), progressStart, 96.dp.toFloat(), secondaryTextPaint)
        val durationText = formatTime(duration)
        canvas.drawText(durationText, progressEnd - secondaryTextPaint.measureText(durationText), 96.dp.toFloat(), secondaryTextPaint)

        val controlsY = 124.dp.toFloat()
        drawPrevious(canvas, width * 0.31f, controlsY)
        drawControlCircle(canvas, width * 0.5f, controlsY, 22.dp.toFloat())
        drawPlayPause(canvas, width * 0.5f, controlsY, 10.dp.toFloat())
        drawNext(canvas, width * 0.69f, controlsY)
    }

    private fun drawArtwork(canvas: Canvas, centerX: Float, centerY: Float, radius: Float, compact: Boolean) {
        artworkPaint.shader = RadialGradient(
            centerX - radius * 0.35f,
            centerY - radius * 0.35f,
            radius * 1.8f,
            intArrayOf(blend(accentColor, Color.WHITE, 0.38f), accentColor, accentColorDeep),
            null,
            Shader.TileMode.CLAMP,
        )
        canvas.drawCircle(centerX, centerY, radius, artworkPaint)
        artworkBitmap?.takeIf { !it.isRecycled }?.let { bitmap ->
            val cropSize = min(bitmap.width, bitmap.height)
            val cropLeft = (bitmap.width - cropSize) / 2
            val cropTop = (bitmap.height - cropSize) / 2
            val source = Rect(cropLeft, cropTop, cropLeft + cropSize, cropTop + cropSize)
            val destination = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
            val clipSave = canvas.save()
            canvas.clipPath(Path().apply { addCircle(centerX, centerY, radius, Path.Direction.CW) })
            canvas.drawBitmap(bitmap, source, destination, artworkBitmapPaint)
            canvas.drawCircle(
                centerX - radius * 0.35f,
                centerY - radius * 0.4f,
                radius * 0.75f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    shader = RadialGradient(
                        centerX - radius * 0.35f,
                        centerY - radius * 0.4f,
                        radius,
                        Color.argb(45, 255, 255, 255),
                        Color.TRANSPARENT,
                        Shader.TileMode.CLAMP,
                    )
                },
            )
            canvas.restoreToCount(clipSave)
        }
        canvas.drawCircle(centerX, centerY, radius - 1.dp, artworkRingPaint)
        if (isPlaying) {
            val orbit = ((SystemClock.uptimeMillis() % 3200L) / 3200f) * 360f
            artworkOrbitPaint.color = withAlpha(accentColor, 205)
            artworkOrbitPaint.setShadowLayer(5.dp.toFloat(), 0f, 0f, withAlpha(accentColor, 145))
            canvas.drawArc(
                RectF(centerX - radius - 1.dp, centerY - radius - 1.dp, centerX + radius + 1.dp, centerY + radius + 1.dp),
                orbit,
                if (compact) 72f else 104f,
                false,
                artworkOrbitPaint,
            )
            artworkOrbitPaint.clearShadowLayer()
        }
        if (compact) {
            drawEqualizer(canvas, centerX, centerY, 5)
        } else if (artworkBitmap == null) {
            val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
                strokeWidth = 2.dp.toFloat()
            }
            val path = Path().apply {
                moveTo(centerX - 12.dp, centerY + 7.dp)
                lineTo(centerX - 12.dp, centerY - 8.dp)
                lineTo(centerX - 4.dp, centerY + 3.dp)
                lineTo(centerX + 4.dp, centerY - 11.dp)
                lineTo(centerX + 12.dp, centerY + 3.dp)
                lineTo(centerX + 12.dp, centerY - 8.dp)
            }
            canvas.drawPath(path, markPaint)
        }
    }

    private fun drawEqualizer(canvas: Canvas, centerX: Float, centerY: Float, bars: Int) {
        val time = if (isPlaying) SystemClock.uptimeMillis() / 130.0 else 0.0
        repeat(bars) { index ->
            val animated = if (isPlaying) abs(sin(time + index * 1.35)).toFloat() else 0.28f
            val barHeight = 4.dp + animated * 10.dp
            val x = centerX + (index - (bars - 1) / 2f) * 5.dp
            canvas.drawLine(
                x,
                centerY - barHeight / 2f,
                x,
                centerY + barHeight / 2f,
                accentPaint.apply { color = blend(accentColor, Color.WHITE, 0.58f) }
            )
        }
        accentPaint.color = accentColor
    }

    private fun drawAmbientEqualizer(canvas: Canvas) {
        val time = if (isPlaying) SystemClock.uptimeMillis() / 190.0 else 0.0
        val baseline = 68.dp.toFloat()
        val left = 84.dp.toFloat()
        val available = (width - left - 26.dp).coerceAtLeast(1f)
        val bars = 22
        val barWidth = available / bars
        accentPaint.strokeWidth = 2.dp.toFloat()
        repeat(bars) { index ->
            val songOffset = ((songMotionSeed ushr (index % 16)) and 7) * 0.09
            val pulse = if (isPlaying) {
                (0.22f + abs(sin(time + index * (0.51 + songOffset))).toFloat() * 0.78f)
            } else {
                0.18f
            }
            val barHeight = 2.dp + pulse * 9.dp
            val x = left + index * barWidth
            accentPaint.color = withAlpha(accentColor, (36 + pulse * 78).toInt())
            canvas.drawLine(x, baseline - barHeight, x, baseline, accentPaint)
        }
        accentPaint.strokeWidth = 3.dp.toFloat()
        accentPaint.color = accentColor
    }

    private fun drawControlCircle(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        val pulse = if (isPlaying) {
            1f + sin(SystemClock.uptimeMillis() / 240.0).toFloat() * 0.035f
        } else {
            1f
        }
        canvas.drawCircle(centerX, centerY, radius * pulse, controlSurfacePaint)
    }

    private fun drawPlayPause(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        iconPaint.style = Paint.Style.FILL
        if (isPlaying) {
            canvas.drawRoundRect(centerX - 5.dp, centerY - radius, centerX - 1.dp, centerY + radius, 1.dp.toFloat(), 1.dp.toFloat(), iconPaint)
            canvas.drawRoundRect(centerX + 1.dp, centerY - radius, centerX + 5.dp, centerY + radius, 1.dp.toFloat(), 1.dp.toFloat(), iconPaint)
        } else {
            val path = Path().apply {
                moveTo(centerX - radius * 0.55f, centerY - radius)
                lineTo(centerX + radius, centerY)
                lineTo(centerX - radius * 0.55f, centerY + radius)
                close()
            }
            canvas.drawPath(path, iconPaint)
        }
        iconPaint.style = Paint.Style.STROKE
    }

    private fun drawPrevious(canvas: Canvas, centerX: Float, centerY: Float) {
        canvas.drawLine(centerX - 8.dp, centerY - 8.dp, centerX - 8.dp, centerY + 8.dp, iconPaint)
        canvas.drawLine(centerX + 7.dp, centerY - 8.dp, centerX - 7.dp, centerY, iconPaint)
        canvas.drawLine(centerX - 7.dp, centerY, centerX + 7.dp, centerY + 8.dp, iconPaint)
    }

    private fun drawNext(canvas: Canvas, centerX: Float, centerY: Float) {
        canvas.drawLine(centerX + 8.dp, centerY - 8.dp, centerX + 8.dp, centerY + 8.dp, iconPaint)
        canvas.drawLine(centerX - 7.dp, centerY - 8.dp, centerX + 7.dp, centerY, iconPaint)
        canvas.drawLine(centerX + 7.dp, centerY, centerX - 7.dp, centerY + 8.dp, iconPaint)
    }

    private fun drawCollapseChevron(canvas: Canvas, centerX: Float, centerY: Float) {
        canvas.drawLine(centerX - 5.dp, centerY + 3.dp, centerX, centerY - 2.dp, iconPaint)
        canvas.drawLine(centerX, centerY - 2.dp, centerX + 5.dp, centerY + 3.dp, iconPaint)
    }

    private fun drawEllipsized(canvas: Canvas, value: String, paint: Paint, x: Float, baseline: Float, availableWidth: Float) {
        if (value.isBlank() || availableWidth <= 0f) return
        var output = value
        while (output.length > 1 && paint.measureText("$output\u2026") > availableWidth) {
            output = output.dropLast(1)
        }
        if (output != value) output += "\u2026"
        canvas.drawText(output, x, baseline, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownAt = SystemClock.uptimeMillis()
                touchDownX = event.x
                touchDownY = event.y
                horizontalGesture = false
                scrubbing = expandedTarget && event.y in 64.dp.toFloat()..94.dp.toFloat()
                if (scrubbing) updateScrubPosition(event.x)
                animatePress(0.975f, 90L, overshoot = false)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (scrubbing) {
                    updateScrubPosition(event.x)
                    invalidate()
                } else {
                    val deltaX = event.x - touchDownX
                    val deltaY = event.y - touchDownY
                    if (!horizontalGesture &&
                        abs(deltaX) > touchSlop &&
                        abs(deltaX) > abs(deltaY) * 1.15f
                    ) {
                        horizontalGesture = true
                    }
                    if (horizontalGesture) {
                        val maxOffset = width * 0.42f
                        swipeOffsetX = (deltaX * 0.58f).coerceIn(-maxOffset, maxOffset)
                        invalidate()
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                scrubbing = false
                horizontalGesture = false
                animateSwipeOffset(0f, 220L)
                animatePress(1f, 260L, overshoot = true)
                return true
            }
            MotionEvent.ACTION_UP -> {
                animatePress(1f, 340L, overshoot = true)
                if (scrubbing) {
                    scrubbing = false
                    onSeekTo(scrubPosition)
                    performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                    invalidate()
                    return true
                }
                val heldFor = SystemClock.uptimeMillis() - touchDownAt
                val deltaX = event.x - touchDownX
                val deltaY = event.y - touchDownY
                if (horizontalGesture) {
                    horizontalGesture = false
                    val threshold = max(44.dp.toFloat(), width * 0.16f)
                    val isFastSwipe = heldFor < 320L && abs(deltaX) > 24.dp
                    if (abs(deltaX) >= threshold || isFastSwipe) {
                        performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                        animateSwipeCommit(if (deltaX < 0f) -1 else 1)
                    } else {
                        animateSwipeOffset(0f, 220L)
                    }
                    return true
                }
                if (heldFor >= 600L && abs(deltaX) < touchSlop && abs(deltaY) < touchSlop) {
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    onOpenApp()
                    return true
                }
                if (deltaY < -touchSlop && expandedTarget) {
                    onSizeChanged(false)
                    return true
                }
                if (deltaY > touchSlop && !expandedTarget) {
                    onSizeChanged(true)
                    return true
                }
                if (!expandedTarget) {
                    if (event.x >= width - 58.dp) {
                        onTogglePlayback()
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        return true
                    }
                    val now = SystemClock.uptimeMillis()
                    if (now - lastTapAt < 320L) {
                        onTogglePlayback()
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        lastTapAt = 0L
                        return true
                    }
                    lastTapAt = now
                    onSizeChanged(true)
                    return true
                }

                when {
                    event.x >= width - 56.dp && event.y <= 64.dp -> onSizeChanged(false)
                    event.y >= 100.dp && event.x < width * 0.41f -> {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onPrevious()
                    }
                    event.y >= 100.dp && event.x < width * 0.59f -> {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onTogglePlayback()
                    }
                    event.y >= 100.dp && event.x < width * 0.79f -> {
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onNext()
                    }
                    event.y < 68.dp -> onOpenApp()
                    else -> onSizeChanged(false)
                }
                performClick()
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun updateScrubPosition(x: Float) {
        val duration = durationProvider().takeIf { it > 0 } ?: return
        val start = 16.dp.toFloat()
        val end = width - 16.dp.toFloat()
        val fraction = ((x - start) / (end - start)).coerceIn(0f, 1f)
        scrubPosition = (duration * fraction).toLong()
    }

    private fun animatePress(target: Float, durationMs: Long, overshoot: Boolean) {
        pressAnimator?.cancel()
        pressAnimator = ValueAnimator.ofFloat(pressScale, target).apply {
            duration = durationMs
            interpolator = if (overshoot) OvershootInterpolator(2.2f) else PathInterpolator(0.2f, 0f, 0f, 1f)
            addUpdateListener {
                pressScale = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    private fun animateSwipeCommit(direction: Int) {
        swipeAnimator?.cancel()
        val exitTarget = direction * width * 0.42f
        swipeAnimator = ValueAnimator.ofFloat(swipeOffsetX, exitTarget).apply {
            duration = 125L
            interpolator = PathInterpolator(0.4f, 0f, 1f, 1f)
            addUpdateListener {
                swipeOffsetX = it.animatedValue as Float
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (direction < 0) onNext() else onPrevious()
                    swipeOffsetX = -direction * width * 0.30f
                    animateSwipeOffset(0f, 300L, overshoot = true)
                }
            })
            start()
        }
    }

    private fun animateSwipeOffset(target: Float, durationMs: Long, overshoot: Boolean = false) {
        swipeAnimator?.cancel()
        swipeAnimator = ValueAnimator.ofFloat(swipeOffsetX, target).apply {
            duration = durationMs
            interpolator = if (overshoot) {
                OvershootInterpolator(0.85f)
            } else {
                PathInterpolator(0.22f, 1f, 0.36f, 1f)
            }
            addUpdateListener {
                swipeOffsetX = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun release() {
        accentAnimator?.cancel()
        pressAnimator?.cancel()
        swipeAnimator?.cancel()
        artworkExecutor.shutdownNow()
        artworkBitmap?.takeIf { !it.isRecycled }?.recycle()
        artworkBitmap = null
    }

    private fun formatTime(milliseconds: Long): String {
        if (milliseconds <= 0L) return "0:00"
        val totalSeconds = milliseconds / 1000L
        return String.format(Locale.US, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }

    private fun lerp(start: Float, end: Float, fraction: Float): Float = start + (end - start) * fraction

    private fun blend(first: Int, second: Int, ratio: Float): Int {
        val t = ratio.coerceIn(0f, 1f)
        return Color.rgb(
            (Color.red(first) + (Color.red(second) - Color.red(first)) * t).toInt(),
            (Color.green(first) + (Color.green(second) - Color.green(first)) * t).toInt(),
            (Color.blue(first) + (Color.blue(second) - Color.blue(first)) * t).toInt(),
        )
    }

    private fun withAlpha(color: Int, alpha: Int): Int =
        Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))

    private val Int.dp: Int
        get() = (this * density).toInt()

    private val Int.sp: Float
        get() = this * resources.displayMetrics.scaledDensity
}
