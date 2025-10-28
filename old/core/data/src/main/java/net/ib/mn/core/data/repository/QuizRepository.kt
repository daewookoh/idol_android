/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.model.ObjectsBaseDataModel
import net.ib.mn.core.data.model.QuizTodayModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.model.AwardModel
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.CouponModel
import net.ib.mn.core.model.ObjectsModel
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.core.model.UpdateInfoModel
import org.json.JSONArray
import org.json.JSONObject


/**
 * @see
 * */

interface QuizRepository {
    suspend fun getQuizToday(): Flow<QuizTodayModel>
    suspend fun postQuizAnswer(
        corrects: String?,
        incorrects: String?,
        session: Int,
        idolId: Int?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getQuizRanking(
        index: Int,
        idolId: Int,
        type: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun writeQuiz(
        content: String,
        choice1: String,
        choice2: String,
        choice3: String,
        choice4: String,
        answer: Int,
        description: String,
        idolId: Int,
        level: Int,
        type: String? = null,
        image: ByteArray? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getAcceptedQuiz(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getMyQuizList(
        limit: Int,
        offset: Int,
        isViewable: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun claimQuizReward(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun plusQuizChallenge(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun continueQuiz(
        sessionId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getQuizReviewList(
        session: Int,
        debug: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun approveQuiz(
        sessionId: Int,
        quizId: Int,
        answerNumber: Int,
        answer: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun reportQuiz(
        quizId: Int,
        content: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getQuizTypeList(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getQuizList(
        idolId: Int?,
        type: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}