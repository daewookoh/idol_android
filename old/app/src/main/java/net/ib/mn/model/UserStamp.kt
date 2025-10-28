package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserStamp(
    @SerializedName("able") val able: Boolean = false,
) : Parcelable