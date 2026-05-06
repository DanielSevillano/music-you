package com.github.musicyou.utils

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import com.github.innertube.Innertube
import com.github.innertube.requests.playlistPageContinuation
import com.github.innertube.utils.plus
import com.github.musicyou.models.Song

val Innertube.SongItem.asMediaItem: MediaItem
    @OptIn(UnstableApi::class)
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name ?: "" })
                .setAlbumTitle(album?.name)
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    Bundle().apply {
                        putString("albumId", album?.endpoint?.browseId)
                        putString("durationText", durationText)
                        putStringArrayList(
                            "artistNames",
                            authors?.filter { it.endpoint != null }?.mapNotNull { it.name }
                                ?.let { ArrayList(it) })
                        putStringArrayList(
                            "artistIds",
                            authors?.mapNotNull { it.endpoint?.browseId }
                                ?.let { ArrayList(it) })
                    }
                )
                .build()
        )
        .build()

val Innertube.VideoItem.asMediaItem: MediaItem
    @OptIn(UnstableApi::class)
    get() = MediaItem.Builder()
        .setMediaId(key)
        .setUri(key)
        .setCustomCacheKey(key)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(info?.name)
                .setArtist(authors?.joinToString("") { it.name ?: "" })
                .setArtworkUri(thumbnail?.url?.toUri())
                .setExtras(
                    Bundle().apply {
                        putString("durationText", durationText)
                        putStringArrayList(
                            "artistNames",
                            if (isOfficialMusicVideo) authors?.filter { it.endpoint != null }
                                ?.mapNotNull { it.name }?.let { ArrayList(it) } else null)
                        putStringArrayList(
                            "artistIds", if (isOfficialMusicVideo) authors
                                ?.mapNotNull { it.endpoint?.browseId }
                                ?.let { ArrayList(it) } else null)
                    }
                )
                .build()
        )
        .build()

val Song.asMediaItem: MediaItem
    @OptIn(UnstableApi::class)
    get() = MediaItem.Builder()
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(title)
                .setArtist(artistsText)
                .setArtworkUri(thumbnailUrl?.toUri())
                .setExtras(
                    Bundle().apply {
                        putString("durationText", durationText)
                    }
                )
                .build()
        )
        .setMediaId(id)
        .setUri(id)
        .setCustomCacheKey(id)
        .build()

fun String?.thumbnail(size: Int): String? {
    return when {
        this?.startsWith("https://lh3") == true -> "$this-w$size-h$size"
        this?.startsWith("https://yt3") == true -> "$this-w$size-h$size-s$size"
        else -> this
    }
}

fun Uri?.thumbnail(size: Int): Uri? {
    return toString().thumbnail(size)?.toUri()
}

fun formatAsDuration(millis: Long) = DateUtils.formatElapsedTime(millis / 1000).removePrefix("0")

suspend fun Result<Innertube.PlaylistOrAlbumPage>.completed(): Result<Innertube.PlaylistOrAlbumPage>? {
    var playlistPage = getOrNull() ?: return null

    while (playlistPage.songsPage?.continuation != null) {
        val continuation = playlistPage.songsPage?.continuation!!
        val otherPlaylistPageResult =
            Innertube.playlistPageContinuation(continuation = continuation) ?: break

        if (otherPlaylistPageResult.isFailure) break

        otherPlaylistPageResult.getOrNull()?.let { otherSongsPage ->
            playlistPage = playlistPage.copy(songsPage = playlistPage.songsPage + otherSongsPage)
        }
    }

    return Result.success(playlistPage)
}

inline val isAtLeastAndroid8
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

inline val isAtLeastAndroid12
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

inline val isAtLeastAndroid13
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU