package net.ib.mn.presentation.comment

import android.net.Uri
import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.domain.model.CommentModel

/**
 * CommentOnly (하트픽 응원 댓글) 화면의 MVI Contract.
 *
 * old 프로젝트: CommentOnlyActivity
 */
class CommentOnlyContract {

    /**
     * UI State for CommentOnly screen.
     */
    data class State(
        val heartPickId: Int = 0,
        val comments: List<CommentModel> = emptyList(),
        val isLoading: Boolean = true,
        val isSubmitting: Boolean = false,
        val hasMore: Boolean = false,
        val commentText: String = "",
        val selectedEmoticonId: Int? = null,
        val selectedEmoticonUrl: String? = null,
        val selectedImageUri: Uri? = null,
        val selectedImageBytes: ByteArray? = null,
        val isGifImage: Boolean = false,
        val showEmoticonPanel: Boolean = false,
        val error: String? = null
    ) : UiState

    /**
     * User intents
     */
    sealed class Intent : UiIntent {
        /** 댓글 목록 로드 */
        data class LoadComments(val heartPickId: Int) : Intent()

        /** 더 많은 댓글 로드 */
        data object LoadMoreComments : Intent()

        /** 댓글 텍스트 변경 */
        data class SetCommentText(val text: String) : Intent()

        /** 이모티콘 선택 */
        data class SelectEmoticon(val emoticonId: Int, val emoticonUrl: String) : Intent()

        /** 이모티콘 선택 해제 */
        data object ClearEmoticon : Intent()

        /** 이모티콘 패널 토글 */
        data object ToggleEmoticonPanel : Intent()

        /** 이모티콘 패널 숨기기 */
        data object HideEmoticonPanel : Intent()

        /** 이미지 선택 */
        data class SelectImage(val uri: Uri, val bytes: ByteArray, val isGif: Boolean) : Intent()

        /** 이미지 선택 해제 */
        data object ClearImage : Intent()

        /** 댓글 작성 */
        data object SubmitComment : Intent()

        /** 프로필 클릭 */
        data class OnProfileClick(val comment: CommentModel) : Intent()

        /** 댓글 이미지 클릭 */
        data class OnImageClick(val imageUrl: String) : Intent()

        /** 댓글 삭제 */
        data class DeleteComment(val commentId: Int) : Intent()

        /** 댓글 신고 */
        data class ReportComment(val commentId: Int) : Intent()

        /** 새로고침 */
        data object Refresh : Intent()
    }

    /**
     * Side effects (일회성 이벤트)
     */
    sealed class Effect : UiEffect {
        /** 토스트 메시지 표시 */
        data class ShowToast(val message: String) : Effect()

        /** 토스트 메시지 표시 (리소스 ID) */
        data class ShowToastRes(val messageResId: Int) : Effect()

        /** 유저 피드로 이동 */
        data class NavigateToFeed(val userId: Int, val nickname: String?) : Effect()

        /** 이미지 상세보기 */
        data class ShowImageDetail(val imageUrl: String) : Effect()

        /** 댓글 작성 완료 (결과 반환용) */
        data class CommentSubmitted(val heartPickId: Int, val commentCount: Int) : Effect()

        /** 첫 댓글로 스크롤 */
        data object ScrollToTop : Effect()

        /** 뒤로가기 */
        data object NavigateBack : Effect()

        /** 댓글 삭제 완료 */
        data object CommentDeleted : Effect()

        /** 댓글 신고 완료 */
        data object CommentReported : Effect()

        /** 댓글 신고 실패 (gcode 기반 에러) */
        data class ReportError(val gcode: Int) : Effect()

        /** 복사 완료 */
        data object CopyCompleted : Effect()
    }
}
