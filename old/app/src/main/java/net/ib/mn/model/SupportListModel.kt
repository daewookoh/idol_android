package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable
import java.util.*

data class SupportListModel (
        @SerializedName("article") val article : ArticleModel,
        @SerializedName("created_at") val created_at : Date,
        @SerializedName("d_day") val d_day : Date,
        @SerializedName("diamond") var diamond : Int,
        @SerializedName("expired_at") val expired_at : Date,
        @SerializedName("goal") val goal : Int,
        @SerializedName("id") val id : Int,
        @SerializedName("idol_id") val idol_id :Int,
        @SerializedName("idol") var idol : IdolModel,
        @SerializedName("image_url") val image_url : String,
        @SerializedName("like") var like : Boolean,
        @SerializedName("status") val status : Int,
        @SerializedName("title") val title : String,
        @SerializedName("type") val type : SupportType,
        @SerializedName("type_id") val type_id : Int,
        @SerializedName("user") val user : UserModel
) : Serializable {
        constructor() :this(
                article = ArticleModel(),
                created_at = Date(),
                d_day =  Date(),
                diamond = -1,
                expired_at = Date(),
                goal = -1,
                id = -1,
                idol_id = -1,
                idol = IdolModel(),
                image_url = "",
                like = false,
                status = -2,
                title = "",
                type = SupportType(-1, "", ""),
                type_id = -1,
                user = UserModel()
        )
}