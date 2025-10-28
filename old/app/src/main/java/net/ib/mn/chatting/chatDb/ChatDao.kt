package net.ib.mn.chatting.chatDb

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import net.ib.mn.chatting.model.MessageModel

@Dao
interface ChatDao {
    @Insert(onConflict = REPLACE)
    fun insertChatMessage(chatMessage: MessageModel)

    @Insert(onConflict = REPLACE)
    fun insertChatMessage(chatMessage: List<MessageModel>)

    @Query("SELECT * FROM chat_message")
    fun getAll(): List<MessageModel>

    @Query("SELECT * FROM chat_message WHERE user_id=:userId AND client_ts=:clientTs AND room_id=:roomId")
    fun getMyMessage(userId: Int, clientTs: Long, roomId: Int): MessageModel

    //reported 된
    @Query("SELECT * FROM chat_message WHERE user_id=:userId AND server_ts=:serverTs AND room_id=:roomId AND reported=:reported")
    fun getReportedMsg(userId: Int, serverTs: Long, roomId: Int, reported: Boolean): MessageModel?

    @Query("SELECT * FROM chat_message WHERE room_id=:roomId ORDER BY server_ts DESC LIMIT :limit OFFSET :offset")
    suspend fun getMessages(roomId: Int, limit: Int, offset: Int): List<MessageModel>

    @Query("SELECT server_ts FROM chat_message WHERE room_id=:roomId AND is_readable=:isReadable ORDER BY server_ts DESC")
    suspend fun getLatestServerts(roomId: Int, isReadable:Boolean): List<Long>

    @Transaction
    @Query("UPDATE chat_message set server_ts =:receiveServerTs , status =:status , is_readable =:isReadable , status_failed =:statusFailed WHERE user_id=:userId AND client_ts=:clientTs AND room_id=:roomId")
    fun update(userId: Int, clientTs: Long, roomId: Int, status: Boolean, receiveServerTs: Long, isReadable: Boolean, statusFailed: Boolean) : Int

    @Transaction
    @Query("UPDATE chat_message set content =:linkMessageContent , is_link_url=:isLinkUrl , content_type=:contentType WHERE user_id=:userId AND client_ts=:clientTs AND room_id=:roomId")
    fun updateLinkMessage(userId: Int, clientTs: Long, roomId: Int,isLinkUrl:Boolean,linkMessageContent:String,contentType: String?)

    @Transaction
    @Query("UPDATE chat_message set status_failed =:statusFailed , server_ts =:serverTs WHERE user_id=:userId AND client_ts=:clientTs AND room_id=:roomId")
    fun updateStatusFailed(userId: Int, clientTs: Long, roomId: Int, statusFailed: Boolean, serverTs: Long) : Int

    @Transaction
    @Query("UPDATE chat_message set reported =:reported, content_type =:contentType WHERE user_id=:userId AND server_ts=:serverTs AND room_id=:roomId")
    fun updateReported(userId: Int, roomId: Int, reported: Boolean, serverTs: Long, contentType: String?)

    //특정 채팅방의  대화 메세지를 지워준다.
    @Query("DELETE FROM chat_message WHERE room_id =:roomId")
    fun deleteChatRoomMessages(roomId: Int)

    //메시지 삭제된거(delete)업데이트용.
    @Query("UPDATE chat_message set deleted=:deleted , content_type=:contentType WHERE room_id =:roomId AND server_ts=:serverTs AND user_id =:userId")
    fun updateChatRoomMessage(roomId: Int, serverTs: Long, userId: Int, deleted: Boolean,contentType:String) : Int

    //메시지 진짜 삭제용.
    @Query("DELETE FROM chat_message  WHERE room_id =:roomId AND server_ts=:serverTs AND user_id =:userId")
    fun deleteChatRoomMessage(roomId: Int, serverTs: Long, userId: Int) : Int

    //메시지 진짜 삭제용.
    @Query("DELETE FROM chat_message  WHERE room_id =:roomId AND client_ts=:clientTs AND user_id =:userId AND status=:status")
    fun deleteDuplicateMessage(roomId: Int, clientTs: Long, userId: Int, status:Boolean) : Int

    //메시지 여러개 삭제용.
    @Query("DELETE FROM chat_message WHERE server_ts>:serverTs")
    fun deleteUnReadableMessages(serverTs: Long)

    //현재 로그인한 아이디를 뽑아서 아이디와 관련된 메시지를 삭제해줍니다.
    @Query("DELETE FROM chat_message")
    fun deleteAccountId()

    @Query("DELETE FROM chat_message")
    fun deleteAll()

    @Query("SELECT COUNT(*) FROM chat_message WHERE room_id=:roomId")
    suspend fun getMessagesCount(roomId: Int): Int
}
