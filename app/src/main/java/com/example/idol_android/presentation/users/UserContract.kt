package com.example.idol_android.presentation.users

import com.example.idol_android.base.UiEffect
import com.example.idol_android.base.UiIntent
import com.example.idol_android.base.UiState
import com.example.idol_android.domain.model.User

/**
 * Contract class defining State, Intent, and Effect for Users screen.
 */
class UserContract {

    /**
     * UI State for Users screen.
     */
    data class State(
        val users: List<User> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    ) : UiState

    /**
     * User intents (user actions).
     */
    sealed class Intent : UiIntent {
        data object LoadUsers : Intent()
        data object RetryLoadUsers : Intent()
        data class OnUserClick(val user: User) : Intent()
    }

    /**
     * Side effects (one-time events).
     */
    sealed class Effect : UiEffect {
        data class ShowToast(val message: String) : Effect()
        data class NavigateToUserDetail(val userId: Int) : Effect()
    }
}
