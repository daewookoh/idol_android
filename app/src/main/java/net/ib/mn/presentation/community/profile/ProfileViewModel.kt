package net.ib.mn.presentation.community.profile

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

            // getStatus API에서 상태 메시지 및 비공개 여부 로드
            loadStatusInfo()
        }
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

    /** 본인 프로필: UserCacheRepository에서 most 정보 조회 */
    private fun loadMostFromCache(): String? {
        val most = userCacheRepository.getUserData()?.most ?: return null
        return LocaleUtil.getLocalizedIdolName(context, most).takeIf { it.isNotEmpty() }
    }

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
    /** Old 호환용 - 한 번에 신고 다이얼로그 표시 */
    data object ShowReportDialog : ReportState
    data object Success : ReportState
    data class Error(val gcode: Int? = null, val message: String? = null) : ReportState
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
    val isFeedPrivate: Boolean = false
)
