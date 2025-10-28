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
import net.ib.mn.core.model.SupportAdTypeListModel
import org.json.JSONObject
import javax.inject.Inject


/**
 * @see
 * */

class GetConfigSelfUseCase @Inject constructor(
    private val repository: ConfigRepository
) {
    suspend operator fun invoke(): Flow<BaseModel<JSONObject>> =
        repository.getConfigSelf()
}