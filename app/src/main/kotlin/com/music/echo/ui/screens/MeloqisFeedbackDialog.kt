package iad1tya.echo.music.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import iad1tya.echo.music.R
import iad1tya.echo.music.feedback.FeedbackSubmission
import iad1tya.echo.music.feedback.MeloqisFeedback
import iad1tya.echo.music.ui.component.DefaultDialog
import kotlinx.coroutines.launch

@Composable
fun MeloqisFeedbackDialog(
    onNotNow: () -> Unit,
    onSubmitted: () -> Unit,
    submit: suspend (FeedbackSubmission) -> Boolean,
    initialCategory: String = "Other",
    initialMessage: String = "",
) {
    var rating by remember { mutableIntStateOf(0) }
    var category by remember(initialCategory) { mutableStateOf(initialCategory) }
    var message by remember(initialMessage) { mutableStateOf(initialMessage.take(800)) }
    var submitting by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val canSubmit = rating > 0 && message.trim().length >= 3 && !submitting

    DefaultDialog(
        onDismiss = { if (!submitting) onNotNow() },
        modifier = Modifier.fillMaxWidth(),
        icon = {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(30.dp),
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.feedback_title),
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.feedback_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    ) {
        Text(
            text = stringResource(R.string.feedback_rating_label),
            style = MaterialTheme.typography.labelLarge,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            (1..5).forEach { value ->
                val selected = value <= rating
                val scale by animateFloatAsState(if (value == rating) 1.18f else 1f, label = "feedback_star")
                val tint by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "feedback_star_color",
                )
                IconButton(
                    onClick = { rating = value; error = false },
                    modifier = Modifier.scale(scale),
                ) {
                    Icon(
                        imageVector = if (selected) Icons.Rounded.Star else Icons.Outlined.StarOutline,
                        contentDescription = stringResource(R.string.feedback_rating_value, value),
                        tint = tint,
                    )
                }
            }
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MeloqisFeedback.categories.forEach { option ->
                AssistChip(
                    onClick = { category = option; error = false },
                    label = { Text(option) },
                    leadingIcon = if (category == option) {
                        { Icon(Icons.Rounded.AutoAwesome, contentDescription = null, Modifier.size(16.dp)) }
                    } else null,
                    shape = RoundedCornerShape(50),
                )
            }
        }

        OutlinedTextField(
            value = message,
            onValueChange = {
                if (it.length <= 800) message = it
                error = false
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 112.dp),
            label = { Text(stringResource(R.string.feedback_message_label)) },
            placeholder = { Text(stringResource(R.string.feedback_message_placeholder)) },
            supportingText = { Text("${message.length}/800") },
            minLines = 3,
            maxLines = 6,
            enabled = !submitting,
            isError = error,
        )

        if (error) {
            Text(
                text = stringResource(R.string.feedback_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onNotNow, enabled = !submitting) {
                Text(stringResource(R.string.feedback_not_now))
            }
            Button(
                onClick = {
                    submitting = true
                    error = false
                    scope.launch {
                        val accepted = submit(
                            FeedbackSubmission(rating, category, message),
                        )
                        submitting = false
                        if (accepted) onSubmitted() else error = true
                    }
                },
                enabled = canSubmit,
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )
                } else {
                    Text(stringResource(R.string.feedback_send))
                }
            }
        }
    }
}
