package com.example.idol_android.presentation.users

import androidx.lifecycle.viewModelScope
import com.example.idol_android.base.BaseViewModel
import com.example.idol_android.data.model.Result
import com.example.idol_android.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Users screen implementing MVI pattern.
 * Repository를 직접 사용하는 단순화된 구조.
 */
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : BaseViewModel<UserContract.State, UserContract.Intent, UserContract.Effect>() {

    override fun createInitialState(): UserContract.State {
        return UserContract.State()
    }

    init {
        loadUsers()
    }

    override fun handleIntent(intent: UserContract.Intent) {
        when (intent) {
            is UserContract.Intent.LoadUsers -> loadUsers()
            is UserContract.Intent.RetryLoadUsers -> loadUsers()
            is UserContract.Intent.OnUserClick -> {
                setEffect { UserContract.Effect.NavigateToUserDetail(intent.user.id) }
            }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            userRepository.getUsers().collect { result ->
                when (result) {
                    is Result.Loading -> {
                        setState { copy(isLoading = true, error = null) }
                    }
                    is Result.Success -> {
                        setState {
                            copy(
                                users = result.data,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    is Result.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = result.exception.message ?: "Unknown error"
                            )
                        }
                        setEffect {
                            UserContract.Effect.ShowToast(
                                result.exception.message ?: "Failed to load users"
                            )
                        }
                    }
                }
            }
        }
    }
}
