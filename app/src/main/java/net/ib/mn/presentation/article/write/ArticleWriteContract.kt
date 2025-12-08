package net.ib.mn.presentation.article.write

import android.net.Uri
import net.ib.mn.base.UiEffect
import net.ib.mn.base.UiIntent
import net.ib.mn.base.UiState
import net.ib.mn.data.local.entity.IdolEntity
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.TagModel

/**
 * 게시글 작성 타입
 */
enum class ArticleWriteType {
    FEED,        // 커뮤니티 피드 (타이틀 X, 태그 X)
    FREE_BOARD,  // 자유게시판 (타이틀 O, 태그 O)
    FAN_TALK     // 팬톡/덕질게시판 (타이틀 O, 태그 X)
}

/**
 * 미디어 타입
 */
enum class MediaType {
    IMAGE,
    VIDEO
}

/**
 * 첨부된 미디어 아이템
 */
data class AttachedMedia(
    val uri: Uri,
    val type: MediaType,
    val thumbnailUri: Uri? = null, // 비디오의 경우 썸네일
    // 이미지 최적화 데이터 (업로드용)
    val optimizedData: ByteArray? = null,
    val width: Int = 0,
    val height: Int = 0,
    val hash: String? = null,
    val mimeType: String = "image/jpeg"
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AttachedMedia

        if (uri != other.uri) return false
        if (type != other.type) return false
        if (thumbnailUri != other.thumbnailUri) return false
        if (optimizedData != null) {
            if (other.optimizedData == null) return false
            if (!optimizedData.contentEquals(other.optimizedData)) return false
        } else if (other.optimizedData != null) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (hash != other.hash) return false
        if (mimeType != other.mimeType) return false

        return true
    }

    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + type.hashCode()
        result = 31 * result + (thumbnailUri?.hashCode() ?: 0)
        result = 31 * result + (optimizedData?.contentHashCode() ?: 0)
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + (hash?.hashCode() ?: 0)
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

/**
 * 링크 프리뷰 데이터
 */
data class LinkPreviewData(
    val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val host: String?
)

/**
 * Article Write Contract - State, Intent, Effect
 */
class ArticleWriteContract {

    data class State(
        // 기본 상태
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,

        // 글쓰기 타입
        val writeType: ArticleWriteType = ArticleWriteType.FEED,

        // 수정 모드
        val isEditMode: Boolean = false,
        val editingArticle: ArticleModel? = null,

        // 아이돌 정보
        val idol: IdolEntity? = null,
        val idolName: String = "",

        // 입력 필드
        val title: String = "",
        val content: String = "",

        // 태그 (자유게시판용)
        val tags: List<TagModel> = emptyList(),
        val selectedTag: TagModel? = null,

        // 첨부 미디어
        val attachedMedia: List<AttachedMedia> = emptyList(),
        val maxMediaCount: Int = MAX_MEDIA_COUNT,

        // 링크 프리뷰
        val linkPreview: LinkPreviewData? = null,
        val isLoadingLinkPreview: Boolean = false,

        // 공개 설정
        val isPrivateToFavorite: Boolean = false, // 최애만 공개
        val showPrivateSetting: Boolean = false,  // 최애 공개 설정 버튼 표시 여부

        // 버튼 상태
        val isPhotoEnabled: Boolean = true,
        val isVideoEnabled: Boolean = true,

        // 이미지 비율 설정 (정사각형 크롭 여부)
        val useSquareImage: Boolean = true,

        // 유효성 검사
        val error: String? = null
    ) : UiState {

        /**
         * 제목 입력 필드 표시 여부
         */
        val showTitleField: Boolean
            get() = writeType == ArticleWriteType.FREE_BOARD || writeType == ArticleWriteType.FAN_TALK

        /**
         * 태그 선택 표시 여부
         */
        val showTagSelector: Boolean
            get() = writeType == ArticleWriteType.FREE_BOARD

        /**
         * 작성/수정 버튼 활성화 여부
         */
        val isSubmitEnabled: Boolean
            get() {
                // 수정 모드에서는 기본적으로 활성화
                if (isEditMode) return true

                // 로딩 중이면 비활성화
                if (isLoading || isSaving) return false

                // 자유게시판: 태그 + (제목 또는 내용 또는 미디어)
                if (writeType == ArticleWriteType.FREE_BOARD) {
                    if (selectedTag == null) return false
                    return title.isNotBlank() || content.isNotBlank() || attachedMedia.isNotEmpty()
                }

                // 팬톡: 제목 필수 + (내용 또는 미디어)
                if (writeType == ArticleWriteType.FAN_TALK) {
                    if (title.isBlank()) return false
                    return content.isNotBlank() || attachedMedia.isNotEmpty()
                }

                // 피드: 내용 또는 미디어
                return content.isNotBlank() || attachedMedia.isNotEmpty()
            }

        /**
         * 미디어 추가 가능 여부
         */
        val canAddMedia: Boolean
            get() = attachedMedia.size < maxMediaCount && linkPreview == null

        /**
         * 비디오가 첨부되어 있는지
         */
        val hasVideo: Boolean
            get() = attachedMedia.any { it.type == MediaType.VIDEO }

        companion object {
            const val MAX_MEDIA_COUNT = 5
            const val MAX_TITLE_LENGTH = 30
            const val MAX_CONTENT_LENGTH = 3000
        }
    }

    sealed class Intent : UiIntent {
        // 초기화
        data class Initialize(
            val writeType: ArticleWriteType,
            val idolId: Int?,
            val editingArticle: ArticleModel? = null,
            val tagId: Int? = null
        ) : Intent()

        // 입력
        data class OnTitleChanged(val title: String) : Intent()
        data class OnContentChanged(val content: String) : Intent()

        // 태그 선택
        data object OnTagSelectorClick : Intent()
        data class OnTagSelected(val tag: TagModel) : Intent()

        // 미디어 첨부
        data object OnPhotoClick : Intent()
        data object OnVideoClick : Intent()
        data class OnMediaSelected(val uris: List<Uri>, val type: MediaType) : Intent()
        data class OnMediaRemoved(val index: Int) : Intent()

        // 링크 프리뷰
        data object OnLinkPreviewRemoved : Intent()

        // 설정
        data object OnSettingClick : Intent()
        data class OnPrivateSettingChanged(val isPrivate: Boolean) : Intent()

        // 이미지 비율 선택 (커뮤니티/자유게시판)
        data class OnImageRatioSelected(val isSquare: Boolean) : Intent()

        // 제출
        data object OnSubmitClick : Intent()
        data object OnBackClick : Intent()

        // 다이얼로그 닫기
        data object DismissDialog : Intent()

        // 작성 취소 확인 (내용 clear 후 나가기)
        data object OnConfirmBack : Intent()
    }

    sealed class Effect : UiEffect {
        // 네비게이션
        data object NavigateBack : Effect()
        data class NavigateBackWithResult(val isEdited: Boolean) : Effect()

        // 권한 요청
        data object RequestPhotoPermission : Effect()
        data object RequestVideoPermission : Effect()

        // 미디어 피커 열기
        data object OpenPhotoPicker : Effect()
        data object OpenVideoPicker : Effect()

        // 이미지 크롭 화면 열기
        data class OpenImageCropper(val uri: Uri, val isSquare: Boolean) : Effect()

        // 이미지 비율 선택 다이얼로그
        data object ShowImageRatioDialog : Effect()

        // 태그 선택 바텀시트
        data object ShowTagSelector : Effect()

        // 설정 바텀시트
        data object ShowSettingBottomSheet : Effect()

        // 뒤로가기 확인 다이얼로그
        data object ShowBackConfirmDialog : Effect()

        // 토스트/스낵바
        data class ShowToast(val message: String) : Effect()
        data class ShowError(val message: String) : Effect()

        // 업로드 알림 (old 프로젝트의 PresignedUrlService와 동일)
        data object ShowUploadingNotification : Effect()
        data class ShowSuccess(
            val message: String,
            val heartReward: Int? = null,
            val writeType: ArticleWriteType,
            val idolId: Int?,
            val tagId: Int? = null
        ) : Effect()

        // 키보드 숨기기
        data object HideKeyboard : Effect()
    }
}
