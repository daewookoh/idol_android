package net.ib.mn.presentation.welcomemission

import net.ib.mn.data.remote.dto.WelcomeMissionItemDto

/**
 * 웰컴 미션 화면 UI 상태
 */
data class WelcomeMissionUiState(
    val isLoading: Boolean = true,
    val missions: List<WelcomeMissionItem> = emptyList(),
    val allClearReward: WelcomeMissionItem? = null,
    val isAllClear: Boolean = false,
    val error: String? = null,
    val claimingMissionKey: String? = null, // 현재 보상 수령 중인 미션 키
    val claimedReward: ClaimedReward? = null // 방금 수령한 보상 정보 (다이얼로그 표시용)
)

/**
 * 개별 미션 항목 UI 모델
 */
data class WelcomeMissionItem(
    val key: String,
    val status: MissionStatus,
    val item: String, // "heart" or "diamond"
    val amount: Int,
    val info: MissionItemInfo?
) {
    companion object {
        fun fromDto(dto: WelcomeMissionItemDto): WelcomeMissionItem {
            return WelcomeMissionItem(
                key = dto.key,
                status = MissionStatus.fromStatus(dto.status),
                item = dto.item,
                amount = dto.amount,
                info = MissionItemInfo.fromKey(dto.key)
            )
        }
    }
}

/**
 * 수령한 보상 정보
 */
data class ClaimedReward(
    val item: String, // "heart" or "diamond"
    val amount: Int
)

/**
 * 미션 클릭 시 이동할 화면
 */
sealed class MissionNavigation {
    data object SetMost : MissionNavigation()
    data object VoteMost : MissionNavigation()
    data object AddFriend : MissionNavigation()
    data object Posting : MissionNavigation()
    data object VideoAd : MissionNavigation()
    data object None : MissionNavigation()
}
