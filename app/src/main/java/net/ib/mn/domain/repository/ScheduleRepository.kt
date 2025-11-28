package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ScheduleModel

interface ScheduleRepository {

    fun getMonthScheduleIcons(
        idolId: Int,
        yearMonth: String,
        locale: String
    ): Flow<ApiResult<Map<Int, String>>>

    fun getDaySchedules(
        idolId: Int,
        yearMonthDay: String,
        locale: String
    ): Flow<ApiResult<List<ScheduleModel>>>

    fun voteSchedule(
        scheduleId: Int,
        vote: String
    ): Flow<ApiResult<Boolean>>

    fun deleteSchedule(
        scheduleId: Int
    ): Flow<ApiResult<Boolean>>
}
