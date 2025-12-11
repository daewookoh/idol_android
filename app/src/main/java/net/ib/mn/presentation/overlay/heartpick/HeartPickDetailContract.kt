package net.ib.mn.presentation.overlay.heartpick

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.HeartPickIdol
import net.ib.mn.domain.model.HeartPickModel

/**
 * HeartPickDetail (하트픽 상세) 화면의 MVI Contract.
 *
 * old 프로젝트: HeartPickActivity, HeartPickPrelaunchActivity
 *
 * 세 가지 상태를 모두 대응:
 * - STATUS_PRELAUNCH (0): 투표 예정 - 아이돌 목록만 표시, 알림 설정 버튼
 * - STATUS_VOTING (1): 투표 중 - 순위 + 투표 버튼
 * - STATUS_VOTE_FINISHED (2): 투표 종료 - 최종 결과 표시
 */
class HeartPickDetailContract {

    /**
     * 하트픽 상태
     */
    enum class HeartPickStatus {
        PRELAUNCH,      // 투표 예정
        VOTING,         // 투표 중
        VOTE_FINISHED   // 투표 종료
    }

    /**
     * UI State for HeartPickDetail screen.
     */
    data class State(
        val heartPick: HeartPickModel? = null,
        val status: HeartPickStatus = HeartPickStatus.VOTING,
        val dDayText: String = "",
        val periodText: String = "",
        val secureBannerUrl: String = "",  // HTTPS로 변환된 배너 URL
        val myIdol: HeartPickIdol? = null,
        val myIdolPosition: Int = -1,
        val isNotifyEnabled: Boolean = false,
        val isRewardExpanded: Boolean = false,
        val isLoading: Boolean = true,
        val error: String? = null
    ) : UiState

    /**
     * User intents
     */
    sealed class Intent : UiIntent {
        /** 하트픽 상세 로드 */
        data class LoadHeartPick(val heartPickId: Int) : Intent()

        /** 공유 버튼 클릭 */
        data object Share : Intent()

        /** 아이돌 클릭 시 커뮤니티로 이동 */
        data class GoToCommunity(val idolId: Int) : Intent()

        /** 댓글 보기/작성 */
        data object GoToComment : Intent()

        /** 투표하기 (진행중 상태) */
        data class Vote(val idol: HeartPickIdol) : Intent()

        /** 알림 설정 토글 (예정 상태) */
        data object ToggleNotification : Intent()

        /** 리워드 펼치기/접기 토글 */
        data object ToggleRewardExpand : Intent()

        /** 댓글 수 업데이트 (CommentOnlyScreen에서 댓글 작성/삭제 후 호출) */
        data class UpdateCommentCount(val newCount: Int) : Intent()

        /** 댓글 수 증가 (댓글 작성 시) */
        data object IncrementCommentCount : Intent()

        /** 댓글 수 감소 (댓글 삭제 시) */
        data object DecrementCommentCount : Intent()
    }

    /**
     * Side effects (일회성 이벤트)
     */
    sealed class Effect : UiEffect {
        /** 토스트 메시지 표시 */
        data class ShowToast(val message: String) : Effect()

        /** 공유 다이얼로그/인텐트 실행 */
        data class ShareHeartPick(val shareText: String) : Effect()

        /** 커뮤니티 화면으로 이동 */
        data class NavigateToCommunity(val idolId: Int, val groupId: Int) : Effect()

        /** 댓글 화면으로 이동 */
        data class NavigateToComment(val heartPickId: Int) : Effect()

        /** 투표 다이얼로그 표시 */
        data class ShowVoteDialog(val idol: HeartPickIdol, val heartPickId: Int) : Effect()

        /** 알림 설정 완료 토스트 */
        data object ShowNotifyEnabledToast : Effect()

        /** 뒤로가기 */
        data object NavigateBack : Effect()
    }
}
