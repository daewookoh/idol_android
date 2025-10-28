/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.repository

import net.ib.mn.core.data.model.ChatRoomCreateModel
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Response


open class BaseRepository {
    protected suspend fun processResponse(
        response: Response<ResponseBody>,
        listener: (JSONObject) -> Unit,
        errorListener: (Throwable) -> Unit
    ) {
        try {
            if (response.isSuccessful) {
                val jsonResponse = response.body()?.string()
                // 204 no content같은 경우도 성공이므로
                val jsonObject = JSONObject(jsonResponse ?: "{}")
                listener(jsonObject)
            } else {
                errorListener(Throwable(response.message()))
            }
        } catch (e: Exception) {
            errorListener(Throwable(e.message))
        }
    }
}
