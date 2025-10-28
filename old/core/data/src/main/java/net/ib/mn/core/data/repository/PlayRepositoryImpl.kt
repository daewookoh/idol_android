/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.PlayApi
import net.ib.mn.core.data.api.ReportApi
import net.ib.mn.core.data.api.SearchApi
import net.ib.mn.core.data.dto.PlayLikeDTO
import net.ib.mn.core.data.dto.ReportDTO
import net.ib.mn.core.data.dto.ReportFeedDTO
import net.ib.mn.core.data.dto.ReportHeartPickDTO
import org.json.JSONObject
import javax.inject.Inject


class PlayRepositoryImpl @Inject constructor(
    private val playApi: PlayApi
) : PlayRepository, BaseRepository() {
    override suspend fun getList(
        offset: Int,
        limit: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = playApi.getList(offset, limit)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getInfo(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = playApi.getInfo(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getToken(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = playApi.getToken(id)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getTopBanners(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = playApi.getTopBanners()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun like(
        liveId: Int,
        heart: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = playApi.like(PlayLikeDTO(liveId, heart))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

}
