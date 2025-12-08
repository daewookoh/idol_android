package net.ib.mn.domain.repository

import kotlinx.coroutines.flow.Flow
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ScheduleModel

/**
 * 스케줄 작성 요청 데이터
 */
data class ScheduleWriteRequest(
    val idolId: Int,
    val idolIds: String?,
    val title: String,
    val category: String,
    val location: String?,
    val lat: String?,
    val lng: String?,
    val url: String?,
    val dtstart: String,
    val duration: Int = 60,
    val allday: Int,
    val extra: String?,
    val locale: String
)

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

    fun writeSchedule(
        request: ScheduleWriteRequest
    ): Flow<ApiResult<ScheduleModel>>

    fun editSchedule(
        scheduleId: Int,
        request: ScheduleWriteRequest
    ): Flow<ApiResult<ScheduleModel>>

    fun getMonthSchedules(
        idolId: Int,
        yearMonth: String,
        locale: String
    ): Flow<ApiResult<List<ScheduleModel>>>
}
