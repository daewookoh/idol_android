/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository.idols

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.IdolsApi
import net.ib.mn.core.data.dto.GiveHeartToIdolDTO
import net.ib.mn.core.model.BaseModel
import net.ib.mn.core.data.model.GiveHeartModel
import net.ib.mn.core.data.repository.BaseRepository
import org.json.JSONObject
import retrofit2.HttpException
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject


/**
 * @see
 * */
class IdolsRepositoryImpl @Inject constructor(
    private val idolsApi: IdolsApi
) : IdolsRepository, BaseRepository() {
    override suspend fun getIdols(
        type: String?,
        category: String?,
        fields: String?
    ): Flow<BaseModel<JSONObject>> = flow {
        try {
            val result = idolsApi.getIdols(type, category, fields)
            emit(BaseModel(data = JSONObject(result)))
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = "HttpException: Server Error", success = false))
    }

    override suspend fun giveHeartToIdol(
        idolId: Int,
        hearts: Long,
        listener: (GiveHeartModel) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val body = GiveHeartToIdolDTO(idolId.toString(), hearts)
            val result = idolsApi.postGiveHeartToIdol(body)
            listener(result)
        } catch (e: Exception) {
            e.printStackTrace()
            errorListener(Throwable(e.message))
        }
    }

    override suspend fun getIdolsWithTs(
        type: String?,
        category: String?,
        fields: String?,
        onServerTime: (Int) -> Unit,
    ): Flow<BaseModel<JSONObject>> = flow {
        try {
            val response = idolsApi.getIdolsWithTs(type, category, fields)
            // 헤더의 Date를 파싱하여 서버의 타임스탬프를 설정
            response.headers()["Date"]?.let {dateString ->
                try {
                    val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    val date = formatter.parse(dateString)
                    date?.let {
                        val ts = (date.time / 1000).toInt()
                        onServerTime(ts)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val jsonResponse = response.body()?.string()
            jsonResponse?.let {
                val jsonObject = JSONObject(it)
                emit(BaseModel(data = jsonObject))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = "HttpException: Server Error", success = false))
    }

    override suspend fun getCharityCount(
    ): Flow<BaseModel<JSONObject>> = flow {
        try {
            val result = idolsApi.getCharityCount()
            emit(BaseModel(data = JSONObject(result)))
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = "HttpException: Server Error", success = false))
    }

    override suspend fun getGroupsForQuiz(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = idolsApi.getGroupsForQuiz()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getIdolsByIds(
        ids: String?,
        fields: String?,
        onServerTime: ((Int) -> Unit)?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = idolsApi.getIdolsByIds(ids, fields)
            // 헤더의 Date를 파싱하여 서버의 타임스탬프를 설정
            response.headers()["Date"]?.let {dateString ->
                try {
                    val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    val date = formatter.parse(dateString)
                    date?.let {
                        val ts = (date.time / 1000).toInt()
                        if (onServerTime != null) {
                            onServerTime(ts)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getIdolsForSearch(
        id: Int?,
        onServerTime: ((Int) -> Unit)?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = idolsApi.getIdolsForSearch(if((id ?: 0) > 0) id else null)
            // 헤더의 Date를 파싱하여 서버의 타임스탬프를 설정
            response.headers()["Date"]?.let {dateString ->
                try {
                    val formatter = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)
                    formatter.timeZone = TimeZone.getTimeZone("UTC")
                    val date = formatter.parse(dateString)
                    date?.let {
                        val ts = (date.time / 1000).toInt()
                        if (onServerTime != null) {
                            onServerTime(ts)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getExcludedIdols(
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = idolsApi.getExcludedIdols()
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getGroupMembers(
        groupId: Int,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = idolsApi.getGroupMembers(groupId)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getWikiName(
        idolId: Int,
        locale: String
    ) : String {
        return try {
            val response = idolsApi.getWikiName(idolId, locale)
            if(response.isSuccessful) {
                val json = JSONObject(response.body()?.string() ?: "")
                if (json.optBoolean("success")) {
                    json.optString("wiki_name") ?: ""
                } else {
                    throw IllegalStateException("API 응답 실패: success=false")
                }
            } else {
                throw IllegalStateException("API 응답 실패: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getCharityHistory(
        type: String,
        locale: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = idolsApi.getCharityHistory(type, locale)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getSuperRookieHistory(
        locale: String,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            val response = idolsApi.getSuperRookieHistory(locale)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getAwardIdols(chartCode: String?, fields: String?): JSONObject {
        return try {
            val response = idolsApi.getAwardIdols(chartCode, fields)
            if (response.isSuccessful) {
                JSONObject(response.body()?.string() ?: "")
            } else {
                throw IllegalStateException("API 응답 실패: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            throw e
        }
    }
}