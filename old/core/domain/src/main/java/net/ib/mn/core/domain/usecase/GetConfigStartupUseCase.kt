/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.domain.usecase

import androidx.lifecycle.LiveData
import androidx.lifecycle.liveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import net.ib.mn.core.data.repository.config.ConfigRepository
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.ObjectsModel


/**
 * @see
 * */

class GetConfigStartupUseCase @Inject constructor(
    private val repository: ConfigRepository
) {
    suspend operator fun invoke(): Flow<BaseModel<ObjectsModel>> = repository.getConfigStartup()
}