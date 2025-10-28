package net.ib.mn.core.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class RecordRoomModel(
    // 기록실 최다 득표 순위 Top 100
    @SerialName("top1_count") val top1Count: List<ChartCodeInfo>,
    // 1등한 횟수
    @SerialName("votes_top_100") val votesTop100: List<ChartCodeInfo>
) : Parcelable
