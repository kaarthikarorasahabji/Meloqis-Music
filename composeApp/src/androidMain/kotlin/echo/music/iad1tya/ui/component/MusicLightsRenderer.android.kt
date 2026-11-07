package echo.music.iad1tya.ui.component

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import android.view.View
import coil3.SingletonImageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import echo.music.iad1tya.domain.mediaservice.handler.MediaPlayerHandler
import echo.music.iad1tya.domain.mediaservice.player.AudioSpectrum
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import org.koin.core.context.GlobalContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

/** Shared renderer: preview, transparent floating layer, clock activity and Android dream. */
internal class MusicLightsView(
    context: Context, private val immersive: Boolean,
    private val overlay: Boolean = false, private val preview: Boolean = false,
) : View(context) {
    var demo = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edge = Path()
    private val effect = Path()
    private val measure = PathMeasure()
    private val point = FloatArray(2)
    private val tangent = FloatArray(2)
    private val envelope = EdgeLightEnvelope()
    private val smooth = envelope.bands
    private val targets = FloatArray(24)
    private val gradientMatrix = Matrix()
    private val prefs = LightsPreferences.prefs(context)
    private var settings = LightsPreferences.read(prefs)
    private var scope: CoroutineScope? = null
    private var metadataJob: Job? = null
    private var title = "Meloqis Music"
    private var artist = ""
    private var artColours: IntArray? = null
    private var colourKey = ""
    private var colours = intArrayOf(Color.MAGENTA, Color.CYAN, Color.MAGENTA)
    private var gradient: SweepGradient? = null
    private var geometryKey = ""
    private var running = false
    private var lastDraw = 0L
    private var lastClock = 0L
    private var lastBattery = 0L
    private var batteryText = ""
    private var clockText = ""
    private var dateText = ""
    private val date = Date()
    private val calendar = Calendar.getInstance()
    private val timeFormat = android.text.format.DateFormat.getTimeFormat(context)
    private val dayFormat = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
    private val redraw = Runnable { if (running) invalidate() }
    private val change = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        val previous = settings
        settings = LightsPreferences.read(prefs)
        if (previous.palette != settings.palette) observeMetadata()
        invalidate()
    }

    fun start() {
        if (running) return
        running = true; lastDraw = 0
        settings = LightsPreferences.read(prefs)
        prefs.registerOnSharedPreferenceChangeListener(change)
        AudioSpectrum.acquire()
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        observeMetadata(); invalidate()
    }
    fun stop() {
        if (!running) return
        running = false
        prefs.unregisterOnSharedPreferenceChangeListener(change)
        scope?.cancel(); scope = null; metadataJob = null
        AudioSpectrum.release(); removeCallbacks(redraw)
    }
    private fun observeMetadata() {
        metadataJob?.cancel()
        if (!running) return
        metadataJob = scope?.launch {
            val player = runCatching { GlobalContext.get().get<MediaPlayerHandler>() }.getOrNull() ?: return@launch
            player.nowPlaying.collectLatest { item ->
                title = item?.metadata?.title ?: "Meloqis Music"
                artist = item?.metadata?.artist.orEmpty()
                artColours = null; colourKey = ""
                if (settings.palette == "Album art") {
                    item?.metadata?.artworkUri?.let { uri ->
                        try {
                            val request = ImageRequest.Builder(context).data(uri).size(64).allowHardware(false).build()
                            val result = SingletonImageLoader.get(context).execute(request) as? SuccessResult
                            artColours = result?.image?.toBitmap()?.let { bitmap ->
                                withContext(Dispatchers.Default) { artworkColours(bitmap) }
                            }
                            colourKey = ""
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (_: Exception) { /* Aurora remains available offline or without artwork. */ }
                    }
                }
                invalidate()
            }
        }
    }
    private fun artworkColours(bitmap: Bitmap): IntArray? {
        // Saturation-weighted hue buckets avoid averaging a colourful cover into brown.
        val weights = FloatArray(12); val sample = FloatArray(3)
        for (y in 0 until bitmap.height step 2) for (x in 0 until bitmap.width step 2) {
            val pixel = bitmap.getPixel(x, y)
            Color.colorToHSV(pixel, sample)
            if (Color.alpha(pixel) > 128 && sample[1] > .2f && sample[2] > .15f)
                weights[(sample[0] / 30).toInt().coerceIn(0, 11)] += sample[1] * sample[2]
        }
        val bucket = weights.indices.maxByOrNull { weights[it] } ?: return null
        if (weights[bucket] == 0f) return null
        val hue = bucket * 30f + 15f
        return intArrayOf(hueColour(hue, .75f), hueColour(hue + 40, .65f), hueColour(hue + 300, .8f), hueColour(hue, .75f))
    }
    private fun hueColour(hue: Float, saturation: Float = .8f): Int = Color.HSVToColor(floatArrayOf((hue % 360 + 360) % 360, saturation, 1f))

    override fun onAttachedToWindow() { super.onAttachedToWindow(); start() }
    override fun onDetachedFromWindow() { stop(); super.onDetachedFromWindow() }
    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)
        if (visibility == VISIBLE && isAttachedToWindow) start() else stop()
    }
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) { geometryKey = ""; colourKey = "" }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!overlay) canvas.drawColor(Color.BLACK)
        val w = width.toFloat(); val h = height.toFloat()
        if (w <= 0 || h <= 0) return
        val unit = if (preview) min(w / 360f, h / 740f) else resources.displayMetrics.density
        val s = settings
        val now = SystemClock.elapsedRealtime()
        val frame = AudioSpectrum.frames.value
        val active = frame.capturedAt > 0 && now - frame.capturedAt < 500
        val demoActive = preview && demo && !active
        val delta = if (lastDraw == 0L) 33f else (now - lastDraw).coerceIn(1, 200).toFloat()
        lastDraw = now
        for (i in smooth.indices) {
            val source = when {
                active -> frame.bands.getOrElse(i) { 0f }
                demoActive -> (.35 + .22 * sin(now / 310.0 + i * .6) + .18 * sin(now / 650.0 - i * .4)).toFloat()
                else -> 0f
            }
            targets[i] = source
        }
        envelope.update(targets, delta, s.sensitivity)
        val energy = smooth.average().toFloat()
        val key = "${s.palette}:${s.hue}:${s.secondHue}:${artColours?.contentHashCode()}:$w:$h"
        if (colourKey != key) {
            colourKey = key
            colours = when (s.palette) {
                "Sunset" -> intArrayOf(0xFFFF587B.toInt(), 0xFFFFBD6B.toInt(), 0xFFDA67F5.toInt(), 0xFFFF587B.toInt())
                "Ocean" -> intArrayOf(0xFF60EED3.toInt(), 0xFF4298FF.toInt(), 0xFF8F92FF.toInt(), 0xFF60EED3.toInt())
                "Neon" -> intArrayOf(0xFFFF48C4.toInt(), 0xFF963EFF.toInt(), 0xFF44F9F0.toInt(), 0xFFFF48C4.toInt())
                "Rainbow" -> IntArray(7) { hueColour(it * 60f) }
                "Custom" -> intArrayOf(hueColour(s.hue), hueColour(s.secondHue), hueColour(s.hue))
                "Album art" -> artColours ?: aurora()
                else -> aurora()
            }
            gradient = SweepGradient(w / 2, h / 2, colours, null)
        }
        gradientMatrix.setRotate(((now / 1000.0 * s.speed * 12) % 360).toFloat(), w / 2, h / 2)
        gradient?.setLocalMatrix(gradientMatrix)
        val geometry = "${s.position}:${s.inset}:${s.radius}:${s.thickness}:$w:$h:$unit"
        if (geometryKey != geometry) {
            geometryKey = geometry; edge.reset()
            val inset = (s.inset * unit + s.thickness * unit / 2).coerceAtMost(min(w, h) / 3)
            val radius = (s.radius * unit).coerceAtMost(min(w - 2 * inset, h - 2 * inset) / 2)
            when (s.position) {
                "Frame" -> edge.addRoundRect(inset, inset, w - inset, h - inset, radius, radius, Path.Direction.CW)
                "Top" -> { edge.moveTo(inset, inset); edge.lineTo(w - inset, inset) }
                "Left" -> { edge.moveTo(inset, inset); edge.lineTo(inset, h - inset) }
                "Right" -> { edge.moveTo(w - inset, inset); edge.lineTo(w - inset, h - inset) }
                "Center" -> { edge.moveTo(inset, h * .73f); edge.lineTo(w - inset, h * .73f) }
                else -> { edge.moveTo(w - inset, h - inset); edge.lineTo(inset, h - inset) }
            }
            measure.setPath(edge, false)
        }
        val ambient = s.ambient || demoActive
        val shouldDraw = active || demoActive || ambient || energy > .01f
        if (shouldDraw) {
            canvas.save()
            if (s.notch > 0) canvas.clipOutRect(w / 2 - s.notch * unit / 2, 0f, w / 2 + s.notch * unit / 2, max(24 * unit, s.inset * unit + s.thickness * unit * 3))
            if (!preview && android.os.Build.VERSION.SDK_INT >= 28) rootWindowInsets?.displayCutout?.boundingRects?.forEach { canvas.clipOutRect(it) }
            val alpha = ((.25f + s.brightness * .75f) * 255).toInt()
            val thickness = s.thickness * unit
            val pulse = if (active || demoActive) energy else (.14 + .08 * sin(now / 1700.0)).toFloat()
            when (s.mode) {
                "Ribbon" -> stroke(canvas, edge, thickness * (1 + energy * .8f), alpha, s.glow, unit)
                "Pulse" -> stroke(canvas, edge, thickness * (1 + pulse * 2), (alpha * (.25f + pulse * .75f)).toInt(), s.glow, unit)
                "Comet" -> {
                    stroke(canvas, edge, thickness * .5f, (alpha * .12f).toInt(), 0f, unit)
                    val length = measure.length
                    val head = ((now / 1000.0 * s.speed / 8) % 1).toFloat()
                    repeat(32) { index ->
                        val fraction = (head - index * .006f + 1) % 1f
                        effect.reset(); measure.getSegment(fraction * length, min(length, fraction * length + length * .009f), effect, true)
                        stroke(canvas, effect, thickness, (alpha * (1 - index / 32f) * (.5f + pulse * .5f)).toInt(), s.glow, unit)
                    }
                }
                "Orbit" -> {
                    val side = s.position in listOf("Left", "Right", "Top", "Bottom")
                    val radius = min(w, h) * if (side) .14f else .34f
                    val cx = when (s.position) { "Left" -> w * .22f; "Right" -> w * .78f; else -> w / 2 }
                    val cy = when (s.position) {
                        "Top" -> radius + 24 * unit
                        "Bottom" -> h - radius - 24 * unit
                        "Left", "Right" -> h / 2
                        else -> if (immersive && s.clock) clockCenterY(h, unit, s) - 15 * unit else h / 2
                    }
                    repeat(64) { index ->
                        val angle = index * 2 * PI / 64 - PI / 2
                        val band = band(index / 64f, s.mirror)
                        val reach = 3 * unit + band * min(w, h) * s.size * .2f
                        effect.reset(); effect.moveTo(cx + cos(angle).toFloat() * radius, cy + sin(angle).toFloat() * radius)
                        effect.lineTo(cx + cos(angle).toFloat() * (radius + reach), cy + sin(angle).toFloat() * (radius + reach))
                        stroke(canvas, effect, thickness, alpha, s.glow, unit)
                    }
                }
                else -> drawSpectrum(canvas, w, h, alpha, thickness, unit, s)
            }
            canvas.restore()
        }
        if (immersive) drawClock(canvas, w, h, unit, now, s)
        if (running) {
            removeCallbacks(redraw)
            val saver = context.getSystemService(PowerManager::class.java).isPowerSaveMode
            postDelayed(redraw, when { saver -> 100L; active || demoActive -> 33L; ambient -> 80L; immersive -> 1000L; else -> 150L })
        }
    }
    private fun band(fraction: Float, mirror: Boolean): Float {
        val f = if (mirror) abs(fraction * 2 - 1) else fraction
        return smooth[(f * 23).toInt().coerceIn(0, 23)]
    }
    private fun drawSpectrum(canvas: Canvas, w: Float, h: Float, alpha: Int, thickness: Float, unit: Float, s: LightsSettings) {
        val count = if (s.position == "Frame") 112 else 40
        effect.reset()
        for (index in 0 until count) {
            val fraction = (index + .5f) / count
            measure.getPosTan(measure.length * fraction, point, tangent)
            val reach = 2 * unit + band(fraction, s.mirror) * min(w, h) * s.size * .35f
            // Paths run clockwise around a frame, so their left normal points inward.
            val direction = if (s.position == "Left") -1 else 1
            val x = point[0] - tangent[1] * reach * direction
            val y = point[1] + tangent[0] * reach * direction
            when (s.mode) {
                "Dots" -> {
                    effect.reset(); effect.addCircle(x, y, max(unit, thickness * .65f), Path.Direction.CW)
                    stroke(canvas, effect, thickness * .6f, alpha, s.glow, unit)
                }
                "Wave" -> if (index == 0) effect.moveTo(x, y) else effect.lineTo(x, y)
                else -> {
                    effect.reset(); effect.moveTo(point[0], point[1]); effect.lineTo(x, y)
                    stroke(canvas, effect, thickness, alpha, s.glow, unit)
                }
            }
        }
        if (s.mode == "Wave") {
            if (s.position == "Frame") effect.close()
            stroke(canvas, effect, thickness, alpha, s.glow, unit)
        }
    }
    private fun stroke(canvas: Canvas, path: Path, width: Float, alpha: Int, glow: Float, unit: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        paint.shader = gradient
        if (glow > .01f) {
            paint.strokeWidth = width + 14 * unit * glow; paint.alpha = (alpha * .07f * glow).toInt(); canvas.drawPath(path, paint)
            paint.strokeWidth = width + 7 * unit * glow; paint.alpha = (alpha * .17f * glow).toInt(); canvas.drawPath(path, paint)
        }
        paint.strokeWidth = width; paint.alpha = alpha.coerceIn(0, 255); canvas.drawPath(path, paint)
        paint.shader = null; paint.alpha = 255; paint.style = Paint.Style.FILL
    }
    private fun drawClock(canvas: Canvas, w: Float, h: Float, unit: Float, now: Long, s: LightsSettings) {
        if (now - lastClock >= 500 || lastClock == 0L) {
            lastClock = now; date.time = System.currentTimeMillis(); calendar.time = date
            clockText = timeFormat.format(date); dateText = dayFormat.format(date)
        }
        if (now - lastBattery > 30_000 || lastBattery == 0L) {
            lastBattery = now
            val manager = context.getSystemService(BatteryManager::class.java)
            val percent = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            batteryText = if (percent in 0..100) "${if (manager.isCharging) "+ " else ""}$percent%" else ""
        }
        canvas.save()
        val phase = (now / 60_000 % 8).toInt()
        canvas.translate(((phase % 3) - 1) * 4 * unit, ((phase / 3) - 1) * 4 * unit)
        val cx = w / 2; val cy = clockCenterY(h, unit, s)
        paint.shader = null; paint.alpha = 255; paint.style = Paint.Style.FILL
        paint.color = Color.rgb(225, 223, 243); paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create("sans-serif-light", Typeface.NORMAL)
        var bottom = cy
        if (s.clock) when (s.clockStyle) {
            "Stacked" -> {
                paint.textSize = 82 * unit
                canvas.drawText(String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.HOUR_OF_DAY)), cx, cy - 10 * unit, paint)
                paint.color = colours[0]
                canvas.drawText(String.format(Locale.getDefault(), "%02d", calendar.get(Calendar.MINUTE)), cx, cy + 70 * unit, paint)
                bottom = cy + 98 * unit
            }
            "Analog" -> {
                val radius = 67 * unit
                paint.strokeCap = Paint.Cap.ROUND; paint.strokeWidth = 2 * unit
                repeat(12) { index ->
                    val a = index * PI / 6
                    canvas.drawLine(cx + sin(a).toFloat() * (radius - 5 * unit), cy - cos(a).toFloat() * (radius - 5 * unit), cx + sin(a).toFloat() * radius, cy - cos(a).toFloat() * radius, paint)
                }
                fun hand(angle: Double, length: Float, width: Float) {
                    paint.strokeWidth = width; canvas.drawLine(cx, cy, cx + sin(angle).toFloat() * length, cy - cos(angle).toFloat() * length, paint)
                }
                hand((calendar.get(Calendar.HOUR) + calendar.get(Calendar.MINUTE) / 60.0) * PI / 6, radius * .53f, 4 * unit)
                paint.color = colours[0]; hand(calendar.get(Calendar.MINUTE) * PI / 30, radius * .8f, 2 * unit)
                canvas.drawCircle(cx, cy, 4 * unit, paint); bottom = cy + radius + 26 * unit
            }
            else -> {
                paint.textSize = (if (s.clockStyle == "Minimal") 43 else 62) * unit
                canvas.drawText(clockText, cx, cy, paint); bottom = cy + 32 * unit
            }
        }
        paint.color = Color.rgb(174, 169, 194); paint.textSize = 13 * unit
        if (s.clock) canvas.drawText(dateText.uppercase(Locale.getDefault()), cx, bottom, paint)
        if (s.battery) { bottom += 26 * unit; canvas.drawText(batteryText, cx, bottom, paint) }
        if (s.details) {
            bottom += 48 * unit; paint.textSize = 17 * unit; paint.color = Color.rgb(225, 223, 243)
            canvas.drawText(fit(title, w * .75f), cx, bottom, paint)
            if (artist.isNotBlank()) { paint.textSize = 12 * unit; paint.color = Color.GRAY; canvas.drawText(fit(artist, w * .75f), cx, bottom + 23 * unit, paint) }
        }
        canvas.restore()
    }
    private fun fit(text: String, width: Float): String {
        val oneLine = text.replace('\n', ' ')
        if (paint.measureText(oneLine) <= width) return oneLine
        val count = paint.breakText(oneLine, true, max(0f, width - paint.measureText("…")), null)
        return oneLine.take(count) + "…"
    }
    private fun clockCenterY(height: Float, unit: Float, s: LightsSettings): Float {
        val top = (if (!s.clock) 20 else when (s.clockStyle) { "Stacked" -> 115; "Analog" -> 90; else -> 80 }) * unit
        val bottom = ((if (!s.clock) 0 else when (s.clockStyle) { "Stacked" -> 98; "Analog" -> 93; else -> 32 }) +
            (if (s.battery) 26 else 0) + (if (s.details) 80 else 0) + 24) * unit
        return (height * s.clockY).coerceIn(top, max(top, height - bottom))
    }
    private fun aurora() = intArrayOf(0xFFB79AFF.toInt(), 0xFF6CF2D2.toInt(), 0xFF658CFF.toInt(), 0xFFB79AFF.toInt())
}

