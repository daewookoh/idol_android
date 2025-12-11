package net.ib.mn.presentation.friend.add

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.FriendRequestResult
import net.ib.mn.data.repository.FriendsRepository
import net.ib.mn.data.repository.NewFriendsRecommendResult
import net.ib.mn.data.repository.SetStatusResult
import net.ib.mn.data.repository.UsersRepository
import javax.inject.Inject

/**
 * FriendAdd (뉴프렌즈) 화면 ViewModel
 *
 * old 프로젝트의 NewFriendsActivity 로직을 MVI 패턴으로 구현.
 */
@HiltViewModel
class FriendAddViewModel @Inject constructor(
    private val usersRepository: UsersRepository,
    private val friendsRepository: FriendsRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : BaseViewModel<FriendAddContract.State, FriendAddContract.Intent, FriendAddContract.Effect>() {

    override fun createInitialState(): FriendAddContract.State = FriendAddContract.State()

    init {
        loadInitialData()
    }

    override fun handleIntent(intent: FriendAddContract.Intent) {
        when (intent) {
            is FriendAddContract.Intent.LoadInitialData -> loadInitialData()
            is FriendAddContract.Intent.RefreshRecommendedUsers -> refreshRecommendedUsers()
            is FriendAddContract.Intent.ToggleNewFriendsApply -> toggleNewFriendsApply()
            is FriendAddContract.Intent.SendFriendRequest -> sendFriendRequest(intent.userId)
        }
    }

    private fun loadInitialData() {
        if (currentState.isLoading) return

        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val userInfo = preferencesManager.userInfo.first()
            val myUserId = userInfo?.id ?: 0

            setState { copy(myUserId = myUserId) }

            // 내 상태 조회
            val statusResult = usersRepository.getStatus(myUserId)
            statusResult.onSuccess { json ->
                val newFriends = json.optString("new_friends", "N")
                val friendAllow = json.optString("friend_allow", "Y")

                setState {
                    copy(
                        isNewFriendsApplied = newFriends == "Y",
                        isAllowFriendReq = friendAllow == "Y"
                    )
                }
            }.onFailure {
                setEffect { FriendAddContract.Effect.ShowDialog(null, context.getString(R.string.desc_failed_to_connect_internet)) }
            }

            loadRecommendedUsers()
            setState { copy(isLoading = false) }
        }
    }

    private suspend fun loadRecommendedUsers() {
        when (val result = usersRepository.newFriendsRecommend()) {
            is NewFriendsRecommendResult.Success -> {
                val myUserId = currentState.myUserId
                val filteredUsers = result.users.filter { it.id != myUserId }
                setState { copy(recommendedUsers = filteredUsers, error = null) }
            }
            is NewFriendsRecommendResult.Error -> {
                setState { copy(error = result.message) }
            }
        }
    }

    private fun refreshRecommendedUsers() {
        setState { copy(isLoading = true) }

        viewModelScope.launch {
            loadRecommendedUsers()
            setState { copy(isLoading = false) }
        }
    }

    private fun toggleNewFriendsApply() {
        val currentApplied = currentState.isNewFriendsApplied

        if (!currentApplied && !currentState.isAllowFriendReq) {
            setEffect { FriendAddContract.Effect.ShowDialog(null, context.getString(R.string.apply_new_friends_blocked)) }
            return
        }

        val newStatus = if (currentApplied) "N" else "Y"

        viewModelScope.launch {
            when (val result = usersRepository.setStatus(newStatus)) {
                is SetStatusResult.Success -> {
                    setState { copy(isNewFriendsApplied = newStatus == "Y") }
                }
                is SetStatusResult.Error -> {
                    val errorMsg = result.message?.takeIf { it.isNotBlank() }
                        ?: context.getString(R.string.desc_failed_to_connect_internet)
                    setEffect { FriendAddContract.Effect.ShowDialog(null, errorMsg) }
                }
            }
        }
    }

    private fun sendFriendRequest(userId: Int) {
        if (userId in currentState.sendingRequestIds) return

        setState { copy(sendingRequestIds = sendingRequestIds + userId) }

        viewModelScope.launch {
            when (val result = friendsRepository.sendFriendRequest(userId)) {
                is FriendRequestResult.Success -> {
                    setState {
                        copy(
                            sendingRequestIds = sendingRequestIds - userId,
                            recommendedUsers = recommendedUsers.filter { it.id != userId }
                        )
                    }
                    setEffect { FriendAddContract.Effect.ShowToast(context.getString(R.string.friend_request_sent)) }
                }
                is FriendRequestResult.Error -> {
                    setState { copy(sendingRequestIds = sendingRequestIds - userId) }

                    val errorMessage = when (result.gcode) {
                        8000 -> context.getString(R.string.error_8000)
                        else -> result.message ?: context.getString(R.string.error_abnormal_exception)
                    }
                    setEffect { FriendAddContract.Effect.ShowDialog(null, errorMessage) }
                }
            }
        }
    }
}
