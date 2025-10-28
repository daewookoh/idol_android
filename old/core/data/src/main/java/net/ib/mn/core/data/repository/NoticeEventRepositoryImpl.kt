/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.NoticeEventApi
import org.json.JSONObject
import javax.inject.Inject


class NoticeEventRepositoryImpl @Inject constructor(
    private val noticeEventApi: NoticeEventApi
) : NoticeEventRepository, BaseRepository() {
    override suspend fun get(
        type: String,
        id: Int,
        isDark: Boolean,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = noticeEventApi.get(type, id, if (isDark) "dark" else null)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }
}
