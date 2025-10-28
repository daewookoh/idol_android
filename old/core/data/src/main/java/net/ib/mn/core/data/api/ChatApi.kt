/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.ChatRoomDTO
import net.ib.mn.core.data.dto.ReportChatRoomDTO
import net.ib.mn.core.data.model.ChatRoomCreateModel
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ChatApi {

    /**
     * 채팅방 개설
     * */
    @POST("chat/")
    suspend fun createChatRoom(
        @Body body: ChatRoomCreateModel
    ): Response<ResponseBody>

    /**
     * 채팅방 정보
     */
    @GET("chat/{id}/")
    suspend fun getChatRoomInfo(
        @Path(value="id") id: Int
    ): Response<ResponseBody>

    /**
     * 채팅방 삭제
     */
    @DELETE("chat/{id}/")
    suspend fun deleteChatRoom(
        @Path(value="id") id: Int
    ): Response<ResponseBody>

    /**
     * 채팅방 신고
     */
    @POST("chat/report/")
    suspend fun reportChatRoom(
        @Body body: ReportChatRoomDTO
    ): Response<ResponseBody>

    /**
     * 채팅방 신고여부
     */
    @GET("chat/report/")
    suspend fun isReportChatRoom(
        @Query("room_id") roomId: Int,
        @Query("user_id") userId: Long
    ): Response<ResponseBody>

    /**
     * 채팅방 나가기
     */
    @POST("chat/leave/")
    suspend fun leaveChatRoom(
        @Body body: ChatRoomDTO
    ): Response<ResponseBody>

    /**
     * 채팅방 참여자
     */
    @GET("chat/members/")
    suspend fun getChatMembers(
        @Query("room_id") roomId: Int
    ): Response<ResponseBody>

    /**
     * 채팅방 참여자 한 명 정보
     */
    @GET("chat/member/")
    suspend fun getChatMember(
        @Query("room_id") roomId: Int,
        @Query("user_id") userId: Long
    ): Response<ResponseBody>

    /**
     * 채팅방 목록
     */
    @GET("chat/")
    suspend fun getChatRoomList(
        @Query("idol") idolId: Int,
        @Query("locale") locale: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int,
    ): Response<ResponseBody>

    /**
     * 내가 참여한 방
     */
    @GET("chat/joinlist/")
    suspend fun getChatRoomJoinList(
        @Query("idol") idolId: Int,
        @Query("locale") locale: String? = null,
        @Query("order_by") orderBy: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int,
    ): Response<ResponseBody>

    /**
     * 채팅방 참여
     */
    @POST("chat/join/")
    suspend fun joinChatRoom(
        @Body body: ChatRoomDTO
    ): Response<ResponseBody>

}