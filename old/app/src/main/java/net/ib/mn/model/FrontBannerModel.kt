package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class FrontBannerModel(
    @SerializedName("event_no") var eventNum : String,
    @SerializedName("go_url") var goUrl : String,
    @SerializedName("target_id") var targetId: Int,
    @SerializedName("target_menu") var targetMenu: String,
    @SerializedName("type") var type: String,
    @SerializedName("url") var url:String,
    @SerializedName("close") var isClosed:Boolean
):Serializable
