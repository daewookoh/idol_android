package net.ib.mn.core.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.ib.mn.core.model.ChartModel

@Serializable
@Parcelize
data class CurrentChartResponse(
    val gcode: Int = 0,
    val main: MainChartModel? = null,
    val objects: List<ChartModel>? = null,
    @SerialName("record_room") val recordRoom: RecordRoomModel? = null,
    val success: Boolean,
    var message: String? = null // 오류 발생시 처리용. 서버에서 주지는 않음
) : Parcelable
