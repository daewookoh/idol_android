package net.ib.mn.chatting.model

import com.google.gson.annotations.SerializedName

data class ChatMsgLinkModel(
    @SerializedName("imageUrl") var imageUrl: String? = null,
    @SerializedName("title") var title: String? = null,
    @SerializedName("desc") var description: String? = null,
    @SerializedName("url") var ogTagUrl: String? = null,
    @SerializedName("detectUrl") var detectUrl: String? = null,
    @SerializedName("content") var originalMsg:String? =null
)
