package net.ib.mn.data.repository

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.ib.mn.data.remote.api.ScheduleApi
import net.ib.mn.data.remote.dto.ScheduleDto
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ScheduleModel
import net.ib.mn.domain.repository.ScheduleRepository
import org.json.JSONObject
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleRepositoryImpl @Inject constructor(
    private val scheduleApi: ScheduleApi,
    private val gson: Gson
) : ScheduleRepository {

    companion object {
        private const val TAG = "ScheduleRepo"
    }

    override fun getMonthScheduleIcons(
        idolId: Int,
        yearMonth: String,
        locale: String
    ): Flow<ApiResult<Map<Int, String>>> = flow {
        emit(ApiResult.Loading)
        Log.d(TAG, "getMonthScheduleIcons: idolId=$idolId, yearMonth=$yearMonth, locale=$locale")
        try {
            val response = scheduleApi.getSchedules(
                idolId = idolId,
                yearMonth = yearMonth,
                includeVotes = 1,
                locale = locale,
                onlyIcon = "Y"
            )
            Log.d(TAG, "getMonthScheduleIcons: response code=${response.code()}")

            if (response.isSuccessful) {
                response.body()?.string()?.let { body ->
                    Log.d(TAG, "getMonthScheduleIcons: body=$body")
                    val json = JSONObject(body)
                    if (json.optBoolean("success", true) && json.optInt("gcode") != 9000) {
                        val objectsArray = json.optJSONArray("objects")
                        val iconMap = mutableMapOf<Int, String>()

                        if (objectsArray != null) {
                            Log.d(TAG, "getMonthScheduleIcons: objectsArray size=${objectsArray.length()}")
                            val listType = object : TypeToken<List<ScheduleDto>>() {}.type
                            val dtoList: List<ScheduleDto> = gson.fromJson(objectsArray.toString(), listType)

                            dtoList.forEach { dto ->
                                dto.dtstart?.let { date ->
                                    val day = date.date
                                    if (!iconMap.containsKey(day)) {
                                        iconMap[day] = dto.category ?: ""
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "getMonthScheduleIcons: iconMap=$iconMap")
                        emit(ApiResult.Success(iconMap))
                    } else {
                        Log.d(TAG, "getMonthScheduleIcons: success=false or gcode=9000")
                        emit(ApiResult.Success(emptyMap()))
                    }
                } ?: run {
                    Log.e(TAG, "getMonthScheduleIcons: Empty response body")
                    emit(ApiResult.Error(Exception("Empty response")))
                }
            } else {
                Log.e(TAG, "getMonthScheduleIcons: API Error ${response.code()}")
                emit(ApiResult.Error(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getMonthScheduleIcons: Exception", e)
            emit(ApiResult.Error(e))
        }
    }

    override fun getDaySchedules(
        idolId: Int,
        yearMonthDay: String,
        locale: String
    ): Flow<ApiResult<List<ScheduleModel>>> = flow {
        emit(ApiResult.Loading)
        Log.d(TAG, "getDaySchedules: idolId=$idolId, yearMonthDay=$yearMonthDay, locale=$locale")
        try {
            val response = scheduleApi.getSchedules(
                idolId = idolId,
                yearMonthDay = yearMonthDay,
                includeVotes = 1,
                locale = locale
            )
            Log.d(TAG, "getDaySchedules: response code=${response.code()}")

            if (response.isSuccessful) {
                response.body()?.string()?.let { body ->
                    Log.d(TAG, "getDaySchedules: body=$body")
                    val json = JSONObject(body)
                    if (json.optBoolean("success", true) && json.optInt("gcode") != 9000) {
                        val objectsArray = json.optJSONArray("objects")
                        val schedules = mutableListOf<ScheduleModel>()

                        if (objectsArray != null) {
                            Log.d(TAG, "getDaySchedules: objectsArray size=${objectsArray.length()}")
                            val listType = object : TypeToken<List<ScheduleDto>>() {}.type
                            val dtoList: List<ScheduleDto> = gson.fromJson(objectsArray.toString(), listType)
                            schedules.addAll(dtoList.map { it.toModel() }.sortedBy { it.dtstart })
                        }
                        Log.d(TAG, "getDaySchedules: schedules size=${schedules.size}")
                        emit(ApiResult.Success(schedules))
                    } else {
                        Log.d(TAG, "getDaySchedules: success=false or gcode=9000")
                        emit(ApiResult.Success(emptyList()))
                    }
                } ?: run {
                    Log.e(TAG, "getDaySchedules: Empty response body")
                    emit(ApiResult.Error(Exception("Empty response")))
                }
            } else {
                Log.e(TAG, "getDaySchedules: API Error ${response.code()}")
                emit(ApiResult.Error(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getDaySchedules: Exception", e)
            emit(ApiResult.Error(e))
        }
    }

    override fun voteSchedule(scheduleId: Int, vote: String): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = scheduleApi.voteSchedule(scheduleId, vote)

            if (response.isSuccessful) {
                response.body()?.string()?.let { body ->
                    val json = JSONObject(body)
                    emit(ApiResult.Success(json.optBoolean("success", false)))
                } ?: emit(ApiResult.Error(Exception("Empty response")))
            } else {
                emit(ApiResult.Error(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
        }
    }

    override fun deleteSchedule(scheduleId: Int): Flow<ApiResult<Boolean>> = flow {
        emit(ApiResult.Loading)
        try {
            val response = scheduleApi.deleteSchedule(scheduleId)

            if (response.isSuccessful) {
                response.body()?.string()?.let { body ->
                    val json = JSONObject(body)
                    emit(ApiResult.Success(json.optBoolean("success", false)))
                } ?: emit(ApiResult.Error(Exception("Empty response")))
            } else {
                emit(ApiResult.Error(Exception("API Error: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
        }
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
