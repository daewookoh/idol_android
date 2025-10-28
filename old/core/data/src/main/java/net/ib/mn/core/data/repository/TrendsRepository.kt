/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import kotlinx.coroutines.flow.Flow
import org.json.JSONObject


interface TrendsRepository {
    suspend fun dailyHistory(
        type: String? = null,
        category: String? = null,
        historyParam: String? = null,
        code: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun rank(
        type: String? = null,
        category: String? = null,
        league: String? = null,
        targetYM: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun recent(
        idolId: Int,
        code: String? = null,
        offset: Int? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun awardRank(
        chartCode: String? = null,
        event: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun awardRecent(
        type: String? = null,
        category: String? = null,
        event: String? = null,
        idolId: Int? = null,
        chartCode: String? = null,
        sourceApp: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun votesTop100(
        isCeleb: Boolean = false
    ): Flow<JSONObject>
}
