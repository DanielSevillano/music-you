package com.github.musicyou.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.musicyou.models.Section

@Composable
fun ChipScaffold(
    tabIndex: Int,
    onTabChanged: (Int) -> Unit,
    tabColumnContent: List<Section>,
    content: @Composable (AnimatedVisibilityScope.(Int) -> Unit)
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                tabColumnContent.forEachIndexed { index, section ->
                    FilterChip(
                        selected = index == tabIndex,
                        onClick = { onTabChanged(index) },
                        label = {
                            Text(text = section.title)
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = section.icon,
                                contentDescription = section.title
                            )
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            AnimatedContent(
                targetState = tabIndex,
                transitionSpec = {
                    val slideDirection = when (targetState > initialState) {
                        true -> AnimatedContentTransitionScope.SlideDirection.Left
                        false -> AnimatedContentTransitionScope.SlideDirection.Right
                    }

                    slideIntoContainer(slideDirection) togetherWith slideOutOfContainer(
                        slideDirection
                    )
                },
                content = content,
                label = "chips"
            )
        }
    }
}