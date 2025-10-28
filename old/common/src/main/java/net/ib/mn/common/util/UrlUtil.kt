package net.ib.mn.common.util

import androidx.core.net.toUri

fun String.updateQueryParameter(param: String, value: String): String {
    if (this.isEmpty() || value.isEmpty()) return this

    val uri = this.toUri()
    val builder = uri.buildUpon()

    val queryParameterNames = uri.queryParameterNames
    builder.clearQuery()
    for (key in queryParameterNames) {
        if (param != key) {
            uri.getQueryParameters(key).forEach { v ->
                builder.appendQueryParameter(key, v)
            }
        }
    }
    builder.appendQueryParameter(param, value)
    return builder.build().toString()
}

// url string에 ver 파라미터를 붙인다
fun String.appendVersion(ver: Int): String {
    return updateQueryParameter("ver", ver.toString())
}