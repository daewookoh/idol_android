/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.model.QuizTodayModel
import net.ib.mn.core.data.repository.QuizRepository
import net.ib.mn.core.data.repository.config.ConfigRepository
import net.ib.mn.core.model.BaseModel
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject


/**
 * @see
 * */
class GetQuizTodayUseCase @Inject constructor(
    private val repository: QuizRepository
) {
    suspend operator fun invoke(): Flow<QuizTodayModel> =
        repository.getQuizToday()
}