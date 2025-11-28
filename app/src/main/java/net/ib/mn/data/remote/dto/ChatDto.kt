package net.ib.mn.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * 채팅방 입장 요청
 */
data class ChatRoomJoinRequest(
    @SerializedName("room_id")
    val roomId: Int
)

/**
 * 채팅방 나가기 요청
 */
data class ChatRoomLeaveRequest(
    @SerializedName("room_id")
    val roomId: Int
)

/**
 * 채팅방 DTO (API 응답)
 */
data class ChatRoomDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("desc")
    val desc: String? = null,
    @SerializedName("idol_id")
    val idolId: Int? = null,
    @SerializedName("is_anonymity")
    val isAnonymity: String? = null,
    @SerializedName("is_default")
    val isDefault: String? = null,
    @SerializedName("is_most_only")
    val isMostOnly: String? = null,
    @SerializedName("level_limit")
    val levelLimit: Int? = null,
    @SerializedName("cur_people")
    val curPeople: Int? = null,
    @SerializedName("max_people")
    val maxPeople: Int? = null,
    @SerializedName("total_msg_cnt")
    val totalMsgCnt: Int? = null,
    @SerializedName("last_msg")
    val lastMsg: String? = null,
    @SerializedName("last_msg_time")
    val lastMsgTime: String? = null,
    @SerializedName("locale")
    val locale: String? = null,
    @SerializedName("created_at")
    val createdAt: String? = null,
    @SerializedName("updated_at")
    val updatedAt: String? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("nickname")
    val nickname: String? = null
)
