package com.github.musicyou.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.innertube.Innertube
import com.github.innertube.utils.plus
import com.github.musicyou.LocalPlayerPadding
import com.github.musicyou.R
import com.github.musicyou.ui.components.ShimmerHost
import com.github.musicyou.ui.styling.Dimensions
import com.github.musicyou.viewmodels.ItemsPageViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun <T : Innertube.Item> ItemsPage(
    tag: String,
    itemContent: @Composable LazyGridItemScope.(T) -> Unit,
    itemPlaceholderContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    initialPlaceholderCount: Int = 8,
    continuationPlaceholderCount: Int = 3,
    emptyItemsText: String = stringResource(id = R.string.no_items_found),
    itemsPageProvider: (suspend (String?) -> Result<Innertube.ItemsPage<T>?>?)? = null,
) {
    val playerPadding = LocalPlayerPadding.current

    val updatedItemsPageProvider by rememberUpdatedState(itemsPageProvider)
    val lazyGridState = rememberLazyGridState()
    val viewModel: ItemsPageViewModel<T> = viewModel()
    val itemsPage: Innertube.ItemsPage<T>? =
        viewModel.itemsMap.getOrDefault(key = tag, defaultValue = null)

    val listLayout = tag.contains("songs") || tag.contains("videos")
    val artistsLayout = tag.contains("artists")

    val shouldLoadMore by remember {
        derivedStateOf {
            lazyGridState.layoutInfo.visibleItemsInfo.any { it.key.toString().contains("loading") }
        }
    }

    LaunchedEffect(shouldLoadMore, updatedItemsPageProvider) {
        if (!shouldLoadMore) return@LaunchedEffect
        val currentItemsPageProvider = updatedItemsPageProvider ?: return@LaunchedEffect

        withContext(Dispatchers.IO) {
            currentItemsPageProvider(itemsPage?.continuation)
        }?.onSuccess {
            if (it == null) {
                if (itemsPage == null) {
                    viewModel.setItems(
                        tag = tag,
                        items = Innertube.ItemsPage(items = null, continuation = null)
                    )
                }
            } else {
                viewModel.setItems(
                    tag = tag,
                    items = itemsPage + it
                )
            }
        }
    }

    LazyVerticalGrid(
        state = lazyGridState,
        columns = GridCells.Adaptive(
            minSize = if (listLayout) 400.dp else if (artistsLayout) 100.dp else 150.dp
        ),
        contentPadding = PaddingValues(
            start = if (listLayout) 0.dp else 8.dp,
            top = 8.dp,
            end = if (listLayout) 0.dp else 8.dp,
            bottom = 16.dp + playerPadding
        ),
        verticalArrangement = Arrangement.spacedBy(if (listLayout) 0.dp else 4.dp),
        modifier = modifier.fillMaxSize()
    ) {
        item(
            key = "anchor",
            span = { GridItemSpan(maxCurrentLineSpan) }
        ) {
            Spacer(modifier = Modifier.height(Dp.Hairline))
        }

        items(
            items = itemsPage?.items ?: emptyList(),
            key = Innertube.Item::key,
            itemContent = itemContent
        )

        if (itemsPage != null && itemsPage.items.isNullOrEmpty()) {
            item(
                key = "empty",
                span = { GridItemSpan(maxCurrentLineSpan) }
            ) {
                Text(
                    text = emptyItemsText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 32.dp)
                        .fillMaxWidth()
                        .alpha(Dimensions.mediumOpacity)
                )
            }
        }

        if (itemsPage == null || itemsPage.continuation != null) {
            val isFirstLoad = itemsPage?.items.isNullOrEmpty()

            items(
                count = if (isFirstLoad) initialPlaceholderCount else continuationPlaceholderCount,
                key = { "loading$it" }
            ) {
                ShimmerHost {
                    itemPlaceholderContent()
                }
            }
        }
    }
}