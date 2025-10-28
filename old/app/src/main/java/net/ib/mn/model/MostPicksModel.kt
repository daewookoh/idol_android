package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MostPicksModel(
    @SerializedName("heartpick") val heartpick : List<Int>? = null,
    @SerializedName("onepick") val onepick : List<Int>? = null,
    @SerializedName("themepick") val themepick : List<Int>? = null,
    @SerializedName("miracle") val miracle: Boolean? = false
): Serializable
