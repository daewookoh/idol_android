package net.ib.mn.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.Serializable

/**
 * 채팅방 모델
 * Room Entity로 로컬 DB에 저장됨
 */
@Entity(tableName = "chat_rooms")
data class ChatRoomModel(
    @PrimaryKey
    @ColumnInfo(name = "id") val roomId: Int = 0,
    @ColumnInfo(name = "title") val title: String = "",
    @ColumnInfo(name = "desc") val desc: String? = null,
    @ColumnInfo(name = "idol_id") val idolId: Int = 0,
    @ColumnInfo(name = "is_anonymity") val isAnonymity: String? = "N",
    @ColumnInfo(name = "is_default") val isDefault: String? = "N",
    @ColumnInfo(name = "is_most_only") val isMostOnly: String? = "N",
    @ColumnInfo(name = "is_viewable") val isViewable: String? = "Y",
    @ColumnInfo(name = "level_limit") val levelLimit: Int = 0,
    @ColumnInfo(name = "cur_people") val curPeopleCount: Int = 0,
    @ColumnInfo(name = "max_people") val maxPeopleCount: Int = 0,
    @ColumnInfo(name = "total_msg_cnt") val totalMsgCount: Int = 0,
    @ColumnInfo(name = "last_msg") val lastMessage: String? = null,
    @ColumnInfo(name = "last_msg_time") val lastMessageTime: String? = null,
    @ColumnInfo(name = "locale") val locale: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "user_id") val userId: Int = 0,
    @ColumnInfo(name = "role") val role: String? = null,
    @ColumnInfo(name = "nickname") val nickName: String? = null,
    @ColumnInfo(name = "is_joined_room") val isJoinedRoom: Boolean = false,
    @ColumnInfo(name = "is_room_filter") val isRoomFilter: Boolean = false,
    @ColumnInfo(name = "account_id") val accountId: Int = 0,
    @ColumnInfo(name = "unread_count") val unreadCount: Int = 0,
    @ColumnInfo(name = "last_read_ts") val lastReadTs: Long = 0L
) : Serializable {

    companion object {
        // 역할 상수
        const val ROLE_OWNER = "O"
        const val ROLE_NORMAL = "N"
        const val ROLE_ADMIN = "A"
    }

    /**
     * 방장 여부 확인
     */
    val isOwner: Boolean
        get() = role == ROLE_OWNER

    /**
     * 익명 채팅방 여부
     */
    val isAnonymousRoom: Boolean
        get() = isAnonymity == "Y"

    /**
     * 기본 채팅방 여부
     */
    val isDefaultRoom: Boolean
        get() = isDefault == "Y"

    /**
     * 최애 전용 채팅방 여부
     */
    val isMostOnlyRoom: Boolean
        get() = isMostOnly == "Y"

    /**
     * 현재 인원 / 최대 인원 텍스트
     */
    val peopleCountText: String
        get() = "$curPeopleCount/$maxPeopleCount"
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

/**
 * 채팅방 필터 타입
 */
enum class ChatRoomFilterType(val value: Int, val orderBy: String) {
    RECENT(0, "-id"),           // 최신순
    MANY_TALK(1, "-total_msg_cnt")  // 대화 많은 순
}
