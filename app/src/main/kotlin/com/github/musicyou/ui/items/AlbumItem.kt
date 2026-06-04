package com.github.musicyou.ui.items

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import com.github.innertube.Innertube
import com.github.musicyou.models.Album
import com.github.musicyou.ui.styling.px
import com.github.musicyou.utils.thumbnail

@Composable
fun AlbumItem(
    modifier: Modifier = Modifier,
    album: Innertube.AlbumItem,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = album.info?.name ?: "",
        subtitle = if (album.authors.isNullOrEmpty()) album.year
        else "${album.authors?.joinToString(separator = "") { it.name ?: "" }} • ${album.year}",
        onClick = onClick
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = album.thumbnail?.url.thumbnail(maxWidth.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(maxWidth)
                    .clip(MaterialTheme.shapes.large)
            )
        }
    }
}

@Composable
fun LocalAlbumItem(
    modifier: Modifier = Modifier,
    album: Album,
    onClick: () -> Unit
) {
    ItemContainer(
        modifier = modifier,
        title = album.title ?: "",
        subtitle = if (album.authorsText.isNullOrEmpty()) album.year
        else "${album.authorsText} • ${album.year}",
        onClick = onClick
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            AsyncImage(
                model = album.thumbnailUrl?.thumbnail(maxWidth.px),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(maxWidth)
                    .clip(MaterialTheme.shapes.large)
            )
        }
    }
}