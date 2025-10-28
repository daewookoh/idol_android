package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class WallpaperModel(
    @SerializedName("idol_id") var idolId: Int = 0,
    @SerializedName("image_urls") var imageUrls: List<String>? = null,
    @SerializedName("total_count") var totalCount: Int = 0,
) : Parcelable