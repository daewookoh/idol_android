/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.HeartpickApi
import net.ib.mn.core.data.api.OnepickApi
import net.ib.mn.core.data.dto.HeartpickVoteDTO
import net.ib.mn.core.data.dto.OnepickVoteDTO
import net.ib.mn.core.data.dto.OpenNotificationDTO
import org.json.JSONObject
import javax.inject.Inject

class OnepickRepositoryImpl @Inject constructor(
    private val onepickApi: OnepickApi
) : OnepickRepository, BaseRepository() {
    override suspend fun get(
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = onepickApi.get(offset, limit)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun vote(
        id: Int,
        voteIds: String,
        voteType: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = onepickApi.vote(OnepickVoteDTO(id, voteIds, voteType))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getResult(
        id: Int,
        status: Boolean?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            var paramStatus: String? = null
            if(status == true) {
                paramStatus = "1"
            }
            val response = onepickApi.getResult(id, paramStatus)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun postOpenImagePickNotification(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = onepickApi.postOpenImagePickNotification(OpenNotificationDTO(id = id))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }
}
