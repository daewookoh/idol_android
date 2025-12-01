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
 * ProfileCommentViewModel - 프로필 댓글 탭 ViewModel
 *
 * Old 프로젝트의 FeedActivity.getFeedComment() 로직을 참고
 * 본인 프로필에서만 표시되는 댓글 목록
 */
@HiltViewModel
class ProfileCommentViewModel @Inject constructor(
    private val usersRepository: UsersRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileCommentUiState>(ProfileCommentUiState.Loading)
    val uiState: StateFlow<ProfileCommentUiState> = _uiState.asStateFlow()

    private var userId: Int = 0
    private var currentOffset: Int = 0
    private val limit: Int = 20
    private val comments = mutableListOf<ProfileCommentItem>()

    /**
     * 댓글 로드
     */
    fun loadComments(userId: Int) {
        if (this.userId == userId && comments.isNotEmpty()) {
            // 이미 로드된 경우 스킵
            return
        }

        this.userId = userId
        currentOffset = 0
        comments.clear()
        _uiState.value = ProfileCommentUiState.Loading

        fetchComments()
    }

    /**
     * 더 많은 댓글 로드
     */
    fun loadMore() {
        if (_uiState.value is ProfileCommentUiState.Loading) return
        fetchComments()
    }

    private fun fetchComments() {
        viewModelScope.launch {
            // TODO: API 구현 필요 - articlesRepository.getFeedActivity(userId, "comment", offset, limit, true)
            // 현재는 빈 목록으로 Empty 상태 표시
            _uiState.value = ProfileCommentUiState.Empty
        }
    }
}

/**
 * ProfileCommentUiState - UI 상태
 */
sealed interface ProfileCommentUiState {
    data object Loading : ProfileCommentUiState
    data object Empty : ProfileCommentUiState
    data class Success(val comments: List<ProfileCommentItem>) : ProfileCommentUiState
    data class Error(val message: String) : ProfileCommentUiState
}

/**
 * ProfileCommentItem - 댓글 데이터
 */
data class ProfileCommentItem(
    val id: String,
    val content: String,
    val articleId: String,
    val createdAt: String
)
