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


interface MessagesRepository {
    suspend fun deleteByType(
        type: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun get(
        type: String? = null,
        after: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun delete(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun claim(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
