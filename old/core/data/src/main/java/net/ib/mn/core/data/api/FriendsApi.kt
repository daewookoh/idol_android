/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.CancelFriendRequestDTO
import net.ib.mn.core.data.dto.DeleteFriendsDTO
import net.ib.mn.core.data.dto.FriendRequestDTO
import net.ib.mn.core.data.dto.GiveHeartDTO
import net.ib.mn.core.data.dto.RespondRequestDTO
import net.ib.mn.core.data.dto.WriteCommentDTO
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query


/**
 * @see
 * */

interface FriendsApi {

    /**
    * 친구 목록 불러오기
    */
    @GET("friends/self/")
    suspend fun getFriendsSelf(
    ): Response<ResponseBody>

    @POST("friends/give_heart/")
    suspend fun giveHeart(
        @Body body: GiveHeartDTO
    ): Response<ResponseBody>

    @POST("friends/give_all_heart/")
    suspend fun giveAllHeart(
        @Body body: GiveHeartDTO
    ): Response<ResponseBody>

    @POST("friends/receive_friendheart/")
    suspend fun receiveFriendHeart(
    ): Response<ResponseBody>

    @POST("friends/req/")
    suspend fun sendFriendRequest(
        @Body body: FriendRequestDTO
    ): Response<ResponseBody>

    @POST("friends/resp/")
    suspend fun respondFriendRequest(
        @Body body: RespondRequestDTO
    ): Response<ResponseBody>

    @POST("friends/resp_all/")
    suspend fun respondAllFriendRequest(
    ): Response<ResponseBody>

    @POST("friends/del/")
    suspend fun cancelFriendRequest(
        @Body body: CancelFriendRequestDTO
    ): Response<ResponseBody>

    @POST("friends/delete_list/")
    suspend fun deleteFriends(
        @Body ids: DeleteFriendsDTO
    ): Response<ResponseBody>

    @GET("friends/friend_info/")
    suspend fun getFriendInfo(
        @Query("friend_id") userId: Int
    ): Response<ResponseBody>
}