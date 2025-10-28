package net.ib.mn.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UploadVideoSpecModel(
    @SerialName("max_bitrate") val maxBitrate: Int = 1000000,
    @SerialName("max_height") val maxHeight: Int = 1080,
    @SerialName("max_width") val maxWidth: Int = 1080,
    @SerialName("max_seconds") val maxSeconds: Int = 60,
    @SerialName("max_size_mb") val maxSizeMB: Int = 100,
)
