package net.ib.mn.model

import com.google.gson.annotations.SerializedName

data class ThemepickRankModel(
    @SerializedName("id") val id: Int = 0,
    @SerializedName("title") val title: String? = null,
    @SerializedName("subtitle") val subtitle: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("vote") val vote: Long = 1L,
    @SerializedName("idol_id") val idolId: Int? = null,
    var rank: Int = 0,
    var isSelected: Boolean = false, //뷰페이저 옆으로 넘겼을때(선택X)여부.
    var isClicked: Boolean = false, //뷰페이저 넘기고 나서 선택되었을때(선택O)여부.
    var lastPlaceVote: Long = 1L,
    var firstPlaceVote: Long = 1L,
    var minPercent : Float = 0f,
)
