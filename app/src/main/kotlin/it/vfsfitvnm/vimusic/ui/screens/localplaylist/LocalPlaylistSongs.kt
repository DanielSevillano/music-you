package it.vfsfitvnm.vimusic.ui.screens.localplaylist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.vfsfitvnm.vimusic.Database
import it.vfsfitvnm.vimusic.LocalPlayerPadding
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.ActionInfo
import it.vfsfitvnm.vimusic.models.LocalMenuState
import it.vfsfitvnm.vimusic.models.Song
import it.vfsfitvnm.vimusic.models.SongPlaylistMap
import it.vfsfitvnm.vimusic.query
import it.vfsfitvnm.vimusic.transaction
import it.vfsfitvnm.vimusic.ui.components.CoverScaffold
import it.vfsfitvnm.vimusic.ui.components.PlaylistThumbnail
import it.vfsfitvnm.vimusic.ui.components.SwipeToActionBox
import it.vfsfitvnm.vimusic.ui.components.themed.InPlaylistMediaItemMenu
import it.vfsfitvnm.vimusic.ui.items.LocalSongItem
import it.vfsfitvnm.vimusic.utils.asMediaItem
import it.vfsfitvnm.vimusic.utils.enqueue
import it.vfsfitvnm.vimusic.utils.forcePlayAtIndex
import it.vfsfitvnm.vimusic.utils.forcePlayFromBeginning
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@Composable
fun LocalPlaylistSongs(
    playlistId: Long,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    var playlistSongs: List<Song> by remember { mutableStateOf(emptyList()) }

    LaunchedEffect(Unit) {
        Database.playlistSongs(playlistId).filterNotNull().collect { playlistSongs = it }
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        playlistSongs = playlistSongs.toMutableList().apply {
            add(to.index - 2, removeAt(from.index - 2))
        }

        query {
            Database.move(
                playlistId = playlistId,
                fromPosition = from.index - 2,
                toPosition = to.index - 2
            )
        }
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = stringResource(id = R.string.song_deleted_playlist)
    val snackBarActionLabel = stringResource(id = R.string.undo)

    Scaffold(
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = playerPadding - 16.dp)
            )
        }
    ) { paddingValues ->
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + playerPadding),
            modifier = Modifier
                .fillMaxSize()
                .consumeWindowInsets(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "thumbnail") {
                CoverScaffold(
                    primaryButton = ActionInfo(
                        enabled = playlistSongs.isNotEmpty(),
                        onClick = {
                            if (playlistSongs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(
                                    playlistSongs.shuffled().map(Song::asMediaItem)
                                )
                            }
                        },
                        icon = Icons.Outlined.Shuffle,
                        description = R.string.shuffle
                    ),
                    secondaryButton = ActionInfo(
                        enabled = playlistSongs.isNotEmpty(),
                        onClick = {
                            playlistSongs.map(Song::asMediaItem).let { mediaItems ->
                                binder?.player?.enqueue(mediaItems)
                            }
                        },
                        icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                        description = R.string.enqueue
                    ),
                    content = {
                        PlaylistThumbnail(playlistId = playlistId)
                    }
                )
            }

            item(key = "spacer") {
                Spacer(modifier = Modifier.height(16.dp))
            }

            itemsIndexed(
                items = playlistSongs,
                key = { _, song -> song.id },
                contentType = { _, song -> song },
            ) { index, song ->

                ReorderableItem(
                    state = reorderableLazyListState,
                    key = song.id
                ) {
                    SwipeToActionBox(
                        primaryAction = ActionInfo(
                            onClick = { binder?.player?.enqueue(song.asMediaItem) },
                            icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                            description = R.string.enqueue
                        ),
                        destructiveAction = ActionInfo(
                            onClick = {
                                transaction {
                                    Database.move(playlistId, index, Int.MAX_VALUE)
                                    Database.delete(
                                        SongPlaylistMap(
                                            song.id,
                                            playlistId,
                                            Int.MAX_VALUE
                                        )
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
                                        val songCount = playlistSongs.size

                                        transaction {
                                            Database.insert(
                                                SongPlaylistMap(
                                                    songId = song.id,
                                                    playlistId = playlistId,
                                                    position = songCount
                                                )
                                            )

                                            Database.move(
                                                playlistId = playlistId,
                                                fromPosition = songCount,
                                                toPosition = index
                                            )
                                        }
                                    }
                                }
                            },
                            icon = Icons.Outlined.PlaylistRemove,
                            description = R.string.remove_from_playlist
                        )
                    ) {
                        LocalSongItem(
                            song = song,
                            onClick = {
                                playlistSongs
                                    .map(Song::asMediaItem)
                                    .let { mediaItems ->
                                        binder?.stopRadio()
                                        binder?.player?.forcePlayAtIndex(
                                            mediaItems,
                                            index
                                        )
                                    }
                            },
                            onLongClick = {
                                menuState.display {
                                    InPlaylistMediaItemMenu(
                                        playlistId = playlistId,
                                        positionInPlaylist = index,
                                        song = song,
                                        onDismiss = menuState::hide,
                                        onGoToAlbum = onGoToAlbum,
                                        onGoToArtist = onGoToArtist
                                    )
                                }
                            },
                            trailingContent = {
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier.draggableHandle()
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.DragHandle,
                                        contentDescription = null,
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}