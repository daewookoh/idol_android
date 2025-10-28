package net.ib.mn.common.exception

import net.ib.mn.common.extension.log
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.CancellationException

object ErrorUtil {

    fun logException(throwable: Throwable) {
        if (canIgnoreThrowable(throwable)) {
            return
        }
        throwable.log()
    }

    fun logException(message: String) {
        logException(IllegalStateException(message))
    }

    fun handleError(throwable: Throwable, errorToasterListener: ErrorToasterListener? = null) {
        throwable.log()
        try {

            val exceptionType = getExceptionType(throwable)
            errorToasterListener?.showErrorToast(exceptionType)
            when (exceptionType) {
                ExceptionType.NETWORK -> {

                }
                ExceptionType.IGNORE -> {
                    // no-op
                }
                ExceptionType.NORMAL -> {
                    logException(throwable)
                }
            }

        } catch (e: Exception) {
            logException(RuntimeException("ErrorUtil::handleError 내부에러: ", e))
        }
    }

    private fun getExceptionType(throwable: Throwable): ExceptionType =
        when (throwable) {
            is UnknownHostException -> {
                ExceptionType.NETWORK
            }
            is SocketTimeoutException,
            is CancellationException -> {
                ExceptionType.IGNORE
            }
            else -> {
                ExceptionType.NORMAL
            }
        }

    fun handleUnExpectedCaseException(
        state: Any?,
        errorToasterListener: ErrorToasterListener? = null
    ) {
        handleUnExpectedCaseException("존재할 수 없는 case : ", state, errorToasterListener)
    }

    fun handleUnExpectedCaseException(
        description: String,
        state: Any?,
        errorToasterListener: ErrorToasterListener? = null
    ) {
        handleError(IllegalArgumentException("$description : $state"), errorToasterListener)
    }

    private fun canIgnoreThrowable(throwable: Throwable): Boolean =
        when (throwable) {
            is ConnectException,
            is SocketTimeoutException,
            is CancellationException,
            is UnknownHostException -> true
            else -> false
        }

}
