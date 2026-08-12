package iad1tya.echo.music.feedback

import android.content.Context
import android.os.Build
import androidx.datastore.preferences.core.edit
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.constants.FeedbackCompletedKey
import iad1tya.echo.music.constants.FeedbackFirstUseAtKey
import iad1tya.echo.music.constants.FeedbackNextPromptAtKey
import iad1tya.echo.music.utils.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONObject
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

data class FeedbackSubmission(
    val rating: Int,
    val category: String,
    val message: String,
)

object MeloqisFeedback {
    internal const val PROMPT_DELAY_MS = 48L * 60L * 60L * 1000L
    internal const val SNOOZE_DELAY_MS = 24L * 60L * 60L * 1000L

    suspend fun initialize(context: Context, now: Long = System.currentTimeMillis()) {
        context.dataStore.edit { preferences ->
            if (preferences[FeedbackFirstUseAtKey] == null) {
                preferences[FeedbackFirstUseAtKey] = now
            }
        }
    }

    suspend fun shouldPrompt(context: Context, now: Long = System.currentTimeMillis()): Boolean {
        val preferences = context.dataStore.data.first()
        return isEligible(
            firstUseAt = preferences[FeedbackFirstUseAtKey],
            completed = preferences[FeedbackCompletedKey] ?: false,
            nextPromptAt = preferences[FeedbackNextPromptAtKey] ?: 0L,
            now = now,
        )
    }

    suspend fun snooze(context: Context, now: Long = System.currentTimeMillis()) {
        context.dataStore.edit {
            it[FeedbackNextPromptAtKey] = now + SNOOZE_DELAY_MS
        }
    }

    suspend fun submit(context: Context, submission: FeedbackSubmission): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                require(submission.rating in 1..5)
                require(submission.category in ALLOWED_CATEGORIES)
                val message = submission.message.trim()
                require(message.length in 3..800)

                val submissionId = UUID.randomUUID().toString()
                val connection = URL(BuildConfig.FEEDBACK_ENDPOINT).openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = "POST"
                    connection.connectTimeout = 10_000
                    connection.readTimeout = 12_000
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("User-Agent", "Meloqis/${BuildConfig.VERSION_NAME}")
                    val payload = JSONObject()
                        .put("submissionId", submissionId)
                        .put("rating", submission.rating)
                        .put("category", submission.category)
                        .put("message", message)
                        .put("appVersion", BuildConfig.VERSION_NAME)
                        .put("androidVersion", Build.VERSION.RELEASE.orEmpty().take(24))
                        .put("sdkInt", Build.VERSION.SDK_INT)
                        .toString()
                    connection.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                    val responseCode = connection.responseCode
                    runCatching {
                        val stream = if (responseCode >= 400) connection.errorStream else connection.inputStream
                        stream?.close()
                    }
                    check(responseCode in 200..299) { "Feedback service returned HTTP $responseCode" }
                } finally {
                    connection.disconnect()
                }

                context.dataStore.edit {
                    it[FeedbackCompletedKey] = true
                    it.remove(FeedbackNextPromptAtKey)
                }
                Unit
            }.onFailure {
                Timber.tag("MeloqisFeedback").w(it, "Feedback submission failed")
            }
        }

    internal fun isEligible(
        firstUseAt: Long?,
        completed: Boolean,
        nextPromptAt: Long,
        now: Long,
    ): Boolean =
        firstUseAt != null &&
            !completed &&
            now >= firstUseAt + PROMPT_DELAY_MS &&
            now >= nextPromptAt

    val categories = listOf(
        "Crash or bug",
        "Playback",
        "Update",
        "Design",
        "Discovery",
        "Capsule",
        "Other",
    )

    private val ALLOWED_CATEGORIES = categories.toSet()
}
