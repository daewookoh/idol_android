package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Award Rank API Response
 * 어워즈 누적순위 응답
 *
 * old 프로젝트의 HallModel 리스트와 동일한 구조
 */
data class AwardRankResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("gcode")
    val gcode: Int = 0,

    @SerializedName("data")
    val data: List<AwardRankItem>? = null
)

/**
 * Award Rank Item
 * 순위 아이템 (old: HallModel)
 */
data class AwardRankItem(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("idol_id")
    val idolId: Int = 0,

    @SerializedName("score")
    val score: Int = 0,

    @SerializedName("heart")
    val heart: Long = 0,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("event")
    val event: String? = null,

    // 순위 (동점자 처리용, 클라이언트에서 계산)
    var rank: Int = -1,

    // 아이돌 정보 (별도 조회 후 매핑)
    var idol: AwardRankIdol? = null
)

/**
 * Award Rank Idol
 * 순위 아이템에 포함된 아이돌 정보
 */
data class AwardRankIdol(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("name")
    val name: String? = null,

    @SerializedName("name_en")
    val nameEn: String? = null,

    @SerializedName("group")
    val group: String? = null,

    @SerializedName("group_en")
    val groupEn: String? = null,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("source_app")
    val sourceApp: String? = null
)
