package echo.music.iad1tya.ui.screen.home

import echomusic.composeapp.generated.resources.search
import echo.music.iad1tya.ui.icon.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import kotlinx.coroutines.CancellationException
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import echo.music.iad1tya.common.CHART_SUPPORTED_COUNTRY
import echo.music.iad1tya.common.Config
import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.domain.data.model.home.HomeItem
import echo.music.iad1tya.domain.manager.DataStoreManager
import echo.music.iad1tya.domain.data.model.home.chart.Chart
import echo.music.iad1tya.domain.data.model.mood.Mood
import echo.music.iad1tya.domain.extension.now
import echo.music.iad1tya.domain.mediaservice.handler.PlaylistType
import echo.music.iad1tya.domain.mediaservice.handler.QueueData
import echo.music.iad1tya.domain.utils.connectArtists
import echo.music.iad1tya.domain.utils.toSongEntity
import echo.music.iad1tya.domain.utils.toListName
import echo.music.iad1tya.domain.utils.toTrack
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.ui.component.rememberHolderPainter
import echo.music.iad1tya.extension.isScrollingUp
import echo.music.iad1tya.ui.component.CenterLoadingBox
import echo.music.iad1tya.ui.component.Chip
import echo.music.iad1tya.ui.component.DropdownButton
import echo.music.iad1tya.ui.component.EndOfPage
import echo.music.iad1tya.ui.component.HomeItem
import echo.music.iad1tya.ui.component.HomeItemContentPlaylist
import echo.music.iad1tya.ui.component.HomeShimmer
import echo.music.iad1tya.ui.component.ItemArtistChart
import echo.music.iad1tya.ui.component.MoodMomentAndGenreHomeItem
import echo.music.iad1tya.ui.component.OfflineErrorState
import echo.music.iad1tya.ui.component.NowPlayingBottomSheet
import echo.music.iad1tya.ui.component.ReviewDialog
import echo.music.iad1tya.ui.component.RippleIconButton
import echo.music.iad1tya.ui.component.ShareSavedLyricsDialog
import echo.music.iad1tya.ui.icon.History
import echo.music.iad1tya.ui.icon.Notifications
import echo.music.iad1tya.ui.icon.Settings
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.icon.Menu
import echo.music.iad1tya.ui.navigation.destination.home.HomeDestination
import echo.music.iad1tya.ui.navigation.destination.home.MoodDestination
import echo.music.iad1tya.ui.navigation.destination.home.SettingsDestination
import echo.music.iad1tya.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import echo.music.iad1tya.ui.navigation.destination.list.ArtistDestination
import echo.music.iad1tya.ui.screen.library.LibraryDynamicPlaylistType
import echo.music.iad1tya.ui.navigation.destination.list.PlaylistDestination
import echo.music.iad1tya.ui.navigation.destination.login.LoginDestination
import echo.music.iad1tya.ui.theme.LocalNowPlayingColor
import echo.music.iad1tya.ui.theme.typo
import echo.music.iad1tya.viewModel.HomeViewModel
import echo.music.iad1tya.ui.component.HomeListeningHistory
import echo.music.iad1tya.ui.component.SongFullWidthItems
import androidx.compose.material3.OutlinedButton
import echo.music.iad1tya.ui.navigation.destination.search.SearchDestination
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_COMMUTE
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_ENERGIZE
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_FEEL_GOOD
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_FOCUS
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_MIX_FOR_YOU
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_PARTY
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_RELAX
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_ROMANCE
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_SAD
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_SLEEP
import echo.music.iad1tya.viewModel.LibraryViewModel
import echo.music.iad1tya.ui.component.GridLibraryPlaylist
import echomusic.composeapp.generated.resources.mix_for_you
import echomusic.composeapp.generated.resources.no_mixes_found
import echo.music.iad1tya.viewModel.HomeViewModel.Companion.HOME_PARAMS_WORKOUT
import echo.music.iad1tya.viewModel.ListState
import echo.music.iad1tya.viewModel.SharedViewModel
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.all
import echomusic.composeapp.generated.resources.cancel
import echomusic.composeapp.generated.resources.chart
import echomusic.composeapp.generated.resources.commute
import echomusic.composeapp.generated.resources.do_not_show_again
import echomusic.composeapp.generated.resources.energize
import echomusic.composeapp.generated.resources.feel_good
import echomusic.composeapp.generated.resources.focus
import echomusic.composeapp.generated.resources.go_to_log_in_page
import echomusic.composeapp.generated.resources.good_afternoon
import echomusic.composeapp.generated.resources.good_evening
import echomusic.composeapp.generated.resources.good_morning
import echomusic.composeapp.generated.resources.good_night
import echomusic.composeapp.generated.resources.let_s_pick_a_playlist_for_you
import echomusic.composeapp.generated.resources.log_in_warning
import echomusic.composeapp.generated.resources.party
import echomusic.composeapp.generated.resources.relax
import echomusic.composeapp.generated.resources.romance
import echomusic.composeapp.generated.resources.sad
import echomusic.composeapp.generated.resources.sleep
import echomusic.composeapp.generated.resources.top_artists
import echomusic.composeapp.generated.resources.warning
import echomusic.composeapp.generated.resources.what_is_best_choice_today
import echomusic.composeapp.generated.resources.workout
import echomusic.composeapp.generated.resources.meloqis_logo


@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun HomeScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: HomeViewModel =
        koinViewModel(),
    sharedViewModel: SharedViewModel =
        koinInject(),
    libraryViewModel: LibraryViewModel =
        koinViewModel(),
    navController: NavController,
    onOpenNowPlaying: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val chartLoading by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val youTubeMixForYou by libraryViewModel.youTubeMixForYou.collectAsStateWithLifecycle()

    val regionChart by viewModel.regionCodeChart.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()

    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsStateWithLifecycle()

    val openAppTime by sharedViewModel.openAppTime.collectAsStateWithLifecycle()
    val shareLyricsPermissions by sharedViewModel.shareSavedLyrics.collectAsStateWithLifecycle()
    val nowPlayingTrack by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val localHistory by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val youtubeHistory by viewModel.youtubeListeningHistory.collectAsStateWithLifecycle()
    val explicitEnabled by viewModel.explicitContentEnabled.collectAsStateWithLifecycle(DataStoreManager.TRUE)

    fun allowedTracks(tracks: List<Track>): List<Track> =
        if (explicitEnabled == DataStoreManager.TRUE) tracks else tracks.filterNot { it.isExplicit }
    fun playTracks(tracks: List<Track>, name: String) {
        val playable = allowedTracks(tracks)
        val first = playable.firstOrNull() ?: return
        viewModel.setQueueData(QueueData.Data(
            listTracks = playable,
            firstPlayedTrack = first,
            playlistId = if (playable.size == 1) "RDAMVM${first.videoId}" else null,
            playlistName = name,
            playlistType = if (playable.size == 1) PlaylistType.RADIO else PlaylistType.LOCAL_PLAYLIST,
        ))
        viewModel.loadMediaItem(first, if (playable.size == 1) Config.SONG_CLICK else Config.PLAYLIST_CLICK, 0)
    }

    val listeningShelf: @Composable () -> Unit = {
        HomeListeningHistory(
            history = (youtubeHistory as? echo.music.iad1tya.viewModel.YouTubeListeningHistory.Loaded)
                ?.let { it.copy(tracks = allowedTracks(it.tracks)) } ?: youtubeHistory,
            localTracks = allowedTracks(localHistory.map { it.toTrack() }),
            currentTrackId = nowPlayingTrack?.mediaItem?.mediaId,
            onPlay = { track ->
                if (track.videoId == nowPlayingTrack?.mediaItem?.mediaId) onOpenNowPlaying()
                else playTracks(listOf(track), track.title)
            },
            onPlayAll = { playTracks(it, "Listen again") },
            onConnect = { navController.navigate(LoginDestination) { launchSingleTop = true } },
            onRetry = viewModel::refreshYouTubeListeningHistory,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    val nowPlayingColor = LocalNowPlayingColor.current

    LaunchedEffect(params) {
        if (params == HOME_PARAMS_MIX_FOR_YOU && youTubeMixForYou.data.isNullOrEmpty()) {
            libraryViewModel.getYouTubeMixedForYou()
        }
    }
    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.refreshYouTubeListeningHistory()
        viewModel.getHomeItemList(params)
        Logger.w("HomeScreen", "onRefresh")
    }
    LaunchedEffect(key1 = reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0) {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                scrollState.animateScrollToItem(0)
                sharedViewModel.reloadDestinationDone()
            } else {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                onRefresh.invoke()
            }
        }
    }
    LaunchedEffect(key1 = loading) {
        if (!loading) {
            isRefreshing = false
            sharedViewModel.reloadDestinationDone()
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
    }


    val shouldStartPaginate =
        remember {
            derivedStateOf {
                homeListState != ListState.PAGINATION_EXHAUST &&
                    (
                        scrollState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                    ) >= (scrollState.layoutInfo.totalItemsCount - 1)
            }
        }

    LaunchedEffect(key1 = shouldStartPaginate.value) {
        Logger.d("HomeScreen", "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d("HomeScreen", "homeListState: $homeListState")
        Logger.d("HomeScreen", "Continuation: $continuation")
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {

            viewModel.getContinueHomeItem(
                continuation,
            )
        }
    }

//    if (shouldShowGetDataSyncIdBottomSheet) {
//        GetDataSyncIdBottomSheet(
//            cookie = youTubeCookie,
//            onDismissRequest = {
//                shouldShowGetDataSyncIdBottomSheet = false
//            },
//        )
//    }





    val headerTint = nowPlayingColor ?: MaterialTheme.colorScheme.primary
    val headerFadeHeight = with(LocalDensity.current) { 220.dp.toPx() }
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(headerTint.copy(alpha = 0.19f), Color.Transparent),
                endY = headerFadeHeight,
            ),
        ),
    ) {
        HomeTopAppBar(navController = navController, accountInfo = accountInfo)
        HomeMoodStrip(
            params = params,
            modifier = Modifier.horizontalScroll(chipRowState),
            onSelect = viewModel::setParams,
        )
        PullToRefreshBox(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
        ) {
            if (params == HOME_PARAMS_MIX_FOR_YOU) {
                GridLibraryPlaylist(
                    navController = navController,
                    contentPadding = PaddingValues(0.dp),
                    data = youTubeMixForYou,
                    emptyText = Res.string.no_mixes_found,
                    onReload = { libraryViewModel.getYouTubeMixedForYou() },
                )
            } else {
                LazyColumn(
                    state = scrollState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (params == null) item(key = "home-listening") { listeningShelf() }
                    if (loading && homeData.isEmpty()) {
                        item(key = "home-loading") { HomeShimmer(isGrid = false) }
                    }
                    if (homeData.isEmpty() && homeListState == ListState.PAGINATION_EXHAUST) {
                        item(key = "home-offline") {
                            OfflineErrorState(
                                onRetry = onRefresh,
                                onOpenDownloaded = {
                                    navController.navigate(LibraryDynamicPlaylistDestination(
                                        type = LibraryDynamicPlaylistType.Downloaded.toStringParams(),
                                    ))
                                },
                            )
                        }
                    }
                    itemsIndexed(homeData, key = { index, item -> "feed:$index:${item.title}" }) { _, item ->
                        Box {
                            Column(
                                modifier =
                                    Modifier
                                        .padding(horizontal = 15.dp),
                            ) {
                                val songsOnly = item.contents.filterNotNull().let { contents ->
                                    contents.isNotEmpty() && contents.all { !it.videoId.isNullOrBlank() }
                                }
                                if (songsOnly) {
                                    HomeSongShelf(
                                        title = item.title,
                                        tracks = allowedTracks(item.contents.filterNotNull().map { it.toTrack() }.distinctBy { it.videoId }),
                                        activeTrackId = nowPlayingTrack?.mediaItem?.mediaId,
                                        onPlay = { playTracks(listOf(it), it.title) },
                                        onPlayAll = { playTracks(it, item.title) },
                                        navController = navController,
                                    )
                                } else {
                                    HomeItem(navController = navController, data = item)
                                }

                            }
                        }
                    }
                    if (homeListState == ListState.PAGINATING) {
                        item(key = "home-loading-more") {
                            CenterLoadingBox(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                            )
                        }
                    }
                    if (homeListState == ListState.PAGINATION_EXHAUST) {
                        items(newRelease, key = { it.hashCode() }) { release ->
                            Box(modifier = Modifier.padding(horizontal = 15.dp)) {
                                HomeItem(
                                    navController = navController,
                                    data = release,
                                )
                            }
                        }
                        moodMomentAndGenre?.let { mood ->
                            item(key = "home-moods") {
                                Box(modifier = Modifier.padding(horizontal = 15.dp)) {
                                    MoodMomentAndGenre(
                                        mood = mood,
                                        navController = navController,
                                    )
                                }
                            }
                        }
                        item {
                            Column(
                                Modifier
                                    .padding(vertical = 10.dp)
                                    .padding(horizontal = 15.dp),
                                verticalArrangement = Arrangement.SpaceBetween,
                            ) {
                                ChartTitle()
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(targetState = regionChart) {
                                    Logger.w("HomeScreen", "regionChart: $it")
                                    if (it != null) {
                                        DropdownButton(
                                            items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                            defaultSelected =
                                                CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(
                                                    CHART_SUPPORTED_COUNTRY.items.indexOf(it),
                                                )
                                                    ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                        ) {
                                            viewModel.exploreChart(
                                                CHART_SUPPORTED_COUNTRY.items[
                                                    CHART_SUPPORTED_COUNTRY.itemsData.indexOf(
                                                        it,
                                                    ),
                                                ],
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Crossfade(
                                    targetState = chartLoading,
                                    label = "Chart",
                                ) { loading ->
                                    if (!loading) {
                                        chart?.let {
                                            ChartData(
                                                chart = it,
                                                navController = navController,
                                            )
                                        }
                                    } else {
                                        CenterLoadingBox(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(400.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item(key = "home-end") { EndOfPage() }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopAppBar(
    navController: NavController,
    accountInfo: Pair<String?, String?>?,
) {
    TopAppBar(
        expandedHeight = 52.dp,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Image(
                    painter = painterResource(Res.drawable.meloqis_logo),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
                Text("Meloqis", style = MaterialTheme.typography.titleLarge)
            }
        },
        actions = {
            IconButton(onClick = { navController.navigate(SearchDestination) { launchSingleTop = true } }) {
                Icon(echoIcons.Search, contentDescription = stringResource(Res.string.search))
            }
            IconButton(onClick = { navController.navigate(SettingsDestination) { launchSingleTop = true } }) {
                val avatar = accountInfo?.second
                if (avatar.isNullOrBlank()) {
                    Icon(echoIcons.Settings, contentDescription = "Settings")
                } else {
                    AsyncImage(
                        model = avatar,
                        contentDescription = "Account and settings",
                        modifier = Modifier.size(30.dp).clip(CircleShape),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    )
}

@Composable
private fun HomeMoodStrip(
    params: String?,
    modifier: Modifier,
    onSelect: (String?) -> Unit,
) {
    val moods = listOf(
        Res.string.all to null,
        Res.string.workout to HOME_PARAMS_WORKOUT,
        Res.string.feel_good to HOME_PARAMS_FEEL_GOOD,
        Res.string.energize to HOME_PARAMS_ENERGIZE,
        Res.string.relax to HOME_PARAMS_RELAX,
        Res.string.focus to HOME_PARAMS_FOCUS,
        Res.string.mix_for_you to HOME_PARAMS_MIX_FOR_YOU,
        Res.string.sleep to HOME_PARAMS_SLEEP,
        Res.string.sad to HOME_PARAMS_SAD,
        Res.string.romance to HOME_PARAMS_ROMANCE,
        Res.string.party to HOME_PARAMS_PARTY,
        Res.string.commute to HOME_PARAMS_COMMUTE,
    )
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        moods.forEach { (label, endpoint) ->
            FilterChip(
                selected = params == endpoint,
                onClick = { onSelect(endpoint) },
                label = { Text(stringResource(label), maxLines = 1) },
                shape = RoundedCornerShape(10.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = null,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeSongShelf(
    title: String,
    tracks: List<Track>,
    activeTrackId: String?,
    onPlay: (Track) -> Unit,
    onPlayAll: (List<Track>) -> Unit,
    navController: NavController,
) {
    if (tracks.isEmpty()) return
    var menuTrack by remember { mutableStateOf<Track?>(null) }
    menuTrack?.let {
        NowPlayingBottomSheet(onDismiss = { menuTrack = null }, song = it.toSongEntity(), navController = navController)
    }
    val pages = tracks.chunked(4)
    val pager = androidx.compose.foundation.pager.rememberPagerState(pageCount = { pages.size })
    Column(Modifier.padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                title, modifier = Modifier.weight(1f).padding(end = 6.dp),
                style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            if (tracks.size > 1) OutlinedButton(onClick = { onPlayAll(tracks) }) { Text("Play all") }
        }
        androidx.compose.foundation.pager.HorizontalPager(state = pager, pageSpacing = 12.dp) { page ->
            Column(Modifier.fillMaxWidth().height((minOf(tracks.size, 4) * 60).dp)) {
                pages[page].forEach { track ->
                    SongFullWidthItems(
                        track = track,
                        steadyText = true,
                        isPlaying = activeTrackId == track.videoId,
                        onMoreClickListener = { menuTrack = track },
                        onClickListener = { onPlay(track) },
                        modifier = Modifier.fillMaxWidth().height(60.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun MoodMomentAndGenre(
    mood: Mood,
    navController: NavController,
) {
    Column(
        Modifier
            .padding(vertical = 8.dp),
    ) {
        Text(
            text = stringResource(Res.string.let_s_pick_a_playlist_for_you),
            style = typo().bodyMedium,
        )
        // One block per section YouTube returned, headed by ITS OWN title. Hard-coding
        // "Moods & moment" / "Genre" here (and reading mood.moodsMoments / mood.genres by
        // index) mislabelled every row as soon as a signed-in account got an extra
        // "For you" section, and hid the real Genres section altogether.
        mood.sections.forEach { section ->
            val gridState = rememberLazyGridState()
            val flingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = gridState))
            Text(
                text = section.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
            )
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.height(210.dp),
                state = gridState,
                flingBehavior = flingBehavior,
            ) {
                items(section.items, key = { it.params }) { item ->

                    MoodMomentAndGenreHomeItem(
                        title = item.title,
                        stripeColor = item.stripeColor,
                    ) {
                        navController.navigate(
                            MoodDestination(
                                item.params,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = stringResource(Res.string.what_is_best_choice_today),
            style = typo().bodyMedium,
        )
        Text(
            text = stringResource(Res.string.chart),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
    }
}

@Composable
fun ChartData(
    chart: Chart,
    navController: NavController,
) {
    var gridWidthDp by remember {
        mutableStateOf(0.dp)
    }
    val density = LocalDensity.current

    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))

    Column(
        Modifier.onGloballyPositioned { coordinates ->
            with(density) {
                gridWidthDp = (coordinates.size.width).toDp()
            }
        },
    ) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(flingBehavior = snapperFlingBehavior) {
                items(item.playlists.size, key = { index ->
                    val data = item.playlists[index]
                    data.id + data.title + index
                }) {
                    HomeItemContentPlaylist(
                        onClick = {
                            navController.navigate(
                                PlaylistDestination(
                                    playlistId = item.playlists[it].id,
                                    isYourYouTubePlaylist = false,
                                ),
                            )
                        },
                        data = item.playlists[it],
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.top_artists),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(240.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index ->
                val item = chart.artists.itemArtists[index]
                item.title + item.browseId + index
            }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(
                    onClick = {
                        navController.navigate(
                            ArtistDestination(
                                channelId = data.browseId,
                            ),
                        )
                    },
                    data = data,
                    widthDp = gridWidthDp,
                )
            }
        }
    }
}
