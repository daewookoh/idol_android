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
import net.ib.mn.core.data.repository.TrendsRepository
import net.ib.mn.core.data.repository.config.ConfigRepository
import net.ib.mn.core.model.BaseModel
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject


/**
 * 최다득표 탑 100
 * */
class TrendsTop100UseCase @Inject constructor(
    private val repository: TrendsRepository
) {
    suspend operator fun invoke(isCeleb: Boolean = false): Flow<JSONObject> =
        repository.votesTop100(isCeleb)
}