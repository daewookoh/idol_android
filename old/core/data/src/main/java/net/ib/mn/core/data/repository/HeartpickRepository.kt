/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject

interface HeartpickRepository {
    suspend fun get(
        id: Int,
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun vote(
        id: Int,
        idolId: Int,
        num: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getReplies(
        id: Int,
        limit: Int,
        cursor: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getReply(
        id: Int,
        translate: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun postReplyMultipart(
        heartPickId: Int,
        emoticonId: Int? = null,
        content: String,
        imageUrl: String? = null,
        image: ByteArray? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun postOpenHeartPickNotification(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getOpenHeartPickNotification(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit

    )
}
