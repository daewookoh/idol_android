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
import net.ib.mn.domain.model.FriendUser
import javax.inject.Inject

/**
 * FriendAdd (뉴프렌즈) 화면 ViewModel
 *
 * old 프로젝트의 NewFriendsActivity 로직을 MVI 패턴으로 구현.
 * - 뉴프렌즈 신청/취소
 * - 뉴프렌즈 추천 목록 조회
 * - 친구 요청 보내기
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator로 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출)에만 집중
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

    /**
     * 초기 데이터 로드
     * 1. 내 상태 정보 조회 (뉴프렌즈 신청 여부, 친구 요청 허용 여부)
     * 2. 뉴프렌즈 추천 목록 조회
     */
    private fun loadInitialData() {
        if (currentState.isLoading) return

        setState { copy(isLoading = true, error = null) }

        viewModelScope.launch {
            // 내 정보 가져오기
            val userInfo = preferencesManager.userInfo.first()
            val myUserId = userInfo?.id ?: 0
            val myNickname = userInfo?.nickname
            val myProfileImage = userInfo?.profileImage
            val myLevel = userInfo?.level ?: 0
            val myLevelHeart = userInfo?.levelHeart ?: 0L

            // 내 상태 조회
            val statusResult = usersRepository.getStatus(myUserId)
            statusResult.onSuccess { json ->
                val newFriends = json.optString("new_friends", "N")
                val friendAllow = json.optString("friend_allow", "Y")
                val statusMessage = json.optString("status_message", "")

                val isApplied = newFriends == "Y"
                val isAllowed = friendAllow == "Y"

                // 내 유저 정보 생성
                val myUser = FriendUser(
                    id = myUserId,
                    nickname = myNickname ?: "",
                    picture = myProfileImage,
                    level = myLevel,
                    levelHeart = myLevelHeart,
                    statusMessage = statusMessage,
                    most = null // 필요시 추가
                )

                setState {
                    copy(
                        isNewFriendsApplied = isApplied,
                        isAllowFriendReq = isAllowed,
                        myStatusMessage = statusMessage,
                        myUser = myUser
                    )
                }
            }.onFailure {
                setEffect { FriendAddContract.Effect.ShowDialog(null, context.getString(R.string.desc_failed_to_connect_internet)) }
            }

            // 뉴프렌즈 추천 목록 조회
            loadRecommendedUsers()

            setState { copy(isLoading = false) }
        }
    }

    /**
     * 뉴프렌즈 추천 목록 조회
     */
    private suspend fun loadRecommendedUsers() {
        when (val result = usersRepository.newFriendsRecommend()) {
            is NewFriendsRecommendResult.Success -> {
                val userInfo = preferencesManager.userInfo.first()
                val myUserId = userInfo?.id ?: 0

                // 내 상태 메시지 업데이트 (목록에 내 정보가 포함되어 있을 경우)
                val myUserInList = result.users.find { it.id == myUserId }
                if (myUserInList != null) {
                    setState { copy(myStatusMessage = myUserInList.statusMessage ?: "") }
                }

                // 기존 친구 목록에서 제외 (old 프로젝트 로직 참조)
                // 여기서는 단순히 전체 목록 반환 (친구 목록 필터링은 Screen에서 필요시 처리)
                val filteredUsers = result.users.toMutableList()

                setState { copy(recommendedUsers = filteredUsers, error = null) }
            }
            is NewFriendsRecommendResult.Error -> {
                setState { copy(error = result.message) }
            }
        }
    }

    /**
     * 뉴프렌즈 추천 목록 새로고침
     */
    private fun refreshRecommendedUsers() {
        setState { copy(isLoading = true) }

        viewModelScope.launch {
            loadRecommendedUsers()
            setState { copy(isLoading = false) }
        }
    }

    /**
     * 뉴프렌즈 신청/취소 토글
     */
    private fun toggleNewFriendsApply() {
        val currentApplied = currentState.isNewFriendsApplied

        // 신청 시도 시 친구 요청 허용 여부 체크
        if (!currentApplied && !currentState.isAllowFriendReq) {
            setEffect { FriendAddContract.Effect.ShowDialog(null, context.getString(R.string.apply_new_friends_blocked)) }
            return
        }

        val newStatus = if (currentApplied) "N" else "Y"

        viewModelScope.launch {
            when (val result = usersRepository.setStatus(newStatus)) {
                is SetStatusResult.Success -> {
                    val myUser = currentState.myUser
                    val recommendedUsers = currentState.recommendedUsers.toMutableList()

                    if (newStatus == "Y") {
                        // 신청됨 -> 내 정보를 목록 맨 앞에 추가
                        setState { copy(isNewFriendsApplied = true) }

                        if (myUser != null) {
                            // 기존 목록에서 내 정보가 없으면 추가
                            val existingIndex = recommendedUsers.indexOfFirst { it.id == myUser.id }
                            if (existingIndex == -1) {
                                recommendedUsers.add(0, myUser)
                            }
                            setState { copy(recommendedUsers = recommendedUsers) }
                        }
                    } else {
                        // 취소됨 -> 내 정보를 목록에서 제거
                        setState { copy(isNewFriendsApplied = false) }

                        if (myUser != null) {
                            val index = recommendedUsers.indexOfFirst { it.id == myUser.id }
                            if (index != -1) {
                                recommendedUsers.removeAt(index)
                            }
                            setState { copy(recommendedUsers = recommendedUsers) }
                        }
                    }
                }
                is SetStatusResult.Error -> {
                    // 실패 시 상태 롤백하지 않고 에러 메시지 표시
                    if (!result.message.isNullOrBlank()) {
                        setEffect { FriendAddContract.Effect.ShowDialog(null, result.message) }
                    } else {
                        setEffect { FriendAddContract.Effect.ShowDialog(null, context.getString(R.string.desc_failed_to_connect_internet)) }
                    }
                }
            }
        }
    }

    /**
     * 친구 요청 보내기
     */
    private fun sendFriendRequest(userId: Int) {
        // 이미 요청 중인지 체크
        if (currentState.sendingRequestIds.contains(userId)) {
            return
        }

        setState { copy(sendingRequestIds = sendingRequestIds + userId) }

        viewModelScope.launch {
            when (val result = friendsRepository.sendFriendRequest(userId)) {
                is FriendRequestResult.Success -> {
                    setState { copy(sendingRequestIds = sendingRequestIds - userId) }

                    // 요청 성공 시 목록에서 제거
                    val recommendedUsers = currentState.recommendedUsers.toMutableList()
                    val index = recommendedUsers.indexOfFirst { it.id == userId }
                    if (index != -1) {
                        recommendedUsers.removeAt(index)
                        setState { copy(recommendedUsers = recommendedUsers) }
                    }

                    setEffect { FriendAddContract.Effect.ShowToast(context.getString(R.string.friend_request_sent)) }
                }
                is FriendRequestResult.Error -> {
                    setState { copy(sendingRequestIds = sendingRequestIds - userId) }

                    // gcode에 따른 에러 메시지 처리
                    val errorMessage = when (result.gcode) {
                        8000 -> context.getString(R.string.error_8000)  // 친구 제한
                        else -> result.message ?: context.getString(R.string.error_abnormal_exception)
                    }

                    setEffect { FriendAddContract.Effect.ShowDialog(null, errorMessage) }
                }
            }
        }
    }
}
