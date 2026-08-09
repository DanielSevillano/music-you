package com.github.innertube.models

import java.util.Locale

class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val platform: String? = null,
    val osVersion: String? = null
) {
    fun toContext(
        localized: Boolean = true,
        visitorData: String? = null
    ) = Context(
        client = Context.Client(
            clientName = clientName,
            clientVersion = clientVersion,
            clientId = clientId,
            osVersion = osVersion,
            platform = platform,
            userAgent = userAgent,
            gl = if (localized) Locale.getDefault().country.ifBlank { "US" } else "US",
            hl = if (localized) Locale.getDefault().language.ifBlank { "en" } else "en",
            visitorData = visitorData
        )
    )

    companion object {
        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20260707.12.00",
            clientId = "67",
            platform = "DESKTOP",
            userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
        )

        val VISION_OS = YouTubeClient(
            clientName = "VISIONOS",
            clientVersion = "1.02",
            clientId = "101",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15",
            osVersion = "26.5.23O47",
        )

        val TVHTML5_SIMPLY_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "TVHTML5_SIMPLY_EMBEDDED_PLAYER",
            clientVersion = "2.0",
            clientId = "85",
            platform = "TV",
            userAgent = "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"
        )
    }
}
