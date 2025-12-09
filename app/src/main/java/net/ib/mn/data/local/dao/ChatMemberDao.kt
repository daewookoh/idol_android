package net.ib.mn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import net.ib.mn.domain.model.ChatMemberModel

/**
 * DAO for chat room members
 *
 * old 프로젝트: app/src/main/java/net/ib/mn/chatting/chatDb/ChatMembersDao.kt
 */
@Dao
interface ChatMemberDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertMember(member: ChatMemberModel)

    @Insert(onConflict = REPLACE)
    suspend fun insertMembers(members: List<ChatMemberModel>)

    @Query("SELECT * FROM chat_members WHERE id=:userId")
    suspend fun getMember(userId: Int): ChatMemberModel?

    @Query("SELECT * FROM chat_members WHERE id=:userId AND room_id=:roomId")
    suspend fun getRoomMember(userId: Int, roomId: Int): ChatMemberModel?

    @Query("UPDATE chat_members SET nickname=:nickname, image_url=:imageUrl, level=:level, deleted=:deleted, role=:role WHERE id=:userId AND room_id=:roomId")
    suspend fun updateMember(
        userId: Int,
        roomId: Int,
        nickname: String?,
        imageUrl: String?,
        level: Int?,
        deleted: Boolean?,
        role: String
    )

    @Query("UPDATE chat_members SET deleted=:deleted, role=:role WHERE id=:userId AND room_id=:roomId")
    suspend fun updateDeletedMember(userId: Int, roomId: Int, deleted: Boolean?, role: String)

    @Query("SELECT * FROM chat_members WHERE room_id=:roomId")
    suspend fun getRoomMembers(roomId: Int): List<ChatMemberModel>?

    @Query("SELECT * FROM chat_members")
    suspend fun getAll(): List<ChatMemberModel>

    @Query("DELETE FROM chat_members")
    suspend fun deleteAll()
}
