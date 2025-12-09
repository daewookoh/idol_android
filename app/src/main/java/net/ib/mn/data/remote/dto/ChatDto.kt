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
 * 채팅방 개설 요청
 */
data class ChatRoomCreateRequest(
    @SerializedName("idol_id")
    val idolId: Int,
    @SerializedName("title")
    val title: String,
    @SerializedName("desc")
    val desc: String? = null,
    @SerializedName("locale")
    val locale: String = "ko",
    @SerializedName("max_people")
    val maxPeople: Int = 300,
    @SerializedName("level_limit")
    val levelLimit: Int = -1,
    @SerializedName("is_anonymity")
    val isAnonymity: String = "N",
    @SerializedName("is_most_only")
    val isMostOnly: String = "N"
)

/**
 * 채팅방 신고 요청
 */
data class ChatRoomReportRequest(
    @SerializedName("room_id")
    val roomId: Int,
    @SerializedName("reason")
    val reason: String
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

/**
 * 채팅방 상세정보 DTO (API 응답)
 */
data class ChatRoomInfoDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("title")
    val title: String? = null,
    @SerializedName("desc")
    val description: String? = null,
    @SerializedName("idol_id")
    val idolId: Int? = null,
    @SerializedName("user_id")
    val userId: Int? = null,
    @SerializedName("socket_url")
    val socketUrl: String? = null,
    @SerializedName("cur_people")
    val curPeople: Int? = null,
    @SerializedName("max_people")
    val maxPeople: Int? = null,
    @SerializedName("level_limit")
    val levelLimit: Int? = null,
    @SerializedName("total_msg_cnt")
    val totalMsgCnt: Int? = null,
    @SerializedName("is_anonymity")
    val isAnonymity: String? = null,
    @SerializedName("is_default")
    val isDefault: String? = null,
    @SerializedName("is_most_only")
    val isMostOnly: String? = null,
    @SerializedName("locale")
    val locale: String? = null,
    @SerializedName("last_msg")
    val lastMsg: String? = null,
    @SerializedName("last_msg_time")
    val lastMsgTime: Long? = null,
    @SerializedName("created_at")
    val createdAt: Long? = null,
    @SerializedName("success")
    val success: Boolean? = null,
    @SerializedName("gcode")
    val gcode: Int? = null
)

/**
 * 채팅방 멤버 DTO (API 응답)
 */
data class ChatMemberDto(
    @SerializedName("id")
    val id: Int = 0,
    @SerializedName("nickname")
    val nickname: String? = null,
    @SerializedName("image_url")
    val imageUrl: String? = null,
    @SerializedName("level")
    val level: Int? = null,
    @SerializedName("role")
    val role: String? = null,
    @SerializedName("most_id")
    val mostId: Int? = null
)
