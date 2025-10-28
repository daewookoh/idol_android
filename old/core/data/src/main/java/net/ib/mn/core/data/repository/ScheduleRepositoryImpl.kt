/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.api.ScheduleApi
import net.ib.mn.core.data.dto.ScheduleVoteDTO
import net.ib.mn.core.data.dto.ScheduleWriteDTO
import org.json.JSONObject
import javax.inject.Inject


/**
 * @see
 * */

// 나중에 일별/월별 스케줄을 use case로 분리하자
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleApi: ScheduleApi
) : ScheduleRepository, BaseRepository() {
    override suspend fun getSchedules(
        idolId: Int,
        yearmonth: String?,
        yearmonthday: String?,
        vote: Int?,
        locale: String,
        onlyIcon: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = scheduleApi.getSchedules(
                idolId = idolId,
                yearmonth = yearmonth,
                yearmonthday = yearmonthday,
                includevotes = vote,
                locale = locale,
                onlyIcon = onlyIcon)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun vote(
        scheduleId: Int,
        vote: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = scheduleApi.postScheduleVote(ScheduleVoteDTO(scheduleId, vote))
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun write(
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
    ) {
        try {
            val response = scheduleApi.postSchedule(
                ScheduleWriteDTO(
                    idolId = idolId,
                    idolIds = idolIds,
                    title = title,
                    category = category,
                    location = location,
                    lat = lat,
                    lng = lng,
                    url = url,
                    dtstart = dtstart,
                    duration = duration,
                    allday = allday,
                    extra = extra,
                    locale = locale
                )
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun edit(
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
    ) {
        try {
            val response = scheduleApi.editSchedule(
                scheduleId,
                ScheduleWriteDTO(
                    idolId = idolId,
                    idolIds = idolIds,
                    title = title,
                    category = category,
                    location = location,
                    lat = lat,
                    lng = lng,
                    url = url,
                    dtstart = dtstart,
                    duration = duration,
                    allday = allday,
                    extra = extra
                )
            )
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun delete(
        scheduleId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = scheduleApi.deleteSchedule(scheduleId)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}