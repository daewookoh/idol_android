package net.ib.mn.domain.model

import com.google.gson.annotations.SerializedName

/**
 * 서포트 광고 타입 모델
 * old 프로젝트의 SupportAdTypeListModel과 동일한 구조
 */
data class SupportAdTypeModel(
    @SerializedName("id")
    val id: Int,

    @SerializedName("image_url")
    val imageUrl: String? = null,

    @SerializedName("image_url2")
    val imageUrl2: String? = null,

    @SerializedName("icon_url")
    val iconUrl: String? = null,

    @SerializedName("name")
    val name: String,

    @SerializedName("period")
    val period: String,

    @SerializedName("description")
    val description: String,

    @SerializedName("location")
    val location: String,

    @SerializedName("goal")
    val goal: Int,

    @SerializedName("require")
    val require: Int,

    @SerializedName("guide")
    val guide: String,

    @SerializedName("is_viewable")
    val isViewable: String,

    @SerializedName("category")
    val category: String,

    @SerializedName("location_image_url")
    val locationImageUrl: String? = null,

    @SerializedName("location_map_url")
    val locationMapUrl: String? = null
)

/**
 * 광고 기간 단위
 * old 프로젝트의 AdDate enum과 동일
 */
enum class AdDate(val value: String) {
    DAY("D"),
    WEEK("W"),
    MONTH("M")
}
