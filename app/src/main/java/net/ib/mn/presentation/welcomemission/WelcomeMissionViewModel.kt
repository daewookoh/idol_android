package net.ib.mn.presentation.welcomemission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.MissionRepository
import javax.inject.Inject

/**
 * 웰컴 미션 ViewModel
 *
 * old 프로젝트의 MissionViewModel과 동일한 로직
 */
@HiltViewModel
class WelcomeMissionViewModel @Inject constructor(
    private val missionRepository: MissionRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WelcomeMissionUiState())
    val uiState: StateFlow<WelcomeMissionUiState> = _uiState.asStateFlow()

    private val _navigation = MutableSharedFlow<MissionNavigation>()
    val navigation = _navigation.asSharedFlow()

    private val _closeScreen = MutableSharedFlow<Boolean>() // true: all clear, false: normal close
    val closeScreen = _closeScreen.asSharedFlow()

    init {
        loadMissions()
    }

    /**
     * 웰컴 미션 목록 로드
     */
    fun loadMissions() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            missionRepository.getWelcomeMission().collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data.missionData
                        if (data != null) {
                            val missions = data.list.map { WelcomeMissionItem.fromDto(it) }
                            val allClearReward = WelcomeMissionItem.fromDto(data.allClearReward)

                            // 모든 미션이 완료(DONE)이고, ALL CLEAR 보상이 수령 가능(REWARD)한지 확인
                            val allMissionsDone = missions.all { it.status == MissionStatus.DONE }
                            val isAllClear = allMissionsDone && allClearReward.status == MissionStatus.DONE

                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    missions = missions,
                                    allClearReward = allClearReward,
                                    isAllClear = isAllClear,
                                    error = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = result.data.msg ?: "Failed to load missions"
                                )
                            }
                        }
                    }

                    is ApiResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = result.message
                            )
                        }
                    }

                    is ApiResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    /**
     * 미션 클릭 처리
     */
    fun onMissionClick(mission: WelcomeMissionItem) {
        viewModelScope.launch {
            when (mission.status) {
                MissionStatus.GOING -> {
                    // 미션 수행을 위한 화면으로 이동
                    val nav = when (mission.info) {
                        MissionItemInfo.WELCOME_SET_MOST -> MissionNavigation.SetMost
                        MissionItemInfo.WELCOME_VOTE_MOST -> MissionNavigation.VoteMost
                        MissionItemInfo.WELCOME_ADD_FRIEND -> MissionNavigation.AddFriend
                        MissionItemInfo.WELCOME_POSTING_MOST -> MissionNavigation.Posting
                        MissionItemInfo.WELCOME_VIDEOAD -> MissionNavigation.VideoAd
                        else -> MissionNavigation.None
                    }
                    _navigation.emit(nav)
                }

                MissionStatus.REWARD -> {
                    // 보상 수령
                    claimReward(mission.key)
                }

                MissionStatus.DONE -> {
                    // 이미 완료된 미션 - 아무 동작 없음
                }
            }
        }
    }

    /**
     * ALL CLEAR 보상 수령
     */
    fun onAllClearClick() {
        val allClearReward = _uiState.value.allClearReward ?: return

        if (allClearReward.status == MissionStatus.REWARD) {
            claimReward(allClearReward.key)
        }
    }

    /**
     * 보상 수령 API 호출
     */
    private fun claimReward(key: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(claimingMissionKey = key) }

            missionRepository.claimMissionReward(key).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val rewardData = result.data.rewardData
                        if (result.data.success && rewardData != null) {
                            // 보상 수령 성공 - 다이얼로그 표시를 위해 저장
                            _uiState.update {
                                it.copy(
                                    claimingMissionKey = null,
                                    claimedReward = ClaimedReward(
                                        item = rewardData.item,
                                        amount = rewardData.amount
                                    )
                                )
                            }
                            // 미션 목록 새로고침
                            loadMissions()
                        } else {
                            _uiState.update {
                                it.copy(
                                    claimingMissionKey = null,
                                    error = result.data.msg ?: "Failed to claim reward"
                                )
                            }
                        }
                    }

                    is ApiResult.Error -> {
                        _uiState.update {
                            it.copy(
                                claimingMissionKey = null,
                                error = result.message
                            )
                        }
                    }

                    is ApiResult.Loading -> {
                        // Loading state is already set
                    }
                }
            }
        }
    }

    /**
     * 보상 다이얼로그 닫기
     */
    fun dismissRewardDialog() {
        _uiState.update { it.copy(claimedReward = null) }
    }

    /**
     * 에러 메시지 클리어
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * 화면 닫기
     */
    fun closeScreen() {
        viewModelScope.launch {
            // ALL CLEAR 상태로 화면 닫기 (플로팅 버튼 숨김 처리를 위해)
            val isAllClear = _uiState.value.isAllClear
            if (isAllClear) {
                preferencesManager.setShowWelcomeMission(false)
            }
            _closeScreen.emit(isAllClear)
        }
    }
}
