package net.ib.mn.presentation.login

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState

/**
 * EmailLoginScreen MVI Contract.
 */
object EmailLoginContract {
    /**
     * State.
     */
    data class State(
        val isLoading: Boolean = false,
        val email: String = "",
        val password: String = "",
        val isPasswordVisible: Boolean = false,
        val emailError: String? = null,
        val passwordError: String? = null,
        val isEmailFocused: Boolean = false,
        val isPasswordFocused: Boolean = false
    ) : UiState

    /**
     * Intent (User Actions).
     */
    sealed class Intent : UiIntent {
        data class EmailChanged(val email: String) : Intent()
        data class PasswordChanged(val password: String) : Intent()
        data class EmailFocusChanged(val isFocused: Boolean) : Intent()
        data class PasswordFocusChanged(val isFocused: Boolean) : Intent()
        data object TogglePasswordVisibility : Intent()
        data object SignIn : Intent()
        data object SignUp : Intent()
        data object ForgotId : Intent()
        data object ForgotPassword : Intent()
        data object NavigateBack : Intent()
    }

    /**
     * Effect (One-time Events).
     */
    sealed class Effect : UiEffect {
        data object NavigateToStartUp : Effect() // old 프로젝트: StartupActivity로 이동
        data object NavigateToMain : Effect()
        data object NavigateToSignUp : Effect()
        data object NavigateToForgotId : Effect()
        data object NavigateToForgotPassword : Effect()
        data object NavigateBack : Effect()
        data class ShowError(val message: String) : Effect()
        data class ShowToast(val message: String) : Effect()
        data class ShowFindIdDialog(val email: String?) : Effect()
    }
}
