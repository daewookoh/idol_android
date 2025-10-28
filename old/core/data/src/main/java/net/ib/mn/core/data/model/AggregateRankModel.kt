package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// app의 model에 있는 것은 나중에 제거 예정
@Serializable
data class AggregateRankModel(
    @SerialName("idol_id") val idolId: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("score") val score: Int = 0,
    @SerialName("score_rank") var scoreRank: Int = 0,
    @SerialName("trend_id") val trendId: Int = 0,
    @SerialName("difference") val difference: Int = 0,
    @SerialName("status") val status: String = "",
    @SerialName("sudden_increase") var suddenIncrease: Boolean = false
)