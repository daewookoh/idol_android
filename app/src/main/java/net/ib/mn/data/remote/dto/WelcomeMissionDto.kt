package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 웰컴 미션 API Response
 *
 * GET /missions/welcome/
 * old 프로젝트의 WelcomeMissionResultModel과 동일한 구조
 */
data class WelcomeMissionResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("object")
    val missionData: WelcomeMissionData?,

    @SerializedName("gcode")
    val gcode: Int?,

    @SerializedName("msg")
    val msg: String?
)

/**
 * 웰컴 미션 데이터
 * old 프로젝트의 WelcomeMissionResultModel과 동일
 */
data class WelcomeMissionData(
    @SerializedName("all_clear_reward")
    val allClearReward: WelcomeMissionItemDto,

    @SerializedName("list")
    val list: List<WelcomeMissionItemDto>
)

/**
 * 개별 미션 항목
 * old 프로젝트의 WelcomeMissionModel과 동일
 */
data class WelcomeMissionItemDto(
    @SerializedName("amount")
    val amount: Int,

    @SerializedName("item")
    val item: String,  // "heart" or "diamond"

    @SerializedName("key")
    val key: String,   // "welcome_join", "welcome_set_most", etc.

    @SerializedName("status")
    val status: String // "G" (진행중), "R" (보상수령가능), "D" (완료)
)

/**
 * 미션 보상 수령 요청
 * POST /missions/reward/
 */
data class ClaimMissionRewardRequest(
    @SerializedName("key")
    val key: String
)

/**
 * 미션 보상 수령 응답
 */
data class ClaimMissionRewardResponse(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("object")
    val rewardData: WelcomeMissionItemDto?,

    @SerializedName("gcode")
    val gcode: Int?,

    @SerializedName("msg")
    val msg: String?
)
