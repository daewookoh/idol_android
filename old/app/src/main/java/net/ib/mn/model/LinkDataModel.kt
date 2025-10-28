package net.ib.mn.model

import com.google.gson.annotations.SerializedName

data class LinkDataModel (
        @SerializedName("imageUrl") var imageUrl: String? = null,
        @SerializedName("title") var title: String? = null,
        @SerializedName("desc") var description: String? = null,
        @SerializedName("host") var host: String? = null,
        @SerializedName("url") var url: String? = null,
        @SerializedName("client_ts") var checkLinkTs: Long = 0L,
        @SerializedName("user_id") var checkLinkUserId:Int? =-1,
        @SerializedName("original_link_text") var originalLinkText:String? =null,
        var uriPath: String? = null,
        var hash: String? = null,
        var srcWidth: Int = 0,
        var srcHeight: Int = 0,
)