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


interface AwardsRepository {
    suspend fun current(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun history(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
