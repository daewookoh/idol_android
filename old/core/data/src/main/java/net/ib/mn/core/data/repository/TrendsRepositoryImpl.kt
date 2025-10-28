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
import net.ib.mn.core.data.api.TrendsApi
import org.json.JSONObject
import javax.inject.Inject


class TrendsRepositoryImpl @Inject constructor(
    private val trendsApi: TrendsApi
) : TrendsRepository, BaseRepository() {

    override suspend fun dailyHistory(
        type: String?,
        category: String?,
        historyParam: String?,
        code: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = trendsApi.dailyHistory(type, category, historyParam, code)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun rank(
        type: String?,
        category: String?,
        league: String?,
        targetYM: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = trendsApi.rank(type, category, league, targetYM)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun recent(
        idolId: Int,
        code: String?,
        offset: Int?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = trendsApi.recent(idolId, code, offset)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun awardRank(
        chartCode: String?,
        event: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = trendsApi.awardRank(chartCode, event)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun awardRecent(
        type: String?,
        category: String?,
        event: String?,
        idolId: Int?,
        chartCode: String?,
        sourceApp: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = trendsApi.awardRecent(type, category, event, idolId, chartCode, sourceApp)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun votesTop100(
        isCeleb: Boolean
    ): Flow<JSONObject> = flow {
        try {
            val response = trendsApi.mostVotesTop100(isCeleb)
            val jsonResponse = response.body()?.string()
            jsonResponse?.let {
                val jsonObject = JSONObject(it)
                emit(jsonObject)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        val json = JSONObject()
        json.put("error", e.message)
        emit(json)
    }
}
