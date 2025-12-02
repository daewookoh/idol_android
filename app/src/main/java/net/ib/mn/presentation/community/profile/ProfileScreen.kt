package net.ib.mn.presentation.community.profile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.presentation.community.profile.subpage.ProfileCommentPage
import net.ib.mn.presentation.community.profile.subpage.ProfilePhotoPage
import net.ib.mn.presentation.community.profile.subpage.ProfilePostPage
import net.ib.mn.ui.components.ExoBottomSheetAction
import net.ib.mn.ui.components.ExoBottomSheetActionItem
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoErrorDialog
import net.ib.mn.ui.components.ExoNameWithGroupColor
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ColorPalette

/**
 * ProfileScreen - 유저 프로필 화면
 *
 * Old 프로젝트의 FeedActivity를 참고하여 Compose로 구현
 * 특정 유저의 프로필, 피드 사진, 활동, 댓글을 표시
 *
 * @param userId 유저 ID
 * @param userNickname 유저 닉네임
 * @param userImageUrl 유저 프로필 이미지 URL
 * @param userLevel 유저 레벨
 * @param mostIdolName 최애 아이돌 이름
 * @param isMine 본인 프로필 여부 (true면 댓글 탭 표시)
 * @param onBackClick 뒤로가기 클릭 이벤트
 */
@Composable
fun ProfileScreen(
    userId: Int,
    userNickname: String,
    userImageUrl: String? = null,
    userLevel: Int = 0,
    mostIdolName: String? = null,
    isMine: Boolean = false,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel 생성 - hiltViewModel with SavedStateHandle
    val viewModel: ProfileViewModel = hiltViewModel(
        key = "user_profile_$userId",
        creationCallback = { factory: ProfileViewModel.Factory ->
            factory.create(
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        "userId" to userId,
                        "userNickname" to userNickname,
                        "userImageUrl" to userImageUrl,
                        "userLevel" to userLevel,
                        "mostIdolName" to mostIdolName,
                        "isMine" to isMine
                    )
                )
            )
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val reportState by viewModel.reportState.collectAsState()
    val friendState by viewModel.friendState.collectAsState()
    val blockState by viewModel.blockState.collectAsState()

    // 신고 플로우 상태
    var showBottomSheet by remember { mutableStateOf(false) }
    var showHeartConfirmDialog by remember { mutableStateOf(false) }
    var showReportReasonDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf("") }
    var currentReportHeart by remember { mutableIntStateOf(0) }

    // 신고 상태 처리
    LaunchedEffect(reportState) {
        when (val state = reportState) {
            is ReportState.ShowBottomSheet -> {
                showBottomSheet = true
                viewModel.resetReportState()
            }
            is ReportState.ShowHeartConfirmDialog -> {
                currentReportHeart = state.reportHeart
                showHeartConfirmDialog = true
                viewModel.resetReportState()
            }
            is ReportState.ShowReportReasonDialog -> {
                showReportReasonDialog = true
                viewModel.resetReportState()
            }
            is ReportState.Success -> {
                android.widget.Toast.makeText(
                    context,
                    context.getString(R.string.report_done),
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                viewModel.resetReportState()
            }
            is ReportState.Error -> {
                // Old: ErrorControl.parseError 참고 - gcode 기반 에러 메시지
                errorDialogMessage = if (state.message != null) {
                    state.message
                } else {
                    when (state.gcode) {
                        2200, 2300 -> context.getString(R.string.error_2200)
                        2201 -> context.getString(R.string.failed_to_report__already_reported)
                        2301 -> context.getString(R.string.failed_to_report_user__already_reported)
                        2202, 2302 -> context.getString(R.string.failed_to_report_2202)
                        2203, 2303 -> context.getString(R.string.failed_to_report_2203)
                        2204, 2304 -> context.getString(R.string.not_enough_heart)
                        else -> context.getString(R.string.failed_to_report)
                    }
                }
                showErrorDialog = true
                viewModel.resetReportState()
            }
            else -> {}
        }
    }

    // 차단 상태 처리
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showUnblockConfirmDialog by remember { mutableStateOf(false) }
    var showBlockErrorDialog by remember { mutableStateOf(false) }
    var blockErrorMessage by remember { mutableStateOf("") }

    LaunchedEffect(blockState) {
        when (val state = blockState) {
            is BlockState.ShowConfirmDialog -> {
                showBlockConfirmDialog = true
                viewModel.resetBlockState()
            }
            is BlockState.ShowUnblockConfirmDialog -> {
                showUnblockConfirmDialog = true
                viewModel.resetBlockState()
            }
            is BlockState.Success -> {
                viewModel.resetBlockState()
            }
            is BlockState.UnblockSuccess -> {
                viewModel.resetBlockState()
            }
            is BlockState.Error -> {
                blockErrorMessage = state.message ?: context.getString(R.string.error_abnormal_default)
                showBlockErrorDialog = true
                viewModel.resetBlockState()
            }
            else -> {}
        }
    }

    // 친구 상태 처리
    var showFriendAlreadyRequestedDialog by remember { mutableStateOf(false) }
    var showFriendAlreadyFriendDialog by remember { mutableStateOf(false) }
    var showFriendErrorDialog by remember { mutableStateOf(false) }
    var friendErrorMessage by remember { mutableStateOf("") }

    LaunchedEffect(friendState) {
        when (val state = friendState) {
            is FriendState.ShowAlreadyRequestedDialog -> {
                showFriendAlreadyRequestedDialog = true
            }
            is FriendState.ShowAlreadyFriendDialog -> {
                showFriendAlreadyFriendDialog = true
            }
            is FriendState.Error -> {
                // Old: ErrorControl.parseError 참고 - gcode 기반 에러 메시지
                friendErrorMessage = when (state.gcode) {
                    8000 -> context.getString(R.string.error_8000) // 친구 수 제한
                    8001 -> context.getString(R.string.error_8001) // 이미 친구
                    8002 -> context.getString(R.string.error_8002) // 이미 요청 보냄
                    else -> state.message ?: context.getString(R.string.error_abnormal_default)
                }
                showFriendErrorDialog = true
            }
            else -> {}
        }
    }

    // 탭 목록 (본인 프로필이면 댓글 탭 포함)
    val tabs = remember(isMine) {
        buildList {
            add(ProfileTab.PHOTO)
            add(ProfileTab.ACTIVITY)
            if (isMine) {
                add(ProfileTab.COMMENT)
            }
        }
    }

    val pagerState = rememberPagerState(pageCount = { tabs.size })

    // 탭 아이콘 리소스 (Old 프로젝트와 동일)
    val tabOnIcons = remember(isMine) {
        buildList {
            add(R.drawable.icon_feed_photo_on)
            add(R.drawable.icon_feed_activity_on)
            if (isMine) {
                add(R.drawable.icon_feed_comment_on)
            }
        }
    }

    val tabOffIcons = remember(isMine) {
        buildList {
            add(R.drawable.icon_feed_photo_off)
            add(R.drawable.icon_feed_activity_off)
            if (isMine) {
                add(R.drawable.icon_feed_comment_off)
            }
        }
    }

    BackHandler {
        onBackClick()
    }

    ExoScaffold(
        useFullScreen = true,
        topBar = {
            // 상단 앱바 (status bar 패딩 포함)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ColorPalette.background100)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 뒤로가기 버튼
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onBackClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_navigation_back),
                            contentDescription = "Back",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // 타이틀 - 본인이면 "My Feed", 아니면 "Profile" (볼드)
                    Text(
                        text = if (isMine) {
                            stringResource(R.string.feed_my_feed)
                        } else {
                            stringResource(R.string.title_profile)
                        },
                        fontSize = 18.sp,
                        lineHeight = 18.sp,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        color = ColorPalette.textDefault,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // 타인의 프로필일 때 우측 액션 버튼들
                    if (!isMine) {
                        // 신고 버튼
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    viewModel.onReportClick()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.btn_navigation_report),
                                contentDescription = "Report",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // 친구 버튼 - 상태에 따라 다른 아이콘과 동작
                        FriendButton(
                            friendState = friendState,
                            onFriendAddClick = { viewModel.onFriendAddClick() },
                            onFriendWaitClick = { viewModel.onFriendWaitClick() },
                            onAlreadyFriendClick = { viewModel.onAlreadyFriendClick() }
                        )
                    }
                }
            }
        }
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }

            is ProfileUiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.message,
                        color = ColorPalette.textGray
                    )
                }
            }

            is ProfileUiState.Success -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .background(ColorPalette.background100)
                ) {
                    // 프로필 헤더
                    ProfileHeader(
                        nickname = state.user.nickname,
                        imageUrl = state.user.imageUrl,
                        level = state.user.level,
                        idolName = state.user.idolName,
                        statusMessage = state.user.statusMessage,
                        isMine = isMine,
                        onNicknameEditClick = {
                            // TODO: 닉네임 수정
                        },
                        onIdolEditClick = {
                            // TODO: 최애 아이돌 수정
                        },
                        onViewMoreClick = {
                            // TODO: 상태 메시지 전체 보기 다이얼로그
                        }
                    )

                    // 아이콘 탭 레이아웃 (Old 스타일)
                    ProfileIconTabRow(
                        tabs = tabs,
                        tabOnIcons = tabOnIcons,
                        tabOffIcons = tabOffIcons,
                        selectedTabIndex = pagerState.currentPage,
                        onTabSelected = { index ->
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        }
                    )

                    // 탭 컨텐츠
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize(),
                        userScrollEnabled = false
                    ) { page ->
                        when (tabs[page]) {
                            ProfileTab.PHOTO -> ProfilePhotoPage(
                                userId = userId,
                                isMine = isMine,
                                isFeedPrivate = state.user.isFeedPrivate,
                                isBlocked = state.user.isBlocked,
                                blockStatusChecked = state.user.blockStatusChecked
                            )
                            ProfileTab.ACTIVITY -> ProfilePostPage(
                                userId = userId,
                                isMine = isMine,
                                isFeedPrivate = state.user.isFeedPrivate,
                                isBlocked = state.user.isBlocked,
                                blockStatusChecked = state.user.blockStatusChecked
                            )
                            ProfileTab.COMMENT -> ProfileCommentPage(userId = userId)
                        }
                    }
                }
            }
        }
    }

    // 1단계: 신고/차단 바텀시트 (Old: bottom_sheet_feed_report.xml)
    if (showBottomSheet) {
        // 현재 차단 상태 확인
        val currentIsBlocked = (uiState as? ProfileUiState.Success)?.user?.isBlocked ?: false

        ExoBottomSheetAction(
            items = listOf(
                ExoBottomSheetActionItem(R.string.report) {
                    viewModel.onReportSelected()
                },
                if (currentIsBlocked) {
                    ExoBottomSheetActionItem(R.string.unblock) {
                        viewModel.onUnblockClick()
                    }
                } else {
                    ExoBottomSheetActionItem(R.string.block) {
                        viewModel.onBlockClick()
                    }
                }
            ),
            onDismissRequest = { showBottomSheet = false }
        )
    }

    // 2단계: 하트 결제 확인 다이얼로그 (Old: dialog_report.xml - ReportFeedDialogFragment)
    if (showHeartConfirmDialog) {
        HeartConfirmDialog(
            reportHeart = currentReportHeart,
            onDismiss = { showHeartConfirmDialog = false },
            onConfirm = {
                showHeartConfirmDialog = false
                viewModel.onHeartConfirmAccepted()
            }
        )
    }

    // 3단계: 신고 사유 입력 다이얼로그 (Old: dialog_default_chat_report_two_btn.xml - ReportReasonDialogFragment)
    if (showReportReasonDialog) {
        ReportReasonDialog(
            onDismiss = { showReportReasonDialog = false },
            onConfirm = { reason ->
                showReportReasonDialog = false
                viewModel.submitReport(reason)
            }
        )
    }

    // 에러 다이얼로그 (Old: Util.showDefaultIdolDialogWithBtn1)
    if (showErrorDialog) {
        ExoErrorDialog(
            message = errorDialogMessage,
            onDismiss = { showErrorDialog = false }
        )
    }

    // 친구 요청 이미 보냄 다이얼로그 (Old: error_8002)
    if (showFriendAlreadyRequestedDialog) {
        ExoErrorDialog(
            message = stringResource(R.string.error_8002),
            onDismiss = {
                showFriendAlreadyRequestedDialog = false
                viewModel.resetFriendState()
            }
        )
    }

    // 친구 에러 다이얼로그
    if (showFriendErrorDialog) {
        ExoErrorDialog(
            message = friendErrorMessage,
            onDismiss = {
                showFriendErrorDialog = false
                viewModel.resetFriendState()
            }
        )
    }

    // 이미 친구 다이얼로그 (Old: error_8003)
    if (showFriendAlreadyFriendDialog) {
        ExoErrorDialog(
            message = stringResource(R.string.error_8003),
            onDismiss = {
                showFriendAlreadyFriendDialog = false
                viewModel.resetFriendState()
            }
        )
    }

    // 차단 확인 다이얼로그
    if (showBlockConfirmDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.block),
            message = stringResource(R.string.block_question),
            onConfirm = {
                showBlockConfirmDialog = false
                viewModel.onBlockConfirmed()
            },
            onDismiss = { showBlockConfirmDialog = false },
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no)
        )
    }

    // 차단 해제 확인 다이얼로그
    if (showUnblockConfirmDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.unblock),
            message = stringResource(R.string.block_question),
            onConfirm = {
                showUnblockConfirmDialog = false
                viewModel.onUnblockConfirmed()
            },
            onDismiss = { showUnblockConfirmDialog = false },
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no)
        )
    }

    // 차단 에러 다이얼로그
    if (showBlockErrorDialog) {
        ExoErrorDialog(
            message = blockErrorMessage,
            onDismiss = { showBlockErrorDialog = false }
        )
    }
}


/**
 * 2단계: 하트 결제 확인 다이얼로그 (Old: dialog_report.xml - ReportFeedDialogFragment)
 * ExoConfirmDialog 사용
 */
@Composable
private fun HeartConfirmDialog(
    reportHeart: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val title = stringResource(R.string.title_report)

    if (reportHeart > 0) {
        // 하트 차감 메시지 - 하트 숫자는 main 색상 (Old: msg_report_user_confirm with HTML color)
        val messageTemplate = stringResource(R.string.msg_report_user_confirm)
        // %1$s를 하트 숫자로 대체하고, 해당 부분만 main 색상 적용
        val parts = messageTemplate.split("%1\$s")
        val annotatedMessage = buildAnnotatedString {
            if (parts.isNotEmpty()) {
                append(parts[0])
            }
            withStyle(style = androidx.compose.ui.text.SpanStyle(color = ColorPalette.main)) {
                append(reportHeart.toString())
            }
            if (parts.size > 1) {
                append(parts[1])
            }
        }

        ExoConfirmDialog(
            title = title,
            message = annotatedMessage,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no)
        )
    } else {
        // 무료 신고 메시지 (Old: report_user_desc)
        ExoConfirmDialog(
            title = title,
            message = stringResource(R.string.report_user_desc),
            onConfirm = onConfirm,
            onDismiss = onDismiss,
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no)
        )
    }
}

/**
 * 3단계: 신고 사유 입력 다이얼로그 (Old: dialog_default_chat_report_two_btn.xml - ReportReasonDialogFragment)
 */
@Composable
private fun ReportReasonDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = { /* 바깥 클릭 시 닫히지 않도록 (Old: isCancelable = false) */ },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Column(
            modifier = Modifier
                .width(290.dp)
                .background(
                    color = colorResource(id = R.color.text_white_black),
                    shape = RoundedCornerShape(6.dp)
                )
                .border(
                    width = 1.dp,
                    color = colorResource(id = R.color.gray150),
                    shape = RoundedCornerShape(6.dp)
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 타이틀: 신고한 이유를 작성해 주세요
            Text(
                text = stringResource(R.string.quiz_report_description),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(id = R.color.text_default),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 10.dp)
            )

            // 입력 필드
            OutlinedTextField(
                value = reason,
                onValueChange = {
                    if (it.length <= 2000) {
                        reason = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .heightIn(min = 60.dp, max = 180.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.quiz_report_hint),
                        fontSize = 13.sp,
                        color = colorResource(id = R.color.gray200)
                    )
                },
                textStyle = TextStyle(
                    fontSize = 13.sp,
                    color = colorResource(id = R.color.text_gray)
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colorResource(id = R.color.gray100),
                    unfocusedBorderColor = colorResource(id = R.color.gray100),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                ),
                shape = RoundedCornerShape(4.dp)
            )

            // 구분선
            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                thickness = 1.dp,
                color = colorResource(id = R.color.gray100)
            )

            // 버튼 Row (확인 왼쪽 | 취소 오른쪽)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                // 확인 버튼 (왼쪽)
                TextButton(
                    onClick = {
                        if (reason.length < 10) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.comment_minimum_characters, 10),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            onConfirm(reason)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomStart = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.confirm),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // 버튼 사이 구분선
                VerticalDivider(
                    modifier = Modifier.fillMaxHeight(),
                    thickness = 1.dp,
                    color = colorResource(id = R.color.gray100)
                )

                // 취소 버튼 (오른쪽)
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(bottomEnd = 6.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = colorResource(id = R.color.gray580)
                    )
                ) {
                    Text(
                        text = stringResource(R.string.btn_cancel),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

/**
 * ProfileTab - 프로필 탭 타입
 */
enum class ProfileTab {
    PHOTO,
    ACTIVITY,
    COMMENT
}

/**
 * ProfileHeader - 유저 프로필 헤더
 *
 * Old: activity_feed.xml의 cl_myinfo 영역
 */
@Composable
private fun ProfileHeader(
    nickname: String,
    imageUrl: String?,
    level: Int,
    idolName: String?,
    statusMessage: String?,
    isMine: Boolean = false,
    onNicknameEditClick: () -> Unit = {},
    onIdolEditClick: () -> Unit = {},
    onViewMoreClick: () -> Unit = {}
) {
    val context = LocalContext.current

    // 상태 메시지 펼침 여부
    var isStatusExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 38.dp, end = 38.dp, top = 30.dp, bottom = 18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top
        ) {
            // 프로필 이미지 (80dp x 80dp)
            ExoProfileImage(
                imageUrl = imageUrl,
                type = ProfileImageType.LARGE,
                rank = -1,  // 랭킹 표시 안함
                contentDescription = nickname
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 레벨 아이콘 + 텍스트(닉네임/최애) Row 구조
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 18.dp),
                verticalAlignment = Alignment.Top
            ) {
                // 레벨 아이콘
                val levelIconRes = getLevelIconRes(context, level)
                Icon(
                    painter = painterResource(levelIconRes),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier
                        .padding(top = 2.dp, end = 6.dp)
                        .height(15.dp)
                )

                // 닉네임 + 최애 아이돌 Column
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 닉네임 (볼드) + 설정 아이콘 (본인일 때)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = nickname,
                            fontSize = 16.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = ColorPalette.textDefault,
                            style = TextStyle(
                                platformStyle = PlatformTextStyle(includeFontPadding = false)
                            ),
                            modifier = Modifier.weight(1f, fill = false)
                        )

                        // 본인 프로필일 때 닉네임 수정 아이콘
                        if (isMine) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onNicknameEditClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icon_community_setting),
                                    contentDescription = "Edit nickname",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // 최애 아이돌 이름 + 설정 아이콘 (본인일 때)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!idolName.isNullOrEmpty()) {
                            ExoNameWithGroupColor(
                                fullName = idolName,
                                nameFontSize = 14.sp,
                                groupFontSize = 10.sp,
                                nameColor = ColorPalette.textDefault,
                                groupColor = ColorPalette.textDimmed,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        } else {
                            // 아이돌 이름이 없을 때 힌트 표시
                            Text(
                                text = stringResource(R.string.none),
                                fontSize = 14.sp,
                                color = ColorPalette.textDimmed,
                                style = TextStyle(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                                ),
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        // 본인 프로필일 때 아이돌 수정 아이콘
                        if (isMine) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Box(
                                modifier = Modifier
                                    .size(19.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onIdolEditClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.icon_community_setting),
                                    contentDescription = "Edit idol",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(11.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 상태 메시지 - Old 프로젝트 FeedActivity와 동일하게 구현
        // 1. 비어있으면: 본인이면 힌트 표시, 타인이면 숨김
        // 2. 내용 있으면: 한 줄에 안 들어가면 "... view more", 한 줄이면 그대로
        // 3. 펼치면 링크 클릭 가능
        // "null" 문자열도 빈 값으로 처리
        val hasStatusMessage = !statusMessage.isNullOrEmpty() && statusMessage != "null"

        // 본인 프로필이고 상태 메시지가 없을 때 힌트 표시
        if (isMine && !hasStatusMessage) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = stringResource(R.string.feed_status_message_hint),
                fontSize = 12.sp,
                color = ColorPalette.textDimmed,
                maxLines = 1,
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onViewMoreClick() },
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }

        // 상태 메시지가 있을 때
        if (hasStatusMessage) {
            Spacer(modifier = Modifier.height(18.dp))

            if (isStatusExpanded) {
                // 펼쳐진 상태 - 전체 메시지 표시, 링크 클릭 가능
                ClickableStatusMessage(
                    statusMessage = statusMessage!!,
                    context = context,
                    isMine = isMine,
                    onEditClick = onViewMoreClick
                )
            } else {
                // 접힌 상태 - 한 줄 + "... view more" (Old: showViewMore)
                // Old 프로젝트: 첫 줄만 표시하고 ... view more 추가
                StatusMessageCollapsed(
                    statusMessage = statusMessage!!,
                    isMine = isMine,
                    onExpandClick = { isStatusExpanded = true },
                    onEditClick = onViewMoreClick
                )
            }
        }
    }
}

/**
 * StatusMessageCollapsed - 접힌 상태 메시지 (한 줄 + "... view more")
 * Old 프로젝트의 showViewMore 함수와 동일한 동작
 */
@Composable
private fun StatusMessageCollapsed(
    statusMessage: String,
    isMine: Boolean,
    onExpandClick: () -> Unit,
    onEditClick: () -> Unit
) {
    // "... view more" 텍스트
    val viewMoreText = "... " + stringResource(R.string.view_more)

    // 첫 번째 줄만 추출 (줄바꿈 기준)
    val firstLine = statusMessage.lineSequence().firstOrNull() ?: statusMessage

    // Old 프로젝트와 동일: 한 줄에 맞으면 텍스트만, 넘치면 "... view more" 추가
    // 두 번의 layout을 피하기 위해 InlineContent 대신 TextMeasurer 사용
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 실제 화면 너비 (dp → px 변환)
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxWidthPx = with(density) { maxWidth.toPx() }

        // TextMeasurer로 텍스트 너비 측정
        val textMeasurer = androidx.compose.ui.text.rememberTextMeasurer()

        // "... view more" 너비 측정
        val viewMoreWidthPx = remember(viewMoreText) {
            textMeasurer.measure(
                text = viewMoreText,
                style = TextStyle(fontSize = 12.sp)
            ).size.width
        }

        // 첫 줄 텍스트 너비 측정
        val textWidthPx = remember(firstLine) {
            textMeasurer.measure(
                text = firstLine,
                style = TextStyle(fontSize = 12.sp)
            ).size.width
        }

        // 텍스트가 넘치는지 확인 (첫 줄만 가지고 판단하거나 여러 줄인 경우)
        val needsViewMore = textWidthPx > (maxWidthPx - viewMoreWidthPx) ||
                statusMessage.contains('\n')

        if (needsViewMore) {
            // 텍스트가 넘치거나 여러 줄인 경우: 텍스트 잘라서 + "... view more"
            Row(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onExpandClick() }
            ) {
                Text(
                    text = firstLine,
                    fontSize = 12.sp,
                    color = ColorPalette.textDefault,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Clip,
                    modifier = Modifier.widthIn(max = with(density) { (maxWidthPx - viewMoreWidthPx).toInt().toDp() }),
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
                Text(
                    text = viewMoreText,
                    fontSize = 12.sp,
                    color = ColorPalette.textDimmed,
                    maxLines = 1,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        } else {
            // 텍스트가 한 줄에 들어가는 경우: 텍스트만 표시
            Text(
                text = firstLine,
                fontSize = 12.sp,
                color = ColorPalette.textDefault,
                maxLines = 1,
                modifier = if (isMine) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onEditClick() }
                } else {
                    Modifier
                },
                style = TextStyle(
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                )
            )
        }
    }
}

/**
 * ClickableStatusMessage - 링크 클릭 가능한 상태 메시지
 */
@Composable
private fun ClickableStatusMessage(
    statusMessage: String,
    context: android.content.Context,
    isMine: Boolean,
    onEditClick: () -> Unit
) {
    // URL 패턴 정규식
    val urlPattern = remember {
        android.util.Patterns.WEB_URL.toRegex()
    }

    // 텍스트에서 URL 추출
    val urlMatches = remember(statusMessage) {
        urlPattern.findAll(statusMessage).toList()
    }

    if (urlMatches.isEmpty()) {
        // 링크 없음 - 일반 텍스트
        Text(
            text = statusMessage,
            fontSize = 12.sp,
            color = ColorPalette.textDefault,
            modifier = if (isMine) {
                Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onEditClick() }
            } else {
                Modifier
            },
            style = TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    } else {
        // 링크 있음 - AnnotatedString 사용
        // 링크 색상: 푸른색 계열 (Old 프로젝트와 동일)
        val linkColor = Color(0xFF1E88E5)  // Material Blue 600
        val annotatedString = remember(statusMessage, linkColor) {
            buildAnnotatedString {
                var lastIndex = 0
                urlMatches.forEach { matchResult ->
                    // URL 이전 텍스트
                    append(statusMessage.substring(lastIndex, matchResult.range.first))

                    // URL 텍스트 (클릭 가능하게)
                    pushStringAnnotation(tag = "URL", annotation = matchResult.value)
                    withStyle(
                        style = androidx.compose.ui.text.SpanStyle(
                            color = linkColor,
                            textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                        )
                    ) {
                        append(matchResult.value)
                    }
                    pop()

                    lastIndex = matchResult.range.last + 1
                }
                // 마지막 URL 이후 텍스트
                if (lastIndex < statusMessage.length) {
                    append(statusMessage.substring(lastIndex))
                }
            }
        }

        androidx.compose.foundation.text.ClickableText(
            text = annotatedString,
            onClick = { offset ->
                annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                    .firstOrNull()?.let { annotation ->
                        // URL 열기
                        var url = annotation.item
                        if (!url.startsWith("http://") && !url.startsWith("https://")) {
                            url = "https://$url"
                        }
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
            },
            style = TextStyle(
                fontSize = 12.sp,
                color = ColorPalette.textDefault,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            )
        )
    }
}

/**
 * ProfileIconTabRow - 아이콘 기반 탭 레이아웃 (Old 스타일)
 * - 위쪽 라인 없음
 * - 아래 인디케이터 표시 (선택된 탭)
 * - 아이콘 크기 작게 (18dp)
 */
@Composable
private fun ProfileIconTabRow(
    tabs: List<ProfileTab>,
    tabOnIcons: List<Int>,
    tabOffIcons: List<Int>,
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(ColorPalette.background100)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onTabSelected(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(
                                    if (selectedTabIndex == index) tabOnIcons[index] else tabOffIcons[index]
                                ),
                                contentDescription = null,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // 선택된 탭 아래 인디케이터
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
            ) {
                tabs.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(2.dp)
                            .background(
                                if (selectedTabIndex == index) ColorPalette.main else Color.Transparent
                            )
                    )
                }
            }
        }

        // 하단 구분선
        HorizontalDivider(
            thickness = 1.dp,
            color = ColorPalette.gray100
        )
    }
}

/**
 * 레벨에 따른 아이콘 리소스 반환
 */
private fun getLevelIconRes(context: android.content.Context, level: Int): Int {
    val maxLevel = 40
    val clampedLevel = level.coerceIn(0, maxLevel)
    val resName = String.format(java.util.Locale.ENGLISH, "icon_level_%d", clampedLevel)
    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
    return if (resId != 0) resId else R.drawable.icon_level_0
}

/**
 * FriendButton - 친구 상태에 따른 버튼 표시
 *
 * Old 프로젝트의 FeedActivity 친구 버튼 로직과 동일:
 * - CanAdd: btn_navigation_friend_add (클릭 시 친구 요청)
 * - AlreadyFriend: btn_navigation_friend_already (클릭 시 이미 친구 다이얼로그)
 * - RequestPending/RequestSent: btn_navigation_friend_waiting (클릭 시 이미 요청 보냄 다이얼로그)
 * - Loading: 로딩 중 (버튼 비활성화)
 */
@Composable
private fun FriendButton(
    friendState: FriendState,
    onFriendAddClick: () -> Unit,
    onFriendWaitClick: () -> Unit,
    onAlreadyFriendClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .then(
                when (friendState) {
                    is FriendState.CanAdd -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFriendAddClick() }
                    is FriendState.RequestPending, is FriendState.RequestSent -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onFriendWaitClick() }
                    is FriendState.AlreadyFriend -> Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onAlreadyFriendClick() }
                    else -> Modifier // Loading, Error, ShowAlreadyRequestedDialog, ShowAlreadyFriendDialog - 클릭 불가
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when (friendState) {
            is FriendState.Loading -> {
                // 로딩 중 - 작은 프로그레스 인디케이터
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = ColorPalette.main,
                    strokeWidth = 2.dp
                )
            }
            is FriendState.CanAdd -> {
                // 친구 추가 가능 - btn_navigation_friend_add
                Icon(
                    painter = painterResource(R.drawable.btn_navigation_friend_add),
                    contentDescription = "Add Friend",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            is FriendState.AlreadyFriend, is FriendState.ShowAlreadyFriendDialog -> {
                // 이미 친구 - btn_navigation_friend_already
                Icon(
                    painter = painterResource(R.drawable.btn_navigation_friend_already),
                    contentDescription = "Already Friend",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            is FriendState.RequestPending, is FriendState.RequestSent, is FriendState.ShowAlreadyRequestedDialog -> {
                // 친구 요청 대기 중 - btn_navigation_friend_waiting
                Icon(
                    painter = painterResource(R.drawable.btn_navigation_friend_waiting),
                    contentDescription = "Friend Request Pending",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
            is FriendState.Error -> {
                // 에러 상태 - 친구 추가 버튼으로 복원 (다시 시도 가능)
                Icon(
                    painter = painterResource(R.drawable.btn_navigation_friend_add),
                    contentDescription = "Add Friend",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
