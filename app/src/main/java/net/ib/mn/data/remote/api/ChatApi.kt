package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ChatRoomCreateRequest
import net.ib.mn.data.remote.dto.ChatRoomJoinRequest
import net.ib.mn.data.remote.dto.ChatRoomLeaveRequest
import net.ib.mn.data.remote.dto.ChatRoomReportRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * 채팅 관련 API
 * old 프로젝트: core/data/src/main/java/net/ib/mn/core/data/api/ChatApi.kt
 */
interface ChatApi {

    /**
     * 채팅방 목록 (전체)
     */
    @GET("chat/")
    suspend fun getChatRoomList(
        @Query("idol") idolId: Int,
        @Query("locale") locale: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): Response<ResponseBody>

    /**
     * 내가 참여한 채팅방 목록
     */
    @GET("chat/joinlist/")
    suspend fun getChatRoomJoinList(
        @Query("idol") idolId: Int,
        @Query("locale") locale: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("limit") limit: Int = 30,
        @Query("offset") offset: Int = 0
    ): Response<ResponseBody>

    /**
     * 채팅방 입장
     */
    @POST("chat/join/")
    suspend fun joinChatRoom(
        @Body request: ChatRoomJoinRequest
    ): Response<ResponseBody>

    /**
     * 채팅방 나가기
     */
    @POST("chat/leave/")
    suspend fun leaveChatRoom(
        @Body request: ChatRoomLeaveRequest
    ): Response<ResponseBody>

    /**
     * 채팅방 개설
     */
    @POST("chat/")
    suspend fun createChatRoom(
        @Body request: ChatRoomCreateRequest
    ): Response<ResponseBody>

    /**
     * 채팅방 정보
     */
    @GET("chat/{id}/")
    suspend fun getChatRoomInfo(
        @Path("id") roomId: Int
    ): Response<ResponseBody>

    /**
     * 채팅방 삭제
     */
    @DELETE("chat/{id}/")
    suspend fun deleteChatRoom(
        @Path("id") roomId: Int
    ): Response<ResponseBody>

    /**
     * 채팅방 신고
     */
    @POST("chat/report/")
    suspend fun reportChatRoom(
        @Body request: ChatRoomReportRequest
    ): Response<ResponseBody>

    /**
     * 채팅방 신고 여부 조회
     */
    @GET("chat/report/")
    suspend fun isReportedChatRoom(
        @Query("room_id") roomId: Int,
        @Query("user_id") userId: Long
    ): Response<ResponseBody>

    /**
     * 채팅방 참여자 목록
     */
    @GET("chat/members/")
    suspend fun getChatMembers(
        @Query("room_id") roomId: Int
    ): Response<ResponseBody>

    /**
     * 채팅방 특정 참여자 정보
     */
    @GET("chat/member/")
    suspend fun getChatMember(
        @Query("room_id") roomId: Int,
        @Query("user_id") userId: Long
    ): Response<ResponseBody>
}
