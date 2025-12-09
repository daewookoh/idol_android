package net.ib.mn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import androidx.room.Transaction
import net.ib.mn.domain.model.ChatMessageModel

/**
 * DAO for chat messages
 *
 * old 프로젝트: app/src/main/java/net/ib/mn/chatting/chatDb/ChatDao.kt
 */
@Dao
interface ChatMessageDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertMessage(message: ChatMessageModel)

    @Insert(onConflict = REPLACE)
    suspend fun insertMessages(messages: List<ChatMessageModel>)

    @Query("SELECT * FROM chat_message")
    suspend fun getAll(): List<ChatMessageModel>

    @Query("SELECT * FROM chat_message WHERE user_id=:userId AND client_ts=:clientTs AND room_id=:roomId")
    suspend fun getMyMessage(userId: Int, clientTs: Long, roomId: Int): ChatMessageModel?

    @Query("SELECT * FROM chat_message WHERE user_id=:userId AND server_ts=:serverTs AND room_id=:roomId AND reported=:reported")
    suspend fun getReportedMessage(userId: Int, serverTs: Long, roomId: Int, reported: Boolean): ChatMessageModel?

    @Query("SELECT * FROM chat_message WHERE room_id=:roomId ORDER BY server_ts DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessages(roomId: Int, limit: Int, offset: Int): List<ChatMessageModel>

    @Query("SELECT server_ts FROM chat_message WHERE room_id=:roomId AND is_readable=:isReadable ORDER BY server_ts DESC")
    suspend fun getLatestServerTs(roomId: Int, isReadable: Boolean): List<Long>

    @Transaction
    @Query("UPDATE chat_message SET server_ts=:receiveServerTs, status=:status, is_readable=:isReadable, status_failed=:statusFailed WHERE user_id=:userId AND client_ts=:clientTs AND room_id=:roomId")
    suspend fun updateMessageStatus(
        userId: Int,
        clientTs: Long,
        roomId: Int,
        status: Boolean,
        receiveServerTs: Long,
        isReadable: Boolean,
        statusFailed: Boolean
    ): Int

    @Transaction
    @Query("UPDATE chat_message SET content=:linkMessageContent, is_link_url=:isLinkUrl, content_type=:contentType WHERE user_id=:userId AND client_ts=:clientTs AND room_id=:roomId")
    suspend fun updateLinkMessage(
        userId: Int,
        clientTs: Long,
        roomId: Int,
        isLinkUrl: Boolean,
        linkMessageContent: String,
        contentType: String?
    )

    @Transaction
    @Query("UPDATE chat_message SET status_failed=:statusFailed, server_ts=:serverTs WHERE user_id=:userId AND client_ts=:clientTs AND room_id=:roomId")
    suspend fun updateStatusFailed(userId: Int, clientTs: Long, roomId: Int, statusFailed: Boolean, serverTs: Long): Int

    @Transaction
    @Query("UPDATE chat_message SET reported=:reported, content_type=:contentType WHERE user_id=:userId AND server_ts=:serverTs AND room_id=:roomId")
    suspend fun updateReported(userId: Int, roomId: Int, reported: Boolean, serverTs: Long, contentType: String?)

    @Transaction
    @Query("UPDATE chat_message SET deleted=:deleted, content_type=:contentType WHERE room_id=:roomId AND server_ts=:serverTs AND user_id=:userId")
    suspend fun updateMessageDeleted(roomId: Int, serverTs: Long, userId: Int, deleted: Boolean, contentType: String): Int

    @Query("DELETE FROM chat_message WHERE room_id=:roomId")
    suspend fun deleteRoomMessages(roomId: Int)

    @Query("DELETE FROM chat_message WHERE room_id=:roomId AND server_ts=:serverTs AND user_id=:userId")
    suspend fun deleteMessage(roomId: Int, serverTs: Long, userId: Int): Int

    @Query("DELETE FROM chat_message WHERE room_id=:roomId AND client_ts=:clientTs AND user_id=:userId AND status=:status")
    suspend fun deleteDuplicateMessage(roomId: Int, clientTs: Long, userId: Int, status: Boolean): Int

    @Query("DELETE FROM chat_message WHERE server_ts>:serverTs")
    suspend fun deleteUnreadableMessages(serverTs: Long)

    @Query("DELETE FROM chat_message")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM chat_message WHERE room_id=:roomId")
    suspend fun getMessagesCount(roomId: Int): Int

    @Query("SELECT * FROM chat_message WHERE room_id=:roomId AND is_first_join_msg=1 LIMIT 1")
    suspend fun getFirstJoinMessage(roomId: Int): ChatMessageModel?
}
