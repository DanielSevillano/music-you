package com.github.musicyou.service

import android.content.ComponentName
import android.content.ContentResolver
import android.content.ServiceConnection
import android.media.session.MediaSession
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Process
import android.service.media.MediaBrowserService
import androidx.annotation.DrawableRes
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.util.UnstableApi
import com.github.musicyou.R
import com.github.musicyou.database
import com.github.musicyou.models.Album
import com.github.musicyou.models.PlaylistPreview
import com.github.musicyou.models.Song
import com.github.musicyou.models.SongWithContentLength
import com.github.musicyou.utils.asMediaItem
import com.github.musicyou.utils.forcePlayAtIndex
import com.github.musicyou.utils.forceSeekToNext
import com.github.musicyou.utils.forceSeekToPrevious
import com.github.musicyou.utils.intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import android.media.MediaDescription as BrowserMediaDescription
import android.media.browse.MediaBrowser.MediaItem as BrowserMediaItem

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerMediaBrowserService : MediaBrowserService(), ServiceConnection {
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private var lastSongs = emptyList<Song>()

    private var bound = false

    override fun onDestroy() {
        if (bound) unbindService(this)
        super.onDestroy()
    }

    override fun onServiceConnected(className: ComponentName, service: IBinder) {
        if (service is PlayerService.Binder) {
            bound = true
            sessionToken = service.mediaSession.sessionToken
            service.mediaSession.setCallback(SessionCallback(binder = service))
        }
    }

    override fun onServiceDisconnected(name: ComponentName) = Unit

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return if (clientUid == Process.myUid()
            || clientUid == Process.SYSTEM_UID
            || clientPackageName == "com.google.android.projection.gearhead"
        ) {
            bindService(intent<PlayerService>(), this, BIND_AUTO_CREATE)
            BrowserRoot(
                MediaId.ROOT,
                Bundle().apply {
                    putInt("android.media.browse.CONTENT_STYLE_BROWSABLE_HINT", 1)
                }
            )
        } else null
    }

    override fun onLoadChildren(parentId: String, result: Result<MutableList<BrowserMediaItem>>) {
        runBlocking(Dispatchers.IO) {
            result.sendResult(
                when (parentId) {
                    MediaId.ROOT -> mutableListOf(
                        songsBrowserMediaItem,
                        playlistsBrowserMediaItem,
                        albumsBrowserMediaItem
                    )

                    MediaId.SONGS -> database
                        .songsByPlayTimeDesc()
                        .first()
                        .take(30)
                        .also { lastSongs = it }
                        .map { it.asBrowserMediaItem }
                        .toMutableList()
                        .apply {
                            if (isNotEmpty()) add(0, shuffleBrowserMediaItem)
                        }

                    MediaId.PLAYLISTS -> database
                        .playlistPreviewsByDateAddedDesc()
                        .first()
                        .map { it.asBrowserMediaItem }
                        .toMutableList()
                        .apply {
                            add(0, favoritesBrowserMediaItem)
                            add(1, offlineBrowserMediaItem)
                        }

                    MediaId.ALBUMS -> database
                        .albumsByRowIdDesc()
                        .first()
                        .map { it.asBrowserMediaItem }
                        .toMutableList()

                    else -> mutableListOf()
                }
            )
        }
    }

    private fun uriFor(@DrawableRes id: Int) = Uri.Builder()
        .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
        .authority(resources.getResourcePackageName(id))
        .appendPath(resources.getResourceTypeName(id))
        .appendPath(resources.getResourceEntryName(id))
        .build()

    private val shuffleBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.SHUFFLE)
                .setTitle(getString(R.string.shuffle))
                .setIconUri(uriFor(R.drawable.shuffle))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val songsBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.SONGS)
                .setTitle(getString(R.string.songs))
                .setIconUri(uriFor(R.drawable.musical_notes))
                .build(),
            BrowserMediaItem.FLAG_BROWSABLE
        )


    private val playlistsBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.PLAYLISTS)
                .setTitle(getString(R.string.playlists))
                .setIconUri(uriFor(R.drawable.playlist))
                .build(),
            BrowserMediaItem.FLAG_BROWSABLE
        )

    private val albumsBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.ALBUMS)
                .setTitle(getString(R.string.albums))
                .setIconUri(uriFor(R.drawable.disc))
                .build(),
            BrowserMediaItem.FLAG_BROWSABLE
        )

    private val favoritesBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.FAVORITES)
                .setTitle(getString(R.string.favorites))
                .setIconUri(uriFor(R.drawable.heart))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val offlineBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.OFFLINE)
                .setTitle(getString(R.string.offline))
                .setIconUri(uriFor(R.drawable.offline))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val Song.asBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.forSong(id))
                .setTitle(title)
                .setSubtitle(artistsText)
                .setIconUri(thumbnailUrl?.toUri())
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val PlaylistPreview.asBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.forPlaylist(playlist.id))
                .setTitle(playlist.name)
                .setSubtitle("$songCount ${getString(R.string.songs).lowercase()}")
                .setIconUri(uriFor(R.drawable.playlist))
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private val Album.asBrowserMediaItem
        inline get() = BrowserMediaItem(
            BrowserMediaDescription.Builder()
                .setMediaId(MediaId.forAlbum(id))
                .setTitle(title)
                .setSubtitle(authorsText)
                .setIconUri(thumbnailUrl?.toUri())
                .build(),
            BrowserMediaItem.FLAG_PLAYABLE
        )

    private inner class SessionCallback(private val binder: PlayerService.Binder) :
        MediaSession.Callback() {
        override fun onPlay() = binder.player.play()
        override fun onPause() = binder.player.pause()
        override fun onSkipToPrevious() = binder.player.forceSeekToPrevious()
        override fun onSkipToNext() = binder.player.forceSeekToNext()
        override fun onSeekTo(pos: Long) = binder.player.seekTo(pos)
        override fun onSkipToQueueItem(id: Long) = binder.player.seekToDefaultPosition(id.toInt())
        override fun onPlayFromSearch(query: String?, extras: Bundle?) {
            if (query.isNullOrBlank()) return
            binder.playFromSearch(query = query)
        }

        @OptIn(UnstableApi::class)
        override fun onPlayFromMediaId(mediaId: String?, extras: Bundle?) {
            val data = mediaId?.split('/') ?: return
            var index = 0

            coroutineScope.launch {
                val mediaItems = when (data.getOrNull(0)) {
                    MediaId.SHUFFLE -> lastSongs

                    MediaId.SONGS -> data
                        .getOrNull(1)
                        ?.let { songId ->
                            index = lastSongs.indexOfFirst { it.id == songId }
                            lastSongs
                        }

                    MediaId.FAVORITES -> database
                        .favorites()
                        .first()
                        .shuffled()

                    MediaId.OFFLINE -> database
                        .songsWithContentLength()
                        .first()
                        .filter { song ->
                            song.contentLength?.let {
                                binder.cache.isCached(song.song.id, 0, it)
                            } == true
                        }
                        .map(SongWithContentLength::song)
                        .shuffled()

                    MediaId.PLAYLISTS -> data
                        .getOrNull(1)
                        ?.toLongOrNull()
                        ?.let(database::playlistWithSongs)
                        ?.first()
                        ?.songs
                        ?.shuffled()

                    MediaId.ALBUMS -> data
                        .getOrNull(1)
                        ?.let(database::albumSongs)
                        ?.first()

                    else -> emptyList()
                }?.map(Song::asMediaItem) ?: return@launch

                withContext(Dispatchers.Main) {
                    binder.player.forcePlayAtIndex(
                        mediaItems = mediaItems,
                        mediaItemIndex = index.coerceIn(0, mediaItems.size)
                    )
                }
            }
        }
    }

    private object MediaId {
        const val ROOT = "root"
        const val SONGS = "songs"
        const val PLAYLISTS = "playlists"
        const val ALBUMS = "albums"

        const val FAVORITES = "favorites"
        const val OFFLINE = "offline"
        const val SHUFFLE = "shuffle"

        fun forSong(id: String) = "songs/$id"
        fun forPlaylist(id: Long) = "playlists/$id"
        fun forAlbum(id: String) = "albums/$id"
    }
}