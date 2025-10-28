/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import org.json.JSONObject

interface ScheduleRepository {
    suspend fun getSchedules(
        idolId: Int,
        yearmonth: String? = null,
        yearmonthday: String? = null,
        vote: Int? = null,
        locale: String,
        onlyIcon: String? = null,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun vote(
        scheduleId: Int,
        vote: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun write(
        idolId: Int,
        idolIds: String?,
        title: String,
        category: String?,
        location: String?,
        lat: String?,
        lng: String?,
        url: String?,
        dtstart: String?,
        duration: Int,
        allday: Int,
        extra: String?,
        locale: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun edit(
        scheduleId: Int,
        idolId: Int,
        idolIds: String?,
        title: String,
        category: String?,
        location: String?,
        lat: String?,
        lng: String?,
        url: String?,
        dtstart: String?,
        duration: Int,
        allday: Int,
        extra: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )

    suspend fun delete(
        scheduleId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    )
}