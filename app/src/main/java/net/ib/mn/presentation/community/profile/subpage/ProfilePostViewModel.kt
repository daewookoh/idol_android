package net.ib.mn.presentation.community.profile.subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.data.repository.UsersRepository
import javax.inject.Inject

/**
 * ProfilePostViewModel - 프로필 게시글 탭 ViewModel
 *
 * Old 프로젝트의 FeedActivity.getFeedArticle() 로직을 참고
 */
@HiltViewModel
class ProfilePostViewModel @Inject constructor(
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfilePostUiState>(ProfilePostUiState.Loading)
    val uiState: StateFlow<ProfilePostUiState> = _uiState.asStateFlow()

    private var userId: Int = 0
    private var currentOffset: Int = 0
    private val limit: Int = 20
    private val posts = mutableListOf<ProfilePostItem>()

    /**
     * 게시글 로드
     */
    fun loadPosts(userId: Int) {
        if (this.userId == userId && posts.isNotEmpty()) {
            // 이미 로드된 경우 스킵
            return
        }

        this.userId = userId
        currentOffset = 0
        posts.clear()
        _uiState.value = ProfilePostUiState.Loading

        fetchPosts()
    }

    /**
     * 더 많은 게시글 로드
     */
    fun loadMore() {
        if (_uiState.value is ProfilePostUiState.Loading) return
        fetchPosts()
    }

    private fun fetchPosts() {
        viewModelScope.launch {
            // TODO: API 구현 필요 - articlesRepository.getFeedActivity(userId, "article", offset, limit, isMine)
            // 현재는 빈 목록으로 Empty 상태 표시
            _uiState.value = ProfilePostUiState.Empty
        }
    }
}

/**
 * ProfilePostUiState - UI 상태
 */
sealed interface ProfilePostUiState {
    data object Loading : ProfilePostUiState
    data object Empty : ProfilePostUiState
    data class Success(val posts: List<ProfilePostItem>) : ProfilePostUiState
    data class Error(val message: String) : ProfilePostUiState
}

/**
 * ProfilePostItem - 게시글 데이터
 */
data class ProfilePostItem(
    val id: String,
    val content: String,
    val imageUrl: String?,
    val heart: Long,
    val commentCount: Int,
    val likeCount: Int,
    val createdAt: String
)
