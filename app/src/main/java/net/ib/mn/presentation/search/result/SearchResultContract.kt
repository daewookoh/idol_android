package net.ib.mn.presentation.search.result

import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.SearchIdolModel
import net.ib.mn.domain.model.SearchSupportModel
import net.ib.mn.domain.model.SearchWallpaperModel

/**
 * SearchResult 화면의 MVI Contract.
 *
 * old 프로젝트의 SearchResultActivity 로직을 MVI 패턴으로 구현.
 * - 아이돌 검색 결과 (최대 3개 + 더보기)
 * - 서포트 검색 결과 (최대 3개 + 더보기)
 * - 배경화면 검색 결과
 * - 잡담게시판 검색 결과 (무한 스크롤)
 * - 커뮤니티 게시글 검색 결과 (무한 스크롤)
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator를 통해 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출, 즐겨찾기 관리)에만 집중
 */
class SearchResultContract {

    /**
     * 검색 결과 섹션 타입
     */
    enum class SectionType {
        IDOL,       // 아이돌
        SUPPORT,    // 서포트
        WALLPAPER,  // 배경화면
        SMALL_TALK, // 잡담게시판
        ARTICLE     // 커뮤니티 게시글
    }

    /**
     * UI State for SearchResult screen.
     *
     * @property keyword 검색 키워드
     * @property idols 아이돌 검색 결과
     * @property showAllIdols 모든 아이돌 표시 여부 (기본 3개만)
     * @property supports 서포트 검색 결과
     * @property showAllSupports 모든 서포트 표시 여부 (기본 3개만)
     * @property wallpapers 배경화면 검색 결과
     * @property smallTalks 잡담게시판 검색 결과
     * @property articles 커뮤니티 게시글 검색 결과
     * @property isLoading 로딩 중 여부
     * @property isLoadingMoreSmallTalk 잡담 추가 로딩 중
     * @property isLoadingMoreArticle 게시글 추가 로딩 중
     * @property hasMoreSmallTalks 잡담 더 있는지
     * @property hasMoreArticles 게시글 더 있는지
     * @property smallTalkOffset 잡담 페이징 오프셋
     * @property articleOffset 게시글 페이징 오프셋
     * @property error 에러 메시지
     * @property isEmpty 검색 결과 없음
     */
    data class State(
        val keyword: String = "",
        val idols: List<SearchIdolModel> = emptyList(),
        val showAllIdols: Boolean = false,
        val supports: List<SearchSupportModel> = emptyList(),
        val showAllSupports: Boolean = false,
        val wallpapers: List<SearchWallpaperModel> = emptyList(),
        val smallTalks: List<ArticleModel> = emptyList(),
        val articles: List<ArticleModel> = emptyList(),
        val isLoading: Boolean = false,
        val isLoadingMoreSmallTalk: Boolean = false,
        val isLoadingMoreArticle: Boolean = false,
        val hasMoreSmallTalks: Boolean = true,
        val hasMoreArticles: Boolean = true,
        val smallTalkOffset: Int = 0,
        val articleOffset: Int = 0,
        val error: String? = null,
        val isEmpty: Boolean = false
    ) : UiState {
        /**
         * 표시할 아이돌 목록 (showAllIdols에 따라 3개 또는 전체)
         */
        val displayIdols: List<SearchIdolModel>
            get() = if (showAllIdols) idols else idols.take(3)

        /**
         * 아이돌 더보기 버튼 표시 여부
         */
        val showIdolsMore: Boolean
            get() = !showAllIdols && idols.size > 3

        /**
         * 표시할 서포트 목록 (showAllSupports에 따라 3개 또는 전체)
         */
        val displaySupports: List<SearchSupportModel>
            get() = if (showAllSupports) supports else supports.take(3)

        /**
         * 서포트 더보기 버튼 표시 여부
         */
        val showSupportsMore: Boolean
            get() = !showAllSupports && supports.size > 3
    }

    /**
     * User intents (비즈니스 로직 관련 액션만).
     * 네비게이션 관련 클릭은 Screen에서 LocalAppNavigator로 직접 처리.
     */
    sealed class Intent : UiIntent {
        /**
         * 검색 실행
         */
        data class Search(val keyword: String) : Intent()

        /**
         * 검색어 업데이트
         */
        data class UpdateSearchQuery(val query: String) : Intent()

        /**
         * 새 검색 (SearchScreen에서 재검색)
         */
        data class NewSearch(val keyword: String) : Intent()

        /**
         * 아이돌 더보기 클릭
         */
        data object ShowAllIdols : Intent()

        /**
         * 서포트 더보기 클릭
         */
        data object ShowAllSupports : Intent()

        /**
         * 잡담 더 로드 (무한 스크롤)
         */
        data object LoadMoreSmallTalks : Intent()

        /**
         * 게시글 더 로드 (무한 스크롤)
         */
        data object LoadMoreArticles : Intent()

        /**
         * 즐겨찾기 토글
         */
        data class ToggleFavorite(val idol: SearchIdolModel) : Intent()

        /**
         * 최애 설정 (다이얼로그 표시)
         */
        data class SetMost(val idol: SearchIdolModel) : Intent()

        /**
         * 최애 설정 확정 (다이얼로그 확인 후)
         */
        data class ConfirmSetMost(val idol: SearchIdolModel) : Intent()

        /**
         * 아이돌 클릭
         */
        data class ClickIdol(val idol: SearchIdolModel) : Intent()

        /**
         * 아이돌 커뮤니티 버튼 클릭
         */
        data class ClickIdolCommunity(val idol: SearchIdolModel) : Intent()

        /**
         * 아이돌 잡담 버튼 클릭
         */
        data class ClickIdolSmallTalk(val idol: SearchIdolModel) : Intent()

        /**
         * 아이돌 스케줄 버튼 클릭
         */
        data class ClickIdolSchedule(val idol: SearchIdolModel) : Intent()

        /**
         * 서포트 클릭
         */
        data class ClickSupport(val support: SearchSupportModel) : Intent()

        /**
         * 배경화면 클릭
         */
        data class ClickWallpaper(val wallpaper: SearchWallpaperModel) : Intent()

        /**
         * 게시글 클릭
         */
        data class ClickArticle(val article: ArticleModel) : Intent()

        /**
         * 뒤로가기
         */
        data object NavigateBack : Intent()
    }

    /**
     * Side effects (일회성 이벤트).
     */
    sealed class Effect : UiEffect {
        /**
         * 아이돌 커뮤니티 화면으로 이동
         */
        data class NavigateToCommunity(
            val idolId: Int,
            val category: String = "community"
        ) : Effect()

        /**
         * 아이돌 잡담 화면으로 이동
         */
        data class NavigateToSmallTalk(val idolId: Int) : Effect()

        /**
         * 아이돌 스케줄 화면으로 이동
         */
        data class NavigateToSchedule(val idolId: Int) : Effect()

        /**
         * 서포트 상세 화면으로 이동
         */
        data class NavigateToSupportDetail(val supportId: Int) : Effect()

        /**
         * 배경화면 상세 화면으로 이동
         */
        data class NavigateToWallpaperDetail(val idolId: Int) : Effect()

        /**
         * 게시글 상세 화면으로 이동
         */
        data class NavigateToArticleDetail(val articleId: String) : Effect()

        /**
         * 뒤로가기
         */
        data object NavigateBack : Effect()

        /**
         * 토스트 메시지 표시
         */
        data class ShowToast(val message: String) : Effect()

        /**
         * 최애 설정 확인 다이얼로그 표시
         */
        data class ShowSetMostDialog(val idol: SearchIdolModel) : Effect()
    }
}
