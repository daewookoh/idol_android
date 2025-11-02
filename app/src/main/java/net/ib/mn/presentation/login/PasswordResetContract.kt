package net.ib.mn.presentation.login

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState

/**
 * PasswordResetScreen MVI Contract.
 * old 프로젝트의 ForgotPasswdFragment와 동일한 비즈니스 로직.
 */
object PasswordResetContract {
    /**
     * State.
     */
    data class State(
        val isLoading: Boolean = false,
        val email: String = "",
        val emailError: String? = null,
        val isEmailFocused: Boolean = false,
        val isEmailValid: Boolean = false
    ) : UiState

    /**
     * Intent (User Actions).
     */
    sealed class Intent : UiIntent {
        data class EmailChanged(val email: String) : Intent()
        data class EmailFocusChanged(val isFocused: Boolean) : Intent()
        data object Submit : Intent()
        data object NavigateBack : Intent()
    }

    /**
     * Effect (One-time Events).
     */
    sealed class Effect : UiEffect {
        data object NavigateBack : Effect()
        data class ShowError(val message: String) : Effect()
        data class ShowSuccessDialog(val message: String) : Effect()
    }
}
