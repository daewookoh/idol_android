/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.idols

import kotlinx.coroutines.flow.Flow
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.data.model.GiveHeartModel
import org.json.JSONObject


/**
 * @see
 * */

interface IdolsRepository {
    suspend fun getIdols(
        type: String? = null,
        category: String? = null,
        fields: String? = null,
    ): Flow<BaseModel<JSONObject>>

    suspend fun giveHeartToIdol(
        idolId: Int,
        hearts: Long,
        listener: (GiveHeartModel) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getIdolsWithTs(
        type: String? = null,
        category: String? = null,
        fields: String? = null,
        onServerTime: (Int) -> Unit,
    ): Flow<BaseModel<JSONObject>>

    suspend fun getCharityCount(
    ): Flow<BaseModel<JSONObject>>

    suspend fun getGroupsForQuiz(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    // 투표후 아이돌 리스트 바꿔주기
    suspend fun getIdolsByIds(
        ids: String? = null,
        fields: String? = null,
        onServerTime: ((Int) -> Unit)? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getIdolsForSearch(
        id: Int? = null,
        onServerTime: ((Int) -> Unit)? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getExcludedIdols(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getGroupMembers(
        groupId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getWikiName(
        idolId: Int,
        locale: String
    ): String

    suspend fun getCharityHistory(
        type: String,
        locale: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getSuperRookieHistory(
        locale: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun getAwardIdols(
        chartCode: String?,
        fields: String?,
    ): JSONObject
}
