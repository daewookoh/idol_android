package net.ib.mn.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import java.util.Date

/**
 * 채팅방 상세 정보 모델
 * Room Entity로 로컬 DB에 저장됨
 * 채팅방 입장 시 서버로부터 받아오는 상세 정보
 */
@Entity(tableName = "chat_room_info")
data class ChatRoomInfoModel(
    @PrimaryKey
    @ColumnInfo(name = "id") val roomId: Int,
    @ColumnInfo(name = "title") val title: String? = null,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "idol_id") val idolId: Int? = null,
    @ColumnInfo(name = "user_id") val userId: Int? = null,
    @ColumnInfo(name = "socket_url") val socketUrl: String? = null,
    @ColumnInfo(name = "cur_people") val curPeople: Int? = null,
    @ColumnInfo(name = "max_people") val maxPeople: Int? = null,
    @ColumnInfo(name = "level_limit") val levelLimit: Int? = null,
    @ColumnInfo(name = "total_msg_cnt") val totalMsgCnt: Int? = null,
    @ColumnInfo(name = "is_anonymity") val isAnonymity: String? = null,
    @ColumnInfo(name = "is_default") val isDefault: String? = null,
    @ColumnInfo(name = "is_most_only") val isMostOnly: String? = null,
    @ColumnInfo(name = "locale") val locale: String? = null,
    @ColumnInfo(name = "last_msg") val lastMsg: String? = null,
    @ColumnInfo(name = "last_msg_time") val lastMsgTime: Long? = null,
    @ColumnInfo(name = "created_at") val createdAt: Long? = null,
    @ColumnInfo(name = "success") val success: Boolean? = null,
    @ColumnInfo(name = "gcode") val gcode: Int? = null,
    @ColumnInfo(name = "account_id") val accountId: Int = 0
) : Serializable {

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
        get() = "${curPeople ?: 0}/${maxPeople ?: 0}"
}

/**
 * 채팅방 생성 요청 모델
 */
data class ChatRoomCreateRequest(
    val idolId: Int,
    val title: String,
    val desc: String? = null,
    val locale: String = "ko",
    val maxPeople: Int = 300,
    val levelLimit: Int = -1,
    val isAnonymity: String = "N",
    val isMostOnly: String = "N"
)

/**
 * 링크 메시지 내용 모델
 */
data class ChatLinkMessageModel(
    val originalMsg: String? = null,
    val url: String? = null,
    val title: String? = null,
    val description: String? = null,
    val imageUrl: String? = null
)
