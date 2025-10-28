package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class ImageUrlModel(
        @SerializedName("id") val id: Int, // article Id
        @SerializedName("image_url") val imageUrl: String
) : Serializable