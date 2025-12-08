package net.ib.mn.presentation.article.write

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
import net.ib.mn.domain.repository.FileUploadData
import net.ib.mn.domain.repository.FilesRepository
import net.ib.mn.domain.repository.IdolRepository
import net.ib.mn.domain.repository.CheckReadyResult
import net.ib.mn.domain.repository.PresignedUrlResult
import net.ib.mn.presentation.article.write.ArticleWriteContract.*
import net.ib.mn.domain.model.UploadVideoSpecModel
import net.ib.mn.util.Constants
import net.ib.mn.util.ImageUtil
import net.ib.mn.util.LinkParser
import net.ib.mn.util.VideoProcessor
import net.ib.mn.util.logD
import net.ib.mn.util.logE
import javax.inject.Inject

private const val TAG = "ArticleWriteViewModel"

/**
 * 게시글 작성/수정 ViewModel
 */
@HiltViewModel
class ArticleWriteViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val idolRepository: IdolRepository,
    private val articlesRepository: ArticlesRepository,
    private val filesRepository: FilesRepository,
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
            is Intent.OnConfirmBack -> onConfirmBack()
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
                editingArticle?.let { loadExistingMedia(it) }

            } catch (e: Exception) {
                setState { copy(isLoading = false, error = e.message) }
                setEffect { Effect.ShowError(e.message ?: "초기화에 실패했습니다.") }
            }
        }
    }

    private fun onTitleChanged(title: String) {
        setState { copy(title = title.take(State.MAX_TITLE_LENGTH)) }
    }

    private fun onContentChanged(content: String) {
        val trimmedContent = content.take(State.MAX_CONTENT_LENGTH)
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
        // 비율 저장 후 사진 피커 열기
        setState { copy(useSquareImage = isSquare) }
        setEffect { Effect.RequestPhotoPermission }
    }

    private fun onMediaSelected(uris: List<Uri>, type: MediaType) {
        viewModelScope.launch {
            val currentMedia = currentState.attachedMedia.toMutableList()
            val availableSlots = currentState.maxMediaCount - currentMedia.size

            // 동영상은 1개만 가능
            if (type == MediaType.VIDEO) {
                currentMedia.clear()
                if (uris.isNotEmpty()) {
                    val videoUri = uris.first()

                    // 동영상 인코딩 시작 알림
                    setState { copy(isLoading = true) }
                    setEffect { Effect.ShowToast("동영상을 처리하는 중...") }

                    // VideoProcessor로 동영상 인코딩
                    val result = VideoProcessor.processVideo(
                        context = context,
                        sourceUri = videoUri,
                        spec = UploadVideoSpecModel(),
                        onProgress = { progress ->
                            logD(TAG, "Video processing: ${(progress * 100).toInt()}%")
                        }
                    )

                    result.fold(
                        onSuccess = { processedVideo ->
                            currentMedia.add(
                                AttachedMedia(
                                    uri = videoUri,
                                    type = MediaType.VIDEO,
                                    optimizedData = processedVideo.byteArray,
                                    width = processedVideo.width,
                                    height = processedVideo.height,
                                    hash = processedVideo.hash,
                                    mimeType = "video/mp4"
                                )
                            )
                            // 임시 파일 삭제는 업로드 후에
                            setState { copy(isLoading = false) }
                        },
                        onFailure = { error ->
                            logE(TAG, "Video processing failed: ${error.message}")
                            setState { copy(isLoading = false) }
                            setEffect { Effect.ShowError(error.message ?: "동영상 처리에 실패했습니다.") }
                            return@launch
                        }
                    )
                }
            } else {
                // 이미지는 최대 개수까지 추가 + 최적화
                uris.take(availableSlots).forEach { uri ->
                    // 이미지 최적화 수행 (백그라운드 스레드)
                    val optimizedImage = withContext(Dispatchers.IO) {
                        ImageUtil.optimizeImage(context, uri)
                    }

                    if (optimizedImage != null) {
                        currentMedia.add(
                            AttachedMedia(
                                uri = uri,
                                type = MediaType.IMAGE,
                                optimizedData = optimizedImage.byteArray,
                                width = optimizedImage.width,
                                height = optimizedImage.height,
                                hash = optimizedImage.hash,
                                mimeType = optimizedImage.mimeType
                            )
                        )
                    } else {
                        // 최적화 실패 시에도 Uri만 저장 (fallback)
                        currentMedia.add(AttachedMedia(uri = uri, type = MediaType.IMAGE))
                        logE(TAG, "Image optimization failed for: $uri")
                    }
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

        // 공개 범위: 최애공개면 "private", 전체공개면 "public" (old 프로젝트 Const.SHOW_PRIVATE/SHOW_PUBLIC)
        val showScope = if (state.isPrivateToFavorite) "private" else "public"

        // 1. 첨부 미디어가 있으면 먼저 S3에 업로드
        val uploadedFiles = if (state.attachedMedia.isNotEmpty()) {
            val files = uploadMediaFiles(state.attachedMedia)
            if (files == null) {
                // 업로드 실패
                setState { copy(isSaving = false) }
                setEffect { Effect.ShowError("파일 업로드에 실패했습니다.") }
                return
            }
            files
        } else {
            emptyList()
        }

        // 2. writeType에 따라 다른 API 호출
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
                    showScope = showScope,
                    files = uploadedFiles
                )
            }
            ArticleWriteType.FREE_BOARD -> {
                // 자유게시판: articles/create/ with FREE_BOARD_IDOL_ID
                articlesRepository.createArticle(
                    idolId = Constants.FREE_BOARD_IDOL_ID,
                    content = state.content,
                    title = state.title,
                    tagId = state.selectedTag?.id?.toString() ?: "1",
                    show = showScope,
                    files = uploadedFiles
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
                    show = showScope,
                    files = uploadedFiles
                )
            }
        }

        apiFlow.collectLatest { result ->
            when (result) {
                is ApiResult.Success -> {
                    val data = result.data

                    // GCode에 따른 처리 (old 프로젝트와 동일)
                    val currentState = uiState.value
                    val writeType = currentState.writeType
                    val idolId = currentState.idol?.id
                    val tagId = currentState.selectedTag?.id

                    // 이미지가 첨부된 경우 checkReady로 처리 완료 대기
                    if (uploadedFiles.isNotEmpty() && data.articleId != null) {
                        val checkResult = waitForCheckReady(data.articleId)
                        if (!checkResult.success) {
                            if (checkResult.gcode == CheckReadyResult.GCODE_UPLOAD_FAILED) {
                                setState { copy(isSaving = false) }
                                setEffect { Effect.ShowError("이미지 업로드에 실패했습니다.") }
                                return@collectLatest
                            }
                        }
                    }

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

                    // 작성 내용 초기화 후 나가기
                    clearContent()
                    setState { copy(isSaving = false) }
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

    /**
     * 이미지 처리 완료 대기
     * Old 프로젝트의 PresignedUrlService.articlesCheckReady() 참고
     *
     * - 1초 딜레이 후 시작
     * - 2초마다 checkReady 호출
     * - 최대 30번 시도 (1분)
     * - success=true면 완료
     * - gcode=3902면 업로드 실패
     */
    private suspend fun waitForCheckReady(articleId: Long): CheckReadyResult {
        return withContext(Dispatchers.IO) {
            // 1초 딜레이 후 시작
            delay(1000)

            var attempts = 0
            val maxAttempts = 30

            while (attempts < maxAttempts) {
                val result = articlesRepository.checkReady(articleId)
                logD(TAG, "checkReady attempt ${attempts + 1}: success=${result.success}, gcode=${result.gcode}")

                if (result.success) {
                    return@withContext result
                }

                // 업로드 실패 (gcode 3902)
                if (result.gcode == CheckReadyResult.GCODE_UPLOAD_FAILED) {
                    return@withContext result
                }

                // 2초 대기 후 재시도
                delay(2000)
                attempts++
            }

            // 최대 시도 횟수 초과
            logE(TAG, "checkReady timeout: exceeded $maxAttempts attempts")
            CheckReadyResult(success = true)  // 타임아웃이어도 성공으로 처리 (old 프로젝트와 동일)
        }
    }

    /**
     * 미디어 파일들을 S3에 업로드
     * Old 프로젝트의 PresignedUrlService.startPresignedAndCreate() 참고
     *
     * @param mediaList 업로드할 미디어 리스트
     * @return 업로드된 파일 정보 리스트, 실패 시 null
     */
    private suspend fun uploadMediaFiles(mediaList: List<AttachedMedia>): List<FileUploadData>? {
        return withContext(Dispatchers.IO) {
            val uploadedFiles = mutableListOf<FileUploadData>()

            for ((index, media) in mediaList.withIndex()) {
                try {
                    // 이미지 데이터 준비
                    val byteArray = media.optimizedData ?: run {
                        // optimizedData가 없으면 원본 Uri에서 읽기
                        val inputStream = context.contentResolver.openInputStream(media.uri)
                        inputStream?.readBytes() ?: run {
                            logE(TAG, "Failed to read file: ${media.uri}")
                            return@withContext null
                        }
                    }

                    val width = media.width
                    val height = media.height
                    val hash = media.hash.orEmpty()
                    val mimeType = media.mimeType

                    // 파일명 생성 (확장자 포함)
                    val extension = when {
                        mimeType.contains("png") -> ".png"
                        mimeType.contains("webp") -> ".webp"
                        mimeType.contains("mp4") || media.type == MediaType.VIDEO -> ".mp4"
                        else -> ".jpg"
                    }
                    val filename = "${System.currentTimeMillis()}_$index$extension"

                    // 파일 타입 결정 (Old 프로젝트와 동일: "st" = 이미지, "mv" = 동영상/GIF)
                    val fileType = if (media.type == MediaType.VIDEO || mimeType.contains("gif")) {
                        Constants.FILE_TYPE_VIDEO
                    } else {
                        Constants.FILE_TYPE_IMAGE
                    }

                    // 1. Presigned URL 요청
                    val presignedResult = filesRepository.getPresignedUrl(
                        bucket = Constants.NCLOUD_ARTICLES_BUCKET,
                        filename = filename,
                        width = width,
                        height = height,
                        imageHash = hash,
                        fileType = fileType
                    )

                    if (!presignedResult.success) {
                        logE(TAG, "Failed to get presigned URL for: $filename")
                        return@withContext null
                    }

                    // 이미 존재하는 이미지인 경우 (gcode = 3900)
                    if (presignedResult.gcode == PresignedUrlResult.GCODE_ALREADY_EXISTS) {
                        uploadedFiles.add(
                            FileUploadData(
                                seq = index + 1,
                                size = byteArray.size.toLong(),
                                savedFilename = presignedResult.savedFilename,
                                originName = filename
                            )
                        )
                        continue
                    }

                    // 2. S3에 파일 업로드
                    val uploadSuccess = filesRepository.writeCdn(
                        url = presignedResult.url,
                        awsAccessKeyId = presignedResult.awsAccessKeyId,
                        acl = presignedResult.acl,
                        key = presignedResult.key,
                        policy = presignedResult.policy,
                        signature = presignedResult.signature,
                        file = byteArray,
                        filename = presignedResult.savedFilename,
                        mimeType = mimeType
                    )

                    if (!uploadSuccess) {
                        logE(TAG, "Failed to upload file to CDN: $filename")
                        return@withContext null
                    }

                    // 3. 업로드 성공 - 파일 정보 저장
                    uploadedFiles.add(
                        FileUploadData(
                            seq = index + 1,
                            size = byteArray.size.toLong(),
                            savedFilename = presignedResult.savedFilename,
                            originName = filename
                        )
                    )

                } catch (e: Exception) {
                    logE(TAG, "Error uploading media: ${media.uri}", e)
                    return@withContext null
                }
            }

            uploadedFiles
        }
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

    /**
     * 작성 취소 확인 - 내용 clear 후 나가기
     */
    private fun onConfirmBack() {
        clearContent()
        setEffect { Effect.NavigateBack }
    }

    /**
     * 작성 중인 내용 초기화
     */
    private fun clearContent() {
        linkParseJob?.cancel()
        setState {
            copy(
                title = "",
                content = "",
                attachedMedia = emptyList(),
                linkPreview = null,
                selectedTag = null,
                isPrivateToFavorite = false,
                isPhotoEnabled = true,
                isVideoEnabled = true,
                useSquareImage = true
            )
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
