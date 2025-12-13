package net.ib.mn.presentation.imagepick.live

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ImagePickIdolModel
import net.ib.mn.domain.model.ImagePickModel

/**
 * ImagePickLive (이미지픽 순위 결과) 화면의 MVI Contract.
 *
 * old 프로젝트: OnepickResultActivity
 *
 * 투표 결과 순위를 보여주는 화면.
 */
class ImagePickLiveContract {

    /**
     * UI State
     */
    data class State(
        val imagePick: ImagePickModel? = null,
        val rankItems: List<ImagePickIdolModel> = emptyList(),
        val periodText: String = "",
        val date: String = "",
        val canVote: Boolean = false,
        val needsVideoAd: Boolean = false,
        val hasVotedToday: Boolean = false,
        val isFinished: Boolean = false,
        val isLoading: Boolean = true,
        val error: String? = null
    ) : UiState

    /**
     * User intents
     */
    sealed class Intent : UiIntent {
        /** 이미지픽 결과 로드 */
        data class LoadResult(val imagePickId: Int) : Intent()

        /** 공유 버튼 클릭 */
        data object Share : Intent()

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

        /** 공유 인텐트 실행 */
        data class ShareImagePick(val shareText: String) : Effect()

        /** 투표 화면으로 이동 (ImagePickDetail) */
        data class NavigateToVote(val imagePickId: Int) : Effect()

        /** 커뮤니티 화면으로 이동 */
        data class NavigateToCommunity(val idolId: Int) : Effect()

        /** 뒤로가기 */
        data object NavigateBack : Effect()

        /** 광고 표시 */
        data object ShowVideoAd : Effect()
    }
}
