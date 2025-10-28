package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class SuperRookie(
    val id: Int = 0,
    @SerializedName("idol_id") val idolId: Int = 0,
    @SerializedName("idol_name") val idolName: String = "",
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("log_text") val logText: String = "",
    val ordinal: Int = 0,
    @SerializedName("refdate") val refDate: String = ""
): Parcelable
