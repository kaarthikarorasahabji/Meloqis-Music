

package iad1tya.echo.music.echomusic.updater


import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Error
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.navigation.NavHostController
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import iad1tya.echo.music.BuildConfig
import iad1tya.echo.music.R
import coil3.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import iad1tya.echo.music.echomusic.updater.downloadmanager.UpdateDownloadWorker
import iad1tya.echo.music.echomusic.updater.downloadmanager.DownloadNotificationManager
import iad1tya.echo.music.echomusic.MeloqisUpdateActionReceiver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URL
import java.net.HttpURLConnection
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.regex.Pattern
import iad1tya.echo.music.ui.component.ChangelogItem
import iad1tya.echo.music.ui.component.leadingItemShape
import iad1tya.echo.music.ui.component.middleItemShape
import iad1tya.echo.music.ui.component.endItemShape
import iad1tya.echo.music.ui.component.detachedItemShape
import iad1tya.echo.music.ui.component.parseMarkdown
import iad1tya.echo.music.ui.component.endItemShape
import iad1tya.echo.music.ui.component.detachedItemShape
import iad1tya.echo.music.ui.component.AnimatedActionButton
import iad1tya.echo.music.ui.component.ExpressiveIconButton
import iad1tya.echo.music.ui.component.ErrorSnackbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.SnackbarHostState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.style.TextDecoration

data class ChangelogSection(val title: String, val items: List<String>)

private data class MeloqisReleaseManifest(
    val version: String,
    val publishedAt: String,
    val downloadUrl: String,
    val sha256: String,
    val sizeMb: String,
)

private suspend fun fetchOfficialMeloqisRelease(): MeloqisReleaseManifest =
    withContext(Dispatchers.IO) {
        val connection = URL(MELOQIS_RELEASE_MANIFEST).openConnection().apply {
            connectTimeout = 8_000
            readTimeout = 8_000
            setRequestProperty("User-Agent", "Meloqis/${BuildConfig.VERSION_NAME}")
            useCaches = false
        }
        val json = connection.getInputStream().bufferedReader().use { JSONObject(it.readText()) }
        val version = json.getString("version").trim()
        val publishedAt = json.optString("publishedAt").trim()
        val downloadUrl = json.getString("downloadUrl").trim()
        val sha256 = json.getString("sha256").trim().lowercase()
        val downloadUri = Uri.parse(downloadUrl)

        require(version.isNotBlank()) { "Missing release version" }
        require(
            downloadUri.scheme == "https" &&
                downloadUri.host == MELOQIS_R2_HOST &&
                downloadUri.path.orEmpty().startsWith("/previews/") &&
                downloadUri.path.orEmpty().endsWith(".apk"),
        ) { "Untrusted release artifact" }
        require(sha256.matches(Regex("[0-9a-f]{64}"))) { "Invalid release checksum" }

        val sizeBytes = runCatching {
            (URL(downloadUrl).openConnection() as HttpURLConnection).run {
                requestMethod = "HEAD"
                connectTimeout = 8_000
                readTimeout = 8_000
                useCaches = false
                connect()
                contentLengthLong.also { disconnect() }
            }
        }.getOrDefault(-1L)
        val sizeMb = if (sizeBytes > 0) {
            String.format(Locale.US, "%.1f", sizeBytes / (1024.0 * 1024.0))
        } else {
            "—"
        }

        MeloqisReleaseManifest(
            version = version,
            publishedAt = publishedAt,
            downloadUrl = downloadUrl,
            sha256 = sha256,
            sizeMb = sizeMb,
        )
    }

private const val MELOQIS_RELEASE_MANIFEST =
    "https://meloqis.axenoraai.in/releases/latest.json"
private const val MELOQIS_R2_HOST =
    "pub-7cad9af12a364d3f928f96a083db320f.r2.dev"

sealed class EchoUpdateStatus {
    object Idle : EchoUpdateStatus()
    object Checking : EchoUpdateStatus()
    data class Available(
        val version: String,
        val changelog: List<ChangelogSection>,
        val size: String,
        val releaseDate: String,
        val description: String?,
        val imageUrl: String?,
        val apkUrl: String?,
        val sha256: String,
    ) : EchoUpdateStatus()

    data class NoUpdate(val version: String) : EchoUpdateStatus()
    data class Error(val message: String) : EchoUpdateStatus()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UpdateScreen(navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<EchoUpdateStatus>(EchoUpdateStatus.NoUpdate(BuildConfig.VERSION_NAME)) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgress by remember { mutableStateOf(0f) }
    var isDownloadComplete by remember { mutableStateOf(false) }
    var downloadedFile by remember { mutableStateOf<File?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    val currentVersion = BuildConfig.VERSION_NAME

    LaunchedEffect(Unit) {
        DownloadNotificationManager.initialize(context)
        status = EchoUpdateStatus.Checking
        status = runCatching { fetchOfficialMeloqisRelease() }
            .fold(
                onSuccess = { release ->
                    if (isNewerVersion(release.version, currentVersion)) {
                        EchoUpdateStatus.Available(
                            version = release.version,
                            changelog = listOf(
                                ChangelogSection(
                                    "Verified Meloqis update",
                                    listOf(
                                        "Downloads directly inside Meloqis",
                                        "SHA-256 integrity checked before installation",
                                        "Android shows the final secure install confirmation",
                                    ),
                                ),
                            ),
                            size = release.sizeMb,
                            releaseDate = release.publishedAt.substringBefore('T'),
                            description = "Official Axenora release artifact. No browser or website redirect is used.",
                            imageUrl = null,
                            apkUrl = release.downloadUrl,
                            sha256 = release.sha256,
                        )
                    } else {
                        EchoUpdateStatus.NoUpdate(currentVersion)
                    }
                },
                onFailure = {
                    EchoUpdateStatus.Error("Could not reach the Meloqis release channel. Check your connection and reopen this page.")
                },
            )
    }

    LaunchedEffect(Unit) {
        val workManager = WorkManager.getInstance(context)
        while (true) {
            val workInfo = withContext(Dispatchers.IO) {
                runCatching {
                    workManager
                        .getWorkInfosForUniqueWork(MeloqisUpdateActionReceiver.UPDATE_DOWNLOAD_WORK)
                        .get()
                        .firstOrNull()
                }.getOrNull()
            }

            when (workInfo?.state) {
                WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED, WorkInfo.State.RUNNING -> {
                    isDownloading = true
                    isDownloadComplete = false
                    downloadProgress = workInfo.progress.getFloat("progress", 0f)
                }
                WorkInfo.State.SUCCEEDED -> {
                    isDownloading = false
                    val completedVersion = workInfo.outputData.getString("version")
                    val filePath = workInfo.outputData.getString("file_path")
                    val expectedVersion = (status as? EchoUpdateStatus.Available)?.version
                    if (completedVersion != null && completedVersion == expectedVersion && filePath != null) {
                        val file = File(filePath)
                        isDownloadComplete = file.exists()
                        downloadedFile = file.takeIf { it.exists() }
                        downloadProgress = if (file.exists()) 1f else 0f
                    }
                }
                WorkInfo.State.FAILED -> {
                    if (isDownloading) {
                        snackbarHostState.showSnackbar(context.getString(R.string.download_failed))
                    }
                    isDownloading = false
                }
                WorkInfo.State.CANCELLED -> {
                    isDownloading = false
                    downloadProgress = 0f
                }
                else -> Unit
            }
            delay(500)
        }
    }

    
    LaunchedEffect(isDownloadComplete, downloadedFile) {
        if (isDownloadComplete && downloadedFile != null) {
            if (!downloadedFile!!.exists()) {
                isDownloadComplete = false
                downloadedFile = null
                downloadProgress = 0f
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    val titleText = if (status is EchoUpdateStatus.Available) {
                        buildAnnotatedString {
                            append(stringResource(R.string.new_update) + " ")
                            withStyle(
                                SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append((status as EchoUpdateStatus.Available).version)
                            }
                        }
                    } else {
                        AnnotatedString("Meloqis releases")
                    }
                    Text(text = titleText, maxLines = 1)
                },
                navigationIcon = {
                    Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp)) {
                        ExpressiveIconButton(
                            onClick = { navController.navigateUp() },
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = stringResource(R.string.cancel),
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(WindowInsets.navigationBars.asPaddingValues())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    when (val currentStatus = status) {
                        is EchoUpdateStatus.Idle, is EchoUpdateStatus.Checking, is EchoUpdateStatus.NoUpdate, is EchoUpdateStatus.Error -> Unit

                        is EchoUpdateStatus.Available -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                AnimatedActionButton(
                                    text = stringResource(R.string.later),
                                    onClick = { navController.navigateUp() },
                                    modifier = Modifier.weight(1f),
                                    isOutlined = true,
                                    enabled = !isDownloading
                                )
                                AnimatedActionButton(
                                    text = if (isDownloading) "${(downloadProgress * 100).toInt()}%" else if (isDownloadComplete) stringResource(R.string.install) else stringResource(R.string.update_available),
                                    onClick = {
                                        if (isDownloadComplete) {
                                            val file = downloadedFile
                                            if (file == null || !file.exists()) {
                                                isDownloadComplete = false
                                                downloadedFile = null
                                                downloadProgress = 0f
                                                return@AnimatedActionButton
                                            }
                                            file.let { f ->
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    if (!context.packageManager.canRequestPackageInstalls()) {
                                                        val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                                            data = Uri.parse("package:${context.packageName}")
                                                        }
                                                        context.startActivity(intent)
                                                        return@let
                                                    }
                                                }
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
                                                val installIntent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "application/vnd.android.package-archive")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                ContextCompat.startActivity(context, installIntent, null)
                                            }
                                        } else {
                                            val urlToDownload = currentStatus.apkUrl ?: return@AnimatedActionButton
                                            context.sendBroadcast(
                                                Intent(context, MeloqisUpdateActionReceiver::class.java).apply {
                                                    action = MeloqisUpdateActionReceiver.ACTION_DOWNLOAD_UPDATE
                                                    putExtra(MeloqisUpdateActionReceiver.EXTRA_VERSION, currentStatus.version)
                                                    putExtra(MeloqisUpdateActionReceiver.EXTRA_DOWNLOAD_URL, urlToDownload)
                                                    putExtra(MeloqisUpdateActionReceiver.EXTRA_SHA256, currentStatus.sha256)
                                                },
                                            )
                                            isDownloading = true
                                            isDownloadComplete = false
                                            downloadProgress = 0f
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isDownloading || isDownloadComplete
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { ErrorSnackbar(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .widthIn(max = 700.dp)
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    val contentModifier = if (status is EchoUpdateStatus.Available) {
                        Modifier.fillMaxWidth()
                    } else {
                        Modifier.fillParentMaxSize()
                    }

                    Box(
                        modifier = contentModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        when (val currentStatus = status) {
                            is EchoUpdateStatus.Checking -> {
                                androidx.compose.material3.ContainedLoadingIndicator(
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                            is EchoUpdateStatus.NoUpdate -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.deployed_app_update),
                                        contentDescription = null,
                                        modifier = Modifier.size(120.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = "Meloqis release channel",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Automatic update alerts are enabled for official Meloqis releases. Android will notify you when a verified APK is available.\nCurrent version: ${currentStatus.version}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            is EchoUpdateStatus.Error -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.error),
                                        contentDescription = null,
                                        modifier = Modifier.size(120.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Text(
                                        text = currentStatus.message,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.error,
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            is EchoUpdateStatus.Available -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.Start
                                ) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = stringResource(R.string.release_date_v, currentStatus.releaseDate),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stringResource(R.string.update_size_v, currentStatus.size),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(24.dp))
                                    if (!currentStatus.imageUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = currentStatus.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(24.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }
                                    if (!currentStatus.description.isNullOrBlank()) {
                                        val annotatedText = currentStatus.description.parseMarkdown()

                                        ClickableText(
                                            text = annotatedText,
                                            onClick = { offset ->
                                                annotatedText.getStringAnnotations("URL", offset, offset).firstOrNull()?.let {
                                                    ContextCompat.startActivity(context, Intent(Intent.ACTION_VIEW, Uri.parse(it.item)), null)
                                                }
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                lineHeight = 20.sp
                                            ),
                                            modifier = Modifier.padding(bottom = 24.dp)
                                        )
                                    }
                                    
                                    currentStatus.changelog.forEach { section ->
                                        Text(
                                            text = section.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground,
                                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                                        )
                                        section.items.forEachIndexed { index, item ->
                                            val shape = when {
                                                section.items.size == 1 -> detachedItemShape()
                                                index == 0 -> leadingItemShape()
                                                index == section.items.size - 1 -> endItemShape()
                                                else -> middleItemShape()
                                            }
                                            ChangelogItem(text = item, shape = shape)
                                            if (index != section.items.size - 1) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                            }
                                        }
                                    }
                                    
                                    if (currentStatus.changelog.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                    if (isDownloading) {
                                        if (downloadProgress > 0f) {
                                            androidx.compose.material3.LinearProgressIndicator(
                                                progress = downloadProgress,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                        } else {
                                            androidx.compose.material3.LinearProgressIndicator(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(4.dp)),
                                                color = MaterialTheme.colorScheme.primary,
                                                trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(24.dp))
                                    }

                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}



const val PREFS_NAME = "settings"
const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
const val KEY_LAST_CHECKED_TIME = "last_checked_time"
const val KEY_BETA_UPDATES = "beta_updates"
const val KEY_UPDATE_AVAILABLE = "update_available"

fun getUpdateAvailableState(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_AVAILABLE, false)
}

fun saveUpdateAvailableState(context: Context, available: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_AVAILABLE, available).apply()
}

fun getAutoUpdateCheckSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_AUTO_UPDATE_CHECK, false)
}

fun saveAutoUpdateCheckSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, enabled).apply()
}

const val KEY_UPDATE_NOTIFICATIONS = "update_notifications"

fun getUpdateNotificationsSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_UPDATE_NOTIFICATIONS, true)
}

fun saveUpdateNotificationsSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_UPDATE_NOTIFICATIONS, enabled).apply()
}

fun saveLastCheckedTime(context: Context, timestamp: String) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putString(KEY_LAST_CHECKED_TIME, timestamp).apply()
}

fun getLastCheckedTime(context: Context): String {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getString(KEY_LAST_CHECKED_TIME, "") ?: ""
}

fun getBetaUpdatesSetting(context: Context): Boolean {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return sharedPrefs.getBoolean(KEY_BETA_UPDATES, false)
}

fun saveBetaUpdatesSetting(context: Context, enabled: Boolean) {
    val sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    sharedPrefs.edit().putBoolean(KEY_BETA_UPDATES, enabled).apply()
}

private fun formatGitHubDate(githubDate: String): String = try {
    val githubFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'")
    val displayFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy, h:mm a")
    val dateTime = LocalDateTime.parse(githubDate, githubFormatter)
    dateTime.format(displayFormatter)
} catch (e: Exception) {
    githubDate
}


fun isNewerVersion(latestVersion: String, currentVersion: String): Boolean {
    val latestVersionClean = latestVersion.removePrefix("b").removePrefix("v")
    val currentVersionClean = currentVersion.removePrefix("b").removePrefix("v")

    val latestParts = latestVersionClean.split(".").map { it.toIntOrNull() ?: 0 }
    val currentParts = currentVersionClean.split(".").map { it.toIntOrNull() ?: 0 }
    
    
    for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
        val latest = latestParts.getOrElse(i) { 0 }
        val current = currentParts.getOrElse(i) { 0 }
        when {
            latest > current -> return true
            latest < current -> return false
        }
    }
    
    
    if (latestVersionClean == currentVersionClean) {
        val latestIsBeta = latestVersion.startsWith("b")
        val currentIsBeta = currentVersion.startsWith("b")
        
        if (currentIsBeta && !latestIsBeta) return true
    }
    
    return false
}


suspend fun checkForUpdate(
    context: Context,
    onSuccess: (tag: String, isAvailable: Boolean, changelog: List<ChangelogSection>, size: String, date: String, description: String?, imageUrl: String?, apkUrl: String?) -> Unit,
    onError: () -> Unit,
) {
    withContext(Dispatchers.IO) {
        try {
            val url = URL("https://api.github.com/repos/kaarthikarorasahabji/Meloqis-Music/releases/latest")
            val json = url.openStream().bufferedReader().use { it.readText() }
            val targetRelease = JSONObject(json)
            
            val currentVersion = BuildConfig.VERSION_NAME
            val targetTagName = targetRelease.getString("tag_name")
            val currentClean = currentVersion.removePrefix("b").removePrefix("v").trim()
            val targetClean = targetTagName.removePrefix("b").removePrefix("v").trim()
            val shouldShow = currentClean != targetClean

            if (shouldShow) {
                val tagWithPrefix = targetRelease.getString("tag_name")
                val displayTag = tagWithPrefix

                
                val changelogList = mutableListOf<ChangelogSection>()
                var description: String? = null
                var imageUrl: String? = null
                try {
                    val changelogUrl =
                        URL("https://github.com/kaarthikarorasahabji/Meloqis-Music/releases/download/$tagWithPrefix/changelog.json")
                    val changelogJson = changelogUrl.openStream().bufferedReader().use { it.readText() }
                    val changelogData = JSONObject(changelogJson)

                    description = changelogData.optString("description").takeIf { it.isNotEmpty() }
                    imageUrl = changelogData.optString("image").takeIf { it.isNotEmpty() }

                    val changelogArray = changelogData.getJSONArray("changelog")
                    for (j in 0 until changelogArray.length()) {
                        val sectionObj = changelogArray.getJSONObject(j)
                        val title = sectionObj.getString("title")
                        val itemsArray = sectionObj.getJSONArray("items")
                        val itemsList = mutableListOf<String>()
                        for (k in 0 until itemsArray.length()) {
                            itemsList.add(itemsArray.getString(k))
                        }
                        changelogList.add(ChangelogSection(title, itemsList))
                    }
                } catch (e: Exception) {
                    var body = targetRelease.optString("body", context.getString(R.string.no_changelog_available))
                    
                    val imageRegex = Regex("!\\[(.*?)\\]\\((.*?)\\)")
                    val match = imageRegex.find(body)
                    if (match != null) {
                        imageUrl = match.groupValues[2]
                        body = body.replace(match.value, "").trim()
                    }
                    description = body
                }

                val publishedAt = targetRelease.getString("published_at")
                val formattedReleaseDate = formatGitHubDate(publishedAt)
                val assets = targetRelease.getJSONArray("assets")

                var apkSizeInMB = ""
                var apkDownloadUrl = ""
                for (j in 0 until assets.length()) {
                    val asset = assets.getJSONObject(j)
                    val assetName = asset.getString("name")
                    if (assetName.endsWith(".apk", ignoreCase = true) && !assetName.lowercase().contains("debug")) {
                        val apkSizeInBytes = asset.getLong("size")
                        apkSizeInMB = String.format("%.1f", apkSizeInBytes / (1024.0 * 1024.0))
                        apkDownloadUrl = asset.getString("browser_download_url")
                        break
                    }
                }

                if (apkDownloadUrl.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        onSuccess(displayTag, true, changelogList, apkSizeInMB, formattedReleaseDate, description, imageUrl, apkDownloadUrl)
                    }
                    return@withContext
                }
            }

            
            withContext(Dispatchers.Main) {
                onSuccess(currentVersion, false, emptyList(), "", "", null, null, null)
            }
        } catch (e: Exception) {
            Log.e("UpdateCheck", "Error checking for updates: ${e.message}", e)
            withContext(Dispatchers.Main) { onError() }
        }
    }
}
fun String.extractUrls(): List<Pair<IntRange, String>> {
    val urlPattern = Pattern.compile(
        "(?:^|[\\s])((https?://|www\\.|pic\\.)[\\w-]+(\\.[\\w-]+)+([/?].*)?)"
    )
    val matcher = urlPattern.matcher(this)
    val urlList = mutableListOf<Pair<IntRange, String>>()

    while (matcher.find()) {
        val url = matcher.group(1)?.trim() ?: continue
        val range = IntRange(matcher.start(1), matcher.end(1) - 1)
        
        val fullUrl = if (url.startsWith("http")) url else "https://$url"
        urlList.add(range to fullUrl)
    }

    return urlList
}
