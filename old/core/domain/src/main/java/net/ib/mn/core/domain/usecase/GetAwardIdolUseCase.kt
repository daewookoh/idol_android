/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.domain.usecase

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.repository.config.ConfigRepository
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.IdolApiModel
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject


/**
 * @see
 * */
class GetAwardIdolUseCase @Inject constructor(
    private val repository: ConfigRepository
) {
    suspend operator fun invoke(chartCode: String): Flow<BaseModel<List<IdolApiModel>>> =
        repository.getAwardIdol(chartCode)
}