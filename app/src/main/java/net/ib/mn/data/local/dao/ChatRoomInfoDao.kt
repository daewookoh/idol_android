package net.ib.mn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import net.ib.mn.domain.model.ChatRoomInfoModel

/**
 * DAO for chat room info (detailed room information)
 *
 * old 프로젝트: app/src/main/java/net/ib/mn/chatting/chatDb/ChatRoomInfoDao.kt
 */
@Dao
interface ChatRoomInfoDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertRoomInfo(roomInfo: ChatRoomInfoModel)

    @Insert(onConflict = REPLACE)
    suspend fun insertRoomsInfo(roomsInfo: List<ChatRoomInfoModel>)

    @Query("SELECT * FROM chat_room_info WHERE id=:roomId")
    suspend fun getRoomInfo(roomId: Int): ChatRoomInfoModel?

    @Query("SELECT * FROM chat_room_info")
    suspend fun getAll(): List<ChatRoomInfoModel>

    @Query("DELETE FROM chat_room_info WHERE id=:roomId")
    suspend fun deleteRoomInfo(roomId: Int)

    @Query("DELETE FROM chat_room_info")
    suspend fun deleteAll()
}
