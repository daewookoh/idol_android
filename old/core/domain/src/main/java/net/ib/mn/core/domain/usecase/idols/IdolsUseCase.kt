/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.domain.usecase.idols

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.model.GiveHeartModel
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.model.SupportAdTypeListModel
import org.json.JSONObject
import javax.inject.Inject


/**
 * @see
 * */

class GetIdolsWithFieldsUseCase @Inject constructor(
    private val repository: IdolsRepository
) {
    suspend operator fun invoke(
        type: String?,
        category: String?,
        fields: String
    ): Flow<BaseModel<JSONObject>> = repository.getIdols(type, category, fields)
}

class GiveHeartToIdolUseCase @Inject constructor(
    private val repository: IdolsRepository
) {
    suspend operator fun invoke(idolId: Int,
                                hearts: Long,
                                listener: (GiveHeartModel) -> Unit,
                                errorListener: (Throwable) -> Unit) =
        repository.giveHeartToIdol(idolId, hearts, listener, errorListener)
}