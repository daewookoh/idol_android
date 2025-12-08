package net.ib.mn.presentation.article.write

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.theartofdev.edmodo.cropper.CropImage
import net.ib.mn.R
import net.ib.mn.presentation.article.write.ArticleWriteContract.*
import net.ib.mn.ui.components.ExoBottomSheet
import net.ib.mn.ui.components.ExoBottomSheetType
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.NotificationUtil

/**
 * 게시글 작성/수정 화면
 *
 * @param writeType 글쓰기 타입 (FEED, FREE_BOARD, FAN_TALK)
 * @param idolId 아이돌 ID (커뮤니티에서 진입 시)
 * @param editingArticleId 수정할 게시글 ID (수정 모드)
 * @param tagId 선택된 태그 ID (자유게시판에서 진입 시)
 * @param onNavigateBack 뒤로가기 콜백
 * @param onNavigateBackWithResult 결과와 함께 뒤로가기 콜백
 */
@Composable
fun ArticleWriteScreen(
    writeType: ArticleWriteType = ArticleWriteType.FEED,
    idolId: Int? = null,
    editingArticleId: String? = null,
    tagId: Int? = null,
    onNavigateBack: () -> Unit,
    onNavigateBackWithResult: (isEdited: Boolean) -> Unit = {},
    viewModel: ArticleWriteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // 다이얼로그 상태
    var showBackConfirmDialog by remember { mutableStateOf(false) }
    var showTagSelectorSheet by remember { mutableStateOf(false) }
    var showSettingSheet by remember { mutableStateOf(false) }
    var showImageRatioBottomSheet by remember { mutableStateOf(false) }
    var showPhotoPermissionDialog by remember { mutableStateOf(false) }
    var pendingMediaType by remember { mutableStateOf<MediaType?>(null) }

    // 크롭 대기 중인 이미지 URI
    var pendingCropUri by remember { mutableStateOf<Uri?>(null) }

    // CropImage launcher (크롭 결과 받기)
    val cropImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val cropResult = CropImage.getActivityResult(result.data)
            cropResult?.uri?.let { croppedUri ->
                viewModel.sendIntent(Intent.OnMediaSelected(listOf(croppedUri), MediaType.IMAGE))
            }
        }
        pendingCropUri = null
    }

    // 이미지 크롭 화면 열기 함수
    fun openCropImage(uri: Uri, isSquare: Boolean) {
        val intent = CropImage.activity(uri)
            .setAllowFlipping(false)
            .setAllowRotation(false)
            .setAllowCounterRotation(false)
            .setInitialCropWindowPaddingRatio(0f)
            .apply {
                if (isSquare) {
                    setAspectRatio(1, 1)
                }
            }
            .getIntent(context)
        cropImageLauncher.launch(intent)
    }

    // PhotoPicker launcher (다중 이미지 선택) - 자유 비율용
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = state.maxMediaCount - state.attachedMedia.size
        )
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.sendIntent(Intent.OnMediaSelected(uris, MediaType.IMAGE))
        }
    }

    // SinglePhotoPicker launcher (단일 이미지 선택) - 정사각형 크롭용
    val singlePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            // 정사각형이면 크롭 화면으로, 아니면 바로 첨부
            if (state.useSquareImage) {
                openCropImage(it, true)
            } else {
                viewModel.sendIntent(Intent.OnMediaSelected(listOf(it), MediaType.IMAGE))
            }
        }
    }

    // VideoPicker launcher (단일 비디오 선택)
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.sendIntent(Intent.OnMediaSelected(listOf(it), MediaType.VIDEO))
        }
    }

    // 권한 요청 launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // 권한 승인 시 PhotoPicker 열기 (정사각형 비율에 따라 분기)
            if (state.useSquareImage) {
                singlePhotoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            }
        } else {
            Toast.makeText(context, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
        }
    }

    val videoPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            videoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        } else {
            Toast.makeText(context, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
        }
    }

    // 권한이 필요한지 확인
    fun needsPhotoPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
    }

    // 사진 피커 실행 (정사각형 비율이면 단일 선택 + 크롭, 자유 비율이면 다중 선택)
    fun launchPhotoPicker() {
        if (state.useSquareImage) {
            // 정사각형: 단일 선택 후 크롭
            singlePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else {
            // 자유 비율: 다중 선택
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
    }

    // 권한 체크 함수
    fun checkAndRequestPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ : PhotoPicker 사용 (권한 불필요)
            launchPhotoPicker()
        } else {
            // Android 12 이하: READ_EXTERNAL_STORAGE 필요
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                launchPhotoPicker()
            } else {
                permissionLauncher.launch(arrayOf(permission))
            }
        }
    }

    fun checkAndRequestVideoPermission() {
        val videoRequest = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            videoPickerLauncher.launch(videoRequest)
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                videoPickerLauncher.launch(videoRequest)
            } else {
                videoPermissionLauncher.launch(arrayOf(permission))
            }
        }
    }

    // 선택적 접근권한 다이얼로그 표시 후 권한 처리
    fun showPermissionDialogAndRequest(mediaType: MediaType) {
        if (needsPhotoPermission()) {
            pendingMediaType = mediaType
            showPhotoPermissionDialog = true
        } else {
            // 권한이 필요없거나 이미 허용된 경우 바로 진행
            when (mediaType) {
                MediaType.IMAGE -> checkAndRequestPhotoPermission()
                MediaType.VIDEO -> checkAndRequestVideoPermission()
            }
        }
    }

    // 초기화
    LaunchedEffect(Unit) {
        viewModel.sendIntent(
            Intent.Initialize(
                writeType = writeType,
                idolId = idolId,
                editingArticle = null, // TODO: editingArticleId로 게시글 로드
                tagId = tagId
            )
        )
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is Effect.NavigateBack -> onNavigateBack()
                is Effect.NavigateBackWithResult -> onNavigateBackWithResult(effect.isEdited)
                is Effect.ShowBackConfirmDialog -> showBackConfirmDialog = true
                is Effect.ShowTagSelector -> showTagSelectorSheet = true
                is Effect.ShowSettingBottomSheet -> showSettingSheet = true
                is Effect.ShowImageRatioDialog -> showImageRatioBottomSheet = true
                is Effect.HideKeyboard -> {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }
                is Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_LONG).show()
                }
                is Effect.ShowUploadingNotification -> {
                    NotificationUtil.showArticleUploadingNotification(context)
                }
                is Effect.ShowSuccess -> {
                    // 글 타입에 따라 다른 네비게이션 목적지 설정
                    val navigateTo = when (effect.writeType) {
                        ArticleWriteType.FEED -> NotificationUtil.NAVIGATE_TO_COMMUNITY_FEED
                        ArticleWriteType.FAN_TALK -> NotificationUtil.NAVIGATE_TO_COMMUNITY_FAN_TALK
                        ArticleWriteType.FREE_BOARD -> NotificationUtil.NAVIGATE_TO_FREE_BOARD
                    }
                    NotificationUtil.showArticleUploadCompleteNotification(
                        context = context,
                        navigateTo = navigateTo,
                        idolId = effect.idolId,
                        tagId = effect.tagId
                    )
                }
                is Effect.RequestPhotoPermission -> {
                    showPermissionDialogAndRequest(MediaType.IMAGE)
                }
                is Effect.RequestVideoPermission -> {
                    showPermissionDialogAndRequest(MediaType.VIDEO)
                }
                is Effect.OpenPhotoPicker -> {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
                is Effect.OpenVideoPicker -> {
                    videoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                    )
                }
                is Effect.OpenImageCropper -> {
                    openCropImage(effect.uri, effect.isSquare)
                }
            }
        }
    }

    // 백버튼 처리 (취소 확인 다이얼로그가 열려있을 때는 무시)
    BackHandler(enabled = !showBackConfirmDialog) {
        viewModel.sendIntent(Intent.OnBackClick)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // 키보드가 올라올 때 전체 레이아웃이 위로 밀리도록
        topBar = {
            ArticleWriteTopBar(
                title = getTitle(state.isEditMode),
                isSubmitEnabled = state.isSubmitEnabled,
                isSaving = state.isSaving,
                onBackClick = { viewModel.sendIntent(Intent.OnBackClick) },
                onSubmitClick = { viewModel.sendIntent(Intent.OnSubmitClick) }
            )
        },
        bottomBar = {
            ArticleWriteBottomBar(
                isPhotoEnabled = state.isPhotoEnabled,
                isVideoEnabled = state.isVideoEnabled,
                showSettingButton = state.showPrivateSetting,
                onPhotoClick = { viewModel.sendIntent(Intent.OnPhotoClick) },
                onVideoClick = { viewModel.sendIntent(Intent.OnVideoClick) },
                onSettingClick = { viewModel.sendIntent(Intent.OnSettingClick) }
            )
        },
        containerColor = ColorPalette.background200,
        contentWindowInsets = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal)
    ) { paddingValues ->
        val contentFocusRequester = remember { FocusRequester() }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(ColorPalette.background200)
        ) {
            // 태그 선택 (자유게시판)
            if (state.showTagSelector) {
                TagSelectorRow(
                    tags = state.tags,
                    selectedTag = state.selectedTag,
                    onClick = { showTagSelectorSheet = true }
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ColorPalette.gray110
                )
            }

            // 제목 입력 (자유게시판, 팬톡)
            if (state.showTitleField) {
                TitleInputField(
                    title = state.title,
                    maxLength = State.MAX_TITLE_LENGTH,
                    placeholder = state.titlePlaceholder,
                    onTitleChange = { viewModel.sendIntent(Intent.OnTitleChanged(it)) }
                )
                HorizontalDivider(
                    thickness = 1.dp,
                    color = ColorPalette.gray110
                )
            }

            // 첨부 미디어 프리뷰
            if (state.attachedMedia.isNotEmpty()) {
                AttachedMediaRow(
                    media = state.attachedMedia,
                    onRemove = { index -> viewModel.sendIntent(Intent.OnMediaRemoved(index)) }
                )
            }

            // 링크 프리뷰
            state.linkPreview?.let { linkPreview ->
                LinkPreviewCard(
                    linkPreview = linkPreview,
                    isLoading = state.isLoadingLinkPreview,
                    onRemove = { viewModel.sendIntent(Intent.OnLinkPreviewRemoved) }
                )
            }

            // 본문 입력 - 남은 전체 영역 차지
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        contentFocusRequester.requestFocus()
                    }
            ) {
                ContentInputField(
                    content = state.content,
                    maxLength = State.MAX_CONTENT_LENGTH,
                    placeholder = state.contentPlaceholder,
                    onContentChange = { viewModel.sendIntent(Intent.OnContentChanged(it)) },
                    focusRequester = contentFocusRequester
                )
            }
        }
    }

    // 뒤로가기 확인 다이얼로그
    if (showBackConfirmDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.article_cancel_title),
            message = stringResource(R.string.article_cancel_msg),
            confirmButtonText = stringResource(R.string.confirm),
            dismissButtonText = stringResource(R.string.btn_cancel),
            onConfirm = {
                showBackConfirmDialog = false
                viewModel.sendIntent(Intent.OnConfirmBack)
            },
            onDismiss = { showBackConfirmDialog = false }
        )
    }

    // 태그 선택 바텀시트
    if (showTagSelectorSheet) {
        TagSelectorBottomSheet(
            tags = state.tags,
            selectedTag = state.selectedTag,
            onTagSelected = { tag ->
                viewModel.sendIntent(Intent.OnTagSelected(tag))
                showTagSelectorSheet = false
            },
            onDismiss = { showTagSelectorSheet = false }
        )
    }

    // 설정 바텀시트 (최애공개)
    if (showSettingSheet) {
        SettingBottomSheet(
            idolName = state.idolName,
            isPrivate = state.isPrivateToFavorite,
            onPrivateChanged = { isPrivate ->
                viewModel.sendIntent(Intent.OnPrivateSettingChanged(isPrivate))
            },
            onDismiss = { showSettingSheet = false }
        )
    }

    // 이미지 비율 선택 바텀시트 (old 프로젝트와 동일)
    if (showImageRatioBottomSheet) {
        ImageRatioBottomSheet(
            onSquareSelected = {
                showImageRatioBottomSheet = false
                viewModel.sendIntent(ArticleWriteContract.Intent.OnImageRatioSelected(true))
            },
            onFreeSelected = {
                showImageRatioBottomSheet = false
                viewModel.sendIntent(ArticleWriteContract.Intent.OnImageRatioSelected(false))
            },
            onDismissRequest = { showImageRatioBottomSheet = false }
        )
    }

    // 선택적 접근권한 안내 다이얼로그
    if (showPhotoPermissionDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.permission_optional),
            message = stringResource(R.string.limited_photo_permission_desc),
            confirmButtonText = stringResource(R.string.set_photo_permission),
            dismissButtonText = stringResource(R.string.btn_cancel),
            onConfirm = {
                showPhotoPermissionDialog = false
                // 권한 요청 진행
                pendingMediaType?.let { mediaType ->
                    when (mediaType) {
                        MediaType.IMAGE -> checkAndRequestPhotoPermission()
                        MediaType.VIDEO -> checkAndRequestVideoPermission()
                    }
                }
                pendingMediaType = null
            },
            onDismiss = {
                showPhotoPermissionDialog = false
                pendingMediaType = null
            }
        )
    }

    // 로딩 표시
    if (state.isLoading || state.isSaving) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = ColorPalette.main)
        }
    }
}

@Composable
private fun getTitle(isEditMode: Boolean): String {
    return stringResource(if (isEditMode) R.string.title_edit else R.string.title_write)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArticleWriteTopBar(
    title: String,
    isSubmitEnabled: Boolean,
    isSaving: Boolean,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background200)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPalette.textDefault
                )
            },
            navigationIcon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBackClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.btn_popup_close),
                        contentDescription = "닫기",
                        modifier = Modifier.size(12.dp),
                        tint = Color.Unspecified
                    )
                }
            },
            actions = {
                Text(
                    text = stringResource(R.string.article_post),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isSubmitEnabled && !isSaving) ColorPalette.main else ColorPalette.gray300,
                    modifier = Modifier
                        .clickable(
                            enabled = isSubmitEnabled && !isSaving,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSubmitClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = ColorPalette.background200
            ),
            windowInsets = WindowInsets.statusBars
        )
        // 하단 구분선
        HorizontalDivider(
            thickness = 1.dp,
            color = ColorPalette.gray110
        )
    }
}

@Composable
private fun ArticleWriteBottomBar(
    isPhotoEnabled: Boolean,
    isVideoEnabled: Boolean,
    showSettingButton: Boolean,
    onPhotoClick: () -> Unit,
    onVideoClick: () -> Unit,
    onSettingClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
            .navigationBarsPadding()
    ) {
        // 상단 구분선
        HorizontalDivider(
            thickness = 1.dp,
            color = ColorPalette.gray110
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 사진 버튼
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        enabled = isPhotoEnabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onPhotoClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (isPhotoEnabled) R.drawable.icon_input_field_photo
                        else R.drawable.icon_input_field_photo_disable
                    ),
                    contentDescription = "사진 첨부",
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
            }

            // 동영상 버튼
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        enabled = isVideoEnabled,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onVideoClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (isVideoEnabled) R.drawable.icon_input_field_video
                        else R.drawable.icon_input_field_video_disable
                    ),
                    contentDescription = "동영상 첨부",
                    modifier = Modifier.size(28.dp),
                    tint = Color.Unspecified
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 설정 버튼 (최애공개 설정)
            if (showSettingButton) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onSettingClick
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_input_field_setting),
                        contentDescription = "설정",
                        modifier = Modifier.size(28.dp),
                        tint = Color.Unspecified
                    )
                }
            }
        }
    }
}

@Composable
private fun TagSelectorRow(
    tags: List<net.ib.mn.domain.model.TagModel>,
    selectedTag: net.ib.mn.domain.model.TagModel?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(51.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(start = 20.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 카테고리 선택 텍스트 (old: tv_tag_option)
        Text(
            text = selectedTag?.name ?: stringResource(R.string.select_category_field),
            fontSize = 14.sp,
            color = ColorPalette.textDefault,
            modifier = Modifier.weight(1f)
        )

        // 화살표 아이콘
        Icon(
            painter = painterResource(R.drawable.icon_arrow_drop_down),
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = ColorPalette.textDefault
        )
    }
}

@Composable
private fun TitleInputField(
    title: String,
    maxLength: Int,
    placeholder: String? = null,
    onTitleChange: (String) -> Unit
) {
    BasicTextField(
        value = title,
        onValueChange = { if (it.length <= maxLength) onTitleChange(it) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        textStyle = TextStyle(
            fontSize = 15.sp,
            color = ColorPalette.textDefault
        ),
        cursorBrush = SolidColor(ColorPalette.textDefault),
        singleLine = true,
        decorationBox = { innerTextField ->
            Box {
                if (title.isEmpty()) {
                    Text(
                        text = placeholder ?: stringResource(R.string.enter_title),
                        fontSize = 15.sp,
                        color = ColorPalette.textDimmed
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun ContentInputField(
    content: String,
    maxLength: Int,
    placeholder: String? = null,
    onContentChange: (String) -> Unit,
    focusRequester: FocusRequester
) {
    BasicTextField(
        value = content,
        onValueChange = { if (it.length <= maxLength) onContentChange(it) },
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .focusRequester(focusRequester),
        textStyle = TextStyle(
            fontSize = 15.sp,
            color = ColorPalette.textDefault,
            lineHeight = 22.sp
        ),
        cursorBrush = SolidColor(ColorPalette.textDefault),
        decorationBox = { innerTextField ->
            Box(modifier = Modifier.fillMaxSize()) {
                if (content.isEmpty()) {
                    Text(
                        text = placeholder ?: stringResource(R.string.write_content),
                        fontSize = 15.sp,
                        color = ColorPalette.textDimmed
                    )
                }
                innerTextField()
            }
        }
    )
}

@Composable
private fun AttachedMediaRow(
    media: List<AttachedMedia>,
    onRemove: (Int) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(media) { index, item ->
            AttachedMediaItem(
                media = item,
                onRemove = { onRemove(index) }
            )
        }
    }
}

@Composable
private fun AttachedMediaItem(
    media: AttachedMedia,
    onRemove: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(6.dp))
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(media.thumbnailUri ?: media.uri)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // 비디오 표시
        if (media.type == MediaType.VIDEO) {
            Icon(
                painter = painterResource(R.drawable.btn_media_play),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .align(Alignment.Center),
                tint = Color.White
            )
        }

        // 삭제 버튼
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(22.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onRemove
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.btn_media_del_nor),
                contentDescription = "삭제",
                modifier = Modifier.size(22.dp),
                tint = Color.Unspecified
            )
        }
    }
}

@Composable
private fun LinkPreviewCard(
    linkPreview: LinkPreviewData,
    isLoading: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = ColorPalette.gray80)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 썸네일
            if (linkPreview.imageUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(linkPreview.imageUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    contentScale = ContentScale.Crop
                )
            } else if (isLoading) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(ColorPalette.gray100, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = ColorPalette.main,
                        strokeWidth = 2.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // 텍스트 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                linkPreview.title?.let { title ->
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = ColorPalette.textDefault,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                linkPreview.description?.let { desc ->
                    Text(
                        text = desc,
                        fontSize = 12.sp,
                        color = ColorPalette.textDimmed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                linkPreview.host?.let { host ->
                    Text(
                        text = host,
                        fontSize = 11.sp,
                        color = ColorPalette.textGray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 삭제 버튼
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onRemove
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_media_del_nor),
                    contentDescription = "삭제",
                    modifier = Modifier.size(22.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

/**
 * 태그 선택 바텀시트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSelectorBottomSheet(
    tags: List<net.ib.mn.domain.model.TagModel>,
    selectedTag: net.ib.mn.domain.model.TagModel?,
    onTagSelected: (net.ib.mn.domain.model.TagModel) -> Unit,
    onDismiss: () -> Unit
) {
    ExoBottomSheet(
        onDismissRequest = onDismiss,
        type = ExoBottomSheetType.LIST,
        containerColor = ColorPalette.background200,
        title = stringResource(R.string.select_category_field)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            tags.forEach { tag ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagSelected(tag) }
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = tag.name,
                        fontSize = 15.sp,
                        color = ColorPalette.textDefault
                    )
                }
            }
        }
    }
}

/**
 * 설정 바텀시트 (최애공개)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingBottomSheet(
    idolName: String,
    isPrivate: Boolean,
    onPrivateChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ExoBottomSheet(
        onDismissRequest = onDismiss,
        type = ExoBottomSheetType.LIST,
        containerColor = ColorPalette.background200,
        title = stringResource(R.string.article_setting)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            // 최애만 공개 Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(51.dp)
                    .clickable { onPrivateChanged(!isPrivate) }
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 텍스트 (왼쪽)
                Text(
                    text = stringResource(R.string.lable_show_private),
                    fontSize = 15.sp,
                    color = ColorPalette.textDefault,
                    modifier = Modifier.weight(1f)
                )

                // 토글 스위치 (오른쪽)
                Switch(
                    checked = isPrivate,
                    onCheckedChange = { onPrivateChanged(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = ColorPalette.main,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = ColorPalette.gray300,
                        uncheckedBorderColor = Color.Transparent,
                        checkedBorderColor = Color.Transparent
                    )
                )
            }
        }
    }
}

/**
 * 이미지 비율 선택 바텀시트
 * old 프로젝트의 bottom_sheet_photo_ratio.xml과 동일한 UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageRatioBottomSheet(
    onSquareSelected: () -> Unit,
    onFreeSelected: () -> Unit,
    onDismissRequest: () -> Unit
) {
    ExoBottomSheet(
        onDismissRequest = onDismissRequest,
        type = ExoBottomSheetType.LIST,
        title = stringResource(R.string.label_image_option)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // 정사각형 옵션 (old: "정사각형(프로필&이붙)") - 좌측 정렬
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onSquareSelected() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.label_image_option_square),
                    style = ExoTypo.body15.copy(fontWeight = FontWeight.Bold),
                    color = ColorPalette.gray900
                )
            }

            // Free / 원본 옵션 - 좌측 정렬
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable { onFreeSelected() }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.label_image_option_free),
                    style = ExoTypo.body15.copy(fontWeight = FontWeight.Bold),
                    color = ColorPalette.gray900
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

