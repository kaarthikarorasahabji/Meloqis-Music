package echo.music.iad1tya.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp

@Composable
fun SupportProjectDialog(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Support the Project",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "If you enjoy Meloqis Music, please consider supporting its development!",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        uriHandler.openUri(
                            "https://intradeus.github.io/http-protocol-redirector/?r=upi://pay?pa=kaarthikdassarorasahabji@sbi%26pn=Kaarthik%20Dass%20Arora%20Sahab%20Ji%26cu=INR"
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                    )
                ) {
                    Text("Support via UPI", color = MaterialTheme.colorScheme.background)
                }

                Text(
                    text = "UPI ID: kaarthikdassarorasahabji@sbi",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://github.com/kaarthikarorasahabji/Meloqis-Music") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Star on GitHub")
                }

                OutlinedButton(
                    onClick = { uriHandler.openUri("https://axenoraai.in") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Visit Axenora AI")
                }

                Text(
                    text = "developed with ❤️ by Kaarthik Dass Arora Sahab Ji",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Don't show again")
            }
        }
    )
}
