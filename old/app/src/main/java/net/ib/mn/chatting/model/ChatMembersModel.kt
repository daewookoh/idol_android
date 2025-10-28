package net.ib.mn.chatting.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName
import java.io.Serializable

@Entity(tableName = "chat_members", primaryKeys = ["id", "room_id"])
data class ChatMembersModel(
    @ColumnInfo(name = "id") @SerializedName("id") var id: Int,
    @ColumnInfo(name = "image_url") @SerializedName("image_url") var imageUrl: String?,
    @ColumnInfo(name = "level") @SerializedName("level") var level: Int,
    @ColumnInfo(name = "nickname") @SerializedName("nickname") var nickname: String,
    @ColumnInfo(name = "role") @SerializedName("role") var role: String,
    @ColumnInfo(name = "room_id") @SerializedName("room_id") var roomId: Int,
    @ColumnInfo(name = "most_id") @SerializedName("most_id") var most:Int,
    @ColumnInfo(name = "deleted") var deleted: Boolean = false,
    @ColumnInfo(name = "account_id") var accountId: Int
) : Serializable
