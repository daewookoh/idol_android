package net.ib.mn.chatting.chatDb

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import net.ib.mn.chatting.model.ChatRoomListModel

@Dao
interface ChatRoomListDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChatRoom(chatRoom: ChatRoomListModel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertChatRooms(chatRooms: List<ChatRoomListModel>)

    @Query("SELECT * FROM chat_rooms WHERE id=:roomId ")
    suspend fun getChatRoom(roomId: Int?): ChatRoomListModel?

    @Query("SELECT * FROM chat_rooms WHERE id=:roomId ")
    fun getChatRoomForNotification(roomId: Int?): ChatRoomListModel?

    @Query("SELECT * FROM chat_rooms WHERE id=:roomId ")
    fun getChatRoomPush(roomId: Int?): ChatRoomListModel?

    @Query("SELECT * FROM chat_rooms")
    fun getAll(): List<ChatRoomListModel>

    @Query("SELECT * FROM chat_rooms WHERE is_JoinedRoom=:isJoinedRoom")
    fun getJoinedRoom (isJoinedRoom:Int?):List<ChatRoomListModel>

    @Query("DELETE FROM chat_rooms")
    fun deleteAll()

    @Query("DELETE FROM chat_rooms where id=:roomId")
    fun deleteRoom(roomId: Int)

    //아이돌 아이디로  채팅룸을 지워줌.
    @Query("DELETE FROM chat_rooms where idol_id=:idolId")
    fun deleteRoomWithIdolId(idolId: Int)

    //아이돌 아이디로  채팅룸 검색
    @Query("SELECT * FROM chat_rooms where idol_id=:idolId")
    fun getChatRoomWithIdolId(idolId: Int):List<ChatRoomListModel>

    //현재 로그인한 아이디를 뽑아서 아이디와 관련된 메시지를 삭제해줍니다.
    @Query("DELETE FROM chat_rooms")
    fun deleteAccountId()

}