package net.ib.mn.core.model

import android.os.Parcelable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.parcelize.Parcelize

/**
 * core/data의 model에 있는 것을 앱에서 사용할 것들만 추려낸 모델 클래스
 * 현 시점에서는 ChartResponseModel과 동일
 */
@Parcelize
@Serializable
data class ChartModel(
    @SerialName("begin_date") val beginDate: String? = null,
    val code: String? = null,
    @SerialName("end_date") val endDate: String? = null,
    val type: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("image_rank_url") val imageRankUrl: String? = null,
    @SerialName("target_month") val targetMonth: Int? = null,
    @SerialName("aggregate_type") val aggregateType: List<String> = emptyList()
) : Parcelable