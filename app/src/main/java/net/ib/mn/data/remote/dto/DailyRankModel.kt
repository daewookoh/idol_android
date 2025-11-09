package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 일일 랭킹 모델
 *
 * hofs/ API 응답 데이터
 * old 프로젝트의 HallModel과 유사
 */
data class DailyRankModel(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("trend_id")
    val trendId: Int = 0,

    @SerializedName("heart")
    val heart: Long = 0,

    @SerializedName("rank")
    val rank: Int = 0,

    @SerializedName("created_at")
    val createdAt: String = "",

    @SerializedName("idol_id")
    val idolId: Int = 0,

    @SerializedName("type")
    val type: String? = null,  // "S" (Solo) or "G" (Group)

    // idol 중첩 객체 (old 프로젝트와 동일)
    @SerializedName("idol")
    val idol: IdolInfo? = null
)

/**
 * Idol 정보 (hofs/ API 응답 내 중첩 객체)
 */
data class IdolInfo(
    @SerializedName("anniversary")
    val anniversary: String? = null,  // "Y" (생일), "E" (데뷔), "C" (컴백), "D" (기념일)

    @SerializedName("anniversary_days")
    val anniversaryDays: Int? = null,

    @SerializedName("type")
    val type: String? = null  // "S" (Solo) or "G" (Group)
)
