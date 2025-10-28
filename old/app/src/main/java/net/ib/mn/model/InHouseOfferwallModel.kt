package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class InHouseOfferwallModel(
        @SerializedName("package") var _package: String,
        @SerializedName("bundle_id") var bundleId: String,
        @SerializedName("ad_id") var adId: Int,
        @SerializedName("desc") var desc: String,
        @SerializedName("icon") var icon: String,
        @SerializedName("key") var key: String,
        @SerializedName("type") var type: String,
        @SerializedName("heart") var heart: Int,
        @SerializedName("callback_url") var callbackUrl: String,
        @SerializedName("title") var title: String,
        @SerializedName("url") var url: String,
        @SerializedName("tag") var tag: String,
        @SerializedName("android_link") val androidLink : String
) : Serializable {
    companion object {
        const val TYPE_APP = "app"
        const val TYPE_VIDEO = "video"
        const val TYPE_LINK = "link"
    }
}