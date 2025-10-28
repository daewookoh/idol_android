package net.ib.mn.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class LiveChatMessageModel(
    @SerializedName("client_ts") var clientTs: Long =0,
    @SerializedName("server_ts") var serverTs: Long =0,
    @SerializedName("user_id") var userId: Int =-1,
    @SerializedName("content_type") var contentType: String? = "",
    @SerializedName("content") var content: String = "",
    @SerializedName("live_id") var liveID: Int=-1,
    @SerializedName("sender_nickname") var senderNickName:String ="",
    @SerializedName("sender_image") var senderImage: String? ="",
    @SerializedName("sender_level") var senderLevel: Int? =0,
    @SerializedName("deleted") var deleted: Boolean? = false,
    @SerializedName("reports") var reports: Int = 0,//리포트된 횟수
    var isReported:Boolean= false,//신고된 메세지 보여줘야됨 여부 ->내가 신고한것들은 보여줘야됨.
    var isMineChat: Boolean = false
):Serializable
