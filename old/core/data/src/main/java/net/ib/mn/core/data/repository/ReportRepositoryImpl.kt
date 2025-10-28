/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.ReportApi
import net.ib.mn.core.data.dto.ReportDTO
import net.ib.mn.core.data.dto.ReportFeedDTO
import net.ib.mn.core.data.dto.ReportHeartPickDTO
import org.json.JSONObject
import javax.inject.Inject


class ReportRepositoryImpl @Inject constructor(
    private val reportApi: ReportApi
) : ReportRepository, BaseRepository() {
    override suspend fun doReport(
        articleId: Long?,
        commentId: Long?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = ReportDTO(articleId, commentId)
            val response = reportApi.doReport(body)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun doReportFeed(
        userId: Long,
        reason: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = ReportFeedDTO(userId, reason)
            val response = reportApi.doReportFeed(body)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun doReportHeartPick(
        replyId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = ReportHeartPickDTO(replyId)
            val response = reportApi.doReportHeartPick(body)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getReportPossible(
        userId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = reportApi.getReportPossible(userId)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}
