package com.github.musicyou.ui.screens.artist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Podcasts
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.innertube.Innertube
import com.github.innertube.models.NavigationEndpoint
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.LocalPlayerServiceBinder
import com.github.musicyou.R
import com.github.musicyou.models.ActionInfo
import com.github.musicyou.models.LocalMenuState
import com.github.musicyou.ui.components.CoverScaffold
import com.github.musicyou.ui.components.NonQueuedMediaItemMenu
import com.github.musicyou.ui.components.ShimmerHost
import com.github.musicyou.ui.components.TextPlaceholder
import com.github.musicyou.ui.items.AlbumItem
import com.github.musicyou.ui.items.ArtistItem
import com.github.musicyou.ui.items.ItemPlaceholder
import com.github.musicyou.ui.items.ListItemPlaceholder
import com.github.musicyou.ui.items.PlaylistItem
import com.github.musicyou.ui.items.SongItem
import com.github.musicyou.ui.styling.Dimensions
import com.github.musicyou.utils.asMediaItem
import com.github.musicyou.utils.forcePlay

@Composable
fun ArtistOverview(
    youtubeArtistPage: Innertube.ArtistPage?,
    onViewAllSongsClick: () -> Unit,
    onViewAllAlbumsClick: () -> Unit,
    onViewAllSinglesClick: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (String) -> Unit,
    thumbnailContent: @Composable () -> Unit,
) {
    val binder = LocalPlayerServiceBinder.current
    val menuState = LocalMenuState.current
    val playerPadding = LocalPlayerPadding.current

    val itemSize = 140.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 16.dp, bottom = 16.dp + playerPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val radioEndpoint = youtubeArtistPage?.radioEndpoint
        val shuffleEndpoint = youtubeArtistPage?.shuffleEndpoint

        CoverScaffold(
            primaryButton = ActionInfo(
                enabled = radioEndpoint != null,
                onClick = {
                    binder?.stopRadio()
                    binder?.playRadio(radioEndpoint)
                },
                icon = Icons.Outlined.Podcasts,
                description = R.string.start_radio
            ),
            secondaryButton = ActionInfo(
                enabled = shuffleEndpoint != null,
                onClick = {
                    binder?.stopRadio()
                    binder?.playRadio(shuffleEndpoint)
                },
                icon = Icons.Outlined.Shuffle,
                description = R.string.shuffle
            ),
            content = thumbnailContent
        )

        if (youtubeArtistPage != null) {
            Spacer(modifier = Modifier.height(Dimensions.spacer))

            youtubeArtistPage.songs?.let { songs ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.songs),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    youtubeArtistPage.songsEndpoint?.let {
                        TextButton(onClick = onViewAllSongsClick) {
                            Text(text = stringResource(id = R.string.view_all))
                        }
                    }
                }

                songs.forEach { song ->
                    SongItem(
                        song = song,
                        onClick = {
                            val mediaItem = song.asMediaItem
                            binder?.stopRadio()
                            binder?.player?.forcePlay(mediaItem)
                            binder?.setupRadio(
                                NavigationEndpoint.Endpoint.Watch(videoId = mediaItem.mediaId)
                            )
                        },
                        onLongClick = {
                            menuState.display {
                                NonQueuedMediaItemMenu(
                                    onDismiss = menuState::hide,
                                    mediaItem = song.asMediaItem,
                                    onGoToAlbum = onAlbumClick
                                )
                            }
                        }
                    )
                }
            }

            youtubeArtistPage.albums?.let { albums ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.albums),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    youtubeArtistPage.albumsEndpoint?.let {
                        TextButton(onClick = onViewAllAlbumsClick) {
                            Text(text = stringResource(id = R.string.view_all))
                        }
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = albums,
                        key = Innertube.AlbumItem::key
                    ) { album ->
                        AlbumItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            album = album,
                            onClick = { onAlbumClick(album.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.singles?.let { singles ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.singles),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    youtubeArtistPage.singlesEndpoint?.let {
                        TextButton(onClick = onViewAllSinglesClick) {
                            Text(text = stringResource(id = R.string.view_all))
                        }
                    }
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = singles,
                        key = Innertube.AlbumItem::key
                    ) { album ->
                        AlbumItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            album = album,
                            onClick = { onAlbumClick(album.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.playlists?.let { playlists ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.playlists),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = playlists,
                        key = Innertube.PlaylistItem::key
                    ) { playlist ->
                        PlaylistItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.featuredPlaylists?.let { playlists ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.featured_on),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = playlists,
                        key = Innertube.PlaylistItem::key
                    ) { playlist ->
                        PlaylistItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            playlist = playlist,
                            onClick = { onPlaylistClick(playlist.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.relatedArtists?.let { artists ->
                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.fans_might_also_like),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    items(
                        items = artists,
                        key = Innertube.ArtistItem::key
                    ) { artist ->
                        ArtistItem(
                            modifier = Modifier.widthIn(max = itemSize),
                            artist = artist,
                            onClick = { onArtistClick(artist.key) }
                        )
                    }
                }
            }

            youtubeArtistPage.description?.let { description ->
                val attributionsIndex = description.lastIndexOf("\n\nFrom Wikipedia")

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                Text(
                    text = stringResource(id = R.string.about),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Text(
                    text = if (attributionsIndex == -1) description
                    else description.substring(0, attributionsIndex),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(Dimensions.mediumOpacity)
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = if (attributionsIndex == -1) 4.dp else 0.dp
                        )
                )

                if (attributionsIndex != -1) {
                    Text(
                        text = "From Wikipedia under Creative Commons Attribution CC-BY-SA 3.0",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier
                            .alpha(Dimensions.mediumOpacity)
                            .padding(
                                start = 16.dp,
                                end = 16.dp,
                                top = 12.dp,
                                bottom = 4.dp
                            )
                    )
                }
            }
        } else {
            ShimmerHost(
                modifier = Modifier.padding(top = 16.dp)
            ) {
                val placeholderModifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)

                TextPlaceholder(modifier = placeholderModifier)

                repeat(5) {
                    ListItemPlaceholder()
                }

                repeat(4) {
                    Spacer(modifier = Modifier.height(Dimensions.spacer))

                    TextPlaceholder(modifier = placeholderModifier)

                    Row(modifier = Modifier.padding(start = 8.dp)) {
                        repeat(2) {
                            ItemPlaceholder(modifier = Modifier.widthIn(max = itemSize))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimensions.spacer))

                TextPlaceholder(modifier = placeholderModifier)

                Row(modifier = Modifier.padding(start = 8.dp)) {
                    repeat(2) {
                        ItemPlaceholder(
                            modifier = Modifier.widthIn(max = itemSize),
                            shape = CircleShape
                        )
                    }
                }
            }
        }
    }
}
