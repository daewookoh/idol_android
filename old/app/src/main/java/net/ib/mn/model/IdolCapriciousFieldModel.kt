package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import net.ib.mn.domain.model.IdolFiledData
import java.io.Serializable

data class IdolCapriciousFieldModel(
    @SerializedName("id") val id: Int,  // Idol id
    @SerializedName("heart") val heart: Long,
    @SerializedName("top3") val top3: String?,
    @SerializedName("top3_type") val top3Type: String?,
    @SerializedName("top3_image_ver") val top3ImageVer: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_url2") val imageUrl2: String? = null,
    @SerializedName("image_url3") val imageUrl3: String? = null
) : Serializable {
    companion object {
        const val TYPE_PHOTO = "P"
        const val TYPE_VIDEO = "V"
    }
}

fun IdolCapriciousFieldModel.toDomain(): IdolFiledData {
    return IdolFiledData(
        id = id,
        heart = heart,
        top3 = top3,
        top3Type = top3Type,
        top3ImageVer = top3ImageVer,
        imageUrl = imageUrl,
        imageUrl2 = imageUrl2,
        imageUrl3 = imageUrl3
    )
}