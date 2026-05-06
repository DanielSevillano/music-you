package com.github.musicyou.ui.items

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.musicyou.ui.components.TextPlaceholder
import com.github.musicyou.ui.styling.shimmer

@Composable
fun ItemContainer(
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    textAlign: TextAlign = TextAlign.Start,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    thumbnail: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .widthIn(max = 200.dp)
            .clip(MaterialTheme.shapes.large)
            .clickable(
                enabled = onClick != null,
                onClick = onClick ?: {}
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio = 1F)
                .clip(shape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            thumbnail()
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (isPlaceholder) {
            TextPlaceholder()
        } else {
            Text(
                text = title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = textAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (isPlaceholder) {
            TextPlaceholder()
        } else {
            subtitle?.let {
                Text(
                    text = subtitle,
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = textAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun ItemPlaceholder(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large
) {
    ItemContainer(
        modifier = modifier,
        isPlaceholder = true,
        title = "",
        shape = shape,
        color = MaterialTheme.colorScheme.shimmer,
        thumbnail = {}
    )
}

@Composable
fun ListItemContainer(
    modifier: Modifier = Modifier,
    isPlaceholder: Boolean = false,
    title: String,
    subtitle: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    maxLines: Int = 1,
    color: Color = MaterialTheme.colorScheme.surfaceVariant,
    containerColor: Color = ListItemDefaults.colors().containerColor,
    thumbnail: @Composable (size: Dp) -> Unit,
    thumbnailHeight: Dp = 56.dp,
    thumbnailAspectRatio: Float = 1F,
    trailingContent: @Composable (() -> Unit)? = null
) {
    ListItem(
        headlineContent = {
            if (isPlaceholder) {
                TextPlaceholder()
            } else {
                Text(
                    text = title,
                    lineHeight = 16.sp,
                    maxLines = maxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        modifier = modifier
            .clip(MaterialTheme.shapes.large)
            .combinedClickable(
                enabled = onClick != null || onLongClick != null,
                onClick = onClick ?: {},
                onLongClick = onLongClick ?: {}
            ),
        supportingContent = {
            if (isPlaceholder) {
                TextPlaceholder()
            } else {
                subtitle?.let {
                    Text(
                        text = subtitle,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .height(height = thumbnailHeight)
                    .aspectRatio(ratio = thumbnailAspectRatio)
                    .clip(MaterialTheme.shapes.medium)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                thumbnail(thumbnailHeight)
            }
        },
        trailingContent = {
            if (trailingContent != null) trailingContent()
            else if (onLongClick != null) {
                IconButton(onClick = onLongClick) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = null
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(
            containerColor = containerColor
        )
    )
}

@Composable
fun ListItemPlaceholder(
    modifier: Modifier = Modifier,
    thumbnailHeight: Dp = 56.dp,
    thumbnailAspectRatio: Float = 1F
) {
    ListItemContainer(
        modifier = modifier,
        isPlaceholder = true,
        title = "",
        color = MaterialTheme.colorScheme.shimmer,
        thumbnail = {},
        thumbnailHeight = thumbnailHeight,
        thumbnailAspectRatio = thumbnailAspectRatio
    )
}