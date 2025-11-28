package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ChatRoomJoinResponse
import net.ib.mn.domain.model.ChatRoomLeaveResponse
import net.ib.mn.domain.model.ChatRoomListResponse

/**
 * 채팅 Repository 인터페이스
 */
interface ChatRepository {

    /**
     * 내가 참여한 채팅방 목록 조회
     */
    fun getJoinedChatRooms(
        idolId: Int,
        locale: String? = null,
        orderBy: Int = ORDER_BY_RECENT,
        limit: Int = 30,
        offset: Int = 0
    ): Flow<ApiResult<ChatRoomListResponse>>

    /**
     * 전체 채팅방 목록 조회
     */
    fun getAllChatRooms(
        idolId: Int,
        locale: String? = null,
        orderBy: Int = ORDER_BY_RECENT,
        limit: Int = 30,
        offset: Int = 0
    ): Flow<ApiResult<ChatRoomListResponse>>

    /**
     * 채팅방 입장
     */
    fun joinChatRoom(roomId: Int): Flow<ApiResult<ChatRoomJoinResponse>>

    /**
     * 채팅방 나가기
     */
    fun leaveChatRoom(roomId: Int): Flow<ApiResult<ChatRoomLeaveResponse>>

    companion object {
        const val ORDER_BY_RECENT = 0    // 최신순
        const val ORDER_BY_TALK_COUNT = 1 // 대화 많은 순
    }
}
