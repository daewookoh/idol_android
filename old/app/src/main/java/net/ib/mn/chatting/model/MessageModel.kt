package net.ib.mn.chatting.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "chat_message", primaryKeys = ["user_id", "server_ts"])
data class MessageModel(
    @ColumnInfo(name = "client_ts") @SerializedName("client_ts") var clientTs: Long,
    @ColumnInfo(name = "content") @SerializedName("content") var content: String,
    @ColumnInfo(name = "content_desc") var content_desc:String?,
    @ColumnInfo(name = "reported") var reported:Boolean,
    @ColumnInfo(name = "seq") var seq: Int,
    @ColumnInfo(name = "status") var status:Boolean=false,
    @ColumnInfo(name = "status_failed") var statusFailed:Boolean=false,
    @ColumnInfo(name = "is_readable") var isReadable: Boolean = true,
    @ColumnInfo(name = "room_id") var roomId: Int=-1,
    @ColumnInfo(name = "server_ts") @SerializedName("server_ts") var serverTs: Long,
    @ColumnInfo(name = "user_id") @SerializedName("user_id") var userId: Int,
    @ColumnInfo(name = "content_type") @SerializedName("content_type") var contentType: String?,
    @ColumnInfo(name = "receive_count") @SerializedName("receive_count") var receiveCount: Int,
    @ColumnInfo(name = "read_count") @SerializedName("read_count") var readCount: Int,
    @ColumnInfo(name = "deleted") @SerializedName("deleted") var deleted: Boolean = false,
    @ColumnInfo(name = "type") @SerializedName("type") var type: String?,
    @ColumnInfo(name = "reports") @SerializedName("reports") var reports: Int? = 0,
    @ColumnInfo(name = "is_first_join_msg") var isFirstJoinMsg:Boolean = false,
    @ColumnInfo(name = "is_link_url") var isLinkUrl:Boolean = false,
    @ColumnInfo(name = "account_id") var accountId: Int,
    var isLastCount :Boolean = false,
    var isSet: Boolean = false
) : Serializable {
    companion object {
        const val CHAT_TYPE_REPORTED = "reported"
        const val CHAT_TYPE_META = "meta"
        const val CHAT_TYPE_NORMAL = "chat"
        const val CHAT_TYPE_TEXT = "text/plain"
        const val CHAT_TYPE_IMAGE = "text/vnd.exodus.image"
        const val cHAT_TYPE_VIDEO = "text/vnd.exodus.video"
        const val CHAT_TYPE_TOAST = "text/vnd.exodus.toast" // toast 메시지.
        const val CHAT_TYPE_FATAL = "text/vnd.exodus.fatal" // 치명적 메시지. 팝업보여주고 채팅방 종료.
        const val CHAT_TYPE_LINK = "text/vnd.exodus.link" // 링크.
    }
}
