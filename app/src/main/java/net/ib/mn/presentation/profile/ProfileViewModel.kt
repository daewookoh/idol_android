package net.ib.mn.presentation.profile

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.repository.BlockResult
import net.ib.mn.data.repository.FriendInfoResult
import net.ib.mn.data.repository.FriendRequestResult
import net.ib.mn.data.repository.FriendUserType
import net.ib.mn.data.repository.FriendsRepository
import net.ib.mn.data.repository.ReportPossibleResult
import net.ib.mn.data.repository.ReportRepository
import net.ib.mn.data.repository.ReportResult
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.util.LocaleUtil

/**
 * ProfileViewModel - 유저 프로필 ViewModel
 *
 * Old 프로젝트의 FeedActivity 로직을 참고하여 구현
 * - 본인 프로필(isMine=true): UserCacheRepository에서 most 정보 조회
 * - 타인 프로필(isMine=false): 전달받은 mostIdolName 사용
 */
@HiltViewModel(assistedFactory = ProfileViewModel.Factory::class)
class ProfileViewModel @AssistedInject constructor(
    @ApplicationContext private val context: Context,
    private val usersRepository: UsersRepository,
    private val userCacheRepository: UserCacheRepository,
    private val reportRepository: ReportRepository,
    private val friendsRepository: FriendsRepository,
    @Assisted private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(savedStateHandle: SavedStateHandle): ProfileViewModel
    }

    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // 신고 관련 상태
    private val _reportState = MutableStateFlow<ReportState>(ReportState.Idle)
    val reportState: StateFlow<ReportState> = _reportState.asStateFlow()

    // 친구 관련 상태
    private val _friendState = MutableStateFlow<FriendState>(FriendState.Loading)
    val friendState: StateFlow<FriendState> = _friendState.asStateFlow()

    // 차단 관련 상태
    private val _blockState = MutableStateFlow<BlockState>(BlockState.Idle)
    val blockState: StateFlow<BlockState> = _blockState.asStateFlow()

    // SavedStateHandle에서 파라미터 읽기
    private val userId: Int = savedStateHandle.get<Int>("userId") ?: 0
    private val isMine: Boolean = savedStateHandle.get<Boolean>("isMine") ?: false

    private var currentData = ProfileData(
        id = userId,
        nickname = savedStateHandle.get<String>("userNickname") ?: "",
        imageUrl = savedStateHandle.get<String>("userImageUrl"),
        level = savedStateHandle.get<Int>("userLevel") ?: 0,
        idolName = savedStateHandle.get<String>("mostIdolName"),
        statusMessage = null,
        isFeedPrivate = false
    )

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Success(user = currentData)

            // 본인 프로필이고 most가 없으면 캐시에서 로드
            if (isMine && currentData.idolName.isNullOrEmpty()) {
                loadMostFromCache()?.let { idolName ->
                    currentData = currentData.copy(idolName = idolName)
                    _uiState.value = ProfileUiState.Success(user = currentData)
                }
            }

            // 타인의 프로필인 경우 차단 상태 확인
            if (!isMine) {
                checkBlockStatus()
            } else {
                // 본인 프로필은 차단 상태 확인 불필요
                currentData = currentData.copy(blockStatusChecked = true)
                _uiState.value = ProfileUiState.Success(user = currentData)
            }

            // getStatus API에서 상태 메시지 및 비공개 여부 로드
            loadStatusInfo()

            // 타인의 프로필인 경우 친구 정보 로드
            if (!isMine) {
                loadFriendInfo()
            }
        }
    }

    /** 차단 상태 확인 */
    private suspend fun checkBlockStatus() {
        val isBlocked = usersRepository.isUserBlocked(userId)
        currentData = currentData.copy(
            isBlocked = isBlocked,
            blockStatusChecked = true
        )
        _uiState.value = ProfileUiState.Success(user = currentData)
    }

    /** getStatus API 호출하여 상태 메시지 및 피드 비공개 여부 로드 */
    private suspend fun loadStatusInfo() {
        try {
            val json = usersRepository.getStatus(userId).getOrNull() ?: return
            if (!json.optBoolean("success")) return

            val statusMessage = json.optString("status_message", "").takeIf { it.isNotEmpty() && it != "null" }
            val feedIsViewable = json.optString("feed_is_viewable", "Y")
            val isFeedPrivate = feedIsViewable == "N" && !isMine

            currentData = currentData.copy(
                statusMessage = statusMessage,
                isFeedPrivate = isFeedPrivate
            )
            _uiState.value = ProfileUiState.Success(user = currentData)
        } catch (e: Exception) {
            // 에러 무시
        }
    }

    /** 친구 정보 로드 */
    private suspend fun loadFriendInfo() {
        when (val result = friendsRepository.getFriendInfo(userId)) {
            is FriendInfoResult.Success -> {
                val newState = when {
                    result.isFriend -> FriendState.AlreadyFriend
                    result.userType.equals(FriendUserType.RECV_USER, ignoreCase = true) -> FriendState.RequestPending
                    else -> FriendState.CanAdd
                }
                updateFriendState(newState)
            }
            is FriendInfoResult.NotFound -> updateFriendState(FriendState.CanAdd)
            is FriendInfoResult.Error -> updateFriendState(FriendState.CanAdd)
        }
    }

    /** 본인 프로필: UserCacheRepository에서 most 정보 조회 */
    private fun loadMostFromCache(): String? {
        val most = userCacheRepository.getUserData()?.most ?: return null
        return LocaleUtil.getLocalizedIdolName(context, most).takeIf { it.isNotEmpty() }
    }

    // ========== 친구 관련 함수 ==========

    /** 친구 추가 버튼 클릭 (Old: action_friend_add) */
    fun onFriendAddClick() {
        viewModelScope.launch {
            _friendState.value = FriendState.Loading

            when (val result = friendsRepository.sendFriendRequest(userId)) {
                is FriendRequestResult.Success -> {
                    updateFriendState(FriendState.RequestSent)
                }
                is FriendRequestResult.Error -> {
                    // 에러 발생 시 에러 메시지 표시 (이전 상태는 lastStableFriendState에 저장됨)
                    _friendState.value = FriendState.Error(
                        gcode = result.gcode,
                        message = result.message
                    )
                }
            }
        }
    }

    /** 친구 요청 대기 버튼 클릭 (Old: action_friend_wait) - 이미 요청 보낸 상태 알림 */
    fun onFriendWaitClick() {
        _friendState.value = FriendState.ShowAlreadyRequestedDialog
    }

    /** 이미 친구 버튼 클릭 (Old: action_friend) - 이미 친구 상태 알림 */
    fun onAlreadyFriendClick() {
        _friendState.value = FriendState.ShowAlreadyFriendDialog
    }

    /** 친구 상태 초기화 (다이얼로그 닫은 후) */
    fun resetFriendState() {
        _friendState.value = lastStableFriendState
    }

    // 마지막 안정적인 친구 상태 (다이얼로그 후 복원용)
    private var lastStableFriendState: FriendState = FriendState.Loading

    /** 친구 상태 업데이트 (안정적인 상태만 저장) */
    private fun updateFriendState(newState: FriendState) {
        // 다이얼로그 상태가 아닌 경우만 저장
        if (newState !is FriendState.ShowAlreadyRequestedDialog &&
            newState !is FriendState.ShowAlreadyFriendDialog &&
            newState !is FriendState.Error) {
            lastStableFriendState = newState
        }
        _friendState.value = newState
    }

    // ========== 신고 관련 함수 ==========

    /** 신고 버튼 클릭 - 바텀시트 표시 (Old: onOptionsItemSelected의 action_report) */
    fun onReportClick() {
        _reportState.value = ReportState.ShowBottomSheet
    }

    /** 바텀시트에서 '신고' 선택 - 신고 가능 여부 확인 (Old: FeedActivity.report()) */
    fun onReportSelected() {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            when (val result = reportRepository.getReportPossible(userId)) {
                is ReportPossibleResult.Success -> {
                    // 신고 가능 - 하트 결제 확인 다이얼로그 표시
                    // Old: reportUser() → ReportFeedDialogFragment
                    _reportState.value = ReportState.ShowHeartConfirmDialog(result.reportHeart)
                }
                is ReportPossibleResult.AlreadyReported -> {
                    _reportState.value = ReportState.Error(gcode = result.gcode)
                }
                is ReportPossibleResult.Error -> {
                    _reportState.value = ReportState.Error(message = result.message)
                }
            }
        }
    }

    /** 하트 결제 확인 다이얼로그에서 확인 - 신고 사유 입력 다이얼로그 표시 */
    fun onHeartConfirmAccepted() {
        _reportState.value = ReportState.ShowReportReasonDialog
    }

    /** 신고 제출 */
    fun submitReport(reason: String) {
        viewModelScope.launch {
            _reportState.value = ReportState.Loading
            when (val result = reportRepository.reportUser(userId, reason)) {
                is ReportResult.Success -> {
                    _reportState.value = ReportState.Success
                }
                is ReportResult.Error -> {
                    _reportState.value = ReportState.Error(gcode = result.gcode)
                }
            }
        }
    }

    /** 신고 상태 초기화 */
    fun resetReportState() {
        _reportState.value = ReportState.Idle
    }

    // ========== 차단 관련 함수 ==========

    /** 차단 버튼 클릭 - 확인 다이얼로그 표시 */
    fun onBlockClick() {
        _blockState.value = BlockState.ShowConfirmDialog
    }

    /** 차단 확인 - API 호출 */
    fun onBlockConfirmed() {
        viewModelScope.launch {
            _blockState.value = BlockState.Loading
            when (val result = usersRepository.addBlock(userId)) {
                is BlockResult.Success -> {
                    // 차단 성공 시 isBlocked = true로 업데이트
                    currentData = currentData.copy(isBlocked = true)
                    _uiState.value = ProfileUiState.Success(user = currentData)
                    _blockState.value = BlockState.Success
                }
                is BlockResult.Error -> {
                    _blockState.value = BlockState.Error(
                        gcode = result.gcode,
                        message = result.message
                    )
                }
            }
        }
    }

    /** 차단 해제 버튼 클릭 - 바로 API 호출 */
    fun onUnblockClick() {
        onUnblockConfirmed()
    }

    /** 차단 해제 확인 - API 호출 */
    fun onUnblockConfirmed() {
        viewModelScope.launch {
            _blockState.value = BlockState.Loading
            when (val result = usersRepository.removeBlock(userId)) {
                is BlockResult.Success -> {
                    // 차단 해제 성공 시 isBlocked = false로 업데이트
                    currentData = currentData.copy(isBlocked = false)
                    _uiState.value = ProfileUiState.Success(user = currentData)
                    _blockState.value = BlockState.UnblockSuccess
                }
                is BlockResult.Error -> {
                    _blockState.value = BlockState.Error(
                        gcode = result.gcode,
                        message = result.message
                    )
                }
            }
        }
    }

    /** 차단 상태 초기화 */
    fun resetBlockState() {
        _blockState.value = BlockState.Idle
    }
}

/** 친구 상태 */
sealed interface FriendState {
    /** 로딩 중 */
    data object Loading : FriendState
    /** 친구 추가 가능 (btn_navigation_friend_add) */
    data object CanAdd : FriendState
    /** 이미 친구 (btn_navigation_friend_already) */
    data object AlreadyFriend : FriendState
    /** 친구 요청 대기 중 - API에서 받은 상태 (btn_navigation_friend_waiting) */
    data object RequestPending : FriendState
    /** 친구 요청 전송 완료 - 방금 보낸 상태 (btn_navigation_friend_waiting) */
    data object RequestSent : FriendState
    /** 이미 요청 보냄 다이얼로그 표시 */
    data object ShowAlreadyRequestedDialog : FriendState
    /** 이미 친구 다이얼로그 표시 (Old: error_8003) */
    data object ShowAlreadyFriendDialog : FriendState
    /** 에러 */
    data class Error(val gcode: Int? = null, val message: String? = null) : FriendState
}

/** 신고 상태 */
sealed interface ReportState {
    data object Idle : ReportState
    data object Loading : ReportState
    /** 1단계: 바텀시트 표시 (신고/차단 선택) */
    data object ShowBottomSheet : ReportState
    /** 2단계: 하트 결제 확인 다이얼로그 */
    data class ShowHeartConfirmDialog(val reportHeart: Int) : ReportState
    /** 3단계: 신고 사유 입력 다이얼로그 */
    data object ShowReportReasonDialog : ReportState
    data object Success : ReportState
    data class Error(val gcode: Int? = null, val message: String? = null) : ReportState
}

/** 차단 상태 */
sealed interface BlockState {
    data object Idle : BlockState
    data object Loading : BlockState
    /** 차단 확인 다이얼로그 표시 */
    data object ShowConfirmDialog : BlockState
    /** 차단 해제 확인 다이얼로그 표시 */
    data object ShowUnblockConfirmDialog : BlockState
    data object Success : BlockState
    data object UnblockSuccess : BlockState
    data class Error(val gcode: Int? = null, val message: String? = null) : BlockState
}

/** ProfileUiState - UI 상태 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val user: ProfileData) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

/** ProfileData - 유저 프로필 데이터 */
data class ProfileData(
    val id: Int,
    val nickname: String,
    val imageUrl: String?,
    val level: Int,
    val idolName: String?,
    val statusMessage: String?,
    val isFeedPrivate: Boolean = false,
    val isBlocked: Boolean = false,
    val blockStatusChecked: Boolean = false
)
