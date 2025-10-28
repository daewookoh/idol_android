/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject


interface SearchRepository {
    suspend fun search(
        keyword: String?,
        category: String?,
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getTrend(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getSuggest(
        q: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
