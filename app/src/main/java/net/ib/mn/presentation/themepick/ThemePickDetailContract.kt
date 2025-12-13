package net.ib.mn.presentation.themepick

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ThemePickIdol
import net.ib.mn.domain.model.ThemePickModel

/**
 * ThemePickDetail (테마픽 상세) 화면의 MVI Contract.
 *
 * old 프로젝트: ThemePickRankActivity, ThemePickResultActivity
 *
 * 세 가지 상태를 모두 대응:
 * - STATUS_PREPARING (0): 투표 예정 - 후보 목록, 알림 설정 버튼
 * - STATUS_PROGRESS (1): 투표 중 - 순위 + 투표 버튼
 * - STATUS_FINISHED (2): 투표 종료 - 최종 결과 표시
 */
class ThemePickDetailContract {

    /**
     * 테마픽 상태
     */
    enum class ThemePickStatus {
        PREPARING,      // 투표 예정
        PROGRESS,       // 투표 중
        FINISHED        // 투표 종료
    }

    /**
     * UI State for ThemePickDetail screen.
     */
    data class State(
        val themePick: ThemePickModel? = null,
        val status: ThemePickStatus = ThemePickStatus.PROGRESS,
        val dDayText: String = "",
        val periodText: String = "",
        val secureImageUrl: String = "",  // HTTPS로 변환된 이미지 URL
        val selectedCandidate: ThemePickIdol? = null,  // 현재 선택된 후보
        val isNotifyEnabled: Boolean = false,  // 알림 설정 여부 (예정 상태)
        val isRewardExpanded: Boolean = false,  // 1위 보상 펼침 여부
        val canVote: Boolean = false,  // 투표 가능 여부
        val needsVideoAd: Boolean = false,  // 광고 시청 후 투표 여부
        val hasVotedToday: Boolean = false,  // 오늘 투표 완료 여부
        val isLoading: Boolean = true,
        val isVoting: Boolean = false,  // 투표 진행 중
        val error: String? = null
    ) : UiState

    /**
     * User intents
     */
    sealed class Intent : UiIntent {
        /** 테마픽 상세 로드 */
        data class LoadThemePick(val themePickId: Int) : Intent()

        /** 공유 버튼 클릭 */
        data object Share : Intent()

        /** 후보 선택 */
        data class SelectCandidate(val candidate: ThemePickIdol) : Intent()

        /** 후보 선택 해제 */
        data object DeselectCandidate : Intent()

        /** 투표하기 (진행중 상태) */
        data object Vote : Intent()

        /** 현재 순위 보기 */
        data object GoToResult : Intent()

        /** 알림 설정 토글 (예정 상태) */
        data object ToggleNotification : Intent()

        /** 리워드 펼치기/접기 토글 */
        data object ToggleRewardExpand : Intent()

        /** 투표 성공 후 데이터 새로고침 */
        data object RefreshAfterVote : Intent()

        /** 광고 시청 완료 후 투표 */
        data object VoteAfterAd : Intent()
    }

    /**
     * Side effects (일회성 이벤트)
     */
    sealed class Effect : UiEffect {
        /** 토스트 메시지 표시 */
        data class ShowToast(val message: String) : Effect()

        /** 공유 다이얼로그/인텐트 실행 */
        data class ShareThemePick(val shareText: String) : Effect()

        /** 결과 화면으로 이동 */
        data class NavigateToResult(val themePickId: Int) : Effect()

        /** 투표 완료 다이얼로그 표시 */
        data class ShowVoteCompleteDialog(
            val candidateName: String,
            val rank: Int,
            val voteGapFromFirst: Long
        ) : Effect()

        /** 알림 설정 완료 토스트 */
        data object ShowNotifyEnabledToast : Effect()

        /** 뒤로가기 */
        data object NavigateBack : Effect()

        /** 광고 시청 필요 */
        data object ShowVideoAd : Effect()

        /** 투표 불가 안내 (이미 투표함) */
        data object ShowAlreadyVotedDialog : Effect()

        /** 참여자 없음 안내 */
        data object ShowNoParticipantsDialog : Effect()
    }
}
