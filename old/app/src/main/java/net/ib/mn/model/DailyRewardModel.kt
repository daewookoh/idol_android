package net.ib.mn.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class DailyRewardModel(
    @SerializedName("key") val key: String? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("item") val item: String? = null,   //heart, diamond
    @SerializedName("amount") val amount: Int = 0,
    @SerializedName("status") val status: String? = null,
    @SerializedName("image_url") val imageUrl: String? = null,
    @SerializedName("image_url_dark") val imageUrlDark: String? = null,
    @SerializedName("banner") val banner: DailyRewardBannerModel? = null,
    @SerializedName("link_url") val linkUrl: String? = null,
    @SerializedName("alert") val alert : String? = null,
    val desc: String? = null,
) : Parcelable

@Parcelize
data class DailyRewardBannerModel(
    @SerializedName("content") val content: String? = null,
    @SerializedName("width") val width: Int = 0,
    @SerializedName("height") val height: Int = 0,
) : Parcelable

@Parcelize
data class DailyRewardGuidLineModel(
    @SerializedName("desc") val desc: String? = null,
    @SerializedName("title") val title: String? = null,
) : Parcelable