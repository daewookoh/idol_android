package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable


data class UserBlockModel (
    @SerializedName("user_id") val id: Int =0,
    @SerializedName("email") val email : String="",
    @SerializedName("nickname") val nickname : String="",
    @SerializedName("level") val level: Int =0,
    @SerializedName("most_id") val mostId: Int=0,
    @SerializedName("image_url") val imageUrl : String="",
    var isBlocked:String = "Y"
):Serializable