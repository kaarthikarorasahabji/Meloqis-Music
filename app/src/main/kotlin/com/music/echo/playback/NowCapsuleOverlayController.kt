package iad1tya.echo.music.playback

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.DecelerateInterpolator
import iad1tya.echo.music.models.MediaMetadata
import kotlin.math.abs
import kotlin.math.sin

/**
 * Optional playback overlay for Android 13+ devices with a centred camera cutout.
 *
 * This controller reads only Meloqis playback state. It does not use notification
 * access, accessibility services, or inspect any other app.
 */
class NowCapsuleOverlayController(
    context: Context,
    private val onTogglePlayback: () -> Unit,
    private val onPrevious: () -> Unit,
    private val onNext: () -> Unit,
    private val onOpenApp: () -> Unit,
) {
    private val appContext = context.applicationContext
    private val windowManager = appContext.getSystemService(WindowManager::class.java)
    private val view = NowCapsuleView(
        context = appContext,
        onTogglePlayback = onTogglePlayback,
        onPrevious = onPrevious,
        onNext = onNext,
        onOpenApp = onOpenApp,
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
        y = 2.dp
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
        if (!attached || (widthPx == targetWidth && heightPx == targetHeight)) return

        val startWidth = widthPx
        val startHeight = heightPx
        sizeAnimator?.cancel()
        sizeAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 240L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animator ->
                val fraction = animator.animatedFraction
                widthPx = (startWidth + (targetWidth - startWidth) * fraction).toInt()
                heightPx = (startHeight + (targetHeight - startHeight) * fraction).toInt()
                layoutParams.width = widthPx
                layoutParams.height = heightPx
                runCatching { windowManager.updateViewLayout(view, layoutParams) }
                view.invalidate()
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
    private val onOpenApp: () -> Unit,
    private val onSizeChanged: (Boolean) -> Unit,
) : View(context) {
    private val density = resources.displayMetrics.density
    val collapsedWidth = 154.dp
    val collapsedHeight = 40.dp
    val expandedWidth = minOf(360.dp, resources.displayMetrics.widthPixels - 24.dp)
    val expandedHeight = 104.dp

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(5, 5, 7) }
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(147, 92, 246)
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3.dp.toFloat()
    }
    private val primaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14.sp
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val secondaryTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(182, 182, 191)
        textSize = 11.sp
    }
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 2.dp.toFloat()
    }
    private val pill = RectF()
    private var title = ""
    private var artists = ""
    private var isPlaying = false
    private var expanded = false
    private var touchDownAt = 0L
    private var touchDownX = 0f
    private var touchDownY = 0f

    fun update(metadata: MediaMetadata, playing: Boolean) {
        title = metadata.title
        artists = metadata.artists.joinToString(", ") { it.name }
        isPlaying = playing
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val radius = if (expanded) 24.dp.toFloat() else height / 2f
        pill.set(0f, 0f, width.toFloat(), height.toFloat())
        canvas.drawRoundRect(pill, radius, radius, backgroundPaint)
        if (expanded) drawExpanded(canvas) else drawCollapsed(canvas)
        if (isPlaying) postInvalidateOnAnimation()
    }

    private fun drawCollapsed(canvas: Canvas) {
        drawEqualizer(canvas, 18.dp.toFloat(), height / 2f)
        primaryTextPaint.textSize = 12.sp
        drawEllipsized(
            canvas,
            if (title.isBlank()) "Meloqis" else title,
            primaryTextPaint,
            38.dp.toFloat(),
            height / 2f - (primaryTextPaint.ascent() + primaryTextPaint.descent()) / 2f,
            width - 72.dp.toFloat(),
        )
        drawPlayPause(canvas, width - 22.dp.toFloat(), height / 2f, 8.dp.toFloat())
    }

    private fun drawExpanded(canvas: Canvas) {
        drawEqualizer(canvas, 24.dp.toFloat(), 29.dp.toFloat())
        primaryTextPaint.textSize = 15.sp
        drawEllipsized(canvas, title.ifBlank { "Meloqis Music" }, primaryTextPaint, 48.dp.toFloat(), 27.dp.toFloat(), width - 66.dp.toFloat())
        drawEllipsized(canvas, artists, secondaryTextPaint, 48.dp.toFloat(), 46.dp.toFloat(), width - 66.dp.toFloat())

        val controlsY = 77.dp.toFloat()
        drawPrevious(canvas, width * 0.31f, controlsY)
        drawPlayPause(canvas, width * 0.5f, controlsY, 11.dp.toFloat())
        drawNext(canvas, width * 0.69f, controlsY)
    }

    private fun drawEqualizer(canvas: Canvas, centerX: Float, centerY: Float) {
        val time = if (isPlaying) System.currentTimeMillis() / 150.0 else 0.0
        repeat(3) { index ->
            val animated = if (isPlaying) abs(sin(time + index * 1.3)).toFloat() else 0.28f
            val barHeight = (5.dp + animated * 12.dp)
            val x = centerX + (index - 1) * 6.dp
            canvas.drawLine(x, centerY - barHeight / 2f, x, centerY + barHeight / 2f, accentPaint)
        }
    }

    private fun drawPlayPause(canvas: Canvas, centerX: Float, centerY: Float, radius: Float) {
        iconPaint.style = Paint.Style.FILL
        if (isPlaying) {
            val halfHeight = radius
            canvas.drawRoundRect(centerX - 5.dp, centerY - halfHeight, centerX - 1.dp, centerY + halfHeight, 1.dp.toFloat(), 1.dp.toFloat(), iconPaint)
            canvas.drawRoundRect(centerX + 1.dp, centerY - halfHeight, centerX + 5.dp, centerY + halfHeight, 1.dp.toFloat(), 1.dp.toFloat(), iconPaint)
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

    private fun drawEllipsized(canvas: Canvas, value: String, paint: Paint, x: Float, baseline: Float, availableWidth: Float) {
        if (value.isBlank()) return
        var output = value
        if (paint.measureText(output) > availableWidth) {
            while (output.length > 1 && paint.measureText("$output…") > availableWidth) {
                output = output.dropLast(1)
            }
            output += "…"
        }
        canvas.drawText(output, x, baseline, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                touchDownAt = System.currentTimeMillis()
                touchDownX = event.x
                touchDownY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val heldFor = System.currentTimeMillis() - touchDownAt
                if (heldFor >= 650L && abs(event.x - touchDownX) < 16.dp && abs(event.y - touchDownY) < 16.dp) {
                    onOpenApp()
                    return true
                }
                if (!expanded) {
                    expanded = true
                    onSizeChanged(true)
                    invalidate()
                    return true
                }
                when {
                    event.y >= 56.dp && event.x < width * 0.4f -> onPrevious()
                    event.y >= 56.dp && event.x < width * 0.6f -> onTogglePlayback()
                    event.y >= 56.dp -> onNext()
                    else -> {
                        expanded = false
                        onSizeChanged(false)
                    }
                }
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private val Int.dp: Int
        get() = (this * density).toInt()

    private val Int.sp: Float
        get() = this * resources.displayMetrics.scaledDensity
}
