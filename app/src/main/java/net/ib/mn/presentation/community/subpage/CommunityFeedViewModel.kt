package net.ib.mn.presentation.community.subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.remote.dto.ArticleVoteResponse
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.NoticeModel
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import javax.inject.Inject

private const val TAG = "CommunityFeedVM"

/**
 * 정렬 타입 (old 프로젝트의 OrderByType과 동일)
 */
enum class OrderByType(val orderBy: String) {
    HEART("-heart"),
    TIME("-created_at"),
    COMMENTS("-num_comments"),
    LIKES("-like_count")
}

/**
 * 뷰 타입 (목록/그리드/배경화면)
 */
enum class ViewType {
    LIST,      // 기본 목록 보기
    GRID,      // 그리드 보기 (이미지만)
    WALLPAPER  // 배경화면 보기
}

/**
 * UI 상태
 */
data class CommunityFeedUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val notices: List<NoticeModel> = emptyList(),
    val articles: List<ArticleModel> = emptyList(),
    val orderBy: OrderByType = OrderByType.HEART,
    val viewType: ViewType = ViewType.LIST,
    val hasNextPage: Boolean = false,
    val error: String? = null
)

/**
 * CommunityFeedViewModel - 커뮤니티 피드 탭 ViewModel
 */
@HiltViewModel
class CommunityFeedViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CommunityFeedUiState())
    val uiState: StateFlow<CommunityFeedUiState> = _uiState.asStateFlow()

    private var currentIdolId: Int = 0
    private var isMost: Boolean = false
    private var nextUrl: String? = null
    private var isLoadingNextPage: Boolean = false

    /**
     * 피드 초기 로드
     * isMost는 PreferencesManager에서 직접 가져옴
     */
    fun loadFeed(idolId: Int) {
        this.currentIdolId = idolId
        this.nextUrl = null

        viewModelScope.launch {
            // PreferencesManager에서 직접 최애 여부 확인
            val mostIdolId = preferencesManager.getMostIdolId()
            isMost = mostIdolId != null && mostIdolId == idolId

            // 기존 데이터가 있으면 로딩 상태를 변경하지 않음 (깜빡거림 방지)
            val hasExistingData = _uiState.value.articles.isNotEmpty()
            logD(TAG, "loadFeed: idolId=$idolId, mostIdolId=$mostIdolId, isMost=$isMost, hasExistingData=$hasExistingData")
            loadFeedInternal(skipLoadingState = hasExistingData)
        }
    }

    /**
     * 실제 피드 로드 (내부용)
     * @param skipLoadingState true이면 로딩 상태를 변경하지 않음 (깜빡거림 방지)
     */
    private suspend fun loadFeedInternal(skipLoadingState: Boolean = false) {
        val existingArticles = _uiState.value.articles
        val existingNotices = _uiState.value.notices

        // skipLoadingState가 false일 때만 로딩 상태 및 데이터 초기화
        if (!skipLoadingState) {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
        }

        val orderBy = _uiState.value.orderBy
        val (imageOnly, primaryFileType) = getFilterParams()

        articlesRepository.getCommunityFeed(
            idolId = currentIdolId,
            isMost = isMost,
            orderBy = orderBy.orderBy,
            imageOnly = imageOnly,
            primaryFileType = primaryFileType
        ).collect { result ->
            when (result) {
                is ApiResult.Loading -> {
                    // 이미 isLoading = true 설정됨
                }
                is ApiResult.Success -> {
                    nextUrl = result.data.nextUrl

                    // 기존 데이터를 Map으로 변환하여 빠른 조회
                    val existingArticlesMap = existingArticles.associateBy { it.id }
                    val existingNoticesMap = existingNotices.associateBy { it.id }

                    // 변경된 항목만 새 객체 사용, 동일하면 기존 객체 재사용
                    val mergedArticles = result.data.articles.map { newArticle ->
                        val existing = existingArticlesMap[newArticle.id]
                        if (existing != null && existing == newArticle) existing else newArticle
                    }

                    val mergedNotices = result.data.notices.map { newNotice ->
                        val existing = existingNoticesMap[newNotice.id]
                        if (existing != null && existing == newNotice) existing else newNotice
                    }

                    // 리스트 자체도 동일하면 기존 리스트 재사용
                    val finalArticles = if (mergedArticles == existingArticles) existingArticles else mergedArticles
                    val finalNotices = if (mergedNotices == existingNotices) existingNotices else mergedNotices

                    // skipLoadingState이고 데이터가 완전히 동일하면 setState 호출하지 않음
                    val isDataSame = finalArticles === existingArticles && finalNotices === existingNotices
                    if (skipLoadingState && isDataSame) {
                        logD(TAG, "Data unchanged, skipping setState")
                        return@collect
                    }

                    _uiState.value = _uiState.value.copy(
                        isLoading = if (skipLoadingState) _uiState.value.isLoading else false,
                        notices = finalNotices,
                        articles = finalArticles,
                        hasNextPage = result.data.nextUrl != null,
                        error = null
                    )
                    logD(TAG, "loadFeed success: notices=${mergedNotices.size}, articles=${mergedArticles.size}, reusedArticles=${finalArticles === existingArticles}")
                }
                is ApiResult.Error -> {
                    if (!skipLoadingState) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.message ?: "Unknown error"
                        )
                    }
                    logE(TAG, "loadFeed error: ${result.message}")
                }
            }
        }
    }

    /**
     * 다음 페이지 로드 (무한 스크롤)
     */
    fun loadNextPage() {
        val url = nextUrl ?: return
        if (isLoadingNextPage) return

        isLoadingNextPage = true
        logD(TAG, "loadNextPage: url=$url")

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)

            val (imageOnly, primaryFileType) = getFilterParams()

            articlesRepository.getCommunityFeedNext(
                nextUrl = url,
                isMost = isMost,
                imageOnly = imageOnly,
                primaryFileType = primaryFileType
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        // 이미 isLoadingMore = true 설정됨
                    }
                    is ApiResult.Success -> {
                        nextUrl = result.data.nextUrl
                        val currentArticles = _uiState.value.articles
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            articles = currentArticles + result.data.articles,
                            hasNextPage = result.data.nextUrl != null
                        )
                        logD(TAG, "loadNextPage success: added ${result.data.articles.size} articles")
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoadingMore = false,
                            error = result.message
                        )
                        logE(TAG, "loadNextPage error: ${result.message}")
                    }
                }
                isLoadingNextPage = false
            }
        }
    }

    /**
     * 정렬 변경
     */
    fun setOrderBy(orderBy: OrderByType) {
        if (_uiState.value.orderBy == orderBy) return

        _uiState.value = _uiState.value.copy(orderBy = orderBy)
        loadFeed(currentIdolId)
    }

    /**
     * 뷰 타입 변경 (목록/그리드/배경화면)
     */
    fun setViewType(viewType: ViewType) {
        if (_uiState.value.viewType == viewType) return

        _uiState.value = _uiState.value.copy(viewType = viewType)
        loadFeed(currentIdolId)
    }

    /**
     * 새로고침
     */
    fun refresh() {
        loadFeed(currentIdolId)
    }

    /**
     * 게시글 투표 (하트 투표)
     *
     * @param articleId 게시글 ID
     * @param hearts 투표할 하트 개수
     * @param onSuccess 성공 콜백
     * @param onError 에러 콜백
     */
    fun voteArticle(
        articleId: String,
        hearts: Long,
        onSuccess: (ArticleVoteResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                logD(TAG, "voteArticle: articleId=$articleId, hearts=$hearts")
                val response = articlesRepository.voteArticle(articleId, hearts)
                if (response.success) {
                    logD(TAG, "voteArticle success: ${response.msg}")
                    // 투표 성공 시 해당 게시글의 하트 수 업데이트
                    updateArticleHeart(articleId, hearts)
                    onSuccess(response)
                } else {
                    logE(TAG, "voteArticle failed: ${response.msg}")
                    onError(response.msg ?: "투표에 실패했습니다.")
                }
            } catch (e: Exception) {
                logE(TAG, "voteArticle error: ${e.message}", e)
                onError(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 게시글 하트 수 로컬 업데이트
     */
    private fun updateArticleHeart(articleId: String, addedHearts: Long) {
        val currentArticles = _uiState.value.articles
        val updatedArticles = currentArticles.map { article ->
            if (article.id == articleId) {
                article.copy(heart = article.heart + addedHearts)
            } else {
                article
            }
        }
        _uiState.value = _uiState.value.copy(articles = updatedArticles)
    }

    /**
     * 게시글 좋아요 API 호출만 (UI는 로컬 상태에서 즉시 업데이트)
     * Old 프로젝트의 communityArticleListener.likeClick과 동일
     *
     * @param articleId 게시글 ID
     * @param like 좋아요 여부
     */
    fun postLike(articleId: String, like: Boolean) {
        viewModelScope.launch {
            try {
                logD(TAG, "postLike: articleId=$articleId, like=$like")
                val response = articlesRepository.likeArticle(articleId, like)
                logD(TAG, "postLike: success=${response.success}, liked=${response.liked}")
            } catch (e: Exception) {
                logE(TAG, "postLike error: ${e.message}", e)
            }
        }
    }

    /**
     * 뷰 타입에 따른 필터 파라미터 반환
     */
    private fun getFilterParams(): Pair<String?, String?> {
        return when (_uiState.value.viewType) {
            ViewType.LIST -> Pair(null, null)
            ViewType.GRID -> Pair("Y", null)
            ViewType.WALLPAPER -> Pair("Y", "wp")
        }
    }

    /**
     * 게시글 삭제 (로컬 리스트에서 제거)
     */
    fun removeArticle(articleId: String) {
        val currentArticles = _uiState.value.articles
        val updatedArticles = currentArticles.filter { it.id != articleId }
        _uiState.value = _uiState.value.copy(articles = updatedArticles)
        logD(TAG, "removeArticle: articleId=$articleId, remaining=${updatedArticles.size}")
    }

    /**
     * 게시글 업데이트 (상세화면에서 돌아올 때 동기화)
     * 투표수, 좋아요수, 댓글수, 제목, 내용 등 전체 업데이트
     */
    fun updateArticle(updatedArticle: ArticleModel) {
        val currentArticles = _uiState.value.articles
        val updatedArticles = currentArticles.map { article ->
            if (article.id == updatedArticle.id) {
                // 기존 article의 모든 필드를 업데이트된 값으로 교체
                // 수정 화면에서 변경된 title, content 등도 반영
                article.copy(
                    title = updatedArticle.title,
                    content = updatedArticle.content,
                    heart = updatedArticle.heart,
                    likeCount = updatedArticle.likeCount,
                    isUserLike = updatedArticle.isUserLike,
                    commentCount = updatedArticle.commentCount,
                    isMostOnly = updatedArticle.isMostOnly
                )
            } else {
                article
            }
        }
        _uiState.value = _uiState.value.copy(articles = updatedArticles)
        logD(TAG, "updateArticle: articleId=${updatedArticle.id}, title=${updatedArticle.title}, heart=${updatedArticle.heart}, likeCount=${updatedArticle.likeCount}, comments=${updatedArticle.commentCount}")
    }
}
