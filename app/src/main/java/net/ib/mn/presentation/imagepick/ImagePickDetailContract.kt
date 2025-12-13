package net.ib.mn.presentation.imagepick

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ImagePickIdolModel
import net.ib.mn.domain.model.ImagePickModel

/**
 * ImagePickDetail (이미지픽 상세/투표) 화면의 MVI Contract.
 *
 * old 프로젝트: OnepickMatchActivity, OnepickResultActivity
 *
 * 세 가지 상태를 대응:
 * - PREPARING: 투표 예정 - 알림 설정 버튼
 * - PROGRESS: 투표 중 - 토너먼트 형식 투표 (3x3 그리드에서 선택)
 * - FINISHED: 투표 종료 - 최종 결과 순위 표시
 */
class ImagePickDetailContract {

    /**
     * 이미지픽 상태
     */
    enum class ImagePickStatus {
        PREPARING,  // 투표 예정
        PROGRESS,   // 투표 진행 중
        FINISHED    // 투표 종료
    }

    /**
     * 토너먼트 라운드
     */
    enum class TournamentRound {
        QUALIFYING,  // 예선
        FINAL        // 결승
    }

    /**
     * UI State
     */
    data class State(
        val imagePick: ImagePickModel? = null,
        val candidates: List<ImagePickIdolModel> = emptyList(),
        val date: String = "",
        val periodText: String = "",
        val status: ImagePickStatus = ImagePickStatus.PROGRESS,
        val isNotifyEnabled: Boolean = false,

        // 투표 상태
        val canVote: Boolean = true,
        val needsVideoAd: Boolean = false,
        val hasVotedToday: Boolean = false,
        val voteType: String = "N",

        // 토너먼트 상태
        val tournamentRound: TournamentRound = TournamentRound.QUALIFYING,
        val currentRoundCandidates: List<ImagePickIdolModel> = emptyList(),
        val selectedPicks: List<Int> = emptyList(),  // 선택한 후보 ID 목록
        val qualifyingWinners: List<ImagePickIdolModel> = emptyList(),  // 예선 통과자
        val totalRounds: Int = 0,
        val currentRoundIndex: Int = 0,
        val dimension: Int = 3,  // 그리드 크기 (3x3)

        // 로딩 상태
        val isLoading: Boolean = true,
        val isVoting: Boolean = false,
        val error: String? = null
    ) : UiState

    /**
     * User intents
     */
    sealed class Intent : UiIntent {
        /** 이미지픽 데이터 로드 */
        data class LoadImagePick(val imagePickId: Int) : Intent()

        /** 알림 설정 토글 */
        data object ToggleNotification : Intent()

        /** 투표 시작 (토너먼트 시작) */
        data object StartVote : Intent()

        /** 이미지 선택 (토너먼트에서) */
        data class SelectImage(val candidate: ImagePickIdolModel) : Intent()

        /** 투표 완료 (최종 선택 후 API 호출) */
        data object SubmitVote : Intent()

        /** 광고 시청 후 투표 */
        data object VoteAfterAd : Intent()

        /** 공유 버튼 클릭 */
        data object Share : Intent()

        /** 결과 화면으로 이동 */
        data object GoToResult : Intent()

        /** 투표 중 나가기 (참여 처리) */
        data object ExitEarly : Intent()
    }

    /**
     * Side effects (일회성 이벤트)
     */
    sealed class Effect : UiEffect {
        /** 토스트 메시지 표시 */
        data class ShowToast(val message: String) : Effect()

        /** 공유 인텐트 실행 */
        data class ShareImagePick(val shareText: String) : Effect()

        /** 결과 화면으로 이동 */
        data class NavigateToResult(val imagePickId: Int) : Effect()

        /** 투표 완료 다이얼로그 표시 */
        data class ShowVoteCompleteDialog(
            val candidateName: String,
            val rank: Int
        ) : Effect()

        /** 알림 설정 완료 토스트 */
        data object ShowNotifyEnabledToast : Effect()

        /** 뒤로가기 */
        data object NavigateBack : Effect()

        /** 광고 표시 */
        data object ShowVideoAd : Effect()

        /** 이미 투표 완료 다이얼로그 */
        data object ShowAlreadyVotedDialog : Effect()

        /** 참여자 없음 다이얼로그 */
        data object ShowNoParticipantsDialog : Effect()

        /** 투표 중 나가기 확인 다이얼로그 */
        data class ShowExitConfirmDialog(val canVote: Boolean) : Effect()
    }
}
