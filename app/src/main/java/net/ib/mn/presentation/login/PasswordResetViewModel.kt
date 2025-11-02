package net.ib.mn.presentation.login

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.viewModelScope
import net.ib.mn.base.BaseViewModel
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.ib.mn.R
import javax.inject.Inject

/**
 * PasswordResetViewModel - 비밀번호 재설정 화면 ViewModel.
 * old 프로젝트의 ForgotPasswdFragment와 동일한 비즈니스 로직.
 */
@HiltViewModel
class PasswordResetViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userRepository: UserRepository
) : BaseViewModel<PasswordResetContract.State, PasswordResetContract.Intent, PasswordResetContract.Effect>() {

    companion object {
        private const val TAG = "PasswordResetViewModel"
    }

    override fun createInitialState(): PasswordResetContract.State {
        return PasswordResetContract.State()
    }

    override fun handleIntent(intent: PasswordResetContract.Intent) {
        when (intent) {
            is PasswordResetContract.Intent.EmailChanged -> {
                setState { 
                    copy(
                        email = intent.email, 
                        emailError = null,
                        isEmailValid = Patterns.EMAIL_ADDRESS.matcher(intent.email).matches()
                    ) 
                }
            }
            is PasswordResetContract.Intent.EmailFocusChanged -> {
                setState { copy(isEmailFocused = intent.isFocused) }
            }
            is PasswordResetContract.Intent.Submit -> handleSubmit()
            is PasswordResetContract.Intent.NavigateBack -> {
                setEffect { PasswordResetContract.Effect.NavigateBack }
            }
        }
    }

    /**
     * 비밀번호 찾기 요청 처리
     * old 프로젝트의 trySubmit() 로직 참고
     */
    private fun handleSubmit() {
        val currentState = uiState.value
        val email = currentState.email.trim()

        // Validation (old 프로젝트의 localValidate()와 동일)
        if (email.isEmpty()) {
            setState { copy(emailError = context.getString(R.string.required_field)) }
            return
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            setState { copy(emailError = context.getString(R.string.invalid_format_email)) }
            return
        }

        // Start find password process
        setState { copy(isLoading = true) }

        viewModelScope.launch {
            try {
                userRepository.findPassword(email).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val response = result.data
                            android.util.Log.d(TAG, "Find password response: success=${response.success}, message=${response.message}")
                            
                            setState { copy(isLoading = false) }
                            
                            if (response.success) {
                                // 성공 시: "비밀번호 안내 메일이 발송되었습니다" 메시지 표시 후 뒤로가기
                                setEffect {
                                    PasswordResetContract.Effect.ShowSuccessDialog(
                                        context.getString(R.string.sent_find_pw_mail)
                                    )
                                }
                            } else {
                                // 실패 시: 에러 메시지 표시
                                // old 프로젝트: ErrorControl.parseError()에서 gcode에 따라 에러 메시지 결정
                                // gcode가 1002인 경우 "이메일을 확인해 주세요" 메시지 표시
                                val errorMessage = when (response.gcode) {
                                    1002 -> context.getString(R.string.error_1002) // "이메일을 확인해 주세요"
                                    else -> response.message ?: context.getString(R.string.error_1002)
                                }
                                setEffect {
                                    PasswordResetContract.Effect.ShowError(errorMessage)
                                }
                            }
                        }
                        is ApiResult.Error -> {
                            android.util.Log.e(TAG, "Find password error: ${result.message}")
                            setState { copy(isLoading = false) }
                            // old 프로젝트: errorListener에서 error_abnormal_exception 표시
                            // 하지만 비밀번호 찾기 화면에서는 "이메일을 확인해 주세요"가 더 적절
                            setEffect {
                                PasswordResetContract.Effect.ShowError(
                                    context.getString(R.string.error_1002)
                                )
                            }
                        }
                        is ApiResult.Loading -> {
                            // Loading state already set above
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Find password exception", e)
                setState { copy(isLoading = false) }
                // old 프로젝트: 예외 발생 시 error_abnormal_exception 표시
                // 하지만 비밀번호 찾기 화면에서는 "이메일을 확인해 주세요"가 더 적절
                setEffect {
                    PasswordResetContract.Effect.ShowError(
                        context.getString(R.string.error_1002)
                    )
                }
            }
        }
    }
}
