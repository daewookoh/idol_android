package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 차트 누적 랭킹 모델
 *
 * charts/ranks/ API 응답 데이터
 * old 프로젝트의 AggregateRankModel과 동일
 */
data class AggregateRankModel(
    @SerializedName("idol_id")
    val idolId: Int = 0,

    @SerializedName("name")
    val name: String = "",

    @SerializedName("score")
    val score: Int = 0,

    @SerializedName("score_rank")
    var scoreRank: Int = 0,

    @SerializedName("trend_id")
    val trendId: Int = 0,

    @SerializedName("difference")
    val difference: Int = 0,

    @SerializedName("status")
    val status: String = "",

    @SerializedName("sudden_increase")
    var suddenIncrease: Boolean = false
)
