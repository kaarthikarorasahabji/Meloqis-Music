package echo.music.iad1tya.media3.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.guava.future
import java.util.concurrent.ExecutionException

@UnstableApi
class CoilBitmapLoader(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
) : BitmapLoader {
    override fun supportsMimeType(mimeType: String): Boolean = true

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> =
        coroutineScope.future(Dispatchers.IO) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
            val options = BitmapFactory.Options()
            while (maxOf(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 512) {
                options.inSampleSize *= 2
            }
            BitmapFactory.decodeByteArray(data, 0, data.size, options)
                ?: error("Could not decode image data")
        }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> =
        coroutineScope.future(Dispatchers.IO) {
            val result =
                (
                    context.imageLoader.execute(
                        ImageRequest
                            .Builder(context)
                            .data(uri)
                            .allowHardware(false)
                            .size(512, 512)
                            .build(),
                    )
                )
            if (result is ErrorResult) {
                throw ExecutionException(result.throwable)
            }
            try {
                result.image?.toBitmap() ?: throw ExecutionException(NullPointerException())
            } catch (e: Exception) {
                throw ExecutionException(e)
            }
        }
}