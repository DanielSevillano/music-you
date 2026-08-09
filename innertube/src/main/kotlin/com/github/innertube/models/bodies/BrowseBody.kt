package com.github.innertube.models.bodies

import com.github.innertube.models.Context
import com.github.innertube.models.YouTubeClient
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class BrowseBody(
    @Transient val localized: Boolean = true,
    val context: Context = YouTubeClient.WEB_REMIX.toContext(localized = localized),
    val browseId: String,
    val params: String? = null
)
