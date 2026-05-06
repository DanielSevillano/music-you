package com.github.musicyou.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.audiofx.AudioEffect
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.QueueMusic
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Equalizer
import androidx.compose.material.icons.outlined.FastForward
import androidx.compose.material.icons.outlined.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.LocalPlayerServiceBinder
import com.github.musicyou.R
import com.github.musicyou.utils.persistentQueueKey
import com.github.musicyou.utils.rememberPreference
import com.github.musicyou.utils.resumePlaybackWhenDeviceConnectedKey
import com.github.musicyou.utils.skipSilenceKey
import com.github.musicyou.utils.toast
import com.github.musicyou.utils.volumeNormalizationKey

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@ExperimentalAnimationApi
@Composable
fun PlayerSettings() {
    val context = LocalContext.current
    val binder = LocalPlayerServiceBinder.current
    val playerPadding = LocalPlayerPadding.current

    var persistentQueue by rememberPreference(persistentQueueKey, false)
    var resumePlaybackWhenDeviceConnected by rememberPreference(
        resumePlaybackWhenDeviceConnectedKey,
        false
    )
    var skipSilence by rememberPreference(skipSilenceKey, false)
    var volumeNormalization by rememberPreference(volumeNormalizationKey, false)
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp + playerPadding)
    ) {
        SwitchSettingEntry(
            title = stringResource(id = R.string.persistent_queue),
            text = stringResource(id = R.string.persistent_queue_description),
            icon = Icons.AutoMirrored.Outlined.QueueMusic,
            isChecked = persistentQueue,
            onCheckedChange = {
                persistentQueue = it
            }
        )

        SwitchSettingEntry(
            title = stringResource(id = R.string.resume_playback),
            text = stringResource(id = R.string.resume_playback_description),
            icon = Icons.Outlined.Replay,
            isChecked = resumePlaybackWhenDeviceConnected,
            onCheckedChange = {
                resumePlaybackWhenDeviceConnected = it
            }
        )

        SwitchSettingEntry(
            title = stringResource(id = R.string.skip_silence),
            text = stringResource(id = R.string.skip_silence_description),
            icon = Icons.Outlined.FastForward,
            isChecked = skipSilence,
            onCheckedChange = {
                skipSilence = it
            }
        )

        SwitchSettingEntry(
            title = stringResource(id = R.string.loudness_normalization),
            text = stringResource(id = R.string.loudness_normalization_description),
            icon = Icons.AutoMirrored.Outlined.VolumeUp,
            isChecked = volumeNormalization,
            onCheckedChange = {
                volumeNormalization = it
            }
        )

        SettingsEntry(
            title = stringResource(id = R.string.equalizer),
            text = stringResource(id = R.string.equalizer_description),
            icon = Icons.Outlined.Equalizer,
            onClick = {
                val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                    putExtra(AudioEffect.EXTRA_AUDIO_SESSION, binder?.player?.audioSessionId)
                    putExtra(AudioEffect.EXTRA_PACKAGE_NAME, context.packageName)
                    putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                }

                try {
                    activityResultLauncher.launch(intent)
                } catch (_: ActivityNotFoundException) {
                    context.toast("Couldn't find an application to equalize audio")
                }
            }
        )
    }
}
