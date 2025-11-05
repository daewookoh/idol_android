package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 투표 요청
 *
 * old 프로젝트의 GiveHeartToIdolDTO와 동일
 */
data class VoteRequest(
    @SerializedName("idol_id") val idolId: String,
    @SerializedName("number") val number: Long
)

/**
 * 투표 후 응답
 *
 * old 프로젝트의 GiveHeartModel과 동일
 */
data class VoteResponse(
    @SerializedName("bonus_heart") val bonusHeart: Int? = null,
    @SerializedName("event_heart") val eventHeart: Boolean = false,
    @SerializedName("event_heart_count") val eventHeartCount: Int = 0,
    @SerializedName("msg") val msg: String? = null,
    @SerializedName("success") val success: Boolean = false
)
