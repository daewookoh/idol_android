package net.ib.mn.core.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ArticleLikeModel(
    val gcode: Int = 0,
    val liked: Boolean = false,
    val success: Boolean,
    var message: String? = null // 오류 발생시 추가하여 emit
): Parcelable