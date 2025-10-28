package net.ib.mn.chatting.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

/**
 * ProjectName: idol_app_renew
 *
 * Description:
 * 유저가 참여한  채팅방 리스트 데이터를  받기 위하 모델
 * */
@Entity(tableName = "chat_join_rooms")
data class ChatRoomJoinListModel(
    @field:PrimaryKey
    @ColumnInfo(name = "id") @SerializedName("id") val roomId:Int =0,
    @ColumnInfo(name = "created_at") @SerializedName("created_at") var createdAt: String?= null,
    @ColumnInfo(name = "cur_people") @SerializedName("cur_people") var curPeopleCount: Int? = 0,  // 현재 방  참여 인원
    @ColumnInfo(name = "desc") @SerializedName("desc") var desc: String? = "",
    @ColumnInfo(name = "idol_id") @SerializedName("idol_id") val idolId: Int? = 0,
    @ColumnInfo(name = "is_anonymity") @SerializedName("is_anonymity") val isAnonymity: String? = "",
    @ColumnInfo(name = "is_default") @SerializedName("is_default") val isDefault: String? = "",
    @ColumnInfo(name = "is_most_only") @SerializedName("is_most_only") val isMostOnly: String? = "",
    @ColumnInfo(name = "is_viewable") @SerializedName("is_viewable") val isViewable: String? = "",
    @ColumnInfo(name = "last_msg") @SerializedName("last_msg") val lastMessage: String? = null,
    @ColumnInfo(name = "last_msg_time") @SerializedName("last_msg_time") val lastMessageTime: String? = null,
    @ColumnInfo(name = "level_limit") @SerializedName("level_limit") val levelLimit: Int = 0,
    @ColumnInfo(name = "locale") @SerializedName("locale") val locale: String?="",
    @ColumnInfo(name = "max_people") @SerializedName("max_people") val maxPeopleCount: Int? = 0,
    @ColumnInfo(name = "title") @SerializedName("title") val title: String? = "",
    @ColumnInfo(name = "total_msg_cnt") @SerializedName("total_msg_cnt") val totalMsgCount: Int? = 0,
    @ColumnInfo(name = "updated_at") @SerializedName("updated_at") val updatedAt: String? = null,
    @ColumnInfo(name = "user_id") @SerializedName("user_id") val userId: Int? = 0,
    var isJoinedRoom: Boolean = false,//내가  참여한  채팅방인지 여부
    var nickName: String?= "",
    var isRoomFilter: Boolean =false

)
