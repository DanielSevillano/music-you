package com.github.musicyou.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Update
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.api.GitHub
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.R
import com.github.musicyou.ui.styling.Dimensions

@Composable
fun About() {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val playerPadding = LocalPlayerPadding.current

    var isShowingDialog by remember { mutableStateOf(false) }
    var latestVersion: String? by rememberSaveable { mutableStateOf(null) }
    var newVersionAvailable: Boolean? by rememberSaveable { mutableStateOf(null) }

    val currentVersion =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 8.dp, bottom = 16.dp + playerPadding)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.app_icon),
            contentDescription = stringResource(id = R.string.app_name),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(125.dp)
                .aspectRatio(1F),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "${stringResource(id = R.string.app_name)} v$currentVersion",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            textAlign = TextAlign.Center
        )

        Button(
            onClick = { isShowingDialog = true },
            modifier = Modifier
                .padding(top = 8.dp)
                .align(Alignment.CenterHorizontally)
        ) {
            Icon(
                imageVector = Icons.Outlined.Update,
                contentDescription = stringResource(id = R.string.check_for_updates)
            )

            Spacer(Modifier.size(ButtonDefaults.IconSpacing))

            Text(text = stringResource(id = R.string.check_for_updates))
        }

        Spacer(modifier = Modifier.height(Dimensions.spacer + 8.dp))

        ListItem(
            headlineContent = {
                Text(text = stringResource(id = R.string.github))
            },
            leadingContent = {
                Icon(
                    painter = painterResource(id = R.drawable.github),
                    contentDescription = stringResource(id = R.string.github)
                )
            },
            modifier = Modifier.clickable {
                uriHandler.openUri("https://github.com/DanielSevillano/music-you")
            }
        )
    }

    if (isShowingDialog) {
        LaunchedEffect(Unit) {
            if (newVersionAvailable == null || latestVersion == null) {
                latestVersion = GitHub.getLastestRelease()?.name
                latestVersion?.let {
                    newVersionAvailable = it > currentVersion
                }
            }
        }

        AlertDialog(
            onDismissRequest = { isShowingDialog = false },
            confirmButton = {
                TextButton(
                    onClick = { isShowingDialog = false }
                ) {
                    Text(text = stringResource(id = R.string.close))
                }
            },
            title = {
                Text(
                    text = stringResource(
                        id = when (newVersionAvailable) {
                            true -> R.string.new_version_available
                            false -> R.string.no_updates_available
                            else -> R.string.checking_for_updates
                        }
                    )
                )
            },
            text = {
                if (newVersionAvailable == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (newVersionAvailable == true) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(
                                id = R.string.version,
                                latestVersion ?: ""
                            ),
                            style = MaterialTheme.typography.titleMedium
                        )

                        FilledTonalButton(
                            onClick = {
                                uriHandler.openUri("https://github.com/DanielSevillano/music-you/releases/latest")
                            }
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.github),
                                contentDescription = stringResource(id = R.string.github)
                            )

                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))

                            Text(text = stringResource(id = R.string.open_in_github))
                        }
                    }
                }
            }
        )
    }
}