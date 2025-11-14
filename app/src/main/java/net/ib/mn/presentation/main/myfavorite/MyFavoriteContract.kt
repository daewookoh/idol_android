package net.ib.mn.presentation.main.myfavorite

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.MostPicksModel

/**
 * My Favorite Contract
 *
 * 최애 관리 화면의 State, Intent, Effect 정의
 */
class MyFavoriteContract {

    /**
     * UI State
     *
     * @property isLoading 로딩 상태
     * @property favoriteIdols 최애 아이돌 리스트
     * @property error 에러 메시지
     * @property topFavorite 1순위 최애 (top3 이미지 표시용)
     * @property mostPicksModel 픽 참여 정보 (Support Bias Bar 표시용)
     */
    data class State(
        val isLoading: Boolean = false,
        val favoriteIdols: List<FavoriteIdol> = emptyList(),
        val error: String? = null,
        val mostFavoriteIdol: MostFavoriteIdol? = null,
        val mostPicksModel: MostPicksModel? = null
    ) : UiState

    /**
     * User Intent
     */
    sealed class Intent : UiIntent {
        /**
         * 페이지가 visible 될 때
         */
        data object OnPageVisible : Intent()

        /**
         * 화면이 보일 때
         */
        data object OnScreenVisible : Intent()

        /**
         * 화면이 숨겨질 때
         */
        data object OnScreenHidden : Intent()

        /**
         * 최애 목록 로드
         */
        data object LoadFavorites : Intent()

        /**
         * 최애 아이돌 클릭
         */
        data class OnIdolClick(val idolId: Int) : Intent()

        /**
         * 최애 설정 버튼 클릭
         */
        data object OnSettingClick : Intent()

        /**
         * 새로고침
         */
        data object RefreshFavorites : Intent()

        /**
         * 투표 성공 (즉시 데이터 업데이트)
         */
        data class OnVoteSuccess(val idolId: Int, val votedHeart: Long) : Intent()

        /**
         * Support Bias Bar 클릭
         */
        data class OnSupportBiasBarClick(val id: Int, val kind: String) : Intent()
    }

    /**
     * Side Effect
     */
    sealed class Effect : UiEffect {
        /**
         * 아이돌 상세 페이지로 이동
         */
        data class NavigateToIdolDetail(val idolId: Int) : Effect()

        /**
         * 최애 설정 페이지로 이동
         */
        data object NavigateToFavoriteSetting : Effect()

        /**
         * 에러 메시지 표시
         */
        data class ShowError(val message: String) : Effect()

        /**
         * 토스트 메시지 표시
         */
        data class ShowToast(val message: String) : Effect()

        /**
         * 웹 페이지로 이동 (Support Bias Bar 클릭)
         */
        data class NavigateToWebPage(val url: String) : Effect()
    }

    /**
     * 최애 아이돌 데이터 모델
     *
     * OLD 프로젝트의 RankingModel과 동일하게 섹션 관련 필드 추가
     */
    data class FavoriteIdol(
        val idolId: Int,
        val name: String,
        val imageUrl: String,
        val rank: Int? = null,
        val score: Long? = null,
        val chartCode: String? = null,        // 차트 코드 (PR_S_F, PR_G_M 등)
        val isSection: Boolean = false,        // 섹션 헤더 여부
        val sectionName: String? = null,       // 섹션 이름 (예: "남자 그룹", "여자 개인")
        val sectionMaxScore: Long? = null,     // 섹션 내 최대 하트 수 (progress bar용)
        val top3ImageUrls: List<String?> = emptyList(),  // Top3 이미지 URL
        val top3VideoUrls: List<String?> = emptyList()   // Top3 비디오 URL
    )

    /**
     * 최애 아이돌 정보 (실시간 랭킹 데이터 기반)
     *
     * SharedPreference의 mostIdolId와 mostChartCode를 기준으로
     * 해당 차트의 실시간 랭킹 데이터에서 정보 추출
     */
    data class MostFavoriteIdol(
        val idolId: Int,
        val name: String,
        val top3ImageUrls: List<String?>,  // 3개의 이미지 URL
        val top3VideoUrls: List<String?>,  // 3개의 비디오 URL
        val rank: Int?,                    // 해당 차트에서의 순위
        val heart: Long?,                  // 해당 차트에서의 하트 수
        val chartCode: String?,            // 기준 차트 코드
        val imageUrl: String?              // 기본 이미지 URL
    )
}
