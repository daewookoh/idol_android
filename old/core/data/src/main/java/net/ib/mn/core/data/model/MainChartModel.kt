package net.ib.mn.core.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class MainChartModel(
    val females: List<ChartCodeInfo>,
    val males: List<ChartCodeInfo>
) : Parcelable