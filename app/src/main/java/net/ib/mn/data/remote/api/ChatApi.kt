package net.ib.mn.data.remote.api

import net.ib.mn.data.remote.dto.ChatRoomJoinRequest
import net.ib.mn.data.remote.dto.ChatRoomLeaveRequest
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * 채팅 관련 API
 * old 프로젝트의 ChatApi.kt 참고
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
}
