package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.ThemePickModel

/**
 * 테마픽 목록 응답
 */
data class ThemePickListResponse(
    @SerializedName("objects") val objects: List<ThemePickModel>? = null,
    @SerializedName("meta") val meta: MetaData? = null
)

/**
 * 테마픽 상세 응답
 *
 * 후보 목록(objects)이 ThemePickModel 내에 포함되어 반환됨
 */
data class ThemePickResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("status") val status: Int = 0,
    @SerializedName("vote") val vote: String = "",
    @SerializedName("count") val count: Int = 0,
    @SerializedName("begin_at") val beginAt: String = "",
    @SerializedName("expired_at") val expiredAt: String = "",
    @SerializedName("image_ratio") val imageRatio: String = "",
    @SerializedName("alarm") val alarm: Boolean = false,
    @SerializedName("vote_id") val voteId: Int = 0,
    @SerializedName("type") val type: String = "I",
    @SerializedName("dummy") val dummy: String? = null,
    @SerializedName("objects") val objects: List<ThemePickIdolDto>? = null,
    @SerializedName("prize") val prize: ThemePickPrizeDto? = null
) {
    /**
     * ThemePickModel로 변환
     */
    fun toModel(): ThemePickModel {
        return ThemePickModel(
            id = id,
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            status = status,
            vote = vote,
            count = count,
            beginAt = beginAt,
            expiredAt = expiredAt,
            imageRatio = imageRatio,
            alarm = alarm,
            voteId = voteId,
            type = type,
            dummy = dummy,
            candidates = objects?.mapTo(ArrayList()) { it.toModel() },
            prize = prize?.toModel()
        )
    }
}

/**
 * 테마픽 후보 DTO
 */
data class ThemePickIdolDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("idol_id") val idolId: Int? = null,
    @SerializedName("title") val title: String = "",
    @SerializedName("subtitle") val subtitle: String = "",
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("vote") val vote: Long = 0L
) {
    fun toModel(): net.ib.mn.domain.model.ThemePickIdol {
        return net.ib.mn.domain.model.ThemePickIdol(
            id = id,
            idolId = idolId,
            title = title,
            subtitle = subtitle,
            imageUrl = imageUrl,
            vote = vote
        )
    }
}

/**
 * 테마픽 상품 DTO
 */
data class ThemePickPrizeDto(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("name") val name: String? = "",
    @SerializedName("image_url") val imageUrl: String? = "",
    @SerializedName("location") val location: String? = ""
) {
    fun toModel(): net.ib.mn.domain.model.PrizeModel {
        return net.ib.mn.domain.model.PrizeModel(
            id = id,
            name = name,
            imageUrl = imageUrl,
            location = location
        )
    }
}

/**
 * 테마픽 투표 요청
 *
 * Old 프로젝트와 동일한 필드명 사용:
 * - themepick_id: 테마픽 ID
 * - themepick_idol_id: 테마픽 후보 ID (ThemePickIdol.id, 아이돌 ID가 아님!)
 */
data class ThemePickVoteRequest(
    @SerializedName("themepick_id") val id: Int,
    @SerializedName("themepick_idol_id") val idolId: Int,
    @SerializedName("vote_type") val voteType: String
)

/**
 * 테마픽 알림 설정 요청
 */
data class OpenNotificationRequest(
    @SerializedName("id") val id: Int
)
