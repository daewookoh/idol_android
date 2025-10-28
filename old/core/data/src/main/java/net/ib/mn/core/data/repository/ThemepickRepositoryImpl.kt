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
import net.ib.mn.core.data.api.ThemepickApi
import net.ib.mn.core.data.dto.HeartpickVoteDTO
import net.ib.mn.core.data.dto.OnepickVoteDTO
import net.ib.mn.core.data.dto.OpenNotificationDTO
import net.ib.mn.core.data.dto.ThemepickVoteDTO
import org.json.JSONObject
import javax.inject.Inject

class ThemepickRepositoryImpl @Inject constructor(
    private val themepickApi: ThemepickApi
) : ThemepickRepository, BaseRepository() {
    override suspend fun get(
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = themepickApi.get(offset, limit)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun vote(
        id: Int,
        idolId: Int,
        voteType: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = themepickApi.vote(ThemepickVoteDTO(id, idolId, voteType))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getResult(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = themepickApi.getResult(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun postOpenThemePickNotification(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = themepickApi.postOpenThemePickNotification(OpenNotificationDTO(id = id))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }
}
