package net.ib.mn.presentation.overlay.profile.subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.repository.ArticlesRepository
import javax.inject.Inject

private const val POST_TYPE = "article"
private const val PAGE_LIMIT = 20

/**
 * ProfilePostViewModel - 프로필 게시글 탭 ViewModel
 */
@HiltViewModel
class ProfilePostViewModel @Inject constructor(
    private val articlesRepository: ArticlesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfilePostUiState>(ProfilePostUiState.Loading)
    val uiState: StateFlow<ProfilePostUiState> = _uiState.asStateFlow()

    private var userId: Int = 0
    private var isSelf: Boolean = false
    private var currentOffset: Int = 0
    private var hasMoreData: Boolean = true
    private var isLoading: Boolean = false
    private val posts = mutableListOf<ArticleModel>()

    fun loadPosts(userId: Int, isSelf: Boolean = false) {
        if (this.userId == userId && posts.isNotEmpty()) return

        this.userId = userId
        this.isSelf = isSelf
        currentOffset = 0
        hasMoreData = true
        posts.clear()
        _uiState.value = ProfilePostUiState.Loading
        fetchPosts()
    }

    fun loadMore() {
        if (isLoading || !hasMoreData) return
        fetchPosts()
    }

    private fun fetchPosts() {
        if (isLoading) return
        isLoading = true

        viewModelScope.launch {
            articlesRepository.getFeedActivity(
                userId = userId,
                type = POST_TYPE,
                offset = currentOffset,
                limit = PAGE_LIMIT,
                isSelf = isSelf
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        if (posts.isEmpty()) {
                            _uiState.value = ProfilePostUiState.Loading
                        }
                    }
                    is ApiResult.Success -> handleSuccess(result.data.articles, result.data.totalCount)
                    is ApiResult.Error -> handleError(result.message)
                }
            }
        }
    }

    private fun handleSuccess(articles: List<ArticleModel>, totalCount: Int) {
        // 비공개 피드
        if (totalCount == -1) {
            _uiState.value = ProfilePostUiState.Private
            isLoading = false
            return
        }

        if (articles.isEmpty()) {
            hasMoreData = false
            if (posts.isEmpty()) {
                _uiState.value = ProfilePostUiState.Empty
            }
        } else {
            posts.addAll(articles)
            currentOffset += articles.size
            hasMoreData = articles.size >= PAGE_LIMIT

            _uiState.value = ProfilePostUiState.Success(
                posts = posts.toList(),
                hasMore = hasMoreData
            )
        }
        isLoading = false
    }

    private fun handleError(message: String?) {
        if (posts.isEmpty()) {
            _uiState.value = ProfilePostUiState.Error(message ?: "Failed to load posts")
        }
        isLoading = false
    }

    /**
     * 게시글 삭제 (로컬 리스트에서 제거)
     */
    fun removeArticle(articleId: String) {
        posts.removeAll { it.id == articleId }
        if (posts.isEmpty()) {
            _uiState.value = ProfilePostUiState.Empty
        } else {
            _uiState.value = ProfilePostUiState.Success(
                posts = posts.toList(),
                hasMore = hasMoreData
            )
        }
    }

    /**
     * 게시글 업데이트 (좋아요/하트 등 상태 변경 시)
     */
    fun updateArticle(updatedArticle: ArticleModel) {
        val index = posts.indexOfFirst { it.id == updatedArticle.id }
        if (index >= 0) {
            posts[index] = updatedArticle
            _uiState.value = ProfilePostUiState.Success(
                posts = posts.toList(),
                hasMore = hasMoreData
            )
        }
    }
}

/** UI 상태 */
sealed interface ProfilePostUiState {
    data object Loading : ProfilePostUiState
    data object Empty : ProfilePostUiState
    data object Private : ProfilePostUiState
    data class Success(val posts: List<ArticleModel>, val hasMore: Boolean = true) : ProfilePostUiState
    data class Error(val message: String) : ProfilePostUiState
}
