package net.ib.mn.presentation.search

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.SearchSuggestModel
import net.ib.mn.domain.model.SearchTrendModel

/**
 * Search 화면의 MVI Contract.
 *
 * old 프로젝트의 SearchHistoryActivity + SearchHistoryViewModel 로직을 MVI 패턴으로 구현.
 * - 핫 트렌드 (인기 검색어)
 * - 최근 검색어 (DataStore 저장)
 * - 자동완성 추천
 * - 인앱 배너
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator를 통해 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출)에만 집중
 */
class SearchContract {

    /**
     * 검색 화면 표시 모드
     * Old 프로젝트와 동일: TRENDS (핫 트렌드) / HISTORY (최근 검색어) / SUGGESTIONS (자동완성)
     */
    enum class DisplayMode {
        TRENDS,      // 핫 트렌드 + 배너 (검색창 포커스 X 또는 입력 X 상태)
        HISTORY,     // 최근 검색어 (검색창 포커스 O + 입력 X 상태)
        SUGGESTIONS  // 자동완성 추천 (검색창 입력 O 상태)
    }

    /**
     * UI State for Search screen.
     *
     * @property searchQuery 현재 검색어
     * @property displayMode 화면 표시 모드
     * @property hotTrends 핫 트렌드 목록
     * @property recentSearches 최근 검색어 목록
     * @property suggestions 자동완성 추천 목록
     * @property banners 인앱 배너 목록
     * @property isLoading 로딩 중 여부
     * @property error 에러 메시지
     */
    data class State(
        val searchQuery: String = "",
        val displayMode: DisplayMode = DisplayMode.TRENDS,
        val hotTrends: List<SearchTrendModel> = emptyList(),
        val recentSearches: List<String> = emptyList(),
        val suggestions: List<SearchSuggestModel> = emptyList(),
        val banners: List<InAppBanner> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null
    ) : UiState

    /**
     * User intents (비즈니스 로직 관련 액션만).
     * 네비게이션은 Screen에서 LocalAppNavigator로 직접 처리.
     */
    sealed class Intent : UiIntent {
        /**
         * 화면 진입 시 검색어 리셋
         */
        data object ResetSearchQuery : Intent()

        /**
         * 검색창 포커스 상태 변경 (Old 프로젝트의 키보드 show/hide 감지와 동일)
         */
        data class SetSearchFocused(val focused: Boolean) : Intent()

        /**
         * 검색어 입력 변경
         */
        data class UpdateSearchQuery(val query: String) : Intent()

        /**
         * 검색 실행 - 최근 검색어 저장 후 콜백 호출
         */
        data class SaveSearchAndNavigate(val keyword: String) : Intent()

        /**
         * 최근 검색어 개별 삭제
         */
        data class DeleteRecentSearch(val keyword: String) : Intent()

        /**
         * 최근 검색어 전체 삭제
         */
        data object ClearAllRecentSearches : Intent()
    }

    /**
     * Side effects (일회성 이벤트).
     * 네비게이션은 Screen에서 직접 처리하므로 여기서는 UI 관련 이벤트만.
     */
    sealed class Effect : UiEffect {
        /**
         * 토스트 메시지 표시
         */
        data class ShowToast(val message: String) : Effect()

        /**
         * 키보드 숨기기
         */
        data object HideKeyboard : Effect()

        /**
         * 검색어 저장 완료 - 네비게이션 진행 가능
         */
        data class SearchSaved(val keyword: String) : Effect()
    }
}
