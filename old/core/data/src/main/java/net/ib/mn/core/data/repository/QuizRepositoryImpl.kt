/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import retrofit2.HttpException
import retrofit2.Callback
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.ArticlesApi
import net.ib.mn.core.data.api.QuizApi
import net.ib.mn.core.data.dto.ApproveQuizDTO
import net.ib.mn.core.data.dto.ContinueQuizDTO
import net.ib.mn.core.data.dto.QuizAnswerDTO
import net.ib.mn.core.data.dto.ReportQuizDTO
import net.ib.mn.core.data.model.ArticleLikeModel
import net.ib.mn.core.data.model.QuizTodayModel
import net.ib.mn.core.model.BaseModel
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Response


/**
 * @see
 * */

class QuizRepositoryImpl @Inject constructor(
    private val quizApi: QuizApi
) : QuizRepository, BaseRepository() {
    override suspend fun getQuizToday(): Flow<QuizTodayModel> {
        try {
            val response = quizApi.getQuizToday()
            return flow {
                emit(response)
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun postQuizAnswer(
        corrects: String?,
        incorrects: String?,
        session: Int,
        idolId: Int?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.postQuizAnswer(QuizAnswerDTO(corrects, incorrects, session, idolId))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getQuizRanking(
        index: Int,
        idolId: Int,
        type: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var path: String? = null
            var date: String? = null
            when(index) {
                0 -> {
                    path = "today"
                }
                1 -> {
                    path = "history"
                    date = "yesterday"
                }
                2 -> {
                    path = "top"
                }
            }
            val __idolId = if( idolId == 0 ) null else idolId
            val response = quizApi.getQuizRanking(path, date, __idolId, type)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun writeQuiz(
        content: String,
        choice1: String,
        choice2: String,
        choice3: String,
        choice4: String,
        answer: Int,
        description: String,
        idolId: Int,
        level: Int,
        type: String?,
        image: ByteArray?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var imagePart: MultipartBody.Part? = null
            image?.let {
                val requestFile = image.toRequestBody("image/jpeg".toMediaTypeOrNull())
                imagePart = MultipartBody.Part.createFormData("imagebin", "image.jpg", requestFile)
            }

            val response = quizApi.writeQuiz(
                content = MultipartBody.Part.createFormData("content", content),
                choice1 = MultipartBody.Part.createFormData("choice1", choice1),
                choice2 = MultipartBody.Part.createFormData("choice2", choice2),
                choice3 = MultipartBody.Part.createFormData("choice3", choice3),
                choice4 = MultipartBody.Part.createFormData("choice4", choice4),
                answer = MultipartBody.Part.createFormData("answer", answer.toString()),
                description = MultipartBody.Part.createFormData("description", description),
                idolId = MultipartBody.Part.createFormData("idol_id", idolId.toString()),
                level = MultipartBody.Part.createFormData("level", level.toString()),
                type = if( type != null) MultipartBody.Part.createFormData("type", type) else null,
                image = imagePart
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getAcceptedQuiz(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.getAcceptedQuiz()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getMyQuizList(
        limit: Int,
        offset: Int,
        isViewable: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.getMyQuizList(limit, offset, isViewable)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun claimQuizReward(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.claimQuizReward()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun plusQuizChallenge(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.plusQuizChallenge()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun continueQuiz(
        sessionId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.continueQuiz(ContinueQuizDTO(sessionId))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getQuizReviewList(
        session: Int,
        debug: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.getQuizReviewList(session, debug)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun approveQuiz(
        sessionId: Int,
        quizId: Int,
        answerNumber: Int,
        answer: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.approveQuiz(ApproveQuizDTO(sessionId, quizId, answerNumber, answer))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun reportQuiz(
        quizId: Int,
        content: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.reportQuiz(ReportQuizDTO(quizId, content))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getQuizTypeList(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.getQuizTypeList()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getQuizList(
        idolId: Int?,
        type: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = quizApi.getQuizList(idolId, type)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}