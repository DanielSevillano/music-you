package it.vfsfitvnm.vimusic.enums

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SortByAlpha
import androidx.compose.ui.graphics.vector.ImageVector
import it.vfsfitvnm.vimusic.R
import it.vfsfitvnm.vimusic.models.SortBy

enum class ArtistSortBy(
    @StringRes override val text: Int,
    override val icon: ImageVector
) : SortBy {
    Name(
        text = R.string.name,
        icon = Icons.Outlined.SortByAlpha
    ),
    DateAdded(
        text = R.string.date_added,
        icon = Icons.Outlined.Schedule
    )
}
