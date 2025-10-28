/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.dto.OnepickVoteDTO
import net.ib.mn.core.data.dto.ThemepickVoteDTO
import org.json.JSONObject

interface ThemepickRepository {
    suspend fun get(
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun vote(
        id: Int,
        idolId: Int,
        voteType: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getResult(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun postOpenThemePickNotification(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
