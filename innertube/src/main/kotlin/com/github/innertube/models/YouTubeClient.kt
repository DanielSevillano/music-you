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

        val WEB_EMBEDDED_PLAYER = YouTubeClient(
            clientName = "WEB_EMBEDDED_PLAYER",
            clientVersion = "2.20260708.00.00",
            clientId = "56",
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15,gzip(gfe)"
        )
    }
}
