package echo.music.iad1tya.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.meloqis_logo
import echomusic.composeapp.generated.resources.support_developer_title
import echomusic.composeapp.generated.resources.support_developer_message
import echomusic.composeapp.generated.resources.support_coffee_button
import echomusic.composeapp.generated.resources.support_later
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun SupportProjectDialog(
    onDismiss: () -> Unit
) {
    val supportDeveloper = rememberDeveloperSupportAction(onOpened = onDismiss)

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Image(painterResource(Res.drawable.meloqis_logo), contentDescription = null, modifier = Modifier.size(48.dp))
        },
        title = {
            Text(
                text = stringResource(Res.string.support_developer_title),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 22.sp,
                lineHeight = 28.sp
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
                    text = stringResource(Res.string.support_developer_message),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = supportDeveloper,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                    )
                ) {
                    Text(stringResource(Res.string.support_coffee_button), color = MaterialTheme.colorScheme.background)
                }

                Text(
                    text = "UPI ID: kaarthikdassarorasahabji@sbi",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 4.dp)
                )


            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.support_later))
            }
        }
    )
}
