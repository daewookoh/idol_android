package net.ib.mn.presentation.overlay.commentonly

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.data.repository.CommentsRepository
import net.ib.mn.data.repository.ReportRepository
import net.ib.mn.data.repository.ReportResult
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.CommentModel
import net.ib.mn.domain.model.TranslateState
import net.ib.mn.domain.repository.ConfigRepository
import net.ib.mn.domain.repository.HeartpickRepository
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import javax.inject.Inject

/**
 * CommentOnly (하트픽 응원 댓글) 화면 ViewModel
 *
 * old 프로젝트: CommentOnlyActivity
 *
 * common/CommentSectionViewModel의 구조를 참고하여 하트픽 전용으로 구현
 */
@HiltViewModel
class CommentOnlyViewModel @Inject constructor(
    private val heartpickRepository: HeartpickRepository,
    private val commentsRepository: CommentsRepository,
    private val reportRepository: ReportRepository,
    private val configRepository: ConfigRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : BaseViewModel<CommentOnlyContract.State, CommentOnlyContract.Intent, CommentOnlyContract.Effect>() {

    companion object {
        private const val TAG = "CommentOnlyVM"
        private const val PAGE_SIZE = 50
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

    // 현재 로그인한 사용자 ID
    val currentUserId: Int
        get() = preferencesManager.getUserIdSync()

    // 현재 로그인한 사용자 레벨 (동기적으로 가져올 수 없으므로 StateFlow 사용)
    private val _currentUserLevel = MutableStateFlow(0)
    val currentUserLevel: Int
        get() = _currentUserLevel.value

    private var nextCursor: String? = null

    init {
        // 유저 레벨 초기화
        viewModelScope.launch {
            _currentUserLevel.value = preferencesManager.getUserLevel()
        }
    }

    override fun createInitialState(): CommentOnlyContract.State = CommentOnlyContract.State()

    override fun handleIntent(intent: CommentOnlyContract.Intent) {
        when (intent) {
            is CommentOnlyContract.Intent.LoadComments -> loadComments(intent.heartPickId)
            is CommentOnlyContract.Intent.LoadMoreComments -> loadMoreComments()
            is CommentOnlyContract.Intent.SetCommentText -> setCommentText(intent.text)
            is CommentOnlyContract.Intent.SelectEmoticon -> selectEmoticon(intent.emoticonId, intent.emoticonUrl)
            is CommentOnlyContract.Intent.ClearEmoticon -> clearEmoticon()
            is CommentOnlyContract.Intent.ToggleEmoticonPanel -> toggleEmoticonPanel()
            is CommentOnlyContract.Intent.HideEmoticonPanel -> hideEmoticonPanel()
            is CommentOnlyContract.Intent.SelectImage -> selectImage(intent.uri, intent.bytes, intent.isGif)
            is CommentOnlyContract.Intent.ClearImage -> clearImage()
            is CommentOnlyContract.Intent.SubmitComment -> submitComment()
            is CommentOnlyContract.Intent.OnProfileClick -> onProfileClick(intent.comment)
            is CommentOnlyContract.Intent.OnImageClick -> onImageClick(intent.imageUrl)
            is CommentOnlyContract.Intent.DeleteComment -> deleteComment(intent.commentId)
            is CommentOnlyContract.Intent.ReportComment -> reportComment(intent.commentId)
            is CommentOnlyContract.Intent.Refresh -> refresh()
        }
    }

    /**
     * 댓글 목록 로드 (초기)
     */
    private fun loadComments(heartPickId: Int) {
        setState { copy(heartPickId = heartPickId, comments = emptyList(), isLoading = true, error = null) }
        nextCursor = null

        viewModelScope.launch {
            heartpickRepository.getReplies(heartPickId, PAGE_SIZE, null).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        // 이미 isLoading = true 설정됨
                    }
                    is ApiResult.Success -> {
                        val response = result.data
                        nextCursor = response.nextCursor
                        setState {
                            copy(
                                comments = response.comments,
                                hasMore = response.hasMore,
                                isLoading = false
                            )
                        }
                        logD(TAG, "Loaded ${response.comments.size} comments")
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false, error = result.message) }
                        setEffect { CommentOnlyContract.Effect.ShowToast(result.message ?: "Failed to load comments") }
                        logE(TAG, "Failed to load comments: ${result.message}")
                    }
                }
            }
        }
    }

    /**
     * 더 많은 댓글 로드 (페이징)
     */
    private fun loadMoreComments() {
        val cursor = nextCursor ?: return
        if (currentState.isLoading) return

        setState { copy(isLoading = true) }

        viewModelScope.launch {
            heartpickRepository.getReplies(currentState.heartPickId, PAGE_SIZE, cursor).collect { result ->
                when (result) {
                    is ApiResult.Loading -> { /* skip */ }
                    is ApiResult.Success -> {
                        val response = result.data
                        nextCursor = response.nextCursor
                        setState {
                            copy(
                                comments = comments + response.comments,
                                hasMore = response.hasMore,
                                isLoading = false
                            )
                        }
                        logD(TAG, "Loaded ${response.comments.size} more comments")
                    }
                    is ApiResult.Error -> {
                        setState { copy(isLoading = false) }
                        logE(TAG, "Failed to load more comments: ${result.message}")
                    }
                }
            }
        }
    }

    /**
     * 댓글 텍스트 변경
     */
    private fun setCommentText(text: String) {
        setState { copy(commentText = text) }
    }

    /**
     * 이모티콘 선택
     */
    private fun selectEmoticon(emoticonId: Int, emoticonUrl: String) {
        setState {
            copy(
                selectedEmoticonId = emoticonId,
                selectedEmoticonUrl = emoticonUrl
            )
        }
    }

    /**
     * 이모티콘 선택 해제
     */
    private fun clearEmoticon() {
        setState {
            copy(
                selectedEmoticonId = null,
                selectedEmoticonUrl = null
            )
        }
    }

    /**
     * 이모티콘 패널 토글
     */
    private fun toggleEmoticonPanel() {
        setState { copy(showEmoticonPanel = !showEmoticonPanel) }
    }

    /**
     * 이모티콘 패널 숨기기
     */
    private fun hideEmoticonPanel() {
        setState { copy(showEmoticonPanel = false) }
    }

    /**
     * 이미지 선택
     */
    private fun selectImage(uri: Uri, bytes: ByteArray, isGif: Boolean) {
        setState {
            copy(
                selectedImageUri = uri,
                selectedImageBytes = bytes,
                isGifImage = isGif
            )
        }
        logD(TAG, "Image selected: isGif=$isGif, size=${bytes.size}")
    }

    /**
     * 이미지 선택 해제
     */
    private fun clearImage() {
        setState {
            copy(
                selectedImageUri = null,
                selectedImageBytes = null,
                isGifImage = false
            )
        }
    }

    /**
     * 댓글 작성
     * 작성된 댓글을 즉시 목록 맨 앞에 추가하여 바로 보여줌
     */
    private fun submitComment() {
        val content = currentState.commentText.trim()
        val emoticonId = currentState.selectedEmoticonId
        val emoticonUrl = currentState.selectedEmoticonUrl
        val imageBytes = currentState.selectedImageBytes

        // old 프로젝트와 동일: 텍스트, 이모티콘, 이미지 중 하나는 있어야 함
        if (content.isEmpty() && emoticonId == null && imageBytes == null) return
        if (currentState.isSubmitting) return

        setState { copy(isSubmitting = true, showEmoticonPanel = false) }

        viewModelScope.launch {
            heartpickRepository.postReply(
                heartPickId = currentState.heartPickId,
                content = content,
                emoticonId = emoticonId,
                imageBytes = imageBytes
            ).collect { result ->
                when (result) {
                    is ApiResult.Loading -> { /* skip */ }
                    is ApiResult.Success -> {
                        logD(TAG, "Comment submitted successfully")

                        val heartPickId = currentState.heartPickId

                        // 입력창 초기화
                        setState {
                            copy(
                                commentText = "",
                                selectedEmoticonId = null,
                                selectedEmoticonUrl = null,
                                selectedImageUri = null,
                                selectedImageBytes = null,
                                isGifImage = false,
                                isSubmitting = false
                            )
                        }

                        // 댓글 수 증가 Effect 전송
                        setEffect {
                            CommentOnlyContract.Effect.CommentSubmitted(
                                heartPickId = heartPickId,
                                commentCount = currentState.comments.size + 1
                            )
                        }

                        // 처음부터 다시 로드
                        loadComments(heartPickId)
                    }
                    is ApiResult.Error -> {
                        setState { copy(isSubmitting = false) }
                        setEffect { CommentOnlyContract.Effect.ShowToast(result.message ?: "Failed to submit comment") }
                        logE(TAG, "Failed to submit comment: ${result.message}")
                    }
                }
            }
        }
    }

    /**
     * 프로필 클릭 처리
     */
    private fun onProfileClick(comment: CommentModel) {
        val userId = comment.user?.id ?: return
        val nickname = comment.user?.nickname
        setEffect { CommentOnlyContract.Effect.NavigateToFeed(userId, nickname) }
    }

    /**
     * 이미지 클릭 처리
     */
    private fun onImageClick(imageUrl: String) {
        setEffect { CommentOnlyContract.Effect.ShowImageDetail(imageUrl) }
    }

    /**
     * 새로고침
     */
    private fun refresh() {
        loadComments(currentState.heartPickId)
    }

    /**
     * 댓글 삭제
     * 삭제 시 목록에서 즉시 제거
     * 하트픽 댓글은 resourceUri를 사용하여 삭제 (replies/{id}/ 형식)
     */
    private fun deleteComment(commentId: Int) {
        // 댓글 목록에서 resourceUri 찾기
        val comment = currentState.comments.find { it.id == commentId }
        val resourceUri = comment?.resourceUri

        if (resourceUri.isNullOrEmpty()) {
            logE(TAG, "Cannot delete comment: resourceUri is null for commentId=$commentId")
            setEffect {
                CommentOnlyContract.Effect.ShowToast(
                    context.getString(R.string.desc_failed_to_connect_internet)
                )
            }
            return
        }

        viewModelScope.launch {
            commentsRepository.deleteCommentByUri(resourceUri).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        // 목록에서 즉시 제거
                        val updatedComments = currentState.comments.filter { it.id != commentId }
                        setState { copy(comments = updatedComments) }

                        // 댓글 수 변경 알림
                        setEffect {
                            CommentOnlyContract.Effect.CommentSubmitted(
                                heartPickId = currentState.heartPickId,
                                commentCount = updatedComments.size
                            )
                        }
                        setEffect { CommentOnlyContract.Effect.CommentDeleted }
                        logD(TAG, "Comment deleted: $commentId (resourceUri: $resourceUri)")
                    }
                    is ApiResult.Error -> {
                        setEffect {
                            CommentOnlyContract.Effect.ShowToast(
                                result.message ?: context.getString(R.string.desc_failed_to_connect_internet)
                            )
                        }
                        logE(TAG, "Failed to delete comment: ${result.message}")
                    }
                    else -> { /* skip */ }
                }
            }
        }
    }

    /**
     * 댓글 신고
     */
    private fun reportComment(commentId: Int) {
        viewModelScope.launch {
            when (val result = reportRepository.reportComment(commentId)) {
                is ReportResult.Success -> {
                    setEffect { CommentOnlyContract.Effect.CommentReported }
                    logD(TAG, "Comment reported: $commentId")
                }
                is ReportResult.Error -> {
                    setEffect { CommentOnlyContract.Effect.ReportError(result.gcode) }
                    logE(TAG, "Failed to report comment: gcode=${result.gcode}")
                }
            }
        }
    }

    /**
     * 댓글 번역 가능 여부 업데이트
     */
    fun updateCommentTranslatable(commentId: Int, isTranslatable: Boolean) {
        val comments = currentState.comments.toMutableList()
        val index = comments.indexOfFirst { it.id == commentId }
        if (index != -1 && comments[index].isTranslatable == null) {
            comments[index] = comments[index].copy(isTranslatable = isTranslatable)
            setState { copy(comments = comments) }
        }
    }

    /**
     * 댓글 번역
     */
    fun translateComment(commentId: Int) {
        val comments = currentState.comments.toMutableList()
        val index = comments.indexOfFirst { it.id == commentId }
        if (index == -1) return

        val comment = comments[index]

        if (comment.translateState == TranslateState.ORIGINAL) {
            // 번역 시작 - TRANSLATING 상태로 변경
            comments[index] = comment.copy(translateState = TranslateState.TRANSLATING)
            setState { copy(comments = comments) }

            // 현재는 기본적으로 원문으로 유지
            viewModelScope.launch {
                delay(500) // 임시 대기
                val updatedComments = currentState.comments.toMutableList()
                val currentIndex = updatedComments.indexOfFirst { it.id == commentId }
                if (currentIndex != -1) {
                    updatedComments[currentIndex] = updatedComments[currentIndex].copy(
                        translateState = TranslateState.ORIGINAL
                    )
                    setState { copy(comments = updatedComments) }
                }
            }
        } else {
            // 이미 번역된 상태 - 원문으로 복원
            comments[index] = comment.copy(
                content = comment.originalContent,
                translateState = TranslateState.ORIGINAL
            )
            setState { copy(comments = comments) }
        }
    }
}
