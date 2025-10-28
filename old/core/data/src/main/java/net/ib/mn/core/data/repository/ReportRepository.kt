/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject


interface ReportRepository {
    suspend fun doReport(
        articleId: Long?,
        commentId: Long?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun doReportFeed(
        userId: Long,
        reason: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun doReportHeartPick(
        replyId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
    suspend fun getReportPossible(
        userId: Long,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
