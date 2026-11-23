package echo.music.iad1tya.ui.screen.home

import echomusic.composeapp.generated.resources.search
import echo.music.iad1tya.ui.icon.Search
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.contentDescription
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import echo.music.iad1tya.common.Config
import echo.music.iad1tya.domain.data.model.browse.album.Track
import echo.music.iad1tya.domain.data.entities.SongEntity
import echo.music.iad1tya.domain.data.model.home.HomeItem
import echo.music.iad1tya.domain.manager.DataStoreManager
import echo.music.iad1tya.domain.mediaservice.handler.PlaylistType
import echo.music.iad1tya.domain.mediaservice.handler.QueueData
import echo.music.iad1tya.domain.utils.toSongEntity
import echo.music.iad1tya.domain.utils.toTrack
import echo.music.iad1tya.logger.Logger
import echo.music.iad1tya.extension.isScrollingUp
import echo.music.iad1tya.ui.component.CenterLoadingBox
import echo.music.iad1tya.ui.component.ChoosePlaybackMoodDialog
import echo.music.iad1tya.ui.component.CreateHomePlaylistDialog
import echo.music.iad1tya.ui.component.PlaybackMoodRow
import echo.music.iad1tya.ui.component.PersonalYouTubeMixes
import echo.music.iad1tya.ui.component.accent
import echo.music.iad1tya.ui.component.EndOfPage
import echo.music.iad1tya.ui.component.HomeItem
import echo.music.iad1tya.ui.component.homeLoadingItems
import echo.music.iad1tya.ui.component.OfflineErrorState
import echo.music.iad1tya.ui.component.NowPlayingBottomSheet
import echo.music.iad1tya.ui.icon.PlaylistAdd
import echo.music.iad1tya.ui.icon.Settings
import echo.music.iad1tya.ui.icon.echoIcons
import echo.music.iad1tya.ui.navigation.destination.home.HomeDestination
import echo.music.iad1tya.ui.navigation.destination.home.SettingsDestination
import echo.music.iad1tya.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import echo.music.iad1tya.ui.screen.library.LibraryDynamicPlaylistType
import echo.music.iad1tya.ui.navigation.destination.list.PlaylistDestination
import echo.music.iad1tya.ui.navigation.destination.login.LoginDestination
import echo.music.iad1tya.ui.theme.LocalNowPlayingColor
import echo.music.iad1tya.viewModel.HomeViewModel
import echo.music.iad1tya.viewModel.YouTubeAccountContent
import echo.music.iad1tya.viewModel.PlaybackMood
import echo.music.iad1tya.ui.component.HomeListeningHistory
import echo.music.iad1tya.ui.component.SongFullWidthItems
import androidx.compose.material3.OutlinedButton
import echo.music.iad1tya.ui.navigation.destination.search.SearchDestination
import echo.music.iad1tya.viewModel.LibraryViewModel
import echo.music.iad1tya.viewModel.ListState
import echo.music.iad1tya.viewModel.SharedViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.flowOf
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import echomusic.composeapp.generated.resources.Res
import echomusic.composeapp.generated.resources.for_you
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
    val loading by viewModel.loading.collectAsStateWithLifecycle()

    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()


    val nowPlayingTrack by sharedViewModel.nowPlayingState.collectAsStateWithLifecycle()
    val currentTrackId = nowPlayingTrack?.mediaItem?.mediaId?.takeIf(String::isNotBlank)
    val playbackMood = key(currentTrackId) {
        val mood by remember(currentTrackId) {
            currentTrackId?.let(viewModel::observePlaybackMood) ?: flowOf(null)
        }.collectAsStateWithLifecycle(initialValue = null)
        mood
    }
    var showMoodPicker by remember(currentTrackId) { mutableStateOf(false) }
    var showPlaylistDialog by rememberSaveable { mutableStateOf(false) }
    var playlistSeed by remember { mutableStateOf<SongEntity?>(null) }
    val localHistory by viewModel.recentlyPlayed.collectAsStateWithLifecycle()
    val youtubeHistory by viewModel.youtubeListeningHistory.collectAsStateWithLifecycle()
    val youtubeMixes by viewModel.youtubePersonalMixes.collectAsStateWithLifecycle()
    val explicitEnabled by viewModel.explicitContentEnabled.collectAsStateWithLifecycle(DataStoreManager.TRUE)

    fun allowedTracks(tracks: List<Track>): List<Track> =
        if (explicitEnabled == DataStoreManager.TRUE) tracks else tracks.filterNot { it.isExplicit }
    fun playTracks(tracks: List<Track>, name: String) {
        val playable = allowedTracks(tracks)
        val first = playable.firstOrNull() ?: return
        viewModel.rememberPlaybackMood(playable, params.takeUnless { loading })
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
            history = (youtubeHistory as? YouTubeAccountContent.Loaded)
                ?.let { it.copy(items = allowedTracks(it.items)) } ?: youtubeHistory,
            localTracks = allowedTracks(localHistory.map { it.toTrack() }),
            currentTrackId = nowPlayingTrack?.mediaItem?.mediaId,
            onPlay = { track ->
                if (track.videoId == nowPlayingTrack?.mediaItem?.mediaId) onOpenNowPlaying()
                else playTracks(listOf(track), track.title)
            },
            onPlayAll = { playTracks(it, "Listen again") },
            onConnect = { navController.navigate(LoginDestination) { launchSingleTop = true } },
            onRetry = viewModel::refreshYouTubeHome,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }

    val nowPlayingColor = LocalNowPlayingColor.current

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
        if (params == null) viewModel.refreshYouTubeHome() else viewModel.loadMoodFeed(params)
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
    val contentLoading = if (params == null) {
        youtubeHistory == YouTubeAccountContent.Loading || youtubeMixes == YouTubeAccountContent.Loading
    } else {
        loading
    }
    LaunchedEffect(contentLoading) {
        if (!contentLoading) {
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

    LaunchedEffect(shouldStartPaginate.value, continuation) {
        Logger.d("HomeScreen", "shouldStartPaginate: ${shouldStartPaginate.value}")
        Logger.d("HomeScreen", "homeListState: $homeListState")
        Logger.d("HomeScreen", "Continuation: $continuation")
        if (shouldStartPaginate.value && homeListState == ListState.IDLE) {

            viewModel.getContinueHomeItem(
                continuation,
            )
        }
    }

    if (showMoodPicker && currentTrackId != null) {
        ChoosePlaybackMoodDialog(
            activeMood = playbackMood,
            onDismiss = { showMoodPicker = false },
            onSelect = { mood ->
                viewModel.setPlaybackMood(currentTrackId, mood)
                showMoodPicker = false
            },
        )
    }
    if (showPlaylistDialog) {
        CreateHomePlaylistDialog(
            currentSong = playlistSeed,
            onDismiss = { showPlaylistDialog = false },
            onCreate = libraryViewModel::createPlaylist,
        )
    }

    val headerTint = playbackMood?.accent ?: nowPlayingColor ?: MaterialTheme.colorScheme.primary
    val headerFadeHeight = with(LocalDensity.current) { 220.dp.toPx() }
    Column(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                colors = listOf(headerTint.copy(alpha = 0.19f), Color.Transparent),
                endY = headerFadeHeight,
            ),
        ),
    ) {
        HomeTopAppBar(
            navController = navController,
            accountInfo = accountInfo,
            onCreatePlaylist = {
                playlistSeed = nowPlayingTrack?.songEntity ?: nowPlayingTrack?.track?.toSongEntity()
                showPlaylistDialog = true
            },
        )
        HomeMoodStrip(
            params = params,
            modifier = Modifier.horizontalScroll(chipRowState),
            onSelect = viewModel::setParams,
        )
        if (currentTrackId != null) {
            PlaybackMoodRow(
                mood = playbackMood,
                onChoose = { showMoodPicker = true },
                onBrowse = { viewModel.setParams(it.endpoint) },
            )
        }
        PullToRefreshBox(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = contentLoading,
        ) {
            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (params == null) {
                    item(key = "home-listening") { listeningShelf() }
                    if (youtubeMixes != YouTubeAccountContent.SignedOut) {
                        item(key = "home-personal-mixes") {
                            PersonalYouTubeMixes(
                                content = youtubeMixes,
                                onRetry = viewModel::refreshYouTubeHome,
                                onOpen = { mix -> navController.navigate(PlaylistDestination(mix.browseId)) },
                            )
                        }
                    }
                    return@LazyColumn
                }
                if (loading && homeData.isEmpty()) {
                    homeLoadingItems()
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
                item(key = "home-end") { EndOfPage() }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopAppBar(
    navController: NavController,
    accountInfo: Pair<String?, String?>?,
    onCreatePlaylist: () -> Unit,
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
            IconButton(onClick = onCreatePlaylist) {
                Icon(echoIcons.PlaylistAdd, contentDescription = "Create playlist")
            }
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
    val moods = listOf(Res.string.for_you to null) + PlaybackMood.entries.map { it.label to it.endpoint }
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
