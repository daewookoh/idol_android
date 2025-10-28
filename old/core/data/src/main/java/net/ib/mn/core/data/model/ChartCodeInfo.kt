package net.ib.mn.core.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@Parcelize
data class ChartCodeInfo(
    val code: String,
    val name: String,
    @SerialName("full_name") val fullName: String
): Parcelable