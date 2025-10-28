/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject


interface NoticeEventRepository {
    suspend fun get(
        type: String,
        id: Int,
        isDark: Boolean,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
