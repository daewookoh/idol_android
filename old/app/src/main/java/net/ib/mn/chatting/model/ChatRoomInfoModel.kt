package net.ib.mn.chatting.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.TypeConverters
import com.google.gson.annotations.SerializedName
import net.ib.mn.chatting.roomMigration.Converters
import net.ib.mn.model.UserModel
import java.io.Serializable
import java.util.*

@Entity(tableName = "chat_room_info", primaryKeys = ["id"])
data class ChatRoomInfoModel(
    @ColumnInfo(name = "created_at") @TypeConverters(Converters::class) @SerializedName("created_at") val createdAt: Date?,
    @ColumnInfo(name = "cur_people") @SerializedName("cur_people") val curPeople: Int?,
    @ColumnInfo(name = "description") @SerializedName("desc") val description: String?,
    @ColumnInfo(name = "gcode") @SerializedName("gcode") val gcode: Int?,
    @ColumnInfo(name = "idol_id") @SerializedName("idol_id") val idolId: Int?,
    @ColumnInfo(name = "is_anonymity") @SerializedName("is_anonymity") val isAnonymity: String?,
    @ColumnInfo(name = "is_default") @SerializedName("is_default") val isDefault: String?,
    @ColumnInfo(name = "is_most_only") @SerializedName("is_most_only") val isMostOnly: String?,
    @ColumnInfo(name = "last_msg") @SerializedName("last_msg") val lastMsg: String?,
    @ColumnInfo(name = "last_msg_time") @TypeConverters(Converters::class) @SerializedName("last_msg_time") val lastMsgTime: Date?,
    @ColumnInfo(name = "level_limit") @SerializedName("level_limit") val levelLimit: Int?,
    @ColumnInfo(name = "locale") @SerializedName("locale") val locale: String?,
    @ColumnInfo(name = "max_people") @SerializedName("max_people") val maxPeople: Int?,
    @ColumnInfo(name = "id") @SerializedName("id") val roomId: Int,
    @ColumnInfo(name = "socket_url") @SerializedName("socket_url") val socketUrl: String?,
    @ColumnInfo(name = "success") @SerializedName("success") val success: Boolean?,
    @ColumnInfo(name = "title") @SerializedName("title") var title: String?,
    @ColumnInfo(name = "total_msg_cnt") @SerializedName("total_msg_cnt") val totalMsgCnt: Int?,
    @ColumnInfo(name = "user_id") @SerializedName("user_id") val userId: Int?,
    @ColumnInfo(name = "account_id") var accountId: Int
) : Serializable