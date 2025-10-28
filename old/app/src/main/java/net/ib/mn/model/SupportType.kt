package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import net.ib.mn.core.model.SupportAdTypeListModel

data class SupportType(
    @SerializedName("id") val id: Int = -1,
    @SerializedName("name") val name: String = "",
    @SerializedName("category") val category: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("goal") val goal: Int = 0,
    @SerializedName("guide") val guide: String = "",
    @SerializedName("icon_url") val iconUrl: String = "",
    @SerializedName("image_url") val imageUrl: String = "",
    @SerializedName("image_url2") val imageUrl2: String = "",
    @SerializedName("is_viewable") val isViewable: String = "",
    @SerializedName("location") val location: String = "",
    @SerializedName("location_image_url") val locationImageUrl: String? = null,
    @SerializedName("location_map_url") val locationMapUrl: String? = null,
    @SerializedName("period") val period: String = "",
    @SerializedName("require") val require: Int = 0
) {
    fun toSupportAdTypeListModel(): SupportAdTypeListModel {
        return  SupportAdTypeListModel(
            id = id,
            name = name,
            category = category,
            description = description,
            goal = goal,
            guide = guide,
            iconUrl = iconUrl,
            imageUrl = imageUrl,
            imageUrl2 = imageUrl2,
            isViewable = isViewable,
            location = location,
            locationMapUrl = locationMapUrl,
            locationImageUrl = locationImageUrl,
            period = period,
            require = require
        )
    }
}