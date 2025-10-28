/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.AwardsApi
import net.ib.mn.core.data.api.TrendsApi
import org.json.JSONObject
import javax.inject.Inject


class AwardsRepositoryImpl @Inject constructor(
    private val awardsApi: AwardsApi
) : AwardsRepository, BaseRepository() {
    override suspend fun current(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = awardsApi.current()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun history(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = awardsApi.history()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }
}
