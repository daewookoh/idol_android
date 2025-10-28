/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.data.api

import android.util.Log
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import okhttp3.logging.HttpLoggingInterceptor


/**
 * @see
 * */

class ApiLogger : HttpLoggingInterceptor.Logger {
    override fun log(message: String) {
        val logName = "ApiLogger"
        if (message.startsWith("{") || message.startsWith("[")) {
            val json = Json { prettyPrint = true }
            val prettyPrintedJson = json.decodeFromString<JsonElement>(message)

            Log.d(logName, prettyPrintedJson.toString())
        } else {
            Log.d(logName, message)
            return
        }
    }
}