package com.github.musicyou.ui.screens.settings

import android.text.format.Formatter
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.LocalPlayerServiceBinder
import com.github.musicyou.R
import com.github.musicyou.enums.CoilDiskCacheMaxSize
import com.github.musicyou.enums.ExoPlayerDiskCacheMaxSize
import com.github.musicyou.ui.components.ConfirmationDialog
import com.github.musicyou.ui.styling.Dimensions
import com.github.musicyou.utils.coilDiskCacheMaxSizeKey
import com.github.musicyou.utils.exoPlayerDiskCacheMaxSizeKey
import com.github.musicyou.utils.rememberPreference

@OptIn(UnstableApi::class)
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

    var isShowingImageCacheDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 16.dp + playerPadding)
    ) {
        context.imageLoader.diskCache?.let { diskCache ->
            var diskCacheSize by remember(diskCache) {
                mutableLongStateOf(diskCache.size)
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
                ).toFloat(),
                actionButton = {
                    FilledIconButton(
                        onClick = { isShowingImageCacheDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp),
                        enabled = diskCacheSize > 0,
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null
                        )
                    }
                }
            )

            EnumValueSelectorSettingsEntry(
                title = stringResource(id = R.string.max_size),
                selectedValue = coilDiskCacheMaxSize,
                onValueSelected = { coilDiskCacheMaxSize = it },
                icon = Icons.Outlined.Image
            )

            if (isShowingImageCacheDialog) {
                ConfirmationDialog(
                    title = stringResource(id = R.string.delete_image_cache),
                    onDismiss = { isShowingImageCacheDialog = false },
                    onConfirm = {
                        diskCache.clear()
                        diskCacheSize = 0
                    }
                )
            }
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
