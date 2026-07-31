

package iad1tya.echo.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.music.innertube.models.AlbumItem
import com.music.innertube.models.ArtistItem
import com.music.innertube.models.PlaylistItem
import com.music.innertube.models.SongItem
import iad1tya.echo.music.LocalDatabase
import iad1tya.echo.music.LocalPlayerAwareWindowInsets
import iad1tya.echo.music.LocalPlayerConnection
import iad1tya.echo.music.R
import iad1tya.echo.music.constants.SuggestionItemHeight
import iad1tya.echo.music.models.toMediaMetadata
import iad1tya.echo.music.playback.queues.YouTubeQueue
import iad1tya.echo.music.ui.component.LocalMenuState
import iad1tya.echo.music.ui.component.YouTubeListItem
import iad1tya.echo.music.utils.listItemShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.draw.clip
import iad1tya.echo.music.ui.menu.YouTubeAlbumMenu
import iad1tya.echo.music.ui.menu.YouTubeArtistMenu
import iad1tya.echo.music.ui.menu.YouTubePlaylistMenu
import iad1tya.echo.music.ui.menu.YouTubeSongMenu
import iad1tya.echo.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val scope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsState()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val viewState by viewModel.viewState.collectAsState()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        viewModel.updateQuery(query)
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Bottom).asPaddingValues(),
        modifier = Modifier
            .fillMaxSize()
            .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.background)
    ) {
        if (query.isBlank()) {
            item(key = "search_landing") {
                SearchLanding(
                    modifier = Modifier.animateItem(),
                    pureBlack = pureBlack,
                )
            }
        } else {
            item(key = "direct_search_$query") {
                DirectSearchItem(
                    query = query,
                    onClick = {
                        onSearch(query)
                        onDismiss()
                    },
                    modifier = Modifier.animateItem(),
                )
            }

            if (viewState.isLoading) {
                item(key = "suggestions_loading") {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .height(2.dp)
                            .animateItem(),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    )
                }
            }
        }

        if (viewState.history.isNotEmpty()) {
            item(key = "history_header") {
                SearchSectionLabel(
                    text = stringResource(
                        if (query.isBlank()) R.string.recent_searches else R.string.searches_from_history
                    ),
                    modifier = Modifier.animateItem(),
                )
            }
        }

        items(viewState.history, key = { "history_${it.query}" }) { history ->
            SuggestionItem(
                query = history.query,
                online = false,
                onClick = {
                    onSearch(history.query)
                    onDismiss()
                },
                onDelete = {
                    database.query {
                        delete(history)
                    }
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                },
                modifier = Modifier.animateItem(),
            )
        }

        if (viewState.suggestions.isNotEmpty()) {
            item(key = "suggestions_header") {
                SearchSectionLabel(
                    text = stringResource(R.string.suggested_searches),
                    modifier = Modifier.animateItem(),
                )
            }
        }

        items(viewState.suggestions, key = { "suggestion_$it" }) { suggestion ->
            SuggestionItem(
                query = suggestion,
                online = true,
                onClick = {
                    onSearch(suggestion)
                    onDismiss()
                },
                onFillTextField = {
                    onQueryChange(TextFieldValue(suggestion, TextRange(suggestion.length)))
                },
                modifier = Modifier.animateItem(),
            )
        }

        if (query.isNotBlank() &&
            !viewState.isLoading &&
            viewState.suggestionsUnavailable &&
            viewState.suggestions.isEmpty()
        ) {
            item(key = "suggestions_unavailable") {
                Text(
                    text = stringResource(R.string.live_suggestions_unavailable, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 12.dp)
                        .animateItem(),
                )
            }
        }

        if (viewState.items.isNotEmpty()) {
            item(key = "search_divider") {
                SearchSectionLabel(
                    text = stringResource(if (viewState.isFromLink) R.string.parsed_from_link else R.string.top_result),
                    modifier = Modifier.animateItem(),
                )
            }
        }

        itemsIndexed(viewState.items, key = { _, it -> "item_${it.id}" }) { index, item ->
            YouTubeListItem(
                item = item,
                isActive = when (item) {
                    is SongItem -> mediaMetadata?.id == item.id
                    is AlbumItem -> mediaMetadata?.album?.id == item.id
                    else -> false
                },
                isPlaying = isPlaying,
                shape = listItemShape(index, viewState.items.size),
                trailingContent = {
                    IconButton(
                        onClick = {
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = scope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null
                        )
                    }
                },
                modifier = Modifier
                    .combinedClickable(
                        onClick = {
                            when (item) {
                                is SongItem -> {
                                    if (item.id == mediaMetadata?.id) {
                                        playerConnection.togglePlayPause()
                                    } else {
                                        playerConnection.playQueue(
                                            YouTubeQueue.radio(item.toMediaMetadata())
                                        )
                                        onDismiss()
                                    }
                                }
                                is AlbumItem -> {
                                    navController.navigate("album/${item.id}")
                                    onDismiss()
                                }
                                is ArtistItem -> {
                                    navController.navigate("artist/${item.id}")
                                    onDismiss()
                                }
                                is PlaylistItem -> {
                                    navController.navigate("online_playlist/${item.id}")
                                    onDismiss()
                                }
                            }
                        },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem -> YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is AlbumItem -> YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is ArtistItem -> YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                    is PlaylistItem -> YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        }
                                    )
                                }
                            }
                        }
                    )
                    .background(if (pureBlack) Color.Black else MaterialTheme.colorScheme.surface)
                    .animateItem()
            )
        }
    }
}

@Composable
private fun SearchLanding(
    modifier: Modifier = Modifier,
    pureBlack: Boolean,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(24.dp))
                .background(
                    if (pureBlack) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)
                    } else {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                    }
                )
                .padding(14.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.sparks),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = stringResource(R.string.find_your_sound),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.find_your_sound_description),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Composable
private fun SearchSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 24.dp, top = 18.dp, end = 24.dp, bottom = 8.dp),
    )
}

@Composable
private fun DirectSearchItem(
    query: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .padding(9.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.search),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp),
        ) {
            Text(
                text = stringResource(R.string.search_meloqis_for),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = query,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            painter = painterResource(R.drawable.arrow_forward),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun SuggestionItem(
    modifier: Modifier = Modifier,
    query: String,
    online: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(SuggestionItemHeight)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)),
    ) {
        Icon(
            painterResource(if (online) R.drawable.search else R.drawable.history),
            contentDescription = null,
            tint = if (online) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp).alpha(if (online) 0.9f else 0.65f)
        )

        Text(
            text = query,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        if (!online) {
            IconButton(
                onClick = onDelete,
                modifier = Modifier.alpha(0.55f),
            ) {
                Icon(
                    painter = painterResource(R.drawable.close),
                    contentDescription = null,
                )
            }
        }

        IconButton(
            onClick = onFillTextField,
            modifier = Modifier.alpha(0.65f),
        ) {
            Icon(
                painter = painterResource(R.drawable.arrow_top_left),
                contentDescription = null,
            )
        }
    }
}
