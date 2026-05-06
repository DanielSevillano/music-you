package com.github.musicyou.ui.screens.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.R
import com.github.musicyou.database
import com.github.musicyou.enums.BuiltInPlaylist
import com.github.musicyou.enums.PlaylistSortBy
import com.github.musicyou.enums.SortOrder
import com.github.musicyou.models.Playlist
import com.github.musicyou.ui.components.HomeScaffold
import com.github.musicyou.ui.components.SortingHeader
import com.github.musicyou.ui.components.TextFieldDialog
import com.github.musicyou.ui.items.BuiltInPlaylistItem
import com.github.musicyou.ui.items.LocalPlaylistItem
import com.github.musicyou.utils.playlistSortByKey
import com.github.musicyou.utils.playlistSortOrderKey
import com.github.musicyou.utils.rememberPreference
import com.github.musicyou.viewmodels.HomePlaylistsViewModel

@Composable
fun HomePlaylists(
    openSearch: () -> Unit,
    openSettings: () -> Unit,
    onBuiltInPlaylist: (Int) -> Unit,
    onPlaylistClick: (Playlist) -> Unit
) {
    val playerPadding = LocalPlayerPadding.current

    var isCreatingANewPlaylist by rememberSaveable { mutableStateOf(false) }
    var sortBy by rememberPreference(playlistSortByKey, PlaylistSortBy.Name)
    var sortOrder by rememberPreference(playlistSortOrderKey, SortOrder.Ascending)

    val viewModel: HomePlaylistsViewModel = viewModel()

    LaunchedEffect(sortBy, sortOrder) {
        viewModel.loadArtists(
            sortBy = sortBy,
            sortOrder = sortOrder
        )
    }

    if (isCreatingANewPlaylist) {
        TextFieldDialog(
            title = stringResource(id = R.string.new_playlist),
            hintText = stringResource(id = R.string.playlist_name_hint),
            onDismiss = {
                isCreatingANewPlaylist = false
            },
            onDone = { text ->
                database.query {
                    database.insert(Playlist(name = text))
                }
            }
        )
    }

    HomeScaffold(
        title = R.string.playlists,
        openSearch = openSearch,
        openSettings = openSettings
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            contentPadding = PaddingValues(
                start = 8.dp,
                end = 8.dp,
                bottom = 16.dp + playerPadding
            ),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(
                key = "header",
                span = { GridItemSpan(maxLineSpan) }
            ) {
                SortingHeader(
                    sortBy = sortBy,
                    changeSortBy = { sortBy = it },
                    sortByEntries = PlaylistSortBy.entries.toList(),
                    sortOrder = sortOrder,
                    toggleSortOrder = { sortOrder = !sortOrder },
                    size = viewModel.items.size,
                    itemCountText = R.plurals.number_of_playlists
                )
            }

            item(key = "favorites") {
                BuiltInPlaylistItem(
                    icon = Icons.Default.Favorite,
                    name = stringResource(id = R.string.favorites),
                    onClick = { onBuiltInPlaylist(BuiltInPlaylist.Favorites.ordinal) }
                )
            }

            item(key = "offline") {
                BuiltInPlaylistItem(
                    icon = Icons.Default.DownloadForOffline,
                    name = stringResource(id = R.string.offline),
                    onClick = { onBuiltInPlaylist(BuiltInPlaylist.Offline.ordinal) }
                )
            }

            item(key = "new") {
                BuiltInPlaylistItem(
                    icon = Icons.Default.Add,
                    name = stringResource(id = R.string.new_playlist),
                    onClick = { isCreatingANewPlaylist = true }
                )
            }

            items(
                items = viewModel.items,
                key = { it.playlist.id }
            ) { playlistPreview ->
                LocalPlaylistItem(
                    modifier = Modifier.animateItem(),
                    playlist = playlistPreview,
                    onClick = { onPlaylistClick(playlistPreview.playlist) }
                )
            }
        }
    }
}