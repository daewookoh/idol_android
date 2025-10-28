package net.ib.mn.core.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TypeListModel(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") var name: String = "",
    @SerialName("type") var type: String? = null,
    @SerialName("type_name") var typeName: String = "",
    @SerialName("is_viewable") var isViewable: String = "",
    @SerialName("ui_color") val uiColor: String = "",
    @SerialName("ui_color_darkmode") val uiColorDarkmode: String = "",
    @SerialName("font_color") val fontColor: String = "",
    @SerialName("font_color_darkmode") val fontColorDarkmode: String = "",
    var isDivided: String = "N",
    var isFemale: Boolean = false,
    var showDivider: Boolean = false, // 구분선 보여줄지 말지. 해외배우 카테고리가 끼어들어가면서 isDivided로 하기 어려움
)