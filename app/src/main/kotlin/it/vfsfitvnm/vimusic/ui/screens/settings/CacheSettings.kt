package it.vfsfitvnm.vimusic.ui.screens.settings

import android.text.format.Formatter
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.annotation.ExperimentalCoilApi
import coil3.imageLoader
import it.vfsfitvnm.vimusic.LocalPlayerPadding
import it.vfsfitvnm.vimusic.LocalPlayerServiceBinder
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.enums.CoilDiskCacheMaxSize
import it.vfsfitvnm.vimusic.enums.ExoPlayerDiskCacheMaxSize
import it.vfsfitvnm.vimusic.ui.styling.Dimensions
import it.vfsfitvnm.vimusic.utils.coilDiskCacheMaxSizeKey
import it.vfsfitvnm.vimusic.utils.exoPlayerDiskCacheMaxSizeKey
import it.vfsfitvnm.vimusic.utils.rememberPreference

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@OptIn(ExperimentalCoilApi::class)
@ExperimentalAnimationApi
@Composable
fun CacheSettings() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val playerPadding = LocalPlayerPadding.current

    var coilDiskCacheMaxSize by rememberPreference(
        coilDiskCacheMaxSizeKey,
        CoilDiskCacheMaxSize.`128MB`
    )
    var exoPlayerDiskCacheMaxSize by rememberPreference(
        exoPlayerDiskCacheMaxSizeKey,
        ExoPlayerDiskCacheMaxSize.`2GB`
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 16.dp + playerPadding)
    ) {
        context.imageLoader.diskCache?.let { diskCache ->
            val diskCacheSize = remember(diskCache) {
                diskCache.size
            }

            Text(
                text = stringResource(id = R.string.image_cache),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )

            SettingsProgress(
                text = Formatter.formatShortFileSize(
                    context,
                    diskCacheSize
                ),
                progress = diskCacheSize.toFloat() / coilDiskCacheMaxSize.bytes.coerceAtLeast(
                    minimumValue = 1
                ).toFloat()
            )

            EnumValueSelectorSettingsEntry(
                title = stringResource(id = R.string.max_size),
                selectedValue = coilDiskCacheMaxSize,
                onValueSelected = { coilDiskCacheMaxSize = it },
                icon = Icons.Outlined.Image
            )
        }

        binder?.cache?.let { cache ->
            val diskCacheSize by remember {
                derivedStateOf {
                    cache.cacheSpace
                }
            }

            Spacer(modifier = Modifier.height(Dimensions.spacer))

            Text(
                text = stringResource(id = R.string.song_cache),
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelLarge
            )

            SettingsProgress(
                text = Formatter.formatShortFileSize(
                    context,
                    diskCacheSize
                ),
                progress = when (val size = exoPlayerDiskCacheMaxSize) {
                    ExoPlayerDiskCacheMaxSize.Unlimited -> 0F
                    else -> (diskCacheSize.toFloat() / size.bytes.toFloat())
                }
            )

            EnumValueSelectorSettingsEntry(
                title = stringResource(id = R.string.max_size),
                selectedValue = exoPlayerDiskCacheMaxSize,
                onValueSelected = { exoPlayerDiskCacheMaxSize = it },
                icon = Icons.Outlined.MusicNote
            )
        }

        SettingsInformation(text = stringResource(id = R.string.cache_information))
    }
}
