package net.ib.mn.data.model

import com.google.gson.annotations.SerializedName

/**
 * 랭킹 페이지의 탭 타입 정보
 * old 프로젝트의 TypeListModel과 동일한 구조
 */
data class TypeListModel(
    @SerializedName("id")
    val id: Int = 0,

    @SerializedName("name")
    var name: String = "",

    @SerializedName("type")
    var type: String? = null,

    @SerializedName("type_name")
    var typeName: String = "",

    // 차트 코드 (API에서 받은 원본 code: "SOLO_M", "PR_G_M" 등)
    var code: String? = null,

    @SerializedName("is_viewable")
    var isViewable: String = "",

    @SerializedName("ui_color")
    val uiColor: String = "",

    @SerializedName("ui_color_darkmode")
    val uiColorDarkmode: String = "",

    @SerializedName("font_color")
    val fontColor: String = "",

    @SerializedName("font_color_darkmode")
    val fontColorDarkmode: String = "",

    var isDivided: String = "N",

    var isFemale: Boolean = false,

    var showDivider: Boolean = false
)
