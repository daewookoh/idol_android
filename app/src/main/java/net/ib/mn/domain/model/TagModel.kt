package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 자유게시판 태그 모델
 */
data class TagModel(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("name")
    val name: String = "",
    @SerializedName("admin_only")
    val adminOnly: String = "N",
    var selected: Boolean = false
)
