package echo.music.iad1tya.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.support_developer_title
import echomusic.composeapp.generated.resources.support_upi_unavailable
import echomusic.composeapp.generated.resources.support_later
import org.jetbrains.compose.resources.stringResource

internal const val DEVELOPER_SUPPORT_UPI = "upi://pay?pa=kaarthikdassarorasahabji%40sbi&pn=Kaarthik%20Dass%20Arora%20Sahab%20Ji&am=50.00&mam=50.00&cu=INR&tn=Meloqis%20developer%20coffee"

@Composable
internal fun rememberDeveloperSupportAction(onOpened: () -> Unit = {}): () -> Unit {
    val uriHandler = LocalUriHandler.current
    var unavailable by remember { mutableStateOf(false) }
    if (unavailable) {
        AlertDialog(
            onDismissRequest = { unavailable = false },
            title = { Text(stringResource(Res.string.support_developer_title)) },
            text = { Text(stringResource(Res.string.support_upi_unavailable)) },
            confirmButton = {
                TextButton(onClick = { unavailable = false }) { Text(stringResource(Res.string.support_later)) }
            },
        )
    }
    return {
        try {
            uriHandler.openUri(DEVELOPER_SUPPORT_UPI)
            onOpened()
        } catch (_: Exception) {
            // Missing UPI handlers should leave an explanation, not crash the app.
            unavailable = true
        }
    }
}
