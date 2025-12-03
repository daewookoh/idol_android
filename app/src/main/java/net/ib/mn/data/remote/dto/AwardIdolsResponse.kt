package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Award Idols API Response
 * 어워즈 실시간 순위 응답 (Daily)
 *
 * old: IdolsApi.getAwardIdols() 응답
 */
data class AwardIdolsResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("objects")
    val objects: List<AwardIdolItem>? = null
)

/**
 * Award Idol Item
 * 실시간 순위 아이템
 */
data class AwardIdolItem(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("heart")
    var heart: Long = 0,

    @SerializedName("top3")
    val top3: String? = null,

    @SerializedName("top3_type")
    val top3Type: String? = null,

    // 순위 (클라이언트에서 계산)
    var rank: Int = -1,

    // 아이돌 정보 (별도 조회 후 매핑)
    var idol: AwardRankIdol? = null
)
