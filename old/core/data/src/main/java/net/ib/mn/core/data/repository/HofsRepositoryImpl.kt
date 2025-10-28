/**
 * Copyright (C) 2025. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author parkboo@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import androidx.core.net.toUri
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import net.ib.mn.core.data.api.HofsApi
import net.ib.mn.core.model.BaseModel
import org.json.JSONObject
import javax.inject.Inject

class HofsRepositoryImpl @Inject constructor(
    private val hofsApi: HofsApi
) : HofsRepository, BaseRepository() {
    override suspend fun get(
        code: String?,
        type: String?, // celeb
        category: String?, // celeb
        historyParam: String?,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            // query parameter 조합
            val params = mutableMapOf<String, String?>()
            if(code != null) {
                params["code"] = code
            }
            // celeb
            if(type != null) {
                params["type"] = type
            }
            if(category != null) {
                params["category"] = category
            }
            // historyParam은 query parameter 형식이므로 다시 분해. 앞에 ? 가 있어야 parse 되므로 ?를 붙여서 parse
            historyParam?.let {
                val uri = "?${it}".toUri()
                uri.queryParameterNames.forEach { key ->
                    val value = uri.getQueryParameter(key)
                    if (value?.isNotEmpty() == true) {
                        params[key] = value
                    }
                }
            }

            val response = hofsApi.get(params)
            processResponse(response, listener, errorListener)
        } catch (e: Exception) {
            errorListener(e)
        }
    }

    override suspend fun getTop1Count(
    ): Flow<BaseModel<JSONObject>> = flow {
        try {
            val result = hofsApi.getTop1Count()
            emit(BaseModel(data = JSONObject(result)))
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }.catch { e ->
        emit(BaseModel(message = "HttpException: Server Error", success = false))
    }
}
