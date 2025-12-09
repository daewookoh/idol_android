package net.ib.mn.presentation.search.result

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import net.ib.mn.base.BaseViewModel
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.SearchIdolModel
import net.ib.mn.domain.model.SearchSupportModel
import net.ib.mn.domain.model.SearchWallpaperModel
import net.ib.mn.domain.repository.FavoritesRepository
import net.ib.mn.domain.repository.SearchRepository
import javax.inject.Inject

/**
 * SearchResult 화면 ViewModel
 *
 * old 프로젝트의 SearchResultActivity를 MVI 패턴으로 재구현
 * - 통합 검색 결과 표시
 * - 아이돌/서포트 더보기
 * - 잡담/게시글 무한 스크롤
 * - 즐겨찾기/최애 설정
 *
 * Navigation 3 활용:
 * - 네비게이션은 Screen에서 LocalAppNavigator로 직접 처리
 * - ViewModel은 비즈니스 로직(상태 관리, API 호출, 즐겨찾기 관리)에만 집중
 */
@HiltViewModel
class SearchResultViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val favoritesRepository: FavoritesRepository
) : BaseViewModel<SearchResultContract.State, SearchResultContract.Intent, SearchResultContract.Effect>() {

    companion object {
        private const val SMALL_TALK_LIMIT = 10
        private const val ARTICLE_LIMIT = 50
    }

    override fun createInitialState(): SearchResultContract.State = SearchResultContract.State()

    override fun handleIntent(intent: SearchResultContract.Intent) {
        when (intent) {
            is SearchResultContract.Intent.Search -> search(intent.keyword)
            is SearchResultContract.Intent.NewSearch -> newSearch(intent.keyword)
            is SearchResultContract.Intent.ShowAllIdols -> showAllIdols()
            is SearchResultContract.Intent.ShowAllSupports -> showAllSupports()
            is SearchResultContract.Intent.LoadMoreSmallTalks -> loadMoreSmallTalks()
            is SearchResultContract.Intent.LoadMoreArticles -> loadMoreArticles()
            is SearchResultContract.Intent.ToggleFavorite -> toggleFavorite(intent.idol)
            is SearchResultContract.Intent.SetMost -> handleSetMost(intent.idol)
            is SearchResultContract.Intent.ClickIdol -> handleIdolClick(intent.idol)
            is SearchResultContract.Intent.ClickIdolCommunity -> handleIdolCommunityClick(intent.idol)
            is SearchResultContract.Intent.ClickIdolSmallTalk -> handleIdolSmallTalkClick(intent.idol)
            is SearchResultContract.Intent.ClickIdolSchedule -> handleIdolScheduleClick(intent.idol)
            is SearchResultContract.Intent.ClickSupport -> handleSupportClick(intent.support)
            is SearchResultContract.Intent.ClickWallpaper -> handleWallpaperClick(intent.wallpaper)
            is SearchResultContract.Intent.ClickArticle -> handleArticleClick(intent.article)
            is SearchResultContract.Intent.NavigateBack -> setEffect { SearchResultContract.Effect.NavigateBack }
        }
    }

    /**
     * 통합 검색 실행
     */
    private fun search(keyword: String) {
        setState {
            copy(
                keyword = keyword,
                isLoading = true,
                error = null,
                isEmpty = false
            )
        }

        searchRepository.search(
            keyword = keyword,
            category = null,
            offset = 0,
            limit = ARTICLE_LIMIT
        )
            .onEach { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        setState { copy(isLoading = true) }
                    }
                    is ApiResult.Success -> {
                        val data = result.data
                        val isEmpty = data.idols.isEmpty() &&
                                data.supports.isEmpty() &&
                                data.wallpapers.isEmpty() &&
                                data.smallTalks.isEmpty() &&
                                data.articles.isEmpty()

                        setState {
                            copy(
                                isLoading = false,
                                idols = data.idols,
                                supports = data.supports,
                                wallpapers = data.wallpapers,
                                smallTalks = data.smallTalks,
                                articles = data.articles,
                                smallTalkOffset = data.smallTalkOffset,
                                hasMoreSmallTalks = data.hasMoreSmallTalks,
                                hasMoreArticles = data.articles.size >= ARTICLE_LIMIT,
                                articleOffset = data.articles.size,
                                isEmpty = isEmpty,
                                error = null
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState {
                            copy(
                                isLoading = false,
                                error = result.error.message
                            )
                        }
                    }
                }
            }
            .catch { e ->
                setState {
                    copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 새 검색 (기존 결과 초기화)
     */
    private fun newSearch(keyword: String) {
        setState {
            copy(
                keyword = keyword,
                idols = emptyList(),
                showAllIdols = false,
                supports = emptyList(),
                showAllSupports = false,
                wallpapers = emptyList(),
                smallTalks = emptyList(),
                articles = emptyList(),
                smallTalkOffset = 0,
                articleOffset = 0,
                hasMoreSmallTalks = true,
                hasMoreArticles = true,
                isEmpty = false
            )
        }
        search(keyword)
    }

    /**
     * 아이돌 더보기
     */
    private fun showAllIdols() {
        setState { copy(showAllIdols = true) }
    }

    /**
     * 서포트 더보기
     */
    private fun showAllSupports() {
        setState { copy(showAllSupports = true) }
    }

    /**
     * 잡담 더 로드 (무한 스크롤)
     */
    private fun loadMoreSmallTalks() {
        if (currentState.isLoadingMoreSmallTalk || !currentState.hasMoreSmallTalks) return

        setState { copy(isLoadingMoreSmallTalk = true) }

        searchRepository.search(
            keyword = currentState.keyword,
            category = "articlev2",  // 잡담게시판
            offset = currentState.smallTalkOffset + currentState.smallTalks.size,
            limit = SMALL_TALK_LIMIT
        )
            .onEach { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data
                        setState {
                            copy(
                                isLoadingMoreSmallTalk = false,
                                smallTalks = smallTalks + data.smallTalks,
                                smallTalkOffset = data.smallTalkOffset,
                                hasMoreSmallTalks = data.hasMoreSmallTalks
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoadingMoreSmallTalk = false) }
                    }
                    is ApiResult.Loading -> { /* ignore */ }
                }
            }
            .catch {
                setState { copy(isLoadingMoreSmallTalk = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 게시글 더 로드 (무한 스크롤)
     */
    private fun loadMoreArticles() {
        if (currentState.isLoadingMoreArticle || !currentState.hasMoreArticles) return

        setState { copy(isLoadingMoreArticle = true) }

        searchRepository.search(
            keyword = currentState.keyword,
            category = "article",
            offset = currentState.articleOffset,
            limit = ARTICLE_LIMIT
        )
            .onEach { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val data = result.data
                        setState {
                            copy(
                                isLoadingMoreArticle = false,
                                articles = articles + data.articles,
                                articleOffset = articleOffset + data.articles.size,
                                hasMoreArticles = data.articles.size >= ARTICLE_LIMIT
                            )
                        }
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoadingMoreArticle = false) }
                    }
                    is ApiResult.Loading -> { /* ignore */ }
                }
            }
            .catch {
                setState { copy(isLoadingMoreArticle = false) }
            }
            .launchIn(viewModelScope)
    }

    /**
     * 즐겨찾기 토글
     */
    private fun toggleFavorite(idol: SearchIdolModel) {
        viewModelScope.launch {
            try {
                val newFavoriteState = !idol.isFavorite

                // UI 즉시 업데이트
                updateIdolFavoriteState(idol.id, newFavoriteState)

                // API 호출
                if (newFavoriteState) {
                    favoritesRepository.addFavorite(idol.id)
                } else {
                    favoritesRepository.removeFavorite(idol.id)
                }
            } catch (e: Exception) {
                // 실패 시 롤백
                updateIdolFavoriteState(idol.id, idol.isFavorite)
                setEffect { SearchResultContract.Effect.ShowToast(e.message ?: "Failed to update favorite") }
            }
        }
    }

    private fun updateIdolFavoriteState(idolId: Int, isFavorite: Boolean) {
        setState {
            copy(
                idols = idols.map {
                    if (it.id == idolId) it.copy(isFavorite = isFavorite) else it
                }
            )
        }
    }

    /**
     * 최애 설정 처리
     */
    private fun handleSetMost(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.ShowSetMostDialog(idol) }
    }

    /**
     * 아이돌 클릭 처리
     */
    private fun handleIdolClick(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.NavigateToCommunity(idol.id) }
    }

    /**
     * 아이돌 커뮤니티 버튼 클릭
     */
    private fun handleIdolCommunityClick(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.NavigateToCommunity(idol.id, "community") }
    }

    /**
     * 아이돌 잡담 버튼 클릭
     */
    private fun handleIdolSmallTalkClick(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.NavigateToSmallTalk(idol.id) }
    }

    /**
     * 아이돌 스케줄 버튼 클릭
     */
    private fun handleIdolScheduleClick(idol: SearchIdolModel) {
        setEffect { SearchResultContract.Effect.NavigateToSchedule(idol.id) }
    }

    /**
     * 서포트 클릭 처리
     */
    private fun handleSupportClick(support: SearchSupportModel) {
        setEffect { SearchResultContract.Effect.NavigateToSupportDetail(support.id) }
    }

    /**
     * 배경화면 클릭 처리
     */
    private fun handleWallpaperClick(wallpaper: SearchWallpaperModel) {
        setEffect { SearchResultContract.Effect.NavigateToWallpaperDetail(wallpaper.id) }
    }

    /**
     * 게시글 클릭 처리
     */
    private fun handleArticleClick(article: ArticleModel) {
        setEffect { SearchResultContract.Effect.NavigateToArticleDetail(article.id) }
    }
}
