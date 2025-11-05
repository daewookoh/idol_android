package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * charts/ranks/ API 응답
 *
 * old 프로젝트의 ObjectsBaseDataModel<List<AggregateRankModel>>과 동일
 */
data class ChartRanksResponse(
    @SerializedName("success")
    val success: Boolean = false,

    @SerializedName("objects")
    val objects: List<AggregateRankModel>? = null,

    @SerializedName("msg")
    val msg: String? = null
)
