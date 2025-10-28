/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.GiveHeartToIdolDTO
import net.ib.mn.core.data.model.GiveHeartModel
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query


/**
 * @see
 * */
interface IdolsApi {

    @GET("idols/")
    suspend fun getIdols(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query(value = "fields", encoded = true) fields: String? = null, // ,를 인코딩하지 않고 그냥 보낸다
    ): String

    /**
     * 아이돌에 투표
     */
    @POST("idols/give_heart/")
    suspend fun postGiveHeartToIdol(
        @Body body: GiveHeartToIdolDTO
    ): GiveHeartModel

    @GET("idols/")
    suspend fun getIdolsWithTs(
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query(value = "fields", encoded = true) fields: String? = null, // ,를 인코딩하지 않고 그냥 보낸다
    ): Response<ResponseBody>

    @GET("idols/charity_count/")
    suspend fun getCharityCount(
    ): String

    /**
     * 퀴즈용 그룹 리스트
     */
    @GET("idols/list_for_quiz/")
    suspend fun getGroupsForQuiz(
    ): Response<ResponseBody>

    /**
     * 투표 후 아이돌 리스트 바꿔주기
     */
    @GET("idols/idol_by_ids/")
    suspend fun getIdolsByIds(
        @Query("ids") idolIds: String? = null,
        @Query("fields", encoded = true) fields: String? = null, // ,를 인코딩하지 않고 그냥 보낸다
    ): Response<ResponseBody>

    @GET("idols/list_for_search/")
    suspend fun getIdolsForSearch(
        @Query("idol_id") idolId: Int? = null,
    ): Response<ResponseBody>

    @GET("idols/list_for_excluded/")
    suspend fun getExcludedIdols(
    ): Response<ResponseBody>

    @GET("idols/list_group_member/")
    suspend fun getGroupMembers(
        @Query("group_id") groupId: Int,
    ): Response<ResponseBody>

    @GET("idols/wiki_name/")
    suspend fun getWikiName(
        @Query("idol_id") idolId: Int,
        @Query("locale") locale: String,
    ): Response<ResponseBody>

    @GET("idols/charity_history/")
    suspend fun getCharityHistory(
        @Query("type") type: String,
        @Query("locale") locale: String,
    ): Response<ResponseBody>

    @GET("idols/super_rookie_history/")
    suspend fun getSuperRookieHistory(
        @Query("locale") locale: String,
    ): Response<ResponseBody>

    // award
    @GET("idols/")
    suspend fun getAwardIdols(
        @Query("chart_code") chartCode: String? = null,
        @Query(value = "fields", encoded = true) fields: String? = null, // ,를 인코딩하지 않고 그냥 보낸다
    ): Response<ResponseBody>
}