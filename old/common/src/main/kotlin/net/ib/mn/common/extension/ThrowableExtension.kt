package net.ib.mn.common.extension

import net.ib.mn.common.util.logE
import net.ib.mn.common.exception.ErrorUtil
import java.io.PrintWriter
import java.io.StringWriter

fun Throwable.log() {
    val stringWriter = StringWriter()
    printStackTrace(PrintWriter(stringWriter))
    logE(stringWriter.toString())
}

fun Throwable.logException() {
    ErrorUtil.logException(this)
}

fun Throwable.handleError() {
    ErrorUtil.handleError(this)
}
