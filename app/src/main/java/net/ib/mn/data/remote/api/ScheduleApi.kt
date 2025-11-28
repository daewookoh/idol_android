package net.ib.mn.data.remote.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ScheduleApi {

    @GET("schedules/")
    suspend fun getSchedules(
        @Query("idol_id") idolId: Int,
        @Query("yearmonth") yearMonth: String? = null,
        @Query("yearmonthday") yearMonthDay: String? = null,
        @Query("includevotes") includeVotes: Int? = null,
        @Query("locale") locale: String,
        @Query("only_icon") onlyIcon: String? = null
    ): Response<ResponseBody>

    @FormUrlEncoded
    @POST("schedules/vote/")
    suspend fun voteSchedule(
        @Field("schedule_id") scheduleId: Int,
        @Field("vote") vote: String
    ): Response<ResponseBody>

    @DELETE("schedules/{id}/")
    suspend fun deleteSchedule(
        @Path("id") scheduleId: Int
    ): Response<ResponseBody>
}
