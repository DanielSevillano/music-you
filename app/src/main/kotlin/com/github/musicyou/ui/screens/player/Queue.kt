package com.github.musicyou.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.DragHandle
import androidx.compose.material.icons.outlined.PlaylistRemove
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import com.github.musicyou.LocalPlayerServiceBinder
import com.github.musicyou.R
import com.github.musicyou.models.ActionInfo
import com.github.musicyou.models.LocalMenuState
import com.github.musicyou.ui.components.MusicBars
import com.github.musicyou.ui.components.QueuedMediaItemMenu
import com.github.musicyou.ui.components.SwipeToActionBox
import com.github.musicyou.ui.components.TooltipIconButton
import com.github.musicyou.ui.items.ListItemPlaceholder
import com.github.musicyou.ui.items.MediaSongItem
import com.github.musicyou.ui.styling.Dimensions
import com.github.musicyou.ui.styling.onOverlay
import com.github.musicyou.utils.DisposableListener
import com.github.musicyou.utils.queueLoopEnabledKey
import com.github.musicyou.utils.rememberPreference
import com.github.musicyou.utils.shouldBePlaying
import com.github.musicyou.utils.shuffleQueue
import com.github.musicyou.utils.windows
import com.valentinilk.shimmer.shimmer
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Queue(
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val player = binder?.player ?: return
    val menuState = LocalMenuState.current

    var queueLoopEnabled by rememberPreference(queueLoopEnabledKey, defaultValue = false)
    var mediaItemIndex by remember {
        mutableIntStateOf(if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex)
    }
    var windows by remember { mutableStateOf(player.currentTimeline.windows) }
    var shouldBePlaying by remember { mutableStateOf(binder.player.shouldBePlaying) }

    player.DisposableListener {
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                mediaItemIndex =
                    if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                windows = timeline.windows
                mediaItemIndex =
                    if (player.mediaItemCount == 0) -1 else player.currentMediaItemIndex
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                shouldBePlaying = binder.player.shouldBePlaying
            }
        }
    }

    val lazyListState = rememberLazyListState(initialFirstVisibleItemIndex = mediaItemIndex)
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        player.moveMediaItem(from.index, to.index)
    }

    val musicBarsTransition = updateTransition(targetState = mediaItemIndex, label = "bars")

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMessage = stringResource(id = R.string.song_deleted_queue)
    val snackbarActionLabel = stringResource(id = R.string.undo)

    Scaffold(
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .background(BottomSheetDefaults.ContainerColor)
                .consumeWindowInsets(paddingValues)
        ) {
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1F)
            ) {
                items(
                    items = windows,
                    key = { it.uid.hashCode() }
                ) { window ->
                    val isPlayingThisMediaItem = mediaItemIndex == window.firstPeriodIndex
                    val currentWindow by rememberUpdatedState(window)

                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = window.uid.hashCode()
                    ) {
                        SwipeToActionBox(
                            destructiveAction = ActionInfo(
                                enabled = !isPlayingThisMediaItem,
                                onClick = {
                                    val deletedMediaItem = window.mediaItem
                                    val itemIndex = currentWindow.firstPeriodIndex

                                    player.removeMediaItem(itemIndex)

                                    scope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()

                                        val result = snackbarHostState.showSnackbar(
                                            message = snackbarMessage,
                                            actionLabel = snackbarActionLabel,
                                            withDismissAction = true,
                                            duration = SnackbarDuration.Short
                                        )

                                        if (result == SnackbarResult.ActionPerformed) player.addMediaItem(
                                            itemIndex,
                                            deletedMediaItem
                                        )
                                    }
                                },
                                icon = Icons.Outlined.PlaylistRemove,
                                description = R.string.remove_from_queue
                            )
                        ) {
                            MediaSongItem(
                                song = window.mediaItem,
                                onClick = {
                                    if (isPlayingThisMediaItem) {
                                        if (shouldBePlaying) player.pause()
                                        else player.play()
                                    } else {
                                        player.seekToDefaultPosition(window.firstPeriodIndex)
                                        player.playWhenReady = true
                                    }
                                },
                                onLongClick = {
                                    menuState.display {
                                        QueuedMediaItemMenu(
                                            mediaItem = window.mediaItem,
                                            indexInQueue = if (isPlayingThisMediaItem) null else window.firstPeriodIndex,
                                            onDismiss = menuState::hide,
                                            onGoToAlbum = onGoToAlbum,
                                            onGoToArtist = onGoToArtist
                                        )
                                    }
                                },
                                onThumbnailContent = {
                                    musicBarsTransition.AnimatedVisibility(
                                        visible = { it == window.firstPeriodIndex },
                                        enter = fadeIn(tween(800)),
                                        exit = fadeOut(tween(800)),
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    color = Color.Black.copy(alpha = 0.25F),
                                                    shape = MaterialTheme.shapes.medium
                                                )
                                        ) {
                                            if (shouldBePlaying) {
                                                MusicBars(
                                                    color = MaterialTheme.colorScheme.onOverlay,
                                                    modifier = Modifier
                                                        .height(24.dp)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.PlayArrow,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(24.dp),
                                                    tint = MaterialTheme.colorScheme.onOverlay
                                                )
                                            }
                                        }
                                    }
                                },
                                trailingContent = {
                                    IconButton(
                                        onClick = {},
                                        modifier = Modifier.draggableHandle()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.DragHandle,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                item {
                    if (binder.isLoadingRadio) {
                        Column(
                            modifier = Modifier.shimmer()
                        ) {
                            repeat(3) { index ->
                                ListItemPlaceholder(
                                    modifier = Modifier.alpha(1f - index * 0.125f)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceColorAtElevation(5.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = pluralStringResource(
                        id = R.plurals.number_of_songs,
                        count = windows.size,
                        windows.size
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )

                Row {
                    TooltipIconButton(
                        description = R.string.shuffle,
                        onClick = {
                            scope
                                .launch { lazyListState.animateScrollToItem(0) }
                                .invokeOnCompletion { player.shuffleQueue() }
                        },
                        icon = Icons.Outlined.Shuffle
                    )

                    TooltipIconButton(
                        description = R.string.queue_loop,
                        onClick = { queueLoopEnabled = !queueLoopEnabled },
                        icon = Icons.Outlined.Repeat,
                        modifier = Modifier.alpha(if (queueLoopEnabled) 1F else Dimensions.lowOpacity)
                    )

                    TooltipIconButton(
                        description = R.string.clear_queue,
                        onClick = {
                            val mediaItems = windows.size
                            when (mediaItemIndex) {
                                0 -> player.removeMediaItems(1, mediaItems)
                                mediaItems - 1 -> player.removeMediaItems(0, mediaItems - 1)
                                else -> {
                                    player.removeMediaItems(0, mediaItemIndex)
                                    player.removeMediaItems(mediaItemIndex + 1, mediaItems)
                                }
                            }
                        },
                        icon = Icons.Outlined.ClearAll,
                        enabled = windows.size >= 2
                    )
                }
            }
        }
    }
}