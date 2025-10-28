/**
 * Copyright (C) 2025. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author parkboo@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.model.BaseModel
import org.json.JSONObject


interface HofsRepository {
    suspend fun get(
        code: String? = null,
        type: String? = null, // celeb
        category: String? = null, // celeb
        historyParam: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getTop1Count(
    ): Flow<BaseModel<JSONObject>>
}
