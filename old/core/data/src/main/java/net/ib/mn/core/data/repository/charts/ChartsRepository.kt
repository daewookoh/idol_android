/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.charts

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.data.model.CurrentChartResponse
import net.ib.mn.core.model.ChartModel
import net.ib.mn.core.model.BaseModel


/**
 * @see
 * */

interface ChartsRepository {
    suspend fun getChartsCurrent(): Flow<CurrentChartResponse>
    suspend fun getChartIdolIds(code: String): Flow<BaseModel<List<Int>>>
    suspend fun getChartRanks(code: String): Flow<BaseModel<List<AggregateRankModel>>>
    suspend fun getIdolChartCodes(): Flow<BaseModel<Map<String, ArrayList<String>>>>
}