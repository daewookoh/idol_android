package net.ib.mn.common.util

import android.util.Log
import net.ib.mn.common.BuildConfig

private const val TAG = "idol"
private fun buildLogMsg(message: String): String {
    val ste = Thread.currentThread().stackTrace[4]
    val fileName = ste.fileName.replace(".java", "").replace(".kt", "")
    return "[$fileName::${ste.methodName} (${ste.fileName}:${ste.lineNumber})]$message"
}

private fun isNotDebugMode() = !BuildConfig.DEBUG

fun logV(paramString: String) {
    if (isNotDebugMode()) return
    Log.v(TAG, buildLogMsg(paramString))
}

fun logD(paramString: String) {
    if (isNotDebugMode()) return
    Log.d(TAG, buildLogMsg(paramString))
}

fun logI(paramString: String) {
    if (isNotDebugMode()) return
    Log.i(TAG, buildLogMsg(paramString))
}

fun logW(paramString: String) {
    if (isNotDebugMode()) return
    Log.w(TAG, buildLogMsg(paramString))
}

fun logE(paramString: String) {
    if (isNotDebugMode()) return
    Log.e(TAG, buildLogMsg(paramString))
}

fun logE(name: String, paramString: String) {
    if (isNotDebugMode()) return
    Log.e(TAG, buildLogMsg("$name: $paramString"))
}

fun logV(tag: String, paramString: String) {
    if (isNotDebugMode()) return
    Log.v(tag, buildLogMsg(paramString))
}

fun logD(tag: String, paramString: String) {
    if (isNotDebugMode()) return
    Log.d(tag, buildLogMsg(paramString))
}

fun logI(tag: String, paramString: String) {
    if (isNotDebugMode()) return
    Log.i(tag, buildLogMsg(paramString))
}

fun logW(tag: String, paramString: String) {
    if (isNotDebugMode()) return
    Log.w(tag, buildLogMsg(paramString))
}

fun logE(tag: String, name: String, paramString: String) {
    if (isNotDebugMode()) return
    Log.e(tag, buildLogMsg("$name: $paramString"))
}
