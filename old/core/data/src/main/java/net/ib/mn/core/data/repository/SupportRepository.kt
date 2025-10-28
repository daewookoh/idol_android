/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.model.ChatRoomCreateModel
import org.json.JSONObject


interface SupportRepository {
    suspend fun getSupports(
        limit: Int,
        offset: Int,
        groupId: String? = null,
        orderBy: String? = null,
        status: String? = null,
        yearMonth: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getSupportDetail(
        id: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun createSupport(
        idolId: Int,
        title: String,
        adId: String,
        utcDate: String,
        image: ByteArray?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun giveDiamond(
        supportId: Int,
        diamond: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun like(
        supportId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getTop5(
        supportId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getInAppBanner(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}
