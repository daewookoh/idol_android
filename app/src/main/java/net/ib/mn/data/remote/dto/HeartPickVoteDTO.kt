package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 하트픽 투표 요청 DTO
 *
 * old 프로젝트: HeartpickVoteDTO와 동일
 *
 * @param heartpickId 하트픽 ID
 * @param heartpickIdolId 하트픽 아이돌 ID (HeartPickIdol.id)
 * @param number 투표할 하트 개수
 */
data class HeartPickVoteDTO(
    @SerializedName("heartpick_id") val heartpickId: Int,
    @SerializedName("heartpick_idol_id") val heartpickIdolId: Int,
    @SerializedName("number") val number: Long
)

/**
 * 하트픽 투표 응답
 *
 * old 프로젝트: HeartPickVoteRewardModel과 동일
 *
 * @param bonusHeart 보너스 하트 (100개 이상 투표 시 지급)
 * @param voted 투표된 총 하트 수
 */
data class HeartPickVoteResponse(
    @SerializedName("bonus_heart") val bonusHeart: Int = 0,
    @SerializedName("voted") val voted: Long = 0
)
