package com.github.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class PlayerResponse(
    val playabilityStatus: PlayabilityStatus?,
    val playerConfig: PlayerConfig?,
    val streamingData: StreamingData?,
    val videoDetails: VideoDetails?,
) {
    @Serializable
    data class PlayabilityStatus(
        val status: String?
    )

    @Serializable
    data class PlayerConfig(
        val audioConfig: AudioConfig?
    ) {
        @Serializable
        data class AudioConfig(
            private val loudnessDb: Double?,
            private val perceptualLoudnessDb: Double?
        ) {
            // For music clients only
            val normalizedLoudnessDb: Float?
                get() = (loudnessDb ?: perceptualLoudnessDb)?.plus(7)?.toFloat()
        }
    }

    @Serializable
    data class StreamingData(
        val adaptiveFormats: List<AdaptiveFormat>?,
        val formats: List<AdaptiveFormat>? = null
    ) {
        val highestQualityFormat: AdaptiveFormat?
            get() {
                val combined = adaptiveFormats.orEmpty() + formats.orEmpty()
                val audioFormats = combined.filter { it.url != null && it.mimeType.startsWith("audio/") }
                if (audioFormats.isNotEmpty()) {
                    return audioFormats.find { it.itag == 251 }
                        ?: audioFormats.find { it.itag == 140 }
                        ?: audioFormats.find { it.itag == 250 }
                        ?: audioFormats.find { it.itag == 249 }
                        ?: audioFormats.find { it.itag == 139 }
                        ?: audioFormats.maxByOrNull { it.bitrate ?: 0L }
                }
                return combined.find { it.url != null && it.mimeType.startsWith("video/") }
            }

        @Serializable
        data class AdaptiveFormat(
            val itag: Int,
            val mimeType: String,
            val bitrate: Long?,
            val averageBitrate: Long?,
            val contentLength: Long?,
            val audioQuality: String?,
            val approxDurationMs: Long?,
            val lastModified: Long?,
            val loudnessDb: Double?,
            val audioSampleRate: Int?,
            val url: String?,
        )
    }

    @Serializable
    data class VideoDetails(
        val videoId: String?
    )
}
