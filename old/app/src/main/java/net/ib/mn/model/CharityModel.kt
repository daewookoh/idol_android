package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CharityModel(
        @SerializedName("image_url") var imageUrl: String,
        @SerializedName("title") var title : String,
        @SerializedName("idol_name") var idolName : String,
        @SerializedName("group_name") var groupName : String,
        @SerializedName("link_url") var linkUrl : String
) : Serializable