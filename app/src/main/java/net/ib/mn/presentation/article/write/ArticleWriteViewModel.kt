package net.ib.mn.presentation.article.write

import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import net.ib.mn.base.BaseViewModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.model.ArticleModel
import net.ib.mn.domain.model.TagModel
import net.ib.mn.domain.repository.ArticlesRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.presentation.article.write.ArticleWriteContract.*
import net.ib.mn.util.Constants
import net.ib.mn.util.LinkParser
import net.ib.mn.util.logE
import javax.inject.Inject

private const val TAG = "ArticleWriteViewModel"

/**
 * 게시글 작성/수정 ViewModel
 */
@HiltViewModel
class ArticleWriteViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val idolRepository: IdolRepository,
    private val articlesRepository: ArticlesRepository,
    private val gson: Gson
) : BaseViewModel<State, Intent, Effect>() {

    private var linkParseJob: Job? = null

    override fun createInitialState(): State = State()

    override fun handleIntent(intent: Intent) {
        when (intent) {
            is Intent.Initialize -> initialize(intent)
            is Intent.OnTitleChanged -> onTitleChanged(intent.title)
            is Intent.OnContentChanged -> onContentChanged(intent.content)
            is Intent.OnTagSelectorClick -> setEffect { Effect.ShowTagSelector }
            is Intent.OnTagSelected -> onTagSelected(intent.tag)
            is Intent.OnPhotoClick -> onPhotoClick()
            is Intent.OnVideoClick -> onVideoClick()
            is Intent.OnMediaSelected -> onMediaSelected(intent.uris, intent.type)
            is Intent.OnMediaRemoved -> onMediaRemoved(intent.index)
            is Intent.OnLinkPreviewRemoved -> onLinkPreviewRemoved()
            is Intent.OnSettingClick -> setEffect { Effect.ShowSettingBottomSheet }
            is Intent.OnPrivateSettingChanged -> onPrivateSettingChanged(intent.isPrivate)
            is Intent.OnImageRatioSelected -> onImageRatioSelected(intent.isSquare)
            is Intent.OnSubmitClick -> onSubmitClick()
            is Intent.OnBackClick -> onBackClick()
            is Intent.DismissDialog -> { /* Dialog dismissed by UI */ }
        }
    }

    private fun initialize(intent: Intent.Initialize) {
        viewModelScope.launch {
            setState { copy(isLoading = true) }

            try {
                // 아이돌 정보 로드
                val idol = intent.idolId?.let { idolRepository.getIdolById(it) }

                // 최애 아이돌 ID 조회
                val mostIdolId = preferencesManager.getMostIdolId()

                // 최애 공개 설정 버튼 표시 여부 결정
                val showPrivateSetting = when (intent.writeType) {
                    ArticleWriteType.FEED -> idol?.id == mostIdolId
                    ArticleWriteType.FAN_TALK -> idol?.id == mostIdolId
                    ArticleWriteType.FREE_BOARD -> false // 자유게시판은 태그 선택에 따라 결정
                }

                // 자유게시판인 경우 태그 목록 로드
                val tags = if (intent.writeType == ArticleWriteType.FREE_BOARD) {
                    loadTags()
                } else {
                    emptyList()
                }

                // 선택된 태그 (intent에서 tagId가 전달된 경우)
                val selectedTag = intent.tagId?.let { tagId ->
                    tags.find { it.id == tagId }
                }

                // 수정 모드인 경우 기존 데이터 설정
                val editingArticle = intent.editingArticle
                val isEditMode = editingArticle != null

                setState {
                    copy(
                        isLoading = false,
                        writeType = intent.writeType,
                        isEditMode = isEditMode,
                        editingArticle = editingArticle,
                        idol = idol,
                        idolName = idol?.name ?: "",
                        title = editingArticle?.title ?: "",
                        content = editingArticle?.content ?: "",
                        tags = tags,
                        selectedTag = selectedTag,
                        showPrivateSetting = showPrivateSetting,
                        isPrivateToFavorite = editingArticle?.isMostOnly == "Y",
                        // 수정 모드에서는 사진/동영상 버튼 숨김
                        isPhotoEnabled = !isEditMode,
                        isVideoEnabled = !isEditMode
                    )
                }

                // 수정 모드에서 기존 미디어 파일 로드
                if (isEditMode && editingArticle != null) {
                    loadExistingMedia(editingArticle)
                }

            } catch (e: Exception) {
                setState { copy(isLoading = false, error = e.message) }
                setEffect { Effect.ShowError(e.message ?: "초기화에 실패했습니다.") }
            }
        }
    }

    private fun onTitleChanged(title: String) {
        val trimmedTitle = if (title.length > State.MAX_TITLE_LENGTH) {
            title.take(State.MAX_TITLE_LENGTH)
        } else {
            title
        }
        setState { copy(title = trimmedTitle) }
    }

    private fun onContentChanged(content: String) {
        val trimmedContent = if (content.length > State.MAX_CONTENT_LENGTH) {
            content.take(State.MAX_CONTENT_LENGTH)
        } else {
            content
        }
        setState { copy(content = trimmedContent) }

        // URL 감지 및 링크 프리뷰 로드
        if (currentState.linkPreview == null && currentState.attachedMedia.isEmpty()) {
            detectAndLoadLinkPreview(trimmedContent)
        }
    }

    private fun detectAndLoadLinkPreview(content: String) {
        linkParseJob?.cancel()
        linkParseJob = viewModelScope.launch {
            delay(500) // 디바운스

            val url = LinkParser.extractFirstUrl(content)
            if (url != null) {
                setState { copy(isLoadingLinkPreview = true) }
                try {
                    val linkData = withContext(Dispatchers.IO) {
                        LinkParser.parse(url)
                    }
                    if (linkData != null) {
                        setState {
                            copy(
                                linkPreview = LinkPreviewData(
                                    url = linkData.url,
                                    title = linkData.title,
                                    description = linkData.description,
                                    imageUrl = linkData.imageUrl,
                                    host = linkData.host
                                ),
                                isLoadingLinkPreview = false,
                                isPhotoEnabled = false,
                                isVideoEnabled = false
                            )
                        }
                    } else {
                        setState { copy(isLoadingLinkPreview = false) }
                    }
                } catch (e: Exception) {
                    setState { copy(isLoadingLinkPreview = false) }
                }
            }
        }
    }

    private fun onTagSelected(tag: TagModel) {
        // FREE_BOARD에서는 최애공개 설정이 없으므로 showPrivateSetting은 항상 false
        setState {
            copy(
                selectedTag = tag,
                showPrivateSetting = false,
                isPrivateToFavorite = false
            )
        }
    }

    private fun onPhotoClick() {
        if (!currentState.canAddMedia) {
            setEffect { Effect.ShowToast("더 이상 미디어를 추가할 수 없습니다.") }
            return
        }

        // 커뮤니티/자유게시판에서는 이미지 비율 선택 다이얼로그 표시
        if (currentState.writeType == ArticleWriteType.FEED ||
            currentState.writeType == ArticleWriteType.FREE_BOARD) {
            setEffect { Effect.ShowImageRatioDialog }
        } else {
            // 팬톡은 바로 사진 피커 열기
            setEffect { Effect.RequestPhotoPermission }
        }
    }

    private fun onVideoClick() {
        if (!currentState.canAddMedia) {
            setEffect { Effect.ShowToast("더 이상 미디어를 추가할 수 없습니다.") }
            return
        }

        if (currentState.attachedMedia.isNotEmpty()) {
            setEffect { Effect.ShowToast("동영상은 다른 미디어와 함께 첨부할 수 없습니다.") }
            return
        }

        setEffect { Effect.RequestVideoPermission }
    }

    private fun onImageRatioSelected(isSquare: Boolean) {
        // 비율 선택 후 사진 피커 열기
        setEffect { Effect.RequestPhotoPermission }
    }

    private fun onMediaSelected(uris: List<Uri>, type: MediaType) {
        val currentMedia = currentState.attachedMedia.toMutableList()
        val availableSlots = currentState.maxMediaCount - currentMedia.size

        // 동영상은 1개만 가능
        if (type == MediaType.VIDEO) {
            currentMedia.clear()
            if (uris.isNotEmpty()) {
                currentMedia.add(AttachedMedia(uri = uris.first(), type = MediaType.VIDEO))
            }
        } else {
            // 이미지는 최대 개수까지 추가
            uris.take(availableSlots).forEach { uri ->
                currentMedia.add(AttachedMedia(uri = uri, type = MediaType.IMAGE))
            }
        }

        // 버튼 상태 업데이트
        val hasVideo = currentMedia.any { it.type == MediaType.VIDEO }
        val isFull = currentMedia.size >= currentState.maxMediaCount

        setState {
            copy(
                attachedMedia = currentMedia,
                isPhotoEnabled = !hasVideo && !isFull,
                isVideoEnabled = currentMedia.isEmpty()
            )
        }
    }

    private fun onMediaRemoved(index: Int) {
        val currentMedia = currentState.attachedMedia.toMutableList()
        if (index in currentMedia.indices) {
            currentMedia.removeAt(index)
        }

        val hasVideo = currentMedia.any { it.type == MediaType.VIDEO }
        val isFull = currentMedia.size >= currentState.maxMediaCount

        setState {
            copy(
                attachedMedia = currentMedia,
                isPhotoEnabled = !hasVideo && !isFull && linkPreview == null,
                isVideoEnabled = currentMedia.isEmpty() && linkPreview == null
            )
        }
    }

    private fun onLinkPreviewRemoved() {
        linkParseJob?.cancel()
        setState {
            copy(
                linkPreview = null,
                isPhotoEnabled = attachedMedia.isEmpty() || !hasVideo,
                isVideoEnabled = attachedMedia.isEmpty()
            )
        }
    }

    private fun onPrivateSettingChanged(isPrivate: Boolean) {
        setState { copy(isPrivateToFavorite = isPrivate) }
    }

    private fun onSubmitClick() {
        setEffect { Effect.HideKeyboard }

        // 유효성 검사
        if (!validateInput()) {
            return
        }

        viewModelScope.launch {
            setState { copy(isSaving = true) }

            // 업로드 중 알림 표시 (old 프로젝트의 PresignedUrlService와 동일)
            setEffect { Effect.ShowUploadingNotification }

            try {
                if (currentState.isEditMode) {
                    updateArticle()
                } else {
                    createArticle()
                }
            } catch (e: Exception) {
                setState { copy(isSaving = false) }
                setEffect { Effect.ShowError(e.message ?: "저장에 실패했습니다.") }
            }
        }
    }

    private fun validateInput(): Boolean {
        val state = currentState

        // 자유게시판: 태그 필수
        if (state.writeType == ArticleWriteType.FREE_BOARD && state.selectedTag == null) {
            setEffect { Effect.ShowToast("카테고리를 선택해주세요.") }
            return false
        }

        // 팬톡: 제목 필수
        if (state.writeType == ArticleWriteType.FAN_TALK && state.title.isBlank()) {
            setEffect { Effect.ShowToast("제목을 입력해주세요.") }
            return false
        }

        // 자유게시판: 제목 또는 내용 필수
        if (state.writeType == ArticleWriteType.FREE_BOARD) {
            if (state.title.isBlank() && state.content.isBlank() && state.attachedMedia.isEmpty()) {
                setEffect { Effect.ShowToast("내용을 입력해주세요.") }
                return false
            }
        }

        // 피드/팬톡: 내용 또는 미디어 필수
        if (state.writeType != ArticleWriteType.FREE_BOARD) {
            if (state.content.isBlank() && state.attachedMedia.isEmpty()) {
                setEffect { Effect.ShowToast("내용을 입력해주세요.") }
                return false
            }
        }

        return true
    }

    private suspend fun createArticle() {
        val state = currentState

        // 공개 범위: 최애공개면 "M", 전체공개면 "A"
        val showScope = if (state.isPrivateToFavorite) "M" else "A"

        // writeType에 따라 다른 API 호출
        // Old 프로젝트 WriteArticleActivity.kt:297-300 참조:
        // - FREE_BOARD: idol_id = FREE_BOARD_IDOL_ID (99990)
        // - FEED/FAN_TALK: idol_id = 실제 아이돌 ID
        val apiFlow = when (state.writeType) {
            ArticleWriteType.FAN_TALK -> {
                // 덕질게시판(팬톡): articles/insert/
                val idolId = state.idol?.id ?: preferencesManager.getMostIdolId() ?: run {
                    logE(TAG, "createArticle - idolId is null for FAN_TALK")
                    setState { copy(isSaving = false) }
                    setEffect { Effect.ShowError("아이돌 정보를 찾을 수 없습니다.") }
                    return
                }
                articlesRepository.insertArticle(
                    idolId = idolId,
                    content = state.content,
                    title = state.title,
                    showScope = showScope
                )
            }
            ArticleWriteType.FREE_BOARD -> {
                // 자유게시판: articles/create/ with FREE_BOARD_IDOL_ID
                articlesRepository.createArticle(
                    idolId = Constants.FREE_BOARD_IDOL_ID,
                    content = state.content,
                    title = state.title,
                    tagId = state.selectedTag?.id?.toString() ?: "1",
                    show = showScope
                )
            }
            ArticleWriteType.FEED -> {
                // 피드: articles/create/ with 실제 아이돌 ID
                val idolId = state.idol?.id ?: preferencesManager.getMostIdolId() ?: run {
                    logE(TAG, "createArticle - idolId is null for FEED")
                    setState { copy(isSaving = false) }
                    setEffect { Effect.ShowError("아이돌 정보를 찾을 수 없습니다.") }
                    return
                }
                articlesRepository.createArticle(
                    idolId = idolId,
                    content = state.content,
                    title = state.title,
                    tagId = state.selectedTag?.id?.toString() ?: "1",
                    show = showScope
                )
            }
        }

        apiFlow.collectLatest { result ->
            when (result) {
                is ApiResult.Success -> {
                    val data = result.data
                    setState { copy(isSaving = false) }

                    // GCode에 따른 처리 (old 프로젝트와 동일)
                    val currentState = uiState.value
                    val writeType = currentState.writeType
                    val idolId = currentState.idol?.id
                    val tagId = currentState.selectedTag?.id

                    when (data.gcode) {
                        GCODE_SUCCESS -> {
                            setEffect {
                                Effect.ShowSuccess(
                                    message = "게시가 완료되었습니다.",
                                    writeType = writeType,
                                    idolId = idolId,
                                    tagId = tagId
                                )
                            }
                        }
                        GCODE_SUCCESS_WITH_HEART -> {
                            setEffect {
                                Effect.ShowSuccess(
                                    message = "게시가 완료되었습니다.\n하트 ${data.provide.toInt()}개가 지급되었습니다.",
                                    heartReward = data.provide.toInt(),
                                    writeType = writeType,
                                    idolId = idolId,
                                    tagId = tagId
                                )
                            }
                        }
                        else -> {
                            setEffect {
                                Effect.ShowSuccess(
                                    message = "게시가 완료되었습니다.",
                                    writeType = writeType,
                                    idolId = idolId,
                                    tagId = tagId
                                )
                            }
                        }
                    }
                    setEffect { Effect.NavigateBackWithResult(isEdited = false) }
                }
                is ApiResult.Error -> {
                    logE(TAG, "createArticle - ${result.message}")
                    setState { copy(isSaving = false) }
                    setEffect { Effect.ShowError(result.message ?: "저장에 실패했습니다.") }
                }
                is ApiResult.Loading -> { /* Loading state handled by isSaving */ }
            }
        }
    }

    private suspend fun updateArticle() {
        // TODO: 수정 API 연동 후 구현
        setState { copy(isSaving = false) }
        setEffect { Effect.NavigateBackWithResult(isEdited = true) }
    }

    private fun onBackClick() {
        val state = currentState

        // 수정 모드에서는 바로 뒤로가기
        if (state.isEditMode) {
            setEffect { Effect.NavigateBack }
            return
        }

        // 작성 중인 내용이 있으면 확인 다이얼로그
        if (state.content.isNotBlank() || state.title.isNotBlank() || state.attachedMedia.isNotEmpty()) {
            setEffect { Effect.ShowBackConfirmDialog }
        } else {
            setEffect { Effect.NavigateBack }
        }
    }

    private suspend fun loadTags(): List<TagModel> {
        // PreferencesManager에서 저장된 태그 목록 가져오기 (FreeBoardViewModel과 동일)
        val tagsJson = preferencesManager.boardTags.first()
        if (tagsJson.isNullOrEmpty()) {
            return createDefaultTags()
        }

        return try {
            val listType = object : TypeToken<List<TagModel>>() {}.type
            val serverTags: List<TagModel> = gson.fromJson(tagsJson, listType)
            // 서버 태그만 반환 (HOT, ALL, 최애 탭은 글쓰기에서 사용하지 않음)
            serverTags
        } catch (e: Exception) {
            logE(TAG, "Failed to parse tags", e)
            createDefaultTags()
        }
    }

    private fun createDefaultTags(): List<TagModel> {
        // 기본 태그 (서버에서 태그를 가져오지 못한 경우)
        return listOf(
            TagModel(id = 1, name = "자유"),
            TagModel(id = 2, name = "덕질"),
            TagModel(id = 3, name = "정보"),
            TagModel(id = 4, name = "유머"),
            TagModel(id = 5, name = "팬아트")
        )
    }

    private fun loadExistingMedia(article: ArticleModel) {
        // 기존 미디어 파일을 Uri로 변환하여 로드
        val mediaList = article.files.mapNotNull { file ->
            file.thumbnailUrl?.let { url ->
                AttachedMedia(
                    uri = Uri.parse(url),
                    type = if (file.isVideo) MediaType.VIDEO else MediaType.IMAGE
                )
            }
        }

        if (mediaList.isNotEmpty()) {
            setState { copy(attachedMedia = mediaList) }
        }
    }

    companion object {
        // GCode 상수 (Old 프로젝트와 동일)
        private const val GCODE_SUCCESS = 0
        private const val GCODE_SUCCESS_WITH_HEART = 1
    }
}
