package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class OnepickIdolModel(
    @SerializedName("id") val id: Int,
    @SerializedName("idol") val idol: IdolModel?,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("vote") val vote: Long = 1L,
    var rank: Int,
    var lastPlaceVoteCount: Long = 1,
    var firstPlaceVoteCount: Long = 1,
    var minPercent: Float = 1f,
) : Serializable