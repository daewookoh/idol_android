/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject

interface OnepickRepository {
    suspend fun get(
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun vote(
        id: Int,
        voteIds: String,
        voteType: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getResult(
        id: Int,
        status: Boolean? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun postOpenImagePickNotification(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
