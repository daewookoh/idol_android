package net.ib.mn.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.CommentsRepository
import net.ib.mn.data.repository.ReportRepository
import net.ib.mn.data.repository.ReportResult
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.CommentModel
import net.ib.mn.domain.model.TranslateState
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import javax.inject.Inject

/**
 * 댓글 섹션 UI 상태
 */
data class CommentSectionUiState(
    val comments: List<CommentModel> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null
)

/**
 * 댓글 섹션 ViewModel
 */
@HiltViewModel
class CommentSectionViewModel @Inject constructor(
    private val commentsRepository: CommentsRepository,
    private val configRepository: ConfigRepository,
    private val reportRepository: ReportRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "CommentSection"
        private const val PAGE_SIZE = 20
        // 댓글 신고 gcode (게시글 신고는 22xx, 댓글 신고는 25xx)
        const val GCODE_ALREADY_REPORTED = 2501
        const val GCODE_DAILY_LIMIT = 2502
        const val GCODE_TIME_LIMIT = 2503
        const val GCODE_NOT_ENOUGH_HEART = 2504
    }

    // 번역 설정 (ConfigRepository에서 가져옴)
    val useTranslation: Boolean
        get() = configRepository.getShowTranslation()

    val translationLocales: List<String>
        get() = configRepository.getTranslationLocales()

    // CDN URL (이모티콘 이미지 URL 생성용)
    val cdnUrl: StateFlow<String> = preferencesManager.cdnUrl
        .map { it ?: "https://cdn.idolchamp.com" }
        .stateIn(viewModelScope, SharingStarted.Lazily, "https://cdn.idolchamp.com")

    private val _uiState = MutableStateFlow(CommentSectionUiState())
    val uiState: StateFlow<CommentSectionUiState> = _uiState.asStateFlow()

    private val _commentText = MutableStateFlow("")
    val commentText: StateFlow<String> = _commentText.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    // 전체 화면 로딩 상태 (댓글 작성 중)
    private val _isFullScreenLoading = MutableStateFlow(false)
    val isFullScreenLoading: StateFlow<Boolean> = _isFullScreenLoading.asStateFlow()

    // 댓글 작성 완료 후 스크롤 이벤트 (첫 댓글로 스크롤)
    private val _scrollToTopEvent = MutableStateFlow(false)
    val scrollToTopEvent: StateFlow<Boolean> = _scrollToTopEvent.asStateFlow()

    // 댓글 수 변경 이벤트 (+1: 추가, -1: 삭제)
    private val _commentCountDelta = MutableStateFlow(0)
    val commentCountDelta: StateFlow<Int> = _commentCountDelta.asStateFlow()

    private val _selectedEmoticonId = MutableStateFlow<Int?>(null)
    val selectedEmoticonId: StateFlow<Int?> = _selectedEmoticonId.asStateFlow()

    // 선택된 이모티콘 URL (프리뷰용)
    private val _selectedEmoticonUrl = MutableStateFlow<String?>(null)
    val selectedEmoticonUrl: StateFlow<String?> = _selectedEmoticonUrl.asStateFlow()

    private val _showEmoticonPanel = MutableStateFlow(false)
    val showEmoticonPanel: StateFlow<Boolean> = _showEmoticonPanel.asStateFlow()

    private var currentArticleId: Long = 0
    private var nextCursor: String? = null

    /**
     * 댓글 목록 로드 (초기)
     */
    fun loadComments(articleId: Long) {
        if (currentArticleId == articleId && _uiState.value.comments.isNotEmpty()) {
            return // 이미 로드된 경우 스킵
        }

        currentArticleId = articleId
        nextCursor = null

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            commentsRepository.getComments(articleId, null, PAGE_SIZE).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        _uiState.value = _uiState.value.copy(isLoading = true)
                    }
                    is ApiResult.Success -> {
                        val response = result.data
                        nextCursor = response.meta?.next
                        _uiState.value = CommentSectionUiState(
                            comments = response.comments,
                            isLoading = false,
                            hasMore = !nextCursor.isNullOrEmpty()
                        )
                        logD(TAG, "Loaded ${response.comments.size} comments")
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.error.message
                        )
                        logE(TAG, "Failed to load comments: ${result.error.message}")
                    }
                }
            }
        }
    }

    /**
     * 더 많은 댓글 로드 (페이징)
     */
    fun loadMoreComments() {
        val cursor = nextCursor ?: return
        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            commentsRepository.getComments(currentArticleId, cursor, PAGE_SIZE).collect { result ->
                when (result) {
                    is ApiResult.Loading -> { /* skip */ }
                    is ApiResult.Success -> {
                        val response = result.data
                        nextCursor = response.meta?.next
                        _uiState.value = _uiState.value.copy(
                            comments = _uiState.value.comments + response.comments,
                            isLoading = false,
                            hasMore = !nextCursor.isNullOrEmpty()
                        )
                        logD(TAG, "Loaded ${response.comments.size} more comments")
                    }
                    is ApiResult.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.error.message
                        )
                    }
                }
            }
        }
    }

    /**
     * 댓글 텍스트 변경
     */
    fun setCommentText(text: String) {
        _commentText.value = text
    }

    /**
     * 이모티콘 선택 (old 프로젝트와 동일)
     */
    fun selectEmoticon(emoticonId: Int, emoticonUrl: String) {
        _selectedEmoticonId.value = emoticonId
        _selectedEmoticonUrl.value = emoticonUrl
    }

    /**
     * 이모티콘 선택 해제 (old: iv_preview_close 클릭)
     */
    fun clearEmoticon() {
        _selectedEmoticonId.value = null
        _selectedEmoticonUrl.value = null
    }

    /**
     * 이모티콘 패널 토글
     */
    fun toggleEmoticonPanel() {
        _showEmoticonPanel.value = !_showEmoticonPanel.value
    }

    fun showEmoticonPanelView() {
        _showEmoticonPanel.value = true
    }

    fun hideEmoticonPanel() {
        _showEmoticonPanel.value = false
    }

    /**
     * 댓글 작성
     * old 프로젝트와 동일하게 mirror DB 반영을 위해 1초 대기 후 새로고침
     * 전체 화면 로딩 표시 후 완료 시 첫 댓글로 스크롤
     * - 텍스트만 있는 경우
     * - 이모티콘만 있는 경우
     * - 둘 다 있는 경우
     */
    fun submitComment(articleId: Long) {
        val content = _commentText.value.trim()
        val emoticonId = _selectedEmoticonId.value
        // old 프로젝트와 동일: 텍스트나 이모티콘 중 하나는 있어야 함
        if (content.isEmpty() && emoticonId == null) return
        if (_isSubmitting.value) return

        _isSubmitting.value = true
        _isFullScreenLoading.value = true  // 전체 화면 로딩 시작

        // API 호출 시작 시 이모티콘 관련 데이터 모두 숨김
        _showEmoticonPanel.value = false
        _selectedEmoticonId.value = null
        _selectedEmoticonUrl.value = null

        viewModelScope.launch {
            commentsRepository.writeComment(articleId, content, emoticonId).collect { result ->
                when (result) {
                    is ApiResult.Loading -> { /* skip */ }
                    is ApiResult.Success -> {
                        _commentText.value = ""
                        _selectedEmoticonId.value = null
                        _selectedEmoticonUrl.value = null
                        logD(TAG, "Comment submitted successfully")

                        // mirror DB 반영을 위해 1초 대기 후 새로고침
                        delay(1000)

                        // 새로고침
                        nextCursor = null

                        commentsRepository.getComments(currentArticleId, null, PAGE_SIZE).collect { refreshResult ->
                            when (refreshResult) {
                                is ApiResult.Loading -> { /* skip */ }
                                is ApiResult.Success -> {
                                    val response = refreshResult.data
                                    nextCursor = response.meta?.next
                                    _uiState.value = CommentSectionUiState(
                                        comments = response.comments,
                                        isLoading = false,
                                        hasMore = !nextCursor.isNullOrEmpty()
                                    )
                                    logD(TAG, "Comments refreshed: ${response.comments.size}")

                                    // 첫 댓글로 스크롤 이벤트 발생
                                    _scrollToTopEvent.value = true
                                    // 댓글 수 +1 이벤트 발생
                                    _commentCountDelta.value = 1
                                }
                                is ApiResult.Error -> {
                                    _uiState.value = _uiState.value.copy(isLoading = false)
                                    logE(TAG, "Failed to refresh comments: ${refreshResult.error.message}")
                                }
                            }
                        }
                        _isSubmitting.value = false
                        _isFullScreenLoading.value = false  // 전체 화면 로딩 종료
                    }
                    is ApiResult.Error -> {
                        _isSubmitting.value = false
                        _isFullScreenLoading.value = false  // 전체 화면 로딩 종료
                        logE(TAG, "Failed to submit comment: ${result.error.message}")
                    }
                }
            }
        }
    }

    /**
     * 스크롤 이벤트 소비
     */
    fun consumeScrollToTopEvent() {
        _scrollToTopEvent.value = false
    }

    /**
     * 댓글 수 변경 이벤트 소비
     */
    fun consumeCommentCountDelta() {
        _commentCountDelta.value = 0
    }

    /**
     * 댓글 삭제
     */
    fun deleteComment(commentId: Int) {
        viewModelScope.launch {
            commentsRepository.deleteComment(commentId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        // 목록에서 제거
                        _uiState.value = _uiState.value.copy(
                            comments = _uiState.value.comments.filter { it.id != commentId }
                        )
                        // 댓글 수 -1 이벤트 발생
                        _commentCountDelta.value = -1
                        logD(TAG, "Comment deleted: $commentId")
                    }
                    is ApiResult.Error -> {
                        logE(TAG, "Failed to delete comment: ${result.error.message}")
                    }
                    else -> { /* skip */ }
                }
            }
        }
    }

    /**
     * 새로고침
     */
    fun refresh() {
        nextCursor = null
        loadComments(currentArticleId)
    }

    /**
     * 댓글 번역
     * old 프로젝트의 CommentTranslationHelper.clickTranslate와 동일한 로직
     */
    fun translateComment(commentId: Int) {
        val comments = _uiState.value.comments.toMutableList()
        val index = comments.indexOfFirst { it.id == commentId }
        if (index == -1) return

        val comment = comments[index]

        if (comment.translateState == TranslateState.ORIGINAL) {
            // 번역 시작 - TRANSLATING 상태로 변경
            comments[index] = comment.copy(translateState = TranslateState.TRANSLATING)
            _uiState.value = _uiState.value.copy(comments = comments)

            viewModelScope.launch {
                commentsRepository.translateComment(commentId).collect { result ->
                    when (result) {
                        is ApiResult.Success -> {
                            val translatedComment = result.data
                            val updatedComments = _uiState.value.comments.toMutableList()
                            val currentIndex = updatedComments.indexOfFirst { it.id == commentId }
                            if (currentIndex != -1) {
                                val currentComment = updatedComments[currentIndex]
                                updatedComments[currentIndex] = currentComment.copy(
                                    originalContent = currentComment.content,
                                    content = translatedComment.content,
                                    translateState = TranslateState.TRANSLATED
                                )
                                _uiState.value = _uiState.value.copy(comments = updatedComments)
                            }
                            logD(TAG, "Comment translated: $commentId")
                        }
                        is ApiResult.Error -> {
                            // 번역 실패 - 원래 상태로 복원
                            val updatedComments = _uiState.value.comments.toMutableList()
                            val currentIndex = updatedComments.indexOfFirst { it.id == commentId }
                            if (currentIndex != -1) {
                                updatedComments[currentIndex] = updatedComments[currentIndex].copy(
                                    translateState = TranslateState.ORIGINAL
                                )
                                _uiState.value = _uiState.value.copy(comments = updatedComments)
                            }
                            logE(TAG, "Failed to translate comment: ${result.error.message}")
                        }
                        else -> { /* skip */ }
                    }
                }
            }
        } else {
            // 이미 번역된 상태 - 원문으로 복원
            comments[index] = comment.copy(
                content = comment.originalContent,
                translateState = TranslateState.ORIGINAL
            )
            _uiState.value = _uiState.value.copy(comments = comments)
        }
    }

    /**
     * 댓글 번역 가능 여부 업데이트
     */
    fun updateCommentTranslatable(commentId: Int, isTranslatable: Boolean) {
        val comments = _uiState.value.comments.toMutableList()
        val index = comments.indexOfFirst { it.id == commentId }
        if (index != -1 && comments[index].isTranslatable == null) {
            comments[index] = comments[index].copy(isTranslatable = isTranslatable)
            _uiState.value = _uiState.value.copy(comments = comments)
        }
    }

    /**
     * 신고 시 차감될 하트 수
     */
    val reportHeart: Int
        get() = configRepository.getReportHeart()

    /**
     * 댓글 신고
     */
    fun reportComment(
        commentId: Int,
        onSuccess: () -> Unit,
        onError: (Int) -> Unit
    ) {
        viewModelScope.launch {
            when (val result = reportRepository.reportComment(commentId)) {
                is ReportResult.Success -> {
                    logD(TAG, "Comment reported: $commentId")
                    onSuccess()
                }
                is ReportResult.Error -> {
                    logE(TAG, "Failed to report comment: gcode=${result.gcode}")
                    onError(result.gcode)
                }
            }
        }
    }
}
