package echo.music.iad1tya.ui.screen.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import echo.music.iad1tya.common.Config
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.expect.ui.PlatformWebView
import echo.music.iad1tya.expect.ui.createWebViewCookieManager
import echo.music.iad1tya.expect.ui.rememberWebViewState
import echo.music.iad1tya.ui.component.DevLogInBottomSheet
import echo.music.iad1tya.ui.component.DevLogInType
import echo.music.iad1tya.ui.component.RippleIconButton
import echo.music.iad1tya.ui.icon.ArrowBackIosNew
import echo.music.iad1tya.ui.icon.LogoDev
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.theme.typo
import echo.music.iad1tya.viewModel.LogInViewModel
import echo.music.iad1tya.viewModel.SettingsViewModel
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import io.ktor.http.URLProtocol
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.log_in
import echomusic.composeapp.generated.resources.login_failed
import echomusic.composeapp.generated.resources.login_success

@OptIn(ExperimentalMaterial3Api::class, ExperimentalHazeMaterialsApi::class)
@Composable
fun LoginScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    viewModel: LogInViewModel = koinViewModel(),
    settingsViewModel: SettingsViewModel = koinViewModel(),
    hideBottomNavigation: () -> Unit,
    showBottomNavigation: () -> Unit,
) {
    val hazeState = rememberHazeState()
    val coroutineScope = rememberCoroutineScope()
    var devLoginSheet by rememberSaveable {
        mutableStateOf(false)
    }
    var checkingAccount by remember { mutableStateOf(false) }

    val state = rememberWebViewState()

    LaunchedEffect(state) {
        snapshotFlow { state.value }.collect {
            Logger.d(
                "LogInScreen",
                "WebViewState: ${
                    when (it) {
                        is echo.music.iad1tya.expect.ui.WebViewState.Finished -> "Finished"
                        is echo.music.iad1tya.expect.ui.WebViewState.Loading -> "Loading ${it.progress}%"
                    }
                }",
            )
        }
    }

    // Hide bottom navigation when entering this screen
    LaunchedEffect(Unit) {
        hideBottomNavigation()
        createWebViewCookieManager().removeAllCookies()
    }

    // Show bottom navigation when leaving this screen
    DisposableEffect(Unit) {
        onDispose {
            showBottomNavigation()
        }
    }

    Box(modifier = Modifier.fillMaxSize().hazeSource(state = hazeState)) {
        Column {
            Spacer(
                Modifier
                    .size(
                        innerPadding.calculateTopPadding() + 64.dp,
                    ),
            )
            // WebView for YouTube Music login
            PlatformWebView(
                state,
                Config.LOG_IN_URL,
                aboveContent = {
                    if (devLoginSheet) {
                        DevLogInBottomSheet(
                            onDismiss = {
                                devLoginSheet = false
                            },
                            onDone = { cookie ->
                                coroutineScope.launch {
                                    val success = settingsViewModel.addAccount(cookie)
                                    if (success) {
                                        viewModel.makeToast(getString(Res.string.login_success))
                                        navController.navigateUp()
                                    } else {
                                        viewModel.makeToast(getString(Res.string.login_failed))
                                    }
                                }
                            },
                            type = DevLogInType.YouTube,
                        )
                    }
                }
            ) { url ->
                val destination = runCatching { Url(url) }.getOrNull()
                if (!checkingAccount && destination?.protocol == URLProtocol.HTTPS &&
                    destination.host == "music.youtube.com" && destination.encodedPath.trimEnd('/').isEmpty()
                ) {
                    val cookies = createWebViewCookieManager().getCookie(Config.YOUTUBE_MUSIC_MAIN_URL)
                    val hasSession = cookies.split(';').any { entry ->
                        val key = entry.trim().substringBefore('=')
                        key in setOf("SAPISID", "__Secure-3PAPISID") && entry.substringAfter('=', "").isNotBlank()
                    }
                    if (!hasSession) return@PlatformWebView
                    checkingAccount = true
                    coroutineScope.launch {
                        try {
                            if (settingsViewModel.addAccount(cookies)) {
                                createWebViewCookieManager().removeAllCookies()
                                viewModel.makeToast(getString(Res.string.login_success))
                                navController.navigateUp()
                            } else {
                                viewModel.makeToast(getString(Res.string.login_failed))
                            }
                        } finally {
                            checkingAccount = false
                        }
                    }
                }
            }
        }

        // Top App Bar with haze effect
        TopAppBar(
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .hazeEffect(state = hazeState, style = HazeMaterials.ultraThin()) {
                        blurEnabled = true
                    },
            title = {
                Text(
                    text = stringResource(Res.string.log_in),
                    style = typo().titleMedium,
                )
            },
            navigationIcon = {
                Box(Modifier.padding(horizontal = 5.dp)) {
                    RippleIconButton(
                        echoIcons.ArrowBackIosNew,
                        Modifier.size(32.dp),
                        true,
                    ) {
                        navController.navigateUp()
                    }
                }
            },
            actions = {
                IconButton(
                    onClick = {
                        devLoginSheet = true
                    },
                ) {
                    Icon(
                        echoIcons.LogoDev,
                        "Developer Mode",
                    )
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
        )
    }
}
