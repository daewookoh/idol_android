package net.ib.mn.domain.model

/**
 * 채팅방 모델
 * old 프로젝트의 ChatRoomListModel 참고
 */
data class ChatRoomModel(
    val roomId: Int = 0,
    val title: String = "",
    val desc: String? = null,
    val idolId: Int = 0,
    val isAnonymity: Boolean = false,
    val isDefault: Boolean = false,
    val isMostOnly: Boolean = false,
    val levelLimit: Int = 0,
    val curPeopleCount: Int = 0,
    val maxPeopleCount: Int = 0,
    val totalMsgCount: Int = 0,
    val lastMessage: String? = null,
    val lastMessageTime: String? = null,
    val locale: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val userId: Int = 0,
    val role: String? = null,
    val nickName: String? = null,
    val isJoinedRoom: Boolean = false
) {
    /**
     * 방장 여부 확인
     */
    val isOwner: Boolean
        get() = role == "O"
}

/**
 * 채팅방 목록 응답
 */
data class ChatRoomListResponse(
    val rooms: List<ChatRoomModel>,
    val totalCount: Int,
    val nextUrl: String?
)

/**
 * 채팅방 입장 응답
 */
data class ChatRoomJoinResponse(
    val success: Boolean,
    val nickname: String? = null,
    val userId: Int? = null,
    val message: String? = null
)

/**
 * 채팅방 나가기 응답
 */
data class ChatRoomLeaveResponse(
    val success: Boolean,
    val message: String? = null
)
