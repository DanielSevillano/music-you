package com.github.musicyou.ui.screens.playlist

import android.content.Intent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LibraryAdd
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.github.innertube.Innertube
import com.github.innertube.requests.playlistPage
import com.github.musicyou.R
import com.github.musicyou.database
import com.github.musicyou.models.Playlist
import com.github.musicyou.models.SongPlaylistMap
import com.github.musicyou.ui.components.TextFieldDialog
import com.github.musicyou.ui.components.TooltipIconButton
import com.github.musicyou.utils.asMediaItem
import com.github.musicyou.utils.completed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(
    browseId: String,
    pop: () -> Unit,
    onGoToAlbum: (String) -> Unit,
    onGoToArtist: (String) -> Unit
) {
    var playlistPage: Innertube.PlaylistOrAlbumPage? by remember { mutableStateOf(null) }
    var isImportingPlaylist by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    LaunchedEffect(Unit) {
        if (playlistPage != null && playlistPage?.songsPage?.continuation == null) return@LaunchedEffect

        playlistPage = withContext(Dispatchers.IO) {
            Innertube.playlistPage(browseId = browseId)?.completed()?.getOrNull()
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            MediumTopAppBar(
                title = {
                    Text(
                        text = playlistPage?.title ?: "",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = pop) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                actions = {
                    val context = LocalContext.current

                    TooltipIconButton(
                        description = R.string.import_playlist,
                        onClick = { isImportingPlaylist = true },
                        icon = Icons.Outlined.LibraryAdd,
                        inTopBar = true
                    )

                    TooltipIconButton(
                        description = R.string.share,
                        onClick = {
                            (playlistPage?.url
                                ?: "https://music.youtube.com/playlist?list=${
                                    browseId.removePrefix(
                                        "VL"
                                    )
                                }").let { url ->
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
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            PlaylistSongList(
                playlistPage = playlistPage,
                onGoToAlbum = onGoToAlbum,
                onGoToArtist = onGoToArtist
            )

            if (isImportingPlaylist) {
                TextFieldDialog(
                    title = stringResource(id = R.string.import_playlist),
                    hintText = stringResource(id = R.string.playlist_name_hint),
                    initialTextInput = playlistPage?.title ?: "",
                    onDismiss = { isImportingPlaylist = false },
                    onDone = { text ->
                        database.query {
                            database.transaction {
                                val playlistId = database.insert(
                                    Playlist(
                                        name = text,
                                        browseId = browseId
                                    )
                                )

                                playlistPage?.songsPage?.items
                                    ?.map(Innertube.SongItem::asMediaItem)
                                    ?.onEach(database::insert)
                                    ?.mapIndexed { index, mediaItem ->
                                        SongPlaylistMap(
                                            songId = mediaItem.mediaId,
                                            playlistId = playlistId,
                                            position = index
                                        )
                                    }?.let(database::insertSongPlaylistMaps)
                            }
                        }
                    }
                )
            }
        }
    }
}