package net.ib.mn.chatting.model

import com.google.gson.annotations.SerializedName

data class ChatRoomJoinModel(
    @SerializedName("nickname")val nickName: String?= "",
    @SerializedName("room_id") val roomId: Int? = 0
)
