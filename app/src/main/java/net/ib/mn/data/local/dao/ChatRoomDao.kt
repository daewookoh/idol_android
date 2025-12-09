package net.ib.mn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ChatRoomModel

/**
 * DAO for chat rooms list
 *
 * old 프로젝트: app/src/main/java/net/ib/mn/chatting/chatDb/ChatRoomListDao.kt
 */
@Dao
interface ChatRoomDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertRoom(room: ChatRoomModel)

    @Insert(onConflict = REPLACE)
    suspend fun insertRooms(rooms: List<ChatRoomModel>)

    @Update
    suspend fun updateRoom(room: ChatRoomModel)

    @Query("SELECT * FROM chat_rooms WHERE id=:roomId")
    suspend fun getRoom(roomId: Int): ChatRoomModel?

    @Query("SELECT * FROM chat_rooms")
    suspend fun getAll(): List<ChatRoomModel>

    @Query("SELECT * FROM chat_rooms WHERE is_joined_room=1 ORDER BY last_msg_time DESC")
    suspend fun getJoinedRooms(): List<ChatRoomModel>

    @Query("SELECT * FROM chat_rooms WHERE is_joined_room=1 ORDER BY last_msg_time DESC")
    fun getJoinedRoomsFlow(): Flow<List<ChatRoomModel>>

    @Query("SELECT * FROM chat_rooms WHERE idol_id=:idolId AND is_joined_room=1 ORDER BY last_msg_time DESC")
    suspend fun getJoinedRoomsByIdolId(idolId: Int): List<ChatRoomModel>

    @Query("SELECT * FROM chat_rooms WHERE idol_id=:idolId AND is_joined_room=1 ORDER BY last_msg_time DESC")
    fun getJoinedRoomsByIdolIdFlow(idolId: Int): Flow<List<ChatRoomModel>>

    @Query("SELECT * FROM chat_rooms WHERE idol_id=:idolId")
    suspend fun getRoomsByIdolId(idolId: Int): List<ChatRoomModel>

    @Query("SELECT * FROM chat_rooms WHERE idol_id=:idolId")
    fun getRoomsByIdolIdFlow(idolId: Int): Flow<List<ChatRoomModel>>

    @Query("UPDATE chat_rooms SET unread_count=:unreadCount WHERE id=:roomId")
    suspend fun updateUnreadCount(roomId: Int, unreadCount: Int)

    @Query("UPDATE chat_rooms SET last_msg=:lastMessage, last_msg_time=:lastMessageTime WHERE id=:roomId")
    suspend fun updateLastMessage(roomId: Int, lastMessage: String?, lastMessageTime: String?)

    @Query("UPDATE chat_rooms SET last_read_ts=:lastReadTs, unread_count=0 WHERE id=:roomId")
    suspend fun markAsRead(roomId: Int, lastReadTs: Long)

    @Query("UPDATE chat_rooms SET is_joined_room=1 WHERE id=:roomId")
    suspend fun markAsJoined(roomId: Int)

    @Query("UPDATE chat_rooms SET is_joined_room=0 WHERE id=:roomId")
    suspend fun markAsLeft(roomId: Int)

    @Transaction
    suspend fun upsertRooms(rooms: List<ChatRoomModel>, isJoinedList: Boolean) {
        rooms.forEach { room ->
            val existingRoom = getRoom(room.roomId)
            if (existingRoom != null) {
                // 기존 방이 있으면 unreadCount와 lastReadTs 유지
                insertRoom(
                    room.copy(
                        unreadCount = existingRoom.unreadCount,
                        lastReadTs = existingRoom.lastReadTs,
                        isJoinedRoom = if (isJoinedList) true else existingRoom.isJoinedRoom
                    )
                )
            } else {
                insertRoom(room.copy(isJoinedRoom = isJoinedList))
            }
        }
    }

    @Query("DELETE FROM chat_rooms WHERE id=:roomId")
    suspend fun deleteRoom(roomId: Int)

    @Query("DELETE FROM chat_rooms WHERE idol_id=:idolId")
    suspend fun deleteRoomsByIdolId(idolId: Int)

    @Query("DELETE FROM chat_rooms")
    suspend fun deleteAll()
}
