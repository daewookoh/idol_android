package net.ib.mn.chatting.model

import com.google.gson.annotations.SerializedName

data class ChatRoomMemberModel(
        @SerializedName("id") val id: Int? = null,
        @SerializedName("image_url") val imageUrl: String? = null,
        @SerializedName("level") val level: Int? = null,
        @SerializedName("nickname") val nickname: String? = null,
        @SerializedName("role") val role: String? = null
)
