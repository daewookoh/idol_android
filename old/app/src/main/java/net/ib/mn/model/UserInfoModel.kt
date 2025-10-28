package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class UserInfoModel(
        @SerializedName("email") val email: String ="",
        @SerializedName("heart") val heart: Int =0,
        @SerializedName("id") val id: Int=0,
        @SerializedName("image_url") val imageUrl : String="",
        @SerializedName("item_no") val itemNo : Int =0,
        @SerializedName("level") val level : Int =0,
        @SerializedName("most_id") val mostId : Int =0,
        @SerializedName("nickname") val nickname : String="",
        @SerializedName("ts") var ts : Int =0
):Serializable
