package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.ImagePickIdolModel
import net.ib.mn.domain.model.ImagePickModel

/**
 * 이미지픽 목록 응답
 */
data class ImagePickListResponse(
    @SerializedName("objects") val objects: List<ImagePickModel>? = null,
    @SerializedName("meta") val meta: MetaData? = null
)

/**
 * 이미지픽 상세 응답
 */
data class ImagePickResponse(
    @SerializedName("object") val `object`: ImagePickModel? = null
)

/**
 * 이미지픽 결과/순위 응답
 * old 프로젝트: OnepickResultActivity에서 사용
 *
 * /onepick/{id}/ API 응답 - 결과 순위와 투표 상태 정보 반환
 */
data class ImagePickResultResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("objects") val objects: List<ImagePickIdolModel>? = null,
    @SerializedName("date") val date: String? = null,
    @SerializedName("vote") val vote: String? = null,
    @SerializedName("onepick_dimension") val dimension: Int = 3,
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("status") val status: Int? = null,
    @SerializedName("count") val count: Int? = null,
    @SerializedName("created_at") val createdAt: String? = null,
    @SerializedName("expired_at") val expiredAt: String? = null,
    @SerializedName("alarm") val alarm: Boolean = false,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("gcode") val gcode: Int? = null
)

/**
 * 이미지픽 투표 요청 DTO
 */
data class ImagePickVoteRequest(
    @SerializedName("id") val id: Int,
    @SerializedName("vote_ids") val voteIds: String,
    @SerializedName("vote_type") val voteType: String
)

/**
 * 이미지픽 알림 설정 요청 DTO
 */
data class ImagePickAlarmRequest(
    @SerializedName("id") val id: Int
)
