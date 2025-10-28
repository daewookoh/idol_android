package net.ib.mn.chatting.chatDb

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import net.ib.mn.chatting.model.ChatMembersModel
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.UserModel

@Dao
interface ChatMembersDao {
    @Insert(onConflict = REPLACE)
    fun insertChatMember(chatMember: ChatMembersModel)

    @Insert(onConflict = REPLACE)
    suspend fun insertChatMembers(chatMembers: List<ChatMembersModel>)

    @Query("SELECT * FROM chat_members WHERE id=:userId")
    fun getChatMember(userId:Int) : ChatMembersModel

    @Query("SELECT * FROM chat_members WHERE id=:userId AND room_id=:roomId")
    suspend fun getChatRoomMember(userId: Int, roomId: Int) : ChatMembersModel?

    @Query("SELECT * FROM chat_members WHERE id=:userId AND room_id=:roomId")
    fun getChatRoomMemberForNotification(userId: Int, roomId: Int) : ChatMembersModel?
    @Query("UPDATE chat_members set nickname=:nickname, image_url=:imageUrl, level=:level , deleted=:deleted,  role=:role WHERE id=:userId AND room_id=:roomId")
    suspend fun updateChatMember(userId: Int?, roomId: Int?,  nickname: String?, imageUrl : String?, level : Int?,  deleted: Boolean?, role:String)

    @Query("UPDATE chat_members set deleted=:deleted , role=:role WHERE id=:userId AND room_id=:roomId")
    suspend fun updateDeletedMember(userId: Int?, roomId: Int?, deleted: Boolean?, role:String)

    @Query("SELECT * FROM CHAT_MEMBERS WHERE room_id=:roomId")
    suspend fun getChatMemberList(roomId: Int): List<ChatMembersModel>?

    //현재 로그인한 아이디를 뽑아서 아이디와 관련된 메시지를 삭제해줍니다.
    @Query("DELETE FROM chat_members")
    fun deleteAccountId()

    @Query("SELECT * FROM chat_members")
    fun getAll(): List<ChatMembersModel>

    @Query("DELETE FROM chat_members")
    fun deleteAll()

}
