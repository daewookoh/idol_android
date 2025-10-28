/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.MessagesApi
import net.ib.mn.core.data.api.TrendsApi
import net.ib.mn.core.data.dto.ClaimMessageDTO
import net.ib.mn.core.data.dto.DeleteMessageByTypeDTO
import org.json.JSONObject
import javax.inject.Inject


class MessagesRepositoryImpl @Inject constructor(
    private val messagesApi: MessagesApi
) : MessagesRepository, BaseRepository() {

    override suspend fun deleteByType(
        type: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = messagesApi.deleteByType(DeleteMessageByTypeDTO(type))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun get(
        type: String?,
        after: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = messagesApi.get(type, after)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun delete(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = messagesApi.delete(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun claim(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = messagesApi.claim(ClaimMessageDTO(id))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }
}
