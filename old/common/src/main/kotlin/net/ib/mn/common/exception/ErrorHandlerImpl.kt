package net.ib.mn.common.exception

import net.ib.mn.lib.component.ErrorHandler
import net.ib.mn.lib.component.Toaster
import javax.inject.Inject

class ErrorHandlerImpl @Inject constructor(
    private val toaster: Toaster,
) : ErrorHandler {

    override fun logException(throwable: Throwable) {
        ErrorUtil.logException(throwable)
    }

    override fun logException(message: String) {
        ErrorUtil.logException(message)
    }

    override fun handleError(throwable: Throwable) {
        ErrorUtil.handleError(throwable, ::showErrorToast)
    }

    override fun handleUnExpectedCaseException(state: Any?) {
        ErrorUtil.handleUnExpectedCaseException(state, ::showErrorToast)
    }

    override fun handleUnExpectedCaseException(description: String, state: Any?) {
        ErrorUtil.handleUnExpectedCaseException(description, state, ::showErrorToast)
    }

    private fun showErrorToast(exceptionType: ExceptionType) =
        when (exceptionType) {
            ExceptionType.IGNORE -> {
                // no-op
            }
            ExceptionType.NETWORK ->
                toaster.showErrorToast("네트워크 상태가 원활하지 않습니다.\n인터넷 연결상태를 확인 후 다시 시도해주세요")

            ExceptionType.NORMAL ->
                toaster.showErrorToast("오류가 발생했습니다.\n계속되면 채팅상담으로 꼭 알려주세요!")
        }
}
