package net.ib.mn.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import net.ib.mn.data.remote.api.ScheduleApi
import net.ib.mn.data.remote.dto.ScheduleDto
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.domain.repository.ScheduleRepository
import net.ib.mn.domain.repository.ScheduleWriteRequest
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ScheduleRepository 구현체
 *
 * BaseRepository의 safeApiCallWithJsonString 패턴 사용
 */
@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleApi: ScheduleApi,
    private val gson: Gson
) : BaseRepository(), ScheduleRepository {

    override fun getMonthScheduleIcons(
        idolId: Int,
        yearMonth: String,
        locale: String
    ): Flow<ApiResult<Map<Int, String>>> = safeApiCallWithJsonString(
        apiCall = {
            scheduleApi.getSchedules(
                idolId = idolId,
                yearMonth = yearMonth,
                includeVotes = 1,
                locale = locale,
                onlyIcon = "Y"
            )
        },
        parser = { json -> parseMonthScheduleIcons(json) }
    )

    override fun getDaySchedules(
        idolId: Int,
        yearMonthDay: String,
        locale: String
    ): Flow<ApiResult<List<ScheduleModel>>> = safeApiCallWithJsonString(
        apiCall = {
            scheduleApi.getSchedules(
                idolId = idolId,
                yearMonthDay = yearMonthDay,
                includeVotes = 1,
                locale = locale
            )
        },
        parser = { json -> parseDaySchedules(json) }
    )

    override fun voteSchedule(scheduleId: Int, vote: String): Flow<ApiResult<Boolean>> =
        safeApiCallWithJsonString(
            apiCall = { scheduleApi.voteSchedule(scheduleId, vote) },
            parser = { json -> JSONObject(json).optBoolean("success", false) }
        )

    override fun deleteSchedule(scheduleId: Int): Flow<ApiResult<Boolean>> =
        safeApiCallWithJsonString(
            apiCall = { scheduleApi.deleteSchedule(scheduleId) },
            parser = { json -> JSONObject(json).optBoolean("success", false) }
        )

    override fun writeSchedule(request: ScheduleWriteRequest): Flow<ApiResult<ScheduleModel>> =
        safeApiCallWithJsonString(
            apiCall = {
                scheduleApi.writeSchedule(
                    idolId = request.idolId,
                    idolIds = request.idolIds,
                    title = request.title,
                    category = request.category,
                    location = request.location,
                    lat = request.lat,
                    lng = request.lng,
                    url = request.url,
                    dtstart = request.dtstart,
                    duration = request.duration,
                    allday = request.allday,
                    extra = request.extra,
                    locale = request.locale
                )
            },
            parser = { json -> parseScheduleResponse(json) }
        )

    override fun editSchedule(
        scheduleId: Int,
        request: ScheduleWriteRequest
    ): Flow<ApiResult<ScheduleModel>> =
        safeApiCallWithJsonString(
            apiCall = {
                scheduleApi.editSchedule(
                    scheduleId = scheduleId,
                    idolId = request.idolId,
                    idolIds = request.idolIds,
                    title = request.title,
                    category = request.category,
                    location = request.location,
                    lat = request.lat,
                    lng = request.lng,
                    url = request.url,
                    dtstart = request.dtstart,
                    duration = request.duration,
                    allday = request.allday,
                    extra = request.extra
                )
            },
            parser = { json -> parseScheduleResponse(json) }
        )

    // ============================================================
    // Private Parsers
    // ============================================================

    private fun parseMonthScheduleIcons(json: String): Map<Int, String> {
        val jsonObject = JSONObject(json)
        if (!jsonObject.optBoolean("success", true) || jsonObject.optInt("gcode") == 9000) {
            return emptyMap()
        }

        val objectsArray = jsonObject.optJSONArray("objects") ?: return emptyMap()
        val listType = object : TypeToken<List<ScheduleDto>>() {}.type
        val dtoList: List<ScheduleDto> = gson.fromJson(objectsArray.toString(), listType)

        val iconMap = mutableMapOf<Int, String>()
        dtoList.forEach { dto ->
            dto.dtstart?.let { date ->
                val day = date.date
                if (!iconMap.containsKey(day)) {
                    iconMap[day] = dto.category ?: ""
                }
            }
        }
        return iconMap
    }

    private fun parseDaySchedules(json: String): List<ScheduleModel> {
        val jsonObject = JSONObject(json)
        if (!jsonObject.optBoolean("success", true) || jsonObject.optInt("gcode") == 9000) {
            return emptyList()
        }

        val objectsArray = jsonObject.optJSONArray("objects") ?: return emptyList()
        val listType = object : TypeToken<List<ScheduleDto>>() {}.type
        val dtoList: List<ScheduleDto> = gson.fromJson(objectsArray.toString(), listType)

        return dtoList.map { it.toModel() }.sortedBy { it.dtstart }
    }

    private fun parseScheduleResponse(json: String): ScheduleModel {
        val jsonObject = JSONObject(json)
        if (!jsonObject.optBoolean("success", false)) {
            throw Exception(jsonObject.optString("msg", "스케줄 저장에 실패했습니다."))
        }

        val objectsArray = jsonObject.optJSONArray("objects")
            ?: throw Exception("스케줄 응답 데이터가 없습니다.")

        if (objectsArray.length() == 0) {
            throw Exception("스케줄 응답 데이터가 비어있습니다.")
        }

        val listType = object : TypeToken<List<ScheduleDto>>() {}.type
        val dtoList: List<ScheduleDto> = gson.fromJson(objectsArray.toString(), listType)

        return dtoList.first().toModel()
    }

    private fun ScheduleDto.toModel() = ScheduleModel(
        id = id,
        articleId = articleId ?: 0,
        title = title ?: "",
        category = category ?: "",
        dtstart = dtstart ?: Date(),
        allday = allday ?: 0,
        location = location,
        url = url,
        extra = extra,
        numComments = numComments ?: 0,
        numYes = numYes ?: 0,
        numNo = numNo ?: 0,
        numDup = numDup ?: 0,
        vote = vote,
        isViewable = isViewable == "Y",
        isReadonly = isReadonly == "Y",
        userId = user?.id ?: 0,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
