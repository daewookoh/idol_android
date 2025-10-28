/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import net.ib.mn.core.data.dto.ApproveQuizDTO
import net.ib.mn.core.data.dto.ContinueQuizDTO
import net.ib.mn.core.data.dto.QuizAnswerDTO
import net.ib.mn.core.data.dto.ReportQuizDTO
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.data.model.CurrentChartResponse
import net.ib.mn.core.data.model.ObjectsBaseDataModel
import net.ib.mn.core.data.model.QuizTodayModel
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query


/**
 * Quiz api
 * */

interface QuizApi {

    /**
     * 현재 진행중인 차트 정보 가져오기
     * */
    @GET("quiz/today/")
    suspend fun getQuizToday(): QuizTodayModel

    /**
     * 퀴즈 답 제출
     */
    @POST("quiz/answer/")
    suspend fun postQuizAnswer(
        @Body body: QuizAnswerDTO
    ): Response<ResponseBody>

    @GET("quiz/ranking/{path}/")
    suspend fun getQuizRanking(
        @Path(value="path") path: String? = null,
        @Query("date") date: String? = null,
        @Query("idol_id") idolId: Int? = null,
        @Query("type") type: String? = null,
    ): Response<ResponseBody>

    /**
     * 퀴즈 작성(multipart)
     */
    @Multipart
    @POST("quiz/")
    suspend fun writeQuiz(
        @Part content: MultipartBody.Part,
        @Part choice1: MultipartBody.Part,
        @Part choice2: MultipartBody.Part,
        @Part choice3: MultipartBody.Part,
        @Part choice4: MultipartBody.Part,
        @Part answer: MultipartBody.Part,
        @Part description: MultipartBody.Part,
        @Part idolId: MultipartBody.Part,
        @Part level: MultipartBody.Part,
        @Part type: MultipartBody.Part? = null,
        @Part image: MultipartBody.Part? = null,
    ): Response<ResponseBody>

    /**
     * 채택된 퀴즈
     */
    @GET("quiz/?myown=1")
    suspend fun getAcceptedQuiz(): Response<ResponseBody>

    /**
     * 내가 작성한 퀴즈
     */
    @GET("quiz/my_list/")
    suspend fun getMyQuizList(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int,
        @Query("is_viewable") isViewable: String,
    ): Response<ResponseBody>

    /**
     * 퀴즈 채택 보상 받기
     */
    @POST("quiz/claim/")
    suspend fun claimQuizReward(): Response<ResponseBody>

    /**
     * 퀴즈 도전 횟수 추가
     */
    @POST("quiz/plus/")
    suspend fun plusQuizChallenge(): Response<ResponseBody>

    /**
     * 퀴즈 이어하기
     */
    @POST("quiz/continue/")
    suspend fun continueQuiz(
        @Body body: ContinueQuizDTO,
    ): Response<ResponseBody>

    /**
     * 리뷰할 퀴즈 리스트
     */
    @GET("quiz/review/")
    suspend fun getQuizReviewList(
        @Query("session") session: Int,
        @Query("debug") debug: String? = null,
    ): Response<ResponseBody>

    /**
     * 퀴즈 승인
     */
    @POST("quiz/approve/")
    suspend fun approveQuiz(
        @Body body: ApproveQuizDTO,
    ): Response<ResponseBody>

    /**
     * 퀴즈 신고
     */
    @POST("quiz/report/")
    suspend fun reportQuiz(
        @Body body: ReportQuizDTO,
    ): Response<ResponseBody>

    /**
     * 퀴즈 타입 (셀럽)
     */
    @GET("quiz/type_list/")
    suspend fun getQuizTypeList(): Response<ResponseBody>

    /**
     * 퀴즈 문제 리스트 가져오기
     */
    @GET("quiz/show/")
    suspend fun getQuizList(
        @Query("idol") idolId: Int?,
        @Query("type") type: String?,
    ): Response<ResponseBody>
}