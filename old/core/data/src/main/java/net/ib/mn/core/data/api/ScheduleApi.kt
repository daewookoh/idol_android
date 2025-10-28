/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.ScheduleVoteDTO
import net.ib.mn.core.data.dto.ScheduleWriteDTO
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query


/**
 * Quiz api
 * */

interface ScheduleApi {
    @GET("schedules/")
    suspend fun getSchedules(
        @Query("idol_id") idolId: Int,
        @Query("yearmonth") yearmonth: String? = null, // 아이콘만 가져오기(월 전체 스케줄)
        @Query("yearmonthday") yearmonthday: String? = null,
        @Query("includevotes") includevotes: Int? = null,
        @Query("locale") locale: String,
        @Query("only_icon") onlyIcon: String? = null,
    ): Response<ResponseBody>

    @POST("schedules/vote/")
    suspend fun postScheduleVote(
        @Body body: ScheduleVoteDTO
    ): Response<ResponseBody>

    @POST("schedules/")
    suspend fun postSchedule(
        @Body body: ScheduleWriteDTO
    ): Response<ResponseBody>

    @POST("schedules/{id}/")
    suspend fun editSchedule(
        @Path(value="id") id: Int,
        @Body body: ScheduleWriteDTO
    ): Response<ResponseBody>

    @DELETE("schedules/{id}/")
    suspend fun deleteSchedule(
        @Path(value="id") id: Int
    ): Response<ResponseBody>
}