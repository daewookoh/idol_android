package net.ib.mn.common.extension

import com.google.gson.GsonBuilder
import net.ib.mn.common.exception.ErrorUtil

val gson = GsonBuilder()
    .setDateFormat("yyyy-MM-dd HH:mm:ss")
    .create()

fun Any.toJson(): String? = gson.toJson(this)

inline fun <reified T> String.fromJson(): T? =
    runCatching {
        gson.fromJson(this, T::class.java)
    }.onFailure {
        ErrorUtil.handleUnExpectedCaseException("[${T::class.java.name}] json 파싱 오류 발생 - ${it.message} : $this")
    }.getOrNull()
