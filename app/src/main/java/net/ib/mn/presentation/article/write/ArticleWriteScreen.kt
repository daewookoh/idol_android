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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.presentation.article.write.ArticleWriteContract.*
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.theme.ColorPalette
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
    var showImageRatioDialog by remember { mutableStateOf(false) }

    // PhotoPicker launcher (다중 이미지 선택)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = state.maxMediaCount - state.attachedMedia.size
        )
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.sendIntent(Intent.OnMediaSelected(uris, MediaType.IMAGE))
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
            // 권한 승인 시 PhotoPicker 열기
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else { Toast.makeText(context, R.string.image_permission_error, Toast.LENGTH_SHORT).show()
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

    // 권한 체크 함수
    fun checkAndRequestPhotoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ : PhotoPicker 사용 (권한 불필요)
            photoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10-12 : READ_EXTERNAL_STORAGE
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                permissionLauncher.launch(arrayOf(permission))
            }
        } else {
            // Android 9 이하
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                photoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            } else {
                permissionLauncher.launch(arrayOf(permission))
            }
        }
    }

    fun checkAndRequestVideoPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            videoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            )
        } else {
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                videoPickerLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                )
            } else {
                videoPermissionLauncher.launch(arrayOf(permission))
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
                is Effect.ShowImageRatioDialog -> showImageRatioDialog = true
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
                    checkAndRequestPhotoPermission()
                }
                is Effect.RequestVideoPermission -> {
                    checkAndRequestVideoPermission()
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
            }
        }
    }

    // 백버튼 처리
    BackHandler {
        viewModel.sendIntent(Intent.OnBackClick)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .imePadding(), // 키보드가 올라올 때 전체 레이아웃이 위로 밀리도록
        topBar = {
            ArticleWriteTopBar(
                title = getTitle(state.writeType, state.isEditMode),
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
                onNavigateBack()
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

    // 이미지 비율 선택 다이얼로그
    if (showImageRatioDialog) {
        ImageRatioDialog(
            onSquareSelected = {
                viewModel.sendIntent(Intent.OnImageRatioSelected(true))
                showImageRatioDialog = false
            },
            onFreeRatioSelected = {
                viewModel.sendIntent(Intent.OnImageRatioSelected(false))
                showImageRatioDialog = false
            },
            onDismiss = { showImageRatioDialog = false }
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
private fun getTitle(writeType: ArticleWriteType, isEditMode: Boolean): String {
    return if (isEditMode) {
        stringResource(R.string.title_edit)
    } else {
        when (writeType) {
            ArticleWriteType.FEED -> stringResource(R.string.title_write)
            ArticleWriteType.FREE_BOARD -> stringResource(R.string.title_write)
            ArticleWriteType.FAN_TALK -> stringResource(R.string.title_write)
        }
    }
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
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.schedule_category),
            fontSize = 14.sp,
            color = ColorPalette.textGray
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = selectedTag?.name ?: stringResource(R.string.label_button_select),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = if (selectedTag != null) ColorPalette.textDefault else ColorPalette.textDimmed
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = ColorPalette.gray300
        )
    }
}

@Composable
private fun TitleInputField(
    title: String,
    maxLength: Int,
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
                        text = stringResource(R.string.enter_title),
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
                        text = stringResource(R.string.write_content),
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
                painter = painterResource(R.drawable.btn_media_del),
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
                    painter = painterResource(R.drawable.btn_media_del),
                    contentDescription = "삭제",
                    modifier = Modifier.size(22.dp),
                    tint = Color.Unspecified
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagSelectorBottomSheet(
    tags: List<net.ib.mn.domain.model.TagModel>,
    selectedTag: net.ib.mn.domain.model.TagModel?,
    onTagSelected: (net.ib.mn.domain.model.TagModel) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorPalette.background200
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_category_field),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPalette.textDefault,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            tags.forEach { tag ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTagSelected(tag) }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tag.name,
                        fontSize = 15.sp,
                        color = if (tag.id == selectedTag?.id) ColorPalette.main else ColorPalette.textDefault
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    if (tag.id == selectedTag?.id) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = ColorPalette.main
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingBottomSheet(
    idolName: String,
    isPrivate: Boolean,
    onPrivateChanged: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = ColorPalette.background200
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.article_setting),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ColorPalette.textDefault,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPrivateChanged(!isPrivate) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = isPrivate,
                    onCheckedChange = { onPrivateChanged(it) },
                    colors = CheckboxDefaults.colors(
                        checkedColor = ColorPalette.main,
                        uncheckedColor = ColorPalette.gray300
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.lable_show_private),
                    fontSize = 14.sp,
                    color = ColorPalette.textDefault
                )
            }
        }
    }
}

@Composable
private fun ImageRatioDialog(
    onSquareSelected: () -> Unit,
    onFreeRatioSelected: () -> Unit,
    onDismiss: () -> Unit
) {
    ExoConfirmDialog(
        title = stringResource(R.string.label_image_option_title),
        message = stringResource(R.string.label_image_option),
        confirmButtonText = "1:1",
        dismissButtonText = stringResource(R.string.label_image_option_free),
        onConfirm = onSquareSelected,
        onDismiss = {
            onFreeRatioSelected()
        }
    )
}
