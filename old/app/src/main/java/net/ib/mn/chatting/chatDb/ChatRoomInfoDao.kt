package net.ib.mn.chatting.chatDb

import androidx.room.*
import androidx.room.OnConflictStrategy.Companion.REPLACE
import net.ib.mn.chatting.model.ChatMembersModel
import net.ib.mn.chatting.model.ChatRoomInfoModel
import net.ib.mn.chatting.model.MessageModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.UserModel

@Dao
interface ChatRoomInfoDao {
    @Insert(onConflict = REPLACE)
    suspend fun insertChatRoomInfo(chatRoomInfo: ChatRoomInfoModel)

    @Insert(onConflict = REPLACE)
    fun insertChatRoomsInfo(chatRoomInfo: List<ChatRoomInfoModel>)

    @Query("SELECT * FROM chat_room_info WHERE id=:roomId")
    suspend fun getChatRoomInfo(roomId:Int): ChatRoomInfoModel

    @Query("SELECT * FROM chat_room_info")
    fun getAll(): List<ChatRoomInfoModel>

    @Query("DELETE FROM chat_room_info WHERE id=:roomId")
    fun deleteRoomInfo(roomId: Int)

    //현재 로그인한 아이디를 뽑아서 아이디와 관련된 메시지를 삭제해줍니다.
    @Query("DELETE FROM chat_room_info")
    fun deleteAccountId()

}
