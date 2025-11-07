package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 이미지픽 도메인 모델
 */
data class ImagePickModel(
    @SerializedName("title") val title: String,
    @SerializedName("subtitle") val subtitle: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: Int,  // 0: 준비중, 1: 진행중, 2: 종료
    @SerializedName("vote") val vote: String,  // 투표 상태 코드
    @SerializedName("count") val count: Int,  // 실제 투표수
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("expired_at") val expiredAt: String,
    @SerializedName("hash_tag") val hashTag: String,
    @SerializedName("resource_uri") val resourceUri: String,
    @SerializedName("vote_type") val voteType: String,
    @SerializedName("alarm") val alarm: Boolean = false
) {
    val id: Int
        get() = resourceUri.split("/").last { it.isNotEmpty() }.toIntOrNull() ?: 0

    companion object {
        const val STATUS_PREPARING = 0
        const val STATUS_PROGRESS = 1
        const val STATUS_FINISHED = 2
    }
}
