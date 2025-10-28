package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.Anniversary
import java.io.Serializable


data class AnniversaryModel(
    @SerializedName("id") var id: Int = 0,  // Idol id
    @SerializedName("anniversary") var anniversary: String? = "N",
    @SerializedName("anniversary_days") val anniversaryDays: Int? = null,
    @SerializedName("burning_day") val burningDay: String? = null,
    @SerializedName("heart") val heart: Long,
    @SerializedName("top3") val top3: String?,
    @SerializedName("top3_type") val top3Type: String?
) : Serializable {
    companion object {
        const val NOTHING = "N"         // 아무 것도 아님
        const val BIRTH = "Y"           // 생일
        const val DEBUT = "E"           // 데뷔일
        const val COMEBACK = "C"        // 컴백일
        const val MEMORIAL_DAY = "D"    // 기념일
        const val ALL_IN_DAY = "B"      // 몰빵일
        const val TYPE_PHOTO = "P"
        const val TYPE_VIDEO = "V"
    }
}

fun AnniversaryModel.toDomain(): Anniversary =
    Anniversary(
        id,
        anniversary,
        anniversaryDays,
        burningDay,
        heart,
        top3,
        top3Type
    )

