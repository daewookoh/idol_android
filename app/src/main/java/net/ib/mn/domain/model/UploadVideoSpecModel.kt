package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 동영상 업로드 스펙 설정
 * 서버에서 설정한 동영상 업로드 제한 정보
 *
 * Old 프로젝트의 UploadVideoSpecModel 참고
 */
data class UploadVideoSpecModel(
    @SerializedName("max_bitrate")
    val maxBitrate: Int = DEFAULT_MAX_BITRATE,

    @SerializedName("max_height")
    val maxHeight: Int = DEFAULT_MAX_HEIGHT,

    @SerializedName("max_width")
    val maxWidth: Int = DEFAULT_MAX_WIDTH,

    @SerializedName("max_seconds")
    val maxSeconds: Int = DEFAULT_MAX_SECONDS,

    @SerializedName("max_size_mb")
    val maxSizeMB: Int = DEFAULT_MAX_SIZE_MB
) {
    companion object {
        const val DEFAULT_MAX_BITRATE = 1_000_000  // 1 Mbps
        const val DEFAULT_MAX_HEIGHT = 1080
        const val DEFAULT_MAX_WIDTH = 1080
        const val DEFAULT_MAX_SECONDS = 60
        const val DEFAULT_MAX_SIZE_MB = 100
    }
}
