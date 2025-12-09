package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ChatMemberModel
import net.ib.mn.domain.model.ChatRoomCreateRequest
import net.ib.mn.domain.model.ChatRoomInfoModel
import net.ib.mn.domain.model.ChatRoomJoinResponse
import net.ib.mn.domain.model.ChatRoomLeaveResponse
import net.ib.mn.domain.model.ChatRoomListResponse

/**
 * 채팅 Repository 인터페이스
 * old 프로젝트: core/data/src/main/java/net/ib/mn/core/data/repository/ChatRepository.kt
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

    /**
     * 채팅방 개설
     */
    fun createChatRoom(request: ChatRoomCreateRequest): Flow<ApiResult<ChatRoomInfoModel>>

    /**
     * 채팅방 정보 조회
     */
    fun getChatRoomInfo(roomId: Int): Flow<ApiResult<ChatRoomInfoModel>>

    /**
     * 채팅방 삭제
     */
    fun deleteChatRoom(roomId: Int): Flow<ApiResult<Boolean>>

    /**
     * 채팅방 신고
     */
    fun reportChatRoom(roomId: Int, reason: String): Flow<ApiResult<Boolean>>

    /**
     * 채팅방 신고 여부 조회
     */
    fun isReportedChatRoom(roomId: Int, userId: Long): Flow<ApiResult<Boolean>>

    /**
     * 채팅방 참여자 목록 조회
     */
    fun getChatMembers(roomId: Int): Flow<ApiResult<List<ChatMemberModel>>>

    /**
     * 채팅방 특정 참여자 정보 조회
     */
    fun getChatMember(roomId: Int, userId: Long): Flow<ApiResult<ChatMemberModel?>>

    /**
     * 채팅방 이미지 업로드
     * old 프로젝트: imagesRepository.uploadImage()
     *
     * @param imageBytes 이미지 바이너리 데이터
     * @param maxSize 이미지 최대 크기 (기본값 1500)
     * @return 업로드 결과 (image_url, thumbnail_url, umjjal_url, thumb_height, thumb_width)
     */
    fun uploadChatImage(
        imageBytes: ByteArray,
        maxSize: Int = 1500
    ): Flow<ApiResult<ChatImageUploadResponse>>

    companion object {
        const val ORDER_BY_RECENT = 0    // 최신순
        const val ORDER_BY_TALK_COUNT = 1 // 대화 많은 순
    }
}

/**
 * 채팅 이미지 업로드 응답
 */
data class ChatImageUploadResponse(
    val imageUrl: String,
    val thumbnailUrl: String,
    val umjjalUrl: String,
    val thumbHeight: Int,
    val thumbWidth: Int
)
