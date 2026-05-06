package com.github.musicyou.ui.screens.artist

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.LocalPlayerServiceBinder
import com.github.musicyou.R
import com.github.musicyou.database
import com.github.musicyou.models.ActionInfo
import com.github.musicyou.models.LocalMenuState
import com.github.musicyou.models.Song
import com.github.musicyou.ui.components.CoverScaffold
import com.github.musicyou.ui.components.NonQueuedMediaItemMenu
import com.github.musicyou.ui.components.ShimmerHost
import com.github.musicyou.ui.items.ListItemPlaceholder
import com.github.musicyou.ui.items.LocalSongItem
import com.github.musicyou.utils.asMediaItem
import com.github.musicyou.utils.enqueue
import com.github.musicyou.utils.forcePlayAtIndex
import com.github.musicyou.utils.forcePlayFromBeginning

@Composable
fun ArtistLocalSongs(
    browseId: String,
    thumbnailContent: @Composable () -> Unit,
    onGoToAlbum: (String) -> Unit
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    var songs: List<Song>? by remember { mutableStateOf(null) }

    LaunchedEffect(Unit) {
        database.artistSongs(browseId).collect { songs = it }
    }

    LazyColumn(
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp + playerPadding),
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item(key = "thumbnail") {
            CoverScaffold(
                primaryButton = ActionInfo(
                    enabled = !songs.isNullOrEmpty(),
                    onClick = {
                        songs?.let { songs ->
                            if (songs.isNotEmpty()) {
                                binder?.stopRadio()
                                binder?.player?.forcePlayFromBeginning(
                                    songs.shuffled().map(Song::asMediaItem)
                                )
                            }
                        }
                    },
                    icon = Icons.Outlined.Shuffle,
                    description = R.string.shuffle
                ),
                secondaryButton = ActionInfo(
                    enabled = !songs.isNullOrEmpty(),
                    onClick = {
                        binder?.player?.enqueue(songs!!.map(Song::asMediaItem))
                    },
                    icon = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    description = R.string.enqueue
                ),
                content = thumbnailContent
            )
        }

        item(key = "spacer") {
            Spacer(modifier = Modifier.height(16.dp))
        }

        songs?.let { songs ->
            itemsIndexed(
                items = songs,
                key = { _, song -> song.id }
            ) { index, song ->
                LocalSongItem(
                    song = song,
                    onClick = {
                        binder?.stopRadio()
                        binder?.player?.forcePlayAtIndex(
                            songs.map(Song::asMediaItem),
                            index
                        )
                    },
                    onLongClick = {
                        menuState.display {
                            NonQueuedMediaItemMenu(
                                onDismiss = menuState::hide,
                                mediaItem = song.asMediaItem,
                                onGoToAlbum = onGoToAlbum
                            )
                        }
                    }
                )
            }
        } ?: item(key = "loading") {
            ShimmerHost {
                repeat(4) {
                    ListItemPlaceholder()
                }
            }
        }
    }
}