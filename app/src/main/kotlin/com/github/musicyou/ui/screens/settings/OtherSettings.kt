package com.github.musicyou.ui.screens.settings

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Battery0Bar
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.R
import com.github.musicyou.service.PlayerMediaBrowserService
import com.github.musicyou.ui.styling.Dimensions
import com.github.musicyou.utils.isAtLeastAndroid12
import com.github.musicyou.utils.isIgnoringBatteryOptimizations
import com.github.musicyou.utils.isInvincibilityEnabledKey
import com.github.musicyou.utils.rememberPreference
import com.github.musicyou.utils.toast

@SuppressLint("BatteryLife")
@ExperimentalAnimationApi
@Composable
fun OtherSettings() {
    val context = LocalContext.current
    val playerPadding = LocalPlayerPadding.current

    var isAndroidAutoEnabled by remember {
        val component = ComponentName(context, PlayerMediaBrowserService::class.java)
        val disabledFlag = PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        val enabledFlag = PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        mutableStateOf(
            value = context.packageManager.getComponentEnabledSetting(component) == enabledFlag,
            policy = object : SnapshotMutationPolicy<Boolean> {
                override fun equivalent(a: Boolean, b: Boolean): Boolean {
                    context.packageManager.setComponentEnabledSetting(
                        component,
                        if (b) enabledFlag else disabledFlag,
                        PackageManager.DONT_KILL_APP
                    )
                    return a == b
                }
            }
        )
    }

    var isInvincibilityEnabled by rememberPreference(isInvincibilityEnabledKey, false)
    var isIgnoringBatteryOptimizations by remember { mutableStateOf(context.isIgnoringBatteryOptimizations) }
    val activityResultLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            isIgnoringBatteryOptimizations = context.isIgnoringBatteryOptimizations
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp + playerPadding)
    ) {
        SwitchSettingEntry(
            title = stringResource(id = R.string.android_auto),
            text = stringResource(id = R.string.android_auto_description),
            icon = Icons.Outlined.DirectionsCar,
            isChecked = isAndroidAutoEnabled,
            onCheckedChange = { isAndroidAutoEnabled = it }
        )

        SettingsInformation(text = stringResource(id = R.string.android_auto_information))

        Spacer(modifier = Modifier.height(Dimensions.spacer))

        Text(
            text = stringResource(id = R.string.service_lifetime),
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 4.dp),
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.labelLarge
        )

        SettingsEntry(
            title = stringResource(id = R.string.ignore_battery_optimizations),
            text = if (isIgnoringBatteryOptimizations) {
                stringResource(id = R.string.already_unrestricted)
            } else {
                stringResource(id = R.string.disable_background_restrictions)
            },
            icon = Icons.Outlined.Battery0Bar,
            onClick = {
                try {
                    activityResultLauncher.launch(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                            data = "package:${context.packageName}".toUri()
                        }
                    )
                } catch (_: ActivityNotFoundException) {
                    try {
                        activityResultLauncher.launch(
                            Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                        )
                    } catch (_: ActivityNotFoundException) {
                        context.toast("Couldn't find battery optimization settings, please whitelist Music You manually")
                    }
                }
            },
            isEnabled = !isIgnoringBatteryOptimizations
        )

        SwitchSettingEntry(
            title = stringResource(id = R.string.service_lifetime),
            text = stringResource(id = R.string.service_lifetime_description),
            icon = Icons.Outlined.Stars,
            isChecked = isInvincibilityEnabled,
            onCheckedChange = { isInvincibilityEnabled = it }
        )

        SettingsInformation(
            text = stringResource(id = R.string.service_lifetime_information) +
                    if (isAtLeastAndroid12) "\n" + stringResource(id = R.string.service_lifetime_information_plus) else ""
        )
    }
}