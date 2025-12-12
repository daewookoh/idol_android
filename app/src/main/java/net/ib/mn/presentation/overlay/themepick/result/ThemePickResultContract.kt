package net.ib.mn.presentation.overlay.themepick.result

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ThemePickIdol
import net.ib.mn.domain.model.ThemePickModel

/**
 * ThemePickResult (테마픽 순위 결과) 화면의 MVI Contract.
 *
 * old 프로젝트: ThemePickResultActivity, ThemePickResultAdapter
 *
 * 투표하고 나서 현재 순위를 보여주는 화면.
 */
class ThemePickResultContract {

    /**
     * UI State for ThemePickResult screen.
     */
    data class State(
        val themePick: ThemePickModel? = null,
        val rankItems: List<ThemePickIdol> = emptyList(),
        val periodText: String = "",
        val secureImageUrl: String = "",
        val isRewardExpanded: Boolean = false,
        val canVote: Boolean = false,
        val needsVideoAd: Boolean = false,
        val hasVotedToday: Boolean = false,
        val isFinished: Boolean = false,
        val isLoading: Boolean = true,
        val isVoting: Boolean = false,
        val error: String? = null
    ) : UiState

    /**
     * User intents
     */
    sealed class Intent : UiIntent {
        /** 테마픽 결과 로드 */
        data class LoadResult(val themePickId: Int) : Intent()

        /** 공유 버튼 클릭 */
        data object Share : Intent()

        /** 리워드 펼치기/접기 토글 */
        data object ToggleRewardExpand : Intent()

        /** 투표하기 버튼 클릭 */
        data object GoToVote : Intent()

        /** 아이템 클릭 (커뮤니티 이동) */
        data class OnItemClick(val idolId: Int?) : Intent()
    }

    /**
     * Side effects (일회성 이벤트)
     */
    sealed class Effect : UiEffect {
        /** 토스트 메시지 표시 */
        data class ShowToast(val message: String) : Effect()

        /** 공유 다이얼로그/인텐트 실행 */
        data class ShareThemePick(val shareText: String) : Effect()

        /** 투표 화면으로 이동 (ThemePickDetail) */
        data class NavigateToVote(val themePickId: Int) : Effect()

        /** 커뮤니티 화면으로 이동 */
        data class NavigateToCommunity(val idolId: Int) : Effect()

        /** 뒤로가기 */
        data object NavigateBack : Effect()
    }
}
