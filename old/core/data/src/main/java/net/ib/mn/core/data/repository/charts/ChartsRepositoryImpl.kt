/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.charts

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.ChartsApi
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.data.model.CurrentChartResponse
import net.ib.mn.core.model.BaseModel
import javax.inject.Inject


/**
 * @see
 * */

class ChartsRepositoryImpl @Inject constructor(
    private val chartsApi: ChartsApi
) : ChartsRepository {
    override suspend fun getChartsCurrent(): Flow<CurrentChartResponse> = flow {
        try {
            val result = chartsApi.getChartsCurrent()
            when {
                result.success -> {
                    emit(result)
                }

                else -> emit(CurrentChartResponse(success = false, message = result.message))
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(CurrentChartResponse(success = false, message = e.message))
    }

    override suspend fun getChartIdolIds(code: String): Flow<BaseModel<List<Int>>> = flow {
        try {
            val result = chartsApi.getChartIdolIds(code)
            when {
                result.success && result.data != null -> emit(BaseModel(data = result.data!!))
                result.success -> emit(BaseModel())
                else -> emit(BaseModel(message = result.msg))
            }
        } catch (e: Exception) {
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = e.message, error = e))
    }

    override suspend fun getChartRanks(code: String): Flow<BaseModel<List<AggregateRankModel>>> =
        flow {
            try {
                val result = chartsApi.getChartRanks(code)
                when {
                    result.success && result.data != null -> emit(BaseModel(data = result.data!!))
                    result.success -> emit(BaseModel())
                    else -> emit(BaseModel(message = result.msg))
                }
            } catch (e: Exception) {
                throw e
            }
        }.catch { e ->
            emit(BaseModel(message = e.message, error = e))
        }

    override suspend fun getIdolChartCodes(): Flow<BaseModel<Map<String, ArrayList<String>>>> =
        flow {
            try {
                val result = chartsApi.getIdolChartCodes()
                when {
                    result.success && result.data != null -> emit(BaseModel(data = result.data!!))
                    result.success -> emit(BaseModel())
                    else -> emit(BaseModel(message = result.msg))
                }
            } catch (e: Exception) {
                throw e
            }
        }.catch { e ->
            emit(BaseModel(message = e.message, error = e))
        }
}