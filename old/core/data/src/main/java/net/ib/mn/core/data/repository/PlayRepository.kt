/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject


interface PlayRepository {
    suspend fun getList(
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getInfo(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getToken(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getTopBanners(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun like(
        liveId: Int,
        heart: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
