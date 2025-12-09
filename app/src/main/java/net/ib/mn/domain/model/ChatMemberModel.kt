package net.ib.mn.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import java.io.Serializable

/**
 * 채팅방 멤버 모델
 * Room Entity로 로컬 DB에 저장됨
 */
@Entity(
    tableName = "chat_members",
    primaryKeys = ["id", "room_id"]
)
data class ChatMemberModel(
    @ColumnInfo(name = "id") val id: Int = 0,
    @ColumnInfo(name = "room_id") val roomId: Int = 0,
    @ColumnInfo(name = "nickname") val nickname: String = "",
    @ColumnInfo(name = "image_url") val imageUrl: String? = null,
    @ColumnInfo(name = "level") val level: Int = 0,
    @ColumnInfo(name = "role") val role: String = "N",
    @ColumnInfo(name = "most_id") val mostId: Int = -1,
    @ColumnInfo(name = "deleted") val deleted: Boolean = false,
    @ColumnInfo(name = "account_id") val accountId: Int = 0
) : Serializable {

    companion object {
        // 역할 상수
        const val ROLE_OWNER = "O"      // 방장
        const val ROLE_NORMAL = "N"     // 일반 참여자
        const val ROLE_ADMIN = "A"      // 관리자
    }

    /**
     * 방장 여부
     */
    val isOwner: Boolean
        get() = role == ROLE_OWNER

    /**
     * 관리자 여부 (방장 포함)
     */
    val isAdmin: Boolean
        get() = role == ROLE_OWNER || role == ROLE_ADMIN

    /**
     * 활성 멤버 여부 (삭제되지 않은 멤버)
     */
    val isActive: Boolean
        get() = !deleted
}
