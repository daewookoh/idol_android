package net.ib.mn.presentation.friend.request

import android.content.Context
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.util.logD
import net.ib.mn.data.repository.FriendsRepository
import javax.inject.Inject

/**
 * FriendRequest 화면 ViewModel
 *
 * old 프로젝트의 FriendsRequestActivity + AcceptFriendsRequestFragment + CancelFriendsRequestFragment 로직을 MVI 패턴으로 구현.
 */
@HiltViewModel
class FriendRequestViewModel @Inject constructor(
    private val friendsRepository: FriendsRepository,
    @ApplicationContext private val context: Context
) : BaseViewModel<FriendRequestContract.State, FriendRequestContract.Intent, FriendRequestContract.Effect>() {

    companion object {
        // old 프로젝트의 ResultCode와 동일
        const val RESULT_FRIEND_REQUESTED = 0x9902
        const val RESULT_FRIEND_REQUEST_CANCELED = 0x9903
    }

    override fun createInitialState(): FriendRequestContract.State = FriendRequestContract.State()

    init {
        loadData()
    }

    override fun handleIntent(intent: FriendRequestContract.Intent) {
        when (intent) {
            is FriendRequestContract.Intent.LoadData -> loadData()
            is FriendRequestContract.Intent.SelectTab -> selectTab(intent.tabIndex)
            is FriendRequestContract.Intent.AcceptRequest -> acceptRequest(intent.userId)
            is FriendRequestContract.Intent.DeclineRequest -> declineRequest(intent.userId)
            is FriendRequestContract.Intent.AcceptAllRequests -> acceptAllRequests()
            is FriendRequestContract.Intent.CancelRequest -> cancelRequest(intent.userId)
        }
    }

    /**
     * 데이터 로드
     */
    private fun loadData() {
        if (currentState.isLoading) return

        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            val response = friendsRepository.getFriendsSelf()
            if (response.success) {
                setState {
                    copy(
                        isLoading = false,
                        requesters = response.requesters,  // 나한테 친구 요청을 한 사람들
                        waiters = response.waiters,        // 내가 친구 요청을 한 사람들
                        error = null
                    )
                }
            } else {
                setState {
                    copy(
                        isLoading = false,
                        error = response.errorMessage ?: context.getString(R.string.error_abnormal_exception)
                    )
                }
            }
        }
    }

    /**
     * 탭 선택
     */
    private fun selectTab(tabIndex: Int) {
        setState { copy(selectedTab = tabIndex) }
    }

    /**
     * 친구 요청 수락
     * old: AcceptFriendsRequestFragment.onItemClicked (btnAcceptAll -> respondFriendRequest with accept=true)
     */
    private fun acceptRequest(userId: Int) {
        viewModelScope.launch {
            val response = friendsRepository.respondFriendRequest(userId, true)

            if (response.success) {
                // 목록에서 해당 유저 제거
                setState {
                    copy(requesters = requesters.filter { it.user.id != userId })
                }
                setEffect { FriendRequestContract.Effect.ShowDialog(null, context.getString(R.string.desc_accepted_friend_request)) }
                setEffect { FriendRequestContract.Effect.FinishWithResult(RESULT_FRIEND_REQUESTED) }
            } else {
                setEffect { FriendRequestContract.Effect.ShowDialog(null, response.message ?: context.getString(R.string.msg_error_ok)) }
            }
        }
    }

    /**
     * 친구 요청 거절
     * old: AcceptFriendsRequestFragment.onItemClicked (btnDecline -> respondFriendRequest with accept=false)
     */
    private fun declineRequest(userId: Int) {
        viewModelScope.launch {
            val response = friendsRepository.respondFriendRequest(userId, false)

            if (response.success) {
                // 목록에서 해당 유저 제거
                setState {
                    copy(requesters = requesters.filter { it.user.id != userId })
                }
            } else {
                setEffect { FriendRequestContract.Effect.ShowDialog(null, response.message?.ifEmpty { context.getString(R.string.msg_error_ok) } ?: context.getString(R.string.msg_error_ok)) }
            }
        }
    }

    /**
     * 모든 친구 요청 수락
     * old: AcceptFriendsRequestFragment.onItemClicked (btnAcceptAll -> respondAllFriendRequest)
     */
    private fun acceptAllRequests() {
        viewModelScope.launch {
            val response = friendsRepository.respondAllFriendRequest()

            if (response.success) {
                // 모든 요청자 목록 비우기
                setState { copy(requesters = emptyList()) }
                setEffect { FriendRequestContract.Effect.ShowDialog(null, context.getString(R.string.desc_accepted_friend_request)) }
                setEffect { FriendRequestContract.Effect.FinishWithResult(RESULT_FRIEND_REQUESTED) }
            } else {
                setEffect { FriendRequestContract.Effect.ShowDialog(null, response.message ?: context.getString(R.string.msg_error_ok)) }
            }
        }
    }

    /**
     * 보낸 친구 요청 취소
     * old: CancelFriendsRequestFragment.onItemClicked (btnCancel -> cancelFriendRequest)
     */
    private fun cancelRequest(userId: Int) {
        logD("FriendRequestVM", "cancelRequest called with userId: $userId")
        viewModelScope.launch {
            val response = friendsRepository.cancelFriendRequest(userId)
            logD("FriendRequestVM", "cancelFriendRequest response: success=${response.success}, message=${response.message}")

            if (response.success) {
                // 목록에서 해당 유저 제거
                setState {
                    copy(waiters = waiters.filter { it.user.id != userId })
                }
                setEffect { FriendRequestContract.Effect.FinishWithResult(RESULT_FRIEND_REQUEST_CANCELED) }
            } else {
                setEffect { FriendRequestContract.Effect.ShowToast(response.message ?: context.getString(R.string.msg_error_ok)) }
            }
        }
    }
}
