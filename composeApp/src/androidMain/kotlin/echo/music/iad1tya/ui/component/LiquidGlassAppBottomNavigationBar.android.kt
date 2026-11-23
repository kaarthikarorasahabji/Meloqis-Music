package echo.music.iad1tya.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import echo.music.iad1tya.domain.data.player.GenericMediaItem
import echo.music.iad1tya.expect.ui.PlatformBackdrop
import echo.music.iad1tya.ui.screen.MiniPlayer
import echo.music.iad1tya.ui.theme.LocalIsDarkTheme
import echo.music.iad1tya.viewModel.SharedViewModel
import kotlin.reflect.KClass

@Composable
actual fun LiquidGlassAppBottomNavigationBar(
    startDestination: Any,
    navController: NavController,
    backdrop: PlatformBackdrop,
    viewModel: SharedViewModel,
    isScrolledToTop: Boolean,
    showAnalyticsTab: Boolean,
    onOpenNowPlaying: () -> Unit,
    reloadDestinationIfNeeded: (KClass<*>) -> Unit,
) {
    val layer = rememberGraphicsLayer()
    val nowPlaying by viewModel.nowPlayingState.collectAsStateWithLifecycle()
    val entry by navController.currentBackStackEntryAsState()
    val tabs = listOfNotNull(
        BottomNavScreen.Home,
        BottomNavScreen.Search,
        BottomNavScreen.Library,
        BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
    )
    var selectedOrdinal by rememberSaveable {
        mutableIntStateOf(tabs.firstOrNull { it.destination == startDestination }?.ordinal ?: BottomNavScreen.Home.ordinal)
    }
    LaunchedEffect(entry, showAnalyticsTab) {
        val destination = entry?.destination
        tabs.firstOrNull { tab -> destination?.hierarchy?.any { it.hasRoute(tab.destination::class) } == true }
            ?.let { selectedOrdinal = it.ordinal }
        if (tabs.none { it.ordinal == selectedOrdinal }) selectedOrdinal = BottomNavScreen.Home.ordinal
    }
    val luminance = if (LocalIsDarkTheme.current) 0.35f else 0.72f
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AnimatedVisibility(nowPlaying?.mediaItem?.let { it != GenericMediaItem.EMPTY } == true) {
            MiniPlayer(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                backdrop = backdrop,
                onClick = onOpenNowPlaying,
                onClose = {
                    viewModel.stopPlayer()
                    viewModel.isServiceRunning = false
                },
            )
        }
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            LiquidGlassTabBar(
                tabs = tabs,
                selectedTab = tabs.indexOfFirst { it.ordinal == selectedOrdinal }.coerceAtLeast(0),
                backdrop = backdrop,
                layer = layer,
                luminance = luminance,
                tabWidth = (maxWidth / tabs.size).coerceAtMost(112.dp),
                onTabSelected = { index ->
                    val tab = tabs[index]
                    if (entry?.destination?.hierarchy?.any { it.hasRoute(tab.destination::class) } == true) {
                        reloadDestinationIfNeeded(tab.destination::class)
                    } else {
                        selectedOrdinal = tab.ordinal
                        navController.navigate(tab.destination) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
            )
        }
    }
}
