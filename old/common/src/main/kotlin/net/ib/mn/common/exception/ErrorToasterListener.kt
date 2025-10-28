package net.ib.mn.common.exception


fun interface ErrorToasterListener {
    fun showErrorToast(exceptionType: ExceptionType)
}
