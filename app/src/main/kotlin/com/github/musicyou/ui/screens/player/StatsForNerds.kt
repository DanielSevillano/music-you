package com.github.musicyou.ui.screens.player

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheSpan
import com.github.innertube.Innertube
import com.github.innertube.requests.player
import com.github.musicyou.LocalPlayerServiceBinder
import com.github.musicyou.R
import com.github.musicyou.database
import com.github.musicyou.models.Format
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
fun StatsForNerds(
    mediaId: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current ?: return

    var cachedBytes by remember(mediaId) {
        mutableLongStateOf(binder.cache.getCachedBytes(mediaId, 0, -1))
    }

    var format by remember {
        mutableStateOf<Format?>(null)
    }

    LaunchedEffect(mediaId) {
        database.format(mediaId).distinctUntilChanged().collectLatest { currentFormat ->
            if (currentFormat?.itag == null) {
                binder.player.currentMediaItem?.takeIf { it.mediaId == mediaId }?.let { mediaItem ->
                    withContext(Dispatchers.IO) {
                        delay(duration = 2.seconds)
                        Innertube.player(videoId = mediaId)?.onSuccess { response ->
                            response.streamingData?.highestQualityFormat?.let { format ->
                                database.insert(mediaItem)
                                database.insert(
                                    Format(
                                        songId = mediaId,
                                        itag = format.itag,
                                        mimeType = format.mimeType,
                                        bitrate = format.bitrate,
                                        loudnessDb = response.playerConfig?.audioConfig?.normalizedLoudnessDb,
                                        contentLength = format.contentLength,
                                        lastModified = format.lastModified
                                    )
                                )
                            }
                        }
                    }
                }
            } else format = currentFormat
        }
    }

    DisposableEffect(mediaId) {
        val listener = object : Cache.Listener {
            override fun onSpanAdded(cache: Cache, span: CacheSpan) {
                cachedBytes += span.length
            }

            override fun onSpanRemoved(cache: Cache, span: CacheSpan) {
                cachedBytes -= span.length
            }

            override fun onSpanTouched(cache: Cache, oldSpan: CacheSpan, newSpan: CacheSpan) = Unit
        }

        binder.cache.addListener(mediaId, listener)

        onDispose {
            binder.cache.removeListener(mediaId, listener)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.close))
            }
        },
        title = {
            Text(text = stringResource(id = R.string.information))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = stringResource(id = R.string.id),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.itag),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.bitrate),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.size),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.cached),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(id = R.string.loudness),
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column {
                        Text(
                            text = mediaId,
                            maxLines = 1
                        )
                        Text(
                            text = format?.itag?.toString()
                                ?: stringResource(id = R.string.unknown),
                            maxLines = 1
                        )
                        Text(
                            text = format?.bitrate?.let { "${it / 1000} kbps" }
                                ?: stringResource(id = R.string.unknown),
                            maxLines = 1
                        )
                        Text(
                            text = format?.contentLength
                                ?.let { Formatter.formatShortFileSize(context, it) }
                                ?: stringResource(
                                    id = R.string.unknown
                                ),
                            maxLines = 1
                        )
                        Text(
                            text = buildString {
                                append(Formatter.formatShortFileSize(context, cachedBytes))

                                format?.contentLength?.let {
                                    append(" (${(cachedBytes.toFloat() / it * 100).roundToInt()}%)")
                                }
                            },
                            maxLines = 1
                        )
                        Text(
                            text = format?.loudnessDb?.let { "%.2f dB".format(it) }
                                ?: stringResource(id = R.string.unknown),
                            maxLines = 1
                        )
                    }
                }

                Button(
                    onClick = { binder.cache.removeResource(mediaId) }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(id = R.string.clear_cache)
                    )

                    Spacer(Modifier.size(ButtonDefaults.IconSpacing))

                    Text(text = stringResource(id = R.string.clear_cache))
                }
            }
        }
    )
}