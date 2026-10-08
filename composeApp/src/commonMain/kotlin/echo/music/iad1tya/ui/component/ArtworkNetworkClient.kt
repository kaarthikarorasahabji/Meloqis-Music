package echo.music.iad1tya.ui.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

/** One client per screen lifetime; redraws must not create new engines and thread pools. */
@Composable
fun rememberArtworkNetworkClient(): HttpClient {
    val client = remember { HttpClient(CIO) }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    return client
}
