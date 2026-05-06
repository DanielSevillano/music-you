package com.github.musicyou.ui.screens.album

import android.content.Intent
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.outlined.Album
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BookmarkAdd
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.innertube.Innertube
import com.github.innertube.requests.albumPage
import com.github.musicyou.R
import com.github.musicyou.database
import com.github.musicyou.models.Album
import com.github.musicyou.models.Section
import com.github.musicyou.models.SongAlbumMap
import com.github.musicyou.ui.components.TabScaffold
import com.github.musicyou.ui.components.TooltipIconButton
import com.github.musicyou.ui.components.adaptiveThumbnailContent
import com.github.musicyou.ui.items.AlbumItem
import com.github.musicyou.ui.items.ItemPlaceholder
import com.github.musicyou.ui.screens.search.ItemsPage
import com.github.musicyou.utils.asMediaItem
import com.github.musicyou.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

@Composable
fun AlbumScreen(
    browseId: String,
    pop: () -> Unit,
    onAlbumClick: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    var album: Album? by remember { mutableStateOf(null) }
    var albumPage: Innertube.PlaylistOrAlbumPage? by remember { mutableStateOf(null) }

    val tabs = listOf(
        Section(stringResource(id = R.string.songs), Icons.Outlined.MusicNote),
        Section(stringResource(id = R.string.other_versions), Icons.Outlined.Album),
        Section(stringResource(id = R.string.related_albums), Icons.Outlined.AutoAwesome)
    )
    val pagerState = rememberPagerState(pageCount = { tabs.size })

    LaunchedEffect(Unit) {
        database
            .album(browseId)
            .combine(snapshotFlow { pagerState.currentPage }) { album, tabIndex -> album to tabIndex }
            .collect { (currentAlbum, tabIndex) ->
                album = currentAlbum

                if (albumPage == null && (currentAlbum?.timestamp == null || tabIndex >= 1)) {
                    withContext(Dispatchers.IO) {
                        Innertube.albumPage(browseId = browseId)
                            ?.completed()
                            ?.onSuccess { currentAlbumPage ->
                                albumPage = currentAlbumPage

                                database.clearAlbum(browseId)

                                database.upsert(
                                    Album(
                                        id = browseId,
                                        title = currentAlbumPage.title,
                                        thumbnailUrl = currentAlbumPage.thumbnail?.url,
                                        year = currentAlbumPage.year,
                                        authorsText = currentAlbumPage.authors
                                            ?.joinToString("") { it.name ?: "" },
                                        shareUrl = currentAlbumPage.url,
                                        timestamp = System.currentTimeMillis(),
                                        bookmarkedAt = album?.bookmarkedAt
                                    ),
                                    currentAlbumPage
                                        .songsPage
                                        ?.items
                                        ?.map(Innertube.SongItem::asMediaItem)
                                        ?.onEach(database::insert)
                                        ?.mapIndexed { position, mediaItem ->
                                            SongAlbumMap(
                                                songId = mediaItem.mediaId,
                                                albumId = browseId,
                                                position = position
                                            )
                                        } ?: emptyList()
                                )
                            }
                    }
                }
            }
    }

    val thumbnailContent =
        adaptiveThumbnailContent(album?.timestamp == null, album?.thumbnailUrl)

    TabScaffold(
        pagerState = pagerState,
        topIconButtonId = Icons.AutoMirrored.Outlined.ArrowBack,
        onTopIconButtonClick = pop,
        sectionTitle = album?.title ?: "",
        appBarActions = {
            val context = LocalContext.current

            TooltipIconButton(
                description = if (album?.bookmarkedAt == null) R.string.add_bookmark else R.string.remove_bookmark,
                onClick = {
                    val bookmarkedAt =
                        if (album?.bookmarkedAt == null) System.currentTimeMillis() else null

                    database.query {
                        album
                            ?.copy(bookmarkedAt = bookmarkedAt)
                            ?.let(database::update)
                    }
                },
                icon = if (album?.bookmarkedAt == null) Icons.Outlined.BookmarkAdd else Icons.Filled.Bookmark,
                inTopBar = true
            )

            TooltipIconButton(
                description = R.string.share,
                onClick = {
                    album?.shareUrl?.let { url ->
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, url)
                        }

                        context.startActivity(
                            Intent.createChooser(
                                sendIntent,
                                null
                            )
                        )
                    }
                },
                icon = Icons.Outlined.Share,
                inTopBar = true
            )
        },
        tabColumnContent = tabs
    ) { index ->
        when (index) {
            0 -> AlbumSongs(
                browseId = browseId,
                thumbnailContent = thumbnailContent,
                onGoToArtist = onGoToArtist
            )

            1 -> {
                ItemsPage(
                    tag = "album/$browseId/alternatives",
                    initialPlaceholderCount = 1,
                    continuationPlaceholderCount = 1,
                    emptyItemsText = stringResource(id = R.string.no_alternative_versions),
                    itemsPageProvider = albumPage?.let { page ->
                        {
                            Result.success(
                                Innertube.ItemsPage(
                                    items = page.otherVersions,
                                    continuation = null
                                )
                            )
                        }
                    },
                    itemContent = { album ->
                        AlbumItem(
                            album = album,
                            onClick = { onAlbumClick(album.key) }
                        )
                    },
                    itemPlaceholderContent = {
                        ItemPlaceholder()
                    }
                )
            }

            2 -> {
                ItemsPage(
                    tag = "album/$browseId/related",
                    initialPlaceholderCount = 1,
                    continuationPlaceholderCount = 1,
                    emptyItemsText = stringResource(id = R.string.no_related_albums),
                    itemsPageProvider = albumPage?.let { page ->
                        {
                            Result.success(
                                Innertube.ItemsPage(
                                    items = page.relatedAlbums,
                                    continuation = null
                                )
                            )
                        }
                    },
                    itemContent = { album ->
                        AlbumItem(
                            album = album,
                            onClick = { onAlbumClick(album.key) }
                        )
                    },
                    itemPlaceholderContent = {
                        ItemPlaceholder()
                    }
                )
            }
        }
    }
}