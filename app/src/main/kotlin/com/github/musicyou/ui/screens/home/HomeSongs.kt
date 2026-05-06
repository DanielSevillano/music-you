package com.github.musicyou.ui.screens.home

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.LocalPlayerServiceBinder
import com.github.musicyou.R
import com.github.musicyou.database
import com.github.musicyou.enums.SongSortBy
import com.github.musicyou.enums.SortOrder
import com.github.musicyou.models.ActionInfo
import com.github.musicyou.models.LocalMenuState
import com.github.musicyou.models.Song
import com.github.musicyou.ui.components.HomeScaffold
import com.github.musicyou.ui.components.InHistoryMediaItemMenu
import com.github.musicyou.ui.components.SortingHeader
import com.github.musicyou.ui.components.SwipeToActionBox
import com.github.musicyou.ui.items.LocalSongItem
import com.github.musicyou.ui.styling.onOverlay
import com.github.musicyou.ui.styling.overlay
import com.github.musicyou.utils.asMediaItem
import com.github.musicyou.utils.enqueue
import com.github.musicyou.utils.forcePlayAtIndex
import com.github.musicyou.utils.forcePlayFromBeginning
import com.github.musicyou.utils.rememberPreference
import com.github.musicyou.utils.songSortByKey
import com.github.musicyou.utils.songSortOrderKey
import com.github.musicyou.viewmodels.HomeSongsViewModel
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun HomeSongs(
    openSearch: () -> Unit,
    openSettings: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    var sortBy by rememberPreference(songSortByKey, SongSortBy.Title)
    var sortOrder by rememberPreference(songSortOrderKey, SortOrder.Ascending)

    val viewModel: HomeSongsViewModel = viewModel()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = stringResource(id = R.string.song_deleted_library)
    val snackBarActionLabel = stringResource(id = R.string.undo)

    LaunchedEffect(sortBy, sortOrder) {
        viewModel.loadSongs(
            sortBy = sortBy,
            sortOrder = sortOrder
        )
    }

    HomeScaffold(
        title = R.string.songs,
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = viewModel.items.isNotEmpty(),
                enter = fadeIn() + scaleIn(),
                exit = scaleOut() + fadeOut()
            ) {
                FloatingActionButton(
                    onClick = {
                        binder?.stopRadio()
                        binder?.player?.forcePlayFromBeginning(
                            viewModel.items.shuffled().map(Song::asMediaItem)
                        )
                    },
                    modifier = Modifier.padding(bottom = playerPadding)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Shuffle,
                        contentDescription = stringResource(id = R.string.shuffle)
                    )
                }
            }
        },
        openSearch = openSearch,
        openSettings = openSettings
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 400.dp),
            contentPadding = PaddingValues(bottom = if (viewModel.items.isNotEmpty()) 16.dp + 72.dp + playerPadding else 16.dp + playerPadding),
            modifier = Modifier.fillMaxSize()
        ) {
            item(
                key = "header",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SortingHeader(
                    sortBy = sortBy,
                    changeSortBy = { sortBy = it },
                    sortByEntries = SongSortBy.entries.toList(),
                    sortOrder = sortOrder,
                    toggleSortOrder = { sortOrder = !sortOrder },
                    size = viewModel.items.size,
                    itemCountText = R.plurals.number_of_songs
                )
            }

            itemsIndexed(
                items = viewModel.items,
                key = { _, song -> song.id }
            ) { index, song ->
                SwipeToActionBox(
                    modifier = Modifier.animateItem(),
                    primaryAction = ActionInfo(
                        onClick = { binder?.player?.enqueue(song.asMediaItem) },
                        icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                        description = R.string.enqueue
                    ),
                    destructiveAction = ActionInfo(
                        onClick = {
                            database.query {
                                binder?.cache?.removeResource(song.id)
                                database.incrementTotalPlayTimeMs(
                                    id = song.id,
                                    addition = -song.totalPlayTimeMs
                                )
                            }

                            scope.launch {
                                snackbarHostState.currentSnackbarData?.dismiss()

                                val result = snackbarHostState.showSnackbar(
                                    message = snackbarMessage,
                                    actionLabel = snackBarActionLabel,
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Short
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    database.query {
                                        database.insert(song = song)
                                        database.incrementTotalPlayTimeMs(
                                            id = song.id,
                                            addition = song.totalPlayTimeMs
                                        )
                                    }
                                }
                            }
                        },
                        icon = Icons.Outlined.Delete,
                        description = R.string.hide
                    )
                ) {
                    LocalSongItem(
                        song = song,
                        onClick = {
                            binder?.stopRadio()
                            binder?.player?.forcePlayAtIndex(
                                viewModel.items.map(Song::asMediaItem),
                                index
                            )
                        },
                        onLongClick = {
                            menuState.display {
                                InHistoryMediaItemMenu(
                                    song = song,
                                    onDismiss = menuState::hide,
                                    onGoToAlbum = onGoToAlbum,
                                    onGoToArtist = onGoToArtist
                                )
                            }
                        },
                        onThumbnailContent = if (sortBy == SongSortBy.PlayTime) ({
                            Text(
                                text = song.formattedTotalPlayTime,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onOverlay,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.overlay
                                            )
                                        ),
                                        shape = MaterialTheme.shapes.medium
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .align(Alignment.BottomCenter)
                            )
                        }) else null
                    )
                }
            }
        }
    }
}