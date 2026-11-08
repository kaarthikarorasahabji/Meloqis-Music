package echo.music.iad1tya.ui.component

import android.content.Context
import android.content.SharedPreferences
import android.graphics.*
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
import kotlin.math.*

/** Activity-local border renderer with a separate, clearly labeled design preview. */
internal class MusicLightsView(
    context: Context, private val transparent: Boolean = true, private val preview: Boolean = false,
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
    private var artColours: IntArray? = null
    private var colourKey = ""
    private var colours = intArrayOf(Color.MAGENTA, Color.CYAN, Color.MAGENTA)
    private var gradient: SweepGradient? = null
    private var geometryKey = ""
    private val screenOrigin = IntArray(2)
    private var cutouts: List<Rect> = emptyList()
    private var running = false
    private var lastDraw = 0L
    private val redraw = Runnable { if (running) invalidate() }
    private val change = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        val previous = settings
        settings = LightsPreferences.read(prefs)
        if (previous.palette != settings.palette) observeMetadata()
        geometryKey = ""
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
        if (!transparent) canvas.drawColor(Color.rgb(15, 17, 26))
        val w = width.toFloat(); val h = height.toFloat()
        val s = settings
        if (w <= 0 || h <= 0 || (!s.enabled && !preview)) return
        val unit = if (preview) min(w / 360f, h / 740f) else resources.displayMetrics.density
        val now = SystemClock.elapsedRealtime()
        val motion = s.motion && android.animation.ValueAnimator.areAnimatorsEnabled()
        val time = if (motion) now / 1000.0 * s.speed else 0.0
        val frame = AudioSpectrum.frames.value
        val active = frame.capturedAt > 0 && now - frame.capturedAt < 500
        val demoActive = preview && demo && !active
        val delta = if (lastDraw == 0L) 33f else (now - lastDraw).coerceIn(1, 200).toFloat()
        lastDraw = now
        for (i in smooth.indices) targets[i] = when {
            !motion -> 0f
            active -> frame.bands.getOrElse(i) { 0f }
            demoActive -> (.35 + .22 * sin(time * 3.2 + i * .6) + .18 * sin(time * 1.5 - i * .4)).toFloat()
            else -> 0f
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
        gradientMatrix.setRotate(((time * 12) % 360).toFloat(), w / 2, h / 2)
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
                else -> { edge.moveTo(w - inset, h - inset); edge.lineTo(inset, h - inset) }
            }
            measure.setPath(edge, false)
            if (!preview && android.os.Build.VERSION.SDK_INT >= 28) {
                getLocationOnScreen(screenOrigin)
                cutouts = rootWindowInsets?.displayCutout?.boundingRects.orEmpty().map { Rect(it).apply { offset(-screenOrigin[0], -screenOrigin[1]) } }
            }
        }
        val ambient = s.ambient || demoActive
        if (active || demoActive || ambient || energy > .01f) {
            canvas.save()
            if (s.notch > 0) canvas.clipOutRect(w / 2 - s.notch * unit / 2, 0f, w / 2 + s.notch * unit / 2, max(24 * unit, s.inset * unit + s.thickness * unit * 3))
            if (!preview) cutouts.forEach { canvas.clipOutRect(it) }
            val alpha = (s.brightness * 230).toInt().coerceIn(1, 230)
            val thickness = s.thickness * unit
            val pulse = if (active || demoActive) energy else (.5 + .5 * sin(time * .9)).toFloat()
            when (s.mode) {
                "Flow" -> stroke(canvas, edge, thickness * (1 + energy * .35f), alpha, s.glow)
                "Breath" -> stroke(canvas, edge, thickness * (1 + pulse * .35f), (alpha * (.3f + pulse * .7f)).toInt(), s.glow)
                "Comet", "Twin trails" -> {
                    stroke(canvas, edge, thickness * .6f, (alpha * .12f).toInt(), 0f)
                    val trails = if (s.mode == "Twin trails") 2 else 1
                    repeat(trails) { trail ->
                        val head = ((time / 9 + trail * .5) % 1).toFloat()
                        repeat(18) { index ->
                            val fraction = (head - index * .011f + 1) % 1f
                            val end = fraction + .014f
                            effect.reset()
                            measure.getSegment(fraction * measure.length, min(1f, end) * measure.length, effect, true)
                            if (end > 1) measure.getSegment(0f, (end - 1) * measure.length, effect, true)
                            stroke(canvas, effect, thickness, (alpha * (1 - index / 18f)).toInt(), s.glow * .65f)
                        }
                    }
                }
                else -> drawSpectrum(canvas, w, h, alpha, thickness, unit, s, time)
            }
            canvas.restore()
        }
        if (running) {
            removeCallbacks(redraw)
            val saver = s.powerSaver && context.getSystemService(PowerManager::class.java).isPowerSaveMode
            postDelayed(redraw, when { !motion -> 1000L; saver -> 100L; active || demoActive -> 33L; ambient -> 66L; else -> 150L })
        }
    }
    private fun band(fraction: Float, mirror: Boolean): Float {
        val f = if (mirror) abs(fraction * 2 - 1) else fraction
        return smooth[(f * 23).toInt().coerceIn(0, 23)]
    }
    private fun drawSpectrum(canvas: Canvas, w: Float, h: Float, alpha: Int, thickness: Float, unit: Float, s: LightsSettings, time: Double) {
        val count = if (s.position == "Frame") 88 else 32
        val maxReach = min(18 * unit, min(w, h) * s.size * .2f)
        effect.reset()
        for (index in 0 until count) {
            val fraction = (index + .5f) / count
            measure.getPosTan(measure.length * fraction, point, tangent)
            val reach = 1.5f * unit + band(fraction, s.mirror) * maxReach
            val direction = if (s.position == "Left") -1 else 1
            val x = point[0] - tangent[1] * reach * direction
            val y = point[1] + tangent[0] * reach * direction
            when (s.mode) {
                "Sparkles" -> {
                    val radius = max(unit * .5f, thickness * (.3f + .2f * sin(time * 1.5 + index).toFloat()))
                    effect.addCircle(x, y, radius, Path.Direction.CW)
                }
                "Wave" -> if (index == 0) effect.moveTo(x, y) else effect.lineTo(x, y)
                else -> { effect.moveTo(point[0], point[1]); effect.lineTo(x, y) }
            }
        }
        if (s.mode == "Wave" && s.position == "Frame") effect.close()
        stroke(canvas, effect, thickness, alpha, s.glow)
    }
    private fun stroke(canvas: Canvas, path: Path, width: Float, alpha: Int, glow: Float) {
        paint.style = Paint.Style.STROKE; paint.strokeCap = Paint.Cap.ROUND; paint.strokeJoin = Paint.Join.ROUND
        paint.shader = gradient
        // Layered strokes work on GPU canvases too; no per-frame BlurMaskFilter allocations.
        if (glow > .02f) {
            paint.strokeWidth = width * (2.5f + glow * 2); paint.alpha = (alpha * glow * .09f).toInt(); canvas.drawPath(path, paint)
            paint.strokeWidth = width * (1.5f + glow); paint.alpha = (alpha * glow * .22f).toInt(); canvas.drawPath(path, paint)
        }
        paint.strokeWidth = width; paint.alpha = alpha.coerceIn(0, 255); canvas.drawPath(path, paint)
        paint.shader = null; paint.alpha = 255
    }
    private fun aurora() = intArrayOf(0xFFB79AFF.toInt(), 0xFF6CF2D2.toInt(), 0xFF658CFF.toInt(), 0xFFB79AFF.toInt())
}
