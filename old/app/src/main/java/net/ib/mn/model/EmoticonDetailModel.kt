package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class EmoticonDetailModel(
    @SerializedName("description") val description: String,
    @SerializedName("id") val id: Int,
    @SerializedName("image_url") var imageUrl: String,
    @SerializedName("thumbnail") var thumbnail: String,
    @SerializedName("title") val title: String,
    @SerializedName("order") val order: Int,
    var emoticonSetId: Int = -1,
    var filePath: String,
    var isSetCategoryImg:Boolean= false
) : Serializable

