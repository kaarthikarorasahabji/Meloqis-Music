package echo.music.iad1tya.ui.component

import android.content.Context
import android.hardware.camera2.*
import android.media.ImageReader
import android.graphics.ImageFormat
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.SystemClock
import android.view.Surface
import android.hardware.display.DisplayManager
import android.view.Display
import android.util.Log
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import com.google.mediapipe.tasks.vision.core.RunningMode
import java.io.File
import java.net.URL
import java.security.MessageDigest
/** Front-camera-only hand recognition. All inference runs on this worker, at most eight FPS. */
internal class HandSwipeCamera(
    private val context: Context,
    private val onError: (String) -> Unit,
    private val onReady: () -> Unit,
    private val onHandPresence: (Boolean) -> Unit,
    private val onSwipe: (AirSwipe) -> Unit,
) {
    private val thread = HandlerThread("MeloqisAir").apply { start() }
    private val worker = Handler(thread.looper)
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var closed = false
    private var device: CameraDevice? = null
    private var session: CameraCaptureSession? = null
    private var reader: ImageReader? = null
    private val pixels = IntArray(256 * 192)
    private val sourceBitmap = Bitmap.createBitmap(256, 192, Bitmap.Config.ARGB_8888)
    private var uprightBitmap: Bitmap? = null
    private var mpImage: MPImage? = null
    private var landmarker: HandLandmarker? = null
    private var lastFrame = 0L
    private var lastHandSeen = 0L
    private var handPresent = false
    private var rotation = 0
    private var sensorOrientation = 0
    private val tracker = HandGestureTracker()

    fun start() {
        worker.post {
            if (closed) return@post
            try {
                val model = loadModel() ?: return@post
                if (closed) return@post
                landmarker = HandLandmarker.createFromOptions(context,
                    HandLandmarker.HandLandmarkerOptions.builder()
                        .setBaseOptions(BaseOptions.builder().setModelAssetPath(model.absolutePath).build())
                        .setRunningMode(RunningMode.VIDEO).setNumHands(1)
                        .setMinHandDetectionConfidence(0.7f).setMinHandPresenceConfidence(0.7f)
                        .setMinTrackingConfidence(0.7f).build())
                val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val id = manager.cameraIdList.firstOrNull {
                    manager.getCameraCharacteristics(it).get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                } ?: return@post fail("No front camera is available. You can keep using the touch controls.")
                val info = manager.getCameraCharacteristics(id)
                sensorOrientation = info.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
                updateOrientation()
                val sizes = info.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)?.getOutputSizes(ImageFormat.YUV_420_888)
                val size = sizes?.filter { it.width >= 320 && it.height >= 240 }?.minByOrNull { it.width.toLong() * it.height }
                    ?: sizes?.maxByOrNull { it.width.toLong() * it.height }
                    ?: return@post fail("This camera does not support Air Controls.")
                val frames = ImageReader.newInstance(size.width, size.height, ImageFormat.YUV_420_888, 2)
                reader = frames
                frames.setOnImageAvailableListener({ source ->
                    if (!closed) analyze(source)
                }, worker)
                manager.openCamera(id, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        if (closed) { camera.close(); return }
                        device = camera
                        try {
                            @Suppress("DEPRECATION")
                            camera.createCaptureSession(listOf(frames.surface), object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(capture: CameraCaptureSession) {
                                    if (closed) { capture.close(); return }
                                    session = capture
                                    try {
                                        val request = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                            addTarget(frames.surface)
                                            set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF)
                                            info.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES)
                                                ?.filter { it.upper <= 15 }?.maxByOrNull { it.upper }
                                                ?.let { set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, it) }
                                        }.build()
                                        capture.setRepeatingRequest(request, null, worker)
                                        main.post { if (!closed) onReady() }
                                    } catch (_: Exception) { fail("Camera could not start. Try again after closing other camera apps.") }
                                }
                                override fun onConfigureFailed(capture: CameraCaptureSession) { capture.close(); fail("Camera setup failed. Touch controls are still available.") }
                            }, worker)
                        } catch (_: Exception) { fail("Camera is unavailable. Try again later.") }
                    }
                    override fun onDisconnected(camera: CameraDevice) { camera.close(); fail("Camera disconnected. Air Controls stopped.") }
                    override fun onError(camera: CameraDevice, error: Int) { camera.close(); fail("Camera is busy or blocked. Air Controls stopped.") }
                }, worker)
            } catch (error: Exception) {
                Log.e("MeloqisAir", "Hand recognition initialization failed", error)
                fail("Hand recognition could not start. Check your connection, camera permission, and privacy settings, then try again.")
            }
            catch (error: LinkageError) {
                Log.e("MeloqisAir", "Hand SDK linkage failed", error)
                fail("The hand detector could not load. Touch controls remain available; please report this error with your app version.")
            }
            catch (_: OutOfMemoryError) { fail("This device is low on memory. Air Controls stopped; touch controls are still available.") }
        }
    }

    private fun analyze(source: ImageReader) {
        try {
            val image = source.acquireLatestImage() ?: return
            try {
                val now = SystemClock.elapsedRealtime()
                // Idle at roughly three FPS; increase only while a hand is present.
                val interval = if (now - lastHandSeen < 2000) 125 else 350
                if (now - lastFrame < interval) return
                lastFrame = now
                updateOrientation()
                val planes = image.planes
                fun sample(plane: android.media.Image.Plane, x: Int, y: Int) =
                    plane.buffer.get(y * plane.rowStride + x * plane.pixelStride).toInt() and 255
                for (y in 0 until 192) for (x in 0 until 256) {
                    val px = ((x + 0.5f) * image.width / 256).toInt().coerceAtMost(image.width - 1)
                    val py = ((y + 0.5f) * image.height / 192).toInt().coerceAtMost(image.height - 1)
                    val luma = (sample(planes[0], px, py) - 16).coerceAtLeast(0)
                    val u = sample(planes[1], px / 2, py / 2) - 128
                    val v = sample(planes[2], px / 2, py / 2) - 128
                    pixels[y * 256 + x] = Color.rgb(
                        ((298 * luma + 409 * v + 128) shr 8).coerceIn(0, 255),
                        ((298 * luma - 100 * u - 208 * v + 128) shr 8).coerceIn(0, 255),
                        ((298 * luma + 516 * u + 128) shr 8).coerceIn(0, 255))
                }
                sourceBitmap.setPixels(pixels, 0, 256, 0, 0, 256, 192)
                val upright = uprightBitmap ?: return
                val canvas = Canvas(upright)
                canvas.translate(upright.width / 2f, upright.height / 2f)
                canvas.rotate(rotation.toFloat())
                canvas.drawBitmap(sourceBitmap, -128f, -96f, null)
                // MPImage owns its bitmap and recycles it on close. Reuse this wrapper
                // between synchronous detections; close it only when the session ends.
                val input = mpImage ?: return
                val landmarks = landmarker?.detectForVideo(input, now)?.landmarks()?.firstOrNull()
                val present = landmarks?.size == 21
                if (present) lastHandSeen = now
                if (present != handPresent) {
                    handPresent = present
                    main.post { if (!closed) onHandPresence(present) }
                }
                val palm = landmarks?.map { 1f - it.x() to it.y() } ?: emptyList()
                tracker.observe(palm, now)?.let { swipe ->
                    main.post { if (!closed) onSwipe(swipe) }
                }
            } finally { image.close() }
        } catch (_: Exception) { if (!closed) fail("Hand recognition stopped. You can restart Air Controls.") }
        catch (_: OutOfMemoryError) { fail("Air Controls stopped because memory is low.") }
    }

    private fun fail(message: String) {
        if (!closed) main.post { if (!closed) { onError(message); close() } }
    }

    fun close() {
        if (closed) return
        closed = true
        worker.post {
            try { session?.close() } catch (_: Exception) { }
            try { device?.close() } catch (_: Exception) { }
            reader?.setOnImageAvailableListener(null, null)
            reader?.close()
            try { landmarker?.close() } catch (_: Exception) { }
            sourceBitmap.recycle()
            if (mpImage != null) mpImage?.close() else uprightBitmap?.recycle()
            thread.quitSafely()
        }
    }

    private fun loadModel(): File? {
        val target = File(context.noBackupFilesDir, "meloqis-hand-v1.task")
        fun valid(file: File): Boolean {
            if (!file.exists() || file.length() != 7819105L) return false
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { stream ->
                val chunk = ByteArray(16 * 1024)
                while (true) { val count = stream.read(chunk); if (count < 0) break; digest.update(chunk, 0, count) }
            }
            return digest.digest().joinToString("") { "%02x".format(it) } == MODEL_SHA
        }
        if (valid(target)) return target
        val temp = File.createTempFile("meloqis-hand-", ".download", context.cacheDir)
        try {
            val connection = URL(MODEL_URL).openConnection().apply { connectTimeout = 15000; readTimeout = 15000 }
            connection.getInputStream().use { stream ->
                temp.outputStream().use { output ->
                    val chunk = ByteArray(16 * 1024)
                    var total = 0L
                    while (!closed) {
                        val count = stream.read(chunk)
                        if (count < 0) break
                        total += count
                        check(total <= 7819105L) { "Unexpected model size" }
                        output.write(chunk, 0, count)
                    }
                }
            }
            if (closed) return null
            check(valid(temp)) { "Hand model verification failed" }
            if (!valid(target)) { target.delete(); check(temp.renameTo(target)) { "Could not save hand model" } }
            return target
        } finally { temp.delete() }
    }

    private fun updateOrientation() {
        val displayRotation = context.getSystemService(DisplayManager::class.java)
            .getDisplay(Display.DEFAULT_DISPLAY)?.rotation ?: Surface.ROTATION_0
        val degrees = when (displayRotation) { Surface.ROTATION_90 -> 90; Surface.ROTATION_180 -> 180; Surface.ROTATION_270 -> 270; else -> 0 }
        // Same relative-image rotation as CameraX for the front-facing lens.
        // Canvas rotates clockwise; mirroring is applied to landmark X afterward.
        val nextRotation = (sensorOrientation + degrees) % 360
        if (uprightBitmap == null || nextRotation != rotation) {
            mpImage?.close()
            rotation = nextRotation
            uprightBitmap = Bitmap.createBitmap(if (rotation % 180 == 0) 256 else 192,
                if (rotation % 180 == 0) 192 else 256, Bitmap.Config.ARGB_8888)
            mpImage = BitmapImageBuilder(uprightBitmap!!).build()
            tracker.reset()
        }
    }

    companion object {
        private const val MODEL_URL = "https://storage.googleapis.com/mediapipe-models/hand_landmarker/hand_landmarker/float16/1/hand_landmarker.task"
        private const val MODEL_SHA = "fbc2a30080c3c557093b5ddfc334698132eb341044ccee322ccf8bcf3607cde1"
    }
}
