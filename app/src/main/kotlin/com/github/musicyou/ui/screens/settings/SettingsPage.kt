package com.github.musicyou.ui.screens.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import com.github.musicyou.enums.SettingsSection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPage(
    section: SettingsSection,
    pop: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    Text(text = stringResource(id = section.resourceId))
                },
                navigationIcon = {
                    IconButton(onClick = pop) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            when (section) {
                SettingsSection.General -> GeneralSettings()
                SettingsSection.Player -> PlayerSettings()
                SettingsSection.Gestures -> GestureSettings()
                SettingsSection.Cache -> CacheSettings()
                SettingsSection.Database -> DatabaseSettings()
                SettingsSection.Other -> OtherSettings()
                SettingsSection.About -> About()
            }
        }
    }
}