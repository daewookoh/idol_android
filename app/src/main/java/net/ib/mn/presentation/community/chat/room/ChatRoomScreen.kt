package net.ib.mn.presentation.community.chat.room

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Alignment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import net.ib.mn.R
import net.ib.mn.data.remote.socket.ChatSocketManager
import net.ib.mn.domain.model.ChatMemberModel
import net.ib.mn.domain.model.ChatMessageModel
import net.ib.mn.domain.model.ChatRoomInfoModel
import net.ib.mn.presentation.common.CommentInput
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoErrorDialog
import net.ib.mn.ui.components.ExoEmoticonPanel
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatRoomScreen(
    roomId: Int,
    nickname: String?,
    userId: Int?,
    role: String?,
    isAnonymity: Boolean,
    title: String,
    onNavigateBack: () -> Unit,
    onNavigateBackWithRefresh: () -> Unit = onNavigateBack,
    viewModel: ChatRoomViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    // Lifecycle 관리: ON_STOP에서 disconnect, ON_START에서 reconnect
    // - ON_PAUSE/ON_RESUME은 무시 (다이얼로그 등에서도 연결 유지)
    // - ON_STOP: 앱이 완전히 백그라운드로 갈 때만 연결 해제
    // - ON_START: 앱이 포그라운드로 돌아올 때 재연결
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    // 앱이 완전히 백그라운드로 갈 때 소켓 연결 해제
                    viewModel.sendIntent(ChatRoomContract.Intent.Disconnect)
                }
                Lifecycle.Event.ON_START -> {
                    // 앱이 포그라운드로 돌아올 때 재연결
                    // 단, 처음 시작할 때는 Initialize에서 연결하므로 이미 연결된 상태면 스킵
                    if (state.connectionState == ChatSocketManager.ConnectionState.DISCONNECTED) {
                        viewModel.sendIntent(ChatRoomContract.Intent.Reconnect)
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // 다이얼로그 상태
    var showDeleteRoomDialog by remember { mutableStateOf(false) }
    var showLeaveRoomDialog by remember { mutableStateOf(false) }
    var showReportRoomDialog by remember { mutableStateOf(false) }
    var showReportReasonDialog by remember { mutableStateOf(false) }
    var showAlreadyReportedDialog by remember { mutableStateOf(false) }
    var alreadyReportedMessage by remember { mutableStateOf("") }
    var reportReason by remember { mutableStateOf("") }
    var reportHeartValue by remember { mutableStateOf(0) }

    // 초기화
    LaunchedEffect(roomId) {
        viewModel.sendIntent(
            ChatRoomContract.Intent.Initialize(
                roomId = roomId,
                nickname = nickname,
                userId = userId,
                role = role,
                isAnonymity = isAnonymity,
                title = title
            )
        )
    }

    // 스크롤 위치 감지
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            val visibleItemsCount = layoutInfo.visibleItemsInfo.size

            val isAtBottom = lastVisibleItem != null && lastVisibleItem.index >= totalItems - 2
            val hiddenItemsCount = totalItems - (lastVisibleItem?.index ?: 0) - 1
            val shouldShowButton = hiddenItemsCount > visibleItemsCount * 2

            isAtBottom to shouldShowButton
        }
            .distinctUntilChanged()
            .collect { (isAtBottom, shouldShowButton) ->
                viewModel.sendIntent(ChatRoomContract.Intent.UpdateScrollPosition(isAtBottom, shouldShowButton))
            }
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ChatRoomContract.Effect.ShowError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ChatRoomContract.Effect.ScrollToBottom -> {
                    if (state.messages.isNotEmpty()) {
                        listState.animateScrollToItem(state.messages.size - 1)
                    }
                }
                is ChatRoomContract.Effect.NavigateBack -> {
                    onNavigateBack()
                }
                is ChatRoomContract.Effect.NavigateBackWithRefresh -> {
                    onNavigateBackWithRefresh()
                }
                is ChatRoomContract.Effect.ShowDeleteConfirmDialog -> {}
                is ChatRoomContract.Effect.ShowReportConfirmDialog -> {}
                is ChatRoomContract.Effect.ShowLeaveConfirmDialog -> {
                    showLeaveRoomDialog = true
                }
                is ChatRoomContract.Effect.ShowDeleteRoomConfirmDialog -> {
                    showDeleteRoomDialog = true
                }
                is ChatRoomContract.Effect.ShowReportRoomConfirmDialog -> {
                    reportHeartValue = effect.reportHeart
                    showReportRoomDialog = true
                }
                is ChatRoomContract.Effect.ShowReportRoomAlreadyReported -> {
                    alreadyReportedMessage = effect.message
                    showAlreadyReportedDialog = true
                }
                is ChatRoomContract.Effect.CopyToClipboard -> {
                    clipboardManager.setText(AnnotatedString(effect.text))
                    Toast.makeText(context, "복사됨", Toast.LENGTH_SHORT).show()
                }
                is ChatRoomContract.Effect.ShowImage -> {}
                is ChatRoomContract.Effect.ShowGalleryPicker -> {}
            }
        }
    }

    // 채팅방 삭제 확인 다이얼로그
    if (showDeleteRoomDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.remove),
            message = stringResource(R.string.chat_room_delete_popup),
            onConfirm = {
                showDeleteRoomDialog = false
                viewModel.confirmDeleteRoom()
            },
            onDismiss = { showDeleteRoomDialog = false }
        )
    }

    // 채팅방 나가기 확인 다이얼로그
    if (showLeaveRoomDialog) {
        // Old 로직: 방장 여부와 멤버 수에 따라 다른 메시지
        val leaveMessage = when {
            !state.isOwner -> stringResource(R.string.chat_room_leave_desc1)
            state.members.size == 1 -> stringResource(R.string.chat_room_leave_desc3)
            else -> stringResource(R.string.chat_room_leave_desc2)
        }

        ExoConfirmDialog(
            title = stringResource(R.string.chat_room_leave),
            message = leaveMessage,
            onConfirm = {
                showLeaveRoomDialog = false
                viewModel.confirmLeaveRoom()
            },
            onDismiss = { showLeaveRoomDialog = false }
        )
    }

    // 채팅방 신고 확인 다이얼로그
    if (showReportRoomDialog) {
        // Old 로직: chat_room_report 문자열에 하트 수 포맷팅
        val reportMessage = String.format(
            stringResource(R.string.chat_room_report),
            reportHeartValue.toString()
        )
        ExoConfirmDialog(
            title = stringResource(R.string.report),
            message = reportMessage,
            onConfirm = {
                showReportRoomDialog = false
                showReportReasonDialog = true
            },
            onDismiss = { showReportRoomDialog = false }
        )
    }

    // 채팅방 신고 사유 입력 다이얼로그
    if (showReportReasonDialog) {
        ReportReasonDialog(
            reason = reportReason,
            onReasonChange = { reportReason = it },
            onConfirm = {
                if (reportReason.length >= 10) {
                    showReportReasonDialog = false
                    viewModel.confirmReportRoom(reportReason)
                    reportReason = ""
                } else {
                    Toast.makeText(context, context.getString(R.string.chat_report_short), Toast.LENGTH_SHORT).show()
                }
            },
            onDismiss = {
                showReportReasonDialog = false
                reportReason = ""
            }
        )
    }

    // 이미 신고된 방 알림 다이얼로그
    if (showAlreadyReportedDialog) {
        ExoErrorDialog(
            message = alreadyReportedMessage,
            onDismiss = { showAlreadyReportedDialog = false }
        )
    }

    // 시스템 뒤로가기 버튼 처리
    BackHandler {
        onNavigateBackWithRefresh()
    }

    // Drawer 상태 동기화
    LaunchedEffect(state.showInfoDrawer) {
        if (state.showInfoDrawer) drawerState.open() else drawerState.close()
    }

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Closed && state.showInfoDrawer) {
            viewModel.sendIntent(ChatRoomContract.Intent.CloseInfoDrawer)
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = state.showInfoDrawer,
            drawerContent = {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    ModalDrawerSheet(
                        modifier = Modifier.width(280.dp),
                        drawerContainerColor = ColorPalette.background100
                    ) {
                        ChatInfoDrawerContent(
                            roomInfo = state.roomInfo,
                            membersCount = state.members.count { !it.deleted },
                            ownerMember = state.members.find { it.role == "O" && !it.deleted },
                            isAnonymous = state.isAnonymous,
                            isOwner = state.isOwner,
                            isDefaultRoom = state.roomInfo?.isDefaultRoom == true,
                            onDeleteRoom = { viewModel.sendIntent(ChatRoomContract.Intent.DeleteRoom) },
                            onReportRoom = { viewModel.sendIntent(ChatRoomContract.Intent.ReportRoom) },
                            onLeaveRoom = { viewModel.sendIntent(ChatRoomContract.Intent.LeaveRoom) },
                            onClose = { viewModel.sendIntent(ChatRoomContract.Intent.CloseInfoDrawer) }
                        )
                    }
                }
            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    topBar = {
                        ExoAppBar(
                            title = {
                                val isConnected = state.connectionState == ChatSocketManager.ConnectionState.AUTH_COMPLETE
                                Column {
                                    Text(
                                        text = state.roomTitle,
                                        style = if (isConnected) ExoTypo.typo18Bold else ExoTypo.typo14Bold,
                                        color = ColorPalette.textDefault,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (!isConnected) {
                                        Text(
                                            text = when (state.connectionState) {
                                                ChatSocketManager.ConnectionState.CONNECTING,
                                                ChatSocketManager.ConnectionState.CONNECTED -> stringResource(R.string.chat_connecting)
                                                ChatSocketManager.ConnectionState.ERROR,
                                                ChatSocketManager.ConnectionState.AUTH_FAILED -> "연결 끊김"
                                                else -> ""
                                            },
                                            style = ExoTypo.typo11,
                                            color = ColorPalette.textDimmed
                                        )
                                    }
                                }
                            },
                            onNavigationClick = onNavigateBackWithRefresh,
                            actions = {
                                IconButton(onClick = { viewModel.sendIntent(ChatRoomContract.Intent.ToggleInfoDrawer) }) {
                                    Icon(
                                        painter = painterResource(R.drawable.btn_navigation_view_more),
                                        contentDescription = "Menu",
                                        modifier = Modifier.size(24.dp),
                                        tint = ColorPalette.textDefault
                                    )
                                }
                            }
                        )
                    },
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding()
                        ) {
                            // 이모티콘 패널 (채팅에서는 선택 시 바로 전송)
                            ExoEmoticonPanel(
                                visible = state.showEmoticonSheet,
                                onEmoticonSelected = { emoticon ->
                                    // 채팅방에서는 이모티콘 선택 시 바로 전송 (emoticonId 전달)
                                    viewModel.sendIntent(ChatRoomContract.Intent.SendEmoticon(emoticon.id))
                                },
                                onEmoticonDoubleClick = { emoticon ->
                                    // 더블클릭도 동일하게 전송
                                    viewModel.sendIntent(ChatRoomContract.Intent.SendEmoticon(emoticon.id))
                                }
                            )

                            CommentInput(
                                value = state.inputText,
                                onValueChange = { viewModel.sendIntent(ChatRoomContract.Intent.UpdateInputText(it)) },
                                onSubmit = { viewModel.sendIntent(ChatRoomContract.Intent.SendMessage) },
                                onAttachClick = { viewModel.sendIntent(ChatRoomContract.Intent.ShowGallery) },
                                onEmoticonClick = {
                                    if (state.showEmoticonSheet) {
                                        viewModel.sendIntent(ChatRoomContract.Intent.HideEmoticonSheet)
                                    } else {
                                        viewModel.sendIntent(ChatRoomContract.Intent.ShowEmoticonSheet)
                                    }
                                },
                                isEmoticonPanelOpen = state.showEmoticonSheet,
                                enabled = state.connectionState == ChatSocketManager.ConnectionState.AUTH_COMPLETE
                            )
                        }
                    },
                    containerColor = ColorPalette.background400 // 채팅방 배경색 (old: @color/background_400)
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        if (state.isLoading && state.messages.isEmpty()) {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.Center),
                                color = ColorPalette.main
                            )
                        } else {
                            MessageList(
                                messages = state.messages,
                                members = state.members,
                                currentUserId = state.userId,
                                cdnUrl = state.cdnUrl,
                                isAnonymous = state.isAnonymous,
                                listState = listState,
                                isLoadingMore = state.isLoadingMore,
                                onLoadMore = { viewModel.sendIntent(ChatRoomContract.Intent.LoadMoreMessages) },
                                onMessageLongClick = { message -> },
                                onImageClick = { url -> }
                            )
                        }

                        if (state.showScrollToBottom) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(16.dp)
                            ) {
                                ScrollToBottomButton(
                                    unreadCount = state.unreadCount,
                                    onClick = { viewModel.sendIntent(ChatRoomContract.Intent.ScrollToBottom) }
                                )
                            }
                        }

                        if (state.isConnecting) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(ColorPalette.gray1000Opacity60),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = ColorPalette.main)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = stringResource(R.string.chat_connecting),
                                        style = ExoTypo.typo14,
                                        color = ColorPalette.fixWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageList(
    messages: List<ChatMessageModel>,
    members: List<ChatMemberModel>,
    currentUserId: Int,
    cdnUrl: String,
    isAnonymous: Boolean,
    listState: LazyListState,
    isLoadingMore: Boolean,
    onLoadMore: () -> Unit,
    onMessageLongClick: (ChatMessageModel) -> Unit,
    onImageClick: (String) -> Unit
) {
    val shouldLoadMore by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex <= 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore && !isLoadingMore && messages.isNotEmpty()) {
            onLoadMore()
        }
    }

    // 초기 로드 완료 시 또는 새 메시지 수신 시 마지막(최신) 메시지로 스크롤
    // (사용자가 위로 스크롤해서 보고 있는 중이 아닐 때만)
    val isAtBottom by remember {
        derivedStateOf {
            val lastVisibleIndex = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= messages.size - 3
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            // 처음 로드 시 또는 사용자가 아래에 있을 때만 자동 스크롤
            if (listState.firstVisibleItemIndex == 0 || isAtBottom) {
                listState.animateScrollToItem(messages.lastIndex)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        if (isLoadingMore) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = ColorPalette.main,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        itemsIndexed(
            items = messages,
            key = { _, message -> "${message.userId}_${message.serverTs}" }
        ) { index, message ->
            val prevMessage = messages.getOrNull(index - 1)
            val nextMessage = messages.getOrNull(index + 1)

            // 이전 메시지와 다른 날짜인지 확인
            val showDateHeader = prevMessage == null || !isSameDay(prevMessage.serverTs, message.serverTs)

            // 그룹의 마지막 메시지 판단 (시간 표시용)
            // Old 로직: 다음 userId가 다르거나, 다음 시간(분단위)이 다를 때
            val isLastOfGroup = nextMessage == null ||
                    nextMessage.userId != message.userId ||
                    nextMessage.isFirstJoinMsg ||
                    !isSameDay(message.serverTs, nextMessage.serverTs) ||
                    isTimeGapLarge(message.serverTs, nextMessage.serverTs)

            // 연속 메시지 그룹 판단 (닉네임/프로필 표시용)
            // 유저 요청: 닉네임은 분과 상관없이 연속될 경우 무제한으로 생략
            // 즉, userId가 바뀌거나 날짜가 바뀔 때만 닉네임 표시
            val isStartOfGroup = prevMessage == null ||
                    prevMessage.userId != message.userId ||
                    prevMessage.isFirstJoinMsg ||
                    !isSameDay(prevMessage.serverTs, message.serverTs)

            // 멤버 정보 가져오기
            val member = members.find { it.id == message.userId && !it.deleted }

            ChatMessageItem(
                message = message,
                member = member,
                cdnUrl = cdnUrl,
                isMyMessage = message.userId == currentUserId,
                isAnonymous = isAnonymous,
                showDateHeader = showDateHeader,
                isStartOfGroup = isStartOfGroup,
                isLastOfGroup = isLastOfGroup,
                onLongClick = { onMessageLongClick(message) },
                onImageClick = onImageClick
            )
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessageModel,
    member: ChatMemberModel?,
    cdnUrl: String,
    isMyMessage: Boolean,
    isAnonymous: Boolean,
    showDateHeader: Boolean,
    isStartOfGroup: Boolean,
    isLastOfGroup: Boolean,
    onLongClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // 날짜 헤더
        if (showDateHeader) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = formatDateHeader(message.serverTs),
                    style = ExoTypo.typo10,
                    color = ColorPalette.gray300
                )
            }
        }

        // 첫 입장 메시지
        if (message.isFirstJoinMsg) {
            FirstJoinMessage()
            return
        }

        // 삭제된 메시지
        if (message.deleted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                contentAlignment = if (isMyMessage) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Text(
                    text = stringResource(R.string.chat_deleted_message),
                    style = ExoTypo.typo11,
                    color = ColorPalette.textDimmed
                )
            }
            return
        }

        // 시스템 메시지
        if (message.contentType == ChatMessageModel.TYPE_TOAST) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = message.content,
                    style = ExoTypo.typo11,
                    color = ColorPalette.textDimmed,
                    textAlign = TextAlign.Center
                )
            }
            return
        }

        // 일반 메시지
        if (isMyMessage) {
            MyMessageBubble(
                message = message,
                member = member,
                cdnUrl = cdnUrl,
                isAnonymous = isAnonymous,
                isStartOfGroup = isStartOfGroup,
                isLastOfGroup = isLastOfGroup,
                onLongClick = onLongClick,
                onImageClick = onImageClick
            )
        } else {
            OtherMessageBubble(
                message = message,
                member = member,
                cdnUrl = cdnUrl,
                isAnonymous = isAnonymous,
                isStartOfGroup = isStartOfGroup,
                isLastOfGroup = isLastOfGroup,
                onLongClick = onLongClick,
                onImageClick = onImageClick
            )
        }
    }
}

/**
 * 첫 입장 메시지 UI (old: item_chat_message_first_join.xml)
 */
@Composable
private fun FirstJoinMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(ColorPalette.gray100)
                .padding(horizontal = 30.dp, vertical = 5.dp)
        ) {
            Text(
                text = stringResource(R.string.chat_first_join),
                style = ExoTypo.typo11.copy(fontWeight = FontWeight.Bold),
                color = ColorPalette.gray900
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.chatroom_enter_warning_msg),
            style = ExoTypo.typo10.copy(fontSize = 8.sp),
            color = ColorPalette.textDimmed,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
    }
}

/**
 * 내 메시지 버블 (old: item_chat_message_mine.xml, item_chat_message_mine_start.xml)
 */
@Composable
private fun MyMessageBubble(
    message: ChatMessageModel,
    member: ChatMemberModel?,
    cdnUrl: String,
    isAnonymous: Boolean,
    isStartOfGroup: Boolean,
    isLastOfGroup: Boolean,
    onLongClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (isStartOfGroup) 6.dp else 2.dp,
                bottom = if (isLastOfGroup) 6.dp else 0.dp,
                end = 10.dp
            ),
        horizontalAlignment = Alignment.End
    ) {
        // 익명방이고 그룹 시작이면 닉네임 표시
        // member가 null이면 아직 로드 중이므로 빈 텍스트 표시 (퇴장한 사용자 X)
        if (isAnonymous && isStartOfGroup && member != null) {
            Text(
                text = member.nickname,
                style = ExoTypo.typo11.copy(fontWeight = FontWeight.Bold),
                color = ColorPalette.gray250,
                modifier = Modifier.padding(end = 5.dp, bottom = 2.dp)
            )
        }

        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.End
        ) {
            // 시간 (마지막 메시지에만 표시)
            if (isLastOfGroup) {
                Text(
                    text = formatMessageTime(message.serverTs),
                    style = ExoTypo.typo10,
                    color = ColorPalette.gray300,
                    modifier = Modifier.padding(end = 3.dp)
                )
            }

            MessageContent(
                message = message,
                cdnUrl = cdnUrl,
                isMyMessage = true,
                isStartOfGroup = isStartOfGroup,
                onLongClick = onLongClick,
                onImageClick = onImageClick
            )
        }
    }
}

/**
 * 다른 사용자 메시지 버블 (old: item_chat_message.xml, item_chat_message_start.xml)
 */
@Composable
private fun OtherMessageBubble(
    message: ChatMessageModel,
    member: ChatMemberModel?,
    cdnUrl: String,
    isAnonymous: Boolean,
    isStartOfGroup: Boolean,
    isLastOfGroup: Boolean,
    onLongClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    val startPadding = if (isAnonymous) 10.dp else 10.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                top = if (isStartOfGroup) 8.dp else 3.dp,
                bottom = if (isLastOfGroup) 6.dp else 0.dp,
                start = startPadding
            )
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 프로필 영역 (익명방이 아닐 때만, 그룹 시작일 때만 표시)
            if (!isAnonymous) {
                if (isStartOfGroup) {
                    ExoProfileImage(
                        imageUrl = member?.imageUrl,
                        type = ProfileImageType.SMALL,
                        contentDescription = "프로필 이미지"
                    )
                } else {
                    Spacer(modifier = Modifier.width(40.dp)) // SMALL 타입 크기와 동일
                }

                Spacer(modifier = Modifier.width(10.dp))
            }

            Column {
                // 닉네임 + 레벨 (그룹 시작일 때만)
                if (isStartOfGroup) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 5.dp)
                    ) {
                        // 레벨 아이콘 (익명방이 아닐 때만, member가 있을 때만)
                        if (!isAnonymous && member != null) {
                            Icon(
                                painter = painterResource(getLevelDrawable(member.level)),
                                contentDescription = null,
                                modifier = Modifier.height(14.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                        }

                        Text(
                            text = member?.nickname ?: stringResource(R.string.chat_leave_user),
                            style = ExoTypo.typo11.copy(fontWeight = FontWeight.Bold),
                            color = if (isAnonymous) ColorPalette.gray250 else ColorPalette.textChat,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.Bottom) {
                    MessageContent(
                        message = message,
                        cdnUrl = cdnUrl,
                        isMyMessage = false,
                        isStartOfGroup = isStartOfGroup,
                        onLongClick = onLongClick,
                        onImageClick = onImageClick
                    )

                    if (isLastOfGroup) {
                        Text(
                            text = formatMessageTime(message.serverTs),
                            style = ExoTypo.typo10,
                            color = ColorPalette.gray300,
                            modifier = Modifier.padding(start = 3.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 메시지 내용 (텍스트/이미지/이모티콘)
 * old 프로젝트: ChatMessageAdapter.kt 참고
 *
 * 이미지/이모티콘 content 형식:
 * {"url": "...", "thumbnail": "...", "is_emoticon": "true/false", "thumb_height": "...", "thumb_width": "..."}
 *
 * @param message 채팅 메시지
 * @param cdnUrl CDN 베이스 URL (이모티콘 URL 변환용)
 * @param isMyMessage 내 메시지 여부
 * @param isStartOfGroup 그룹 시작 여부
 * @param onLongClick 롱클릭 콜백
 * @param onImageClick 이미지 클릭 콜백
 */
@Composable
private fun MessageContent(
    message: ChatMessageModel,
    cdnUrl: String,
    isMyMessage: Boolean,
    isStartOfGroup: Boolean,
    onLongClick: () -> Unit,
    onImageClick: (String) -> Unit
) {
    when (message.contentType) {
        ChatMessageModel.TYPE_IMAGE -> {
            ImageMessageContent(
                message = message,
                cdnUrl = cdnUrl,
                onImageClick = onImageClick
            )
        }
        else -> {
            // 텍스트 메시지 말풍선
            TextMessageBubble(
                message = message,
                isMyMessage = isMyMessage,
                isStartOfGroup = isStartOfGroup,
                onLongClick = onLongClick
            )
        }
    }
}

/**
 * 이미지/이모티콘 메시지 컨텐츠
 * old 프로젝트: ChatMessageAdapter 참고
 * - 이모티콘: 150dp x 150dp 크기로 표시
 * - 일반 이미지: 썸네일 URL을 사용, 비율 유지하며 최대 200dp
 *
 * @param message 채팅 메시지
 * @param cdnUrl CDN 베이스 URL (이모티콘 URL 변환용)
 * @param onImageClick 이미지 클릭 콜백
 */
@Composable
private fun ImageMessageContent(
    message: ChatMessageModel,
    cdnUrl: String,
    onImageClick: (String) -> Unit
) {
    // 이모티콘인 경우 cdnUrl 기반 올바른 URL 사용, 아니면 기존 URL 사용
    val displayUrl = if (message.isEmoticon && cdnUrl.isNotEmpty()) {
        // old 프로젝트 형식: ${cdnUrl}/e/${emoticonId}.t.webp
        message.getCorrectEmoticonUrl(cdnUrl)?.toSecureUrl()
            ?: (message.displayImageUrl ?: message.content).toSecureUrl()
    } else {
        (message.displayImageUrl ?: message.content).toSecureUrl()
    }
    val originalUrl = (message.imageUrl ?: message.content).toSecureUrl()

    if (message.isEmoticon) {
        // 이모티콘: 고정 크기 150dp x 150dp (old 프로젝트와 동일)
        AsyncImage(
            model = displayUrl,
            contentDescription = null,
            modifier = Modifier
                .size(150.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable { onImageClick(originalUrl) }
        )
    } else {
        // 일반 이미지: 썸네일 표시, 비율 유지
        val thumbWidth = message.thumbWidth
        val thumbHeight = message.thumbHeight

        // 최대 너비 200dp 기준으로 크기 계산 (old 프로젝트와 유사)
        val maxWidthDp = 200.dp
        val modifier = if (thumbWidth > 0 && thumbHeight > 0) {
            // 비율 계산하여 높이 설정
            val aspectRatio = thumbWidth.toFloat() / thumbHeight.toFloat()
            Modifier
                .widthIn(max = maxWidthDp)
                .aspectRatio(aspectRatio)
        } else {
            Modifier.widthIn(max = maxWidthDp)
        }

        Box {
            AsyncImage(
                model = displayUrl,
                contentDescription = null,
                modifier = modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick(originalUrl) }
            )

            // GIF 표시 아이콘 (old 프로젝트: binding.gifImage)
            if (message.isGif) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                        .background(
                            color = Color.Black.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "GIF",
                        style = ExoTypo.typo10,
                        color = Color.White
                    )
                }
            }
        }
    }
}

/**
 * 텍스트 메시지 말풍선
 */
@Composable
private fun TextMessageBubble(
    message: ChatMessageModel,
    isMyMessage: Boolean,
    isStartOfGroup: Boolean,
    onLongClick: () -> Unit
) {
    // 말풍선 모양 - 그룹 시작이면 위쪽에 꼬리 (뾰족한 모서리)
    val shape = if (isMyMessage) {
        RoundedCornerShape(
            topStart = 16.dp,
            topEnd = if (isStartOfGroup) 0.dp else 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    } else {
        RoundedCornerShape(
            topStart = if (isStartOfGroup) 0.dp else 16.dp,
            topEnd = 16.dp,
            bottomStart = 16.dp,
            bottomEnd = 16.dp
        )
    }

    Box(
        modifier = Modifier
            .widthIn(max = 203.dp)
            .clip(shape)
            .background(if (isMyMessage) ColorPalette.main else ColorPalette.background200) // old: bg_mytalk (핑크), bg_usertalk (흰색)
            .clickable(onClick = onLongClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = message.content,
            style = ExoTypo.typo13,
            color = if (isMyMessage) ColorPalette.fixWhite else ColorPalette.gray900
        )
    }
}

@Composable
private fun ScrollToBottomButton(
    unreadCount: Int,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(ColorPalette.main)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.icon_arrow_drop_down),
            contentDescription = "Scroll to bottom",
            modifier = Modifier.size(20.dp),
            tint = ColorPalette.fixWhite
        )

        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(ColorPalette.fixMain),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    style = ExoTypo.typo10,
                    color = ColorPalette.fixWhite
                )
            }
        }
    }
}

@Composable
private fun ChatInfoDrawerContent(
    roomInfo: ChatRoomInfoModel?,
    membersCount: Int,
    ownerMember: ChatMemberModel?,
    isAnonymous: Boolean,
    isOwner: Boolean,
    isDefaultRoom: Boolean,
    onDeleteRoom: () -> Unit,
    onReportRoom: () -> Unit,
    onLeaveRoom: () -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorPalette.background100)
    ) {
        // 상단 툴바: 닫기(forward) + 삭제/신고 아이콘
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(ColorPalette.navigationBar)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(
                    painter = painterResource(R.drawable.ic_action_forward),
                    contentDescription = "Close",
                    tint = ColorPalette.textDefault
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 기본방이 아닐 때만 아이콘 표시
            if (!isDefaultRoom) {
                if (isOwner) {
                    // 방장일 때: 삭제 아이콘
                    IconButton(onClick = onDeleteRoom) {
                        Icon(
                            painter = painterResource(R.drawable.btn_navigation_delete),
                            contentDescription = "Delete Room",
                            modifier = Modifier.size(24.dp),
                            tint = ColorPalette.textDefault
                        )
                    }
                } else {
                    // 방장이 아닐 때: 신고 아이콘
                    IconButton(onClick = onReportRoom) {
                        Icon(
                            painter = painterResource(R.drawable.btn_navigation_report),
                            contentDescription = "Report Room",
                            modifier = Modifier.size(24.dp),
                            tint = ColorPalette.textDefault
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 30.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.chat_room_number_of_person),
                    style = ExoTypo.typo14.copy(fontWeight = FontWeight.Bold),
                    color = ColorPalette.gray900
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.icon_people),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.Unspecified
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = membersCount.toString(),
                    style = ExoTypo.typo11.copy(fontWeight = FontWeight.Bold),
                    color = ColorPalette.gray300
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ColorPalette.gray200, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.chat_room_status),
                style = ExoTypo.typo14.copy(fontWeight = FontWeight.Bold),
                color = ColorPalette.gray900
            )

            Spacer(modifier = Modifier.height(15.dp))

            Column(modifier = Modifier.padding(start = 13.dp)) {
                // 그룹/멤버 공개방 - 주석처리
//                Text(
//                    text = "• ${if (roomInfo?.isMostOnlyRoom == true) stringResource(R.string.lable_show_private) else stringResource(R.string.chat_room_most_and_group)}",
//                    style = ExoTypo.typo11,
//                    color = ColorPalette.gray900
//                )
//                Spacer(modifier = Modifier.height(7.dp))

                // 닉네임/익명방 표시: isAnonymous 파라미터 사용 (roomInfo 대신)
                Text(
                    text = "• ${if (isAnonymous) stringResource(R.string.chat_room_anonymous) else stringResource(R.string.chat_room_nickname)}",
                    style = ExoTypo.typo11,
                    color = ColorPalette.gray900
                )
                // 레벨 제한 (old 프로젝트: 항상 표시)
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = "• ${String.format(stringResource(R.string.chat_room_level_limit), roomInfo?.levelLimit ?: 0)}",
                    style = ExoTypo.typo11,
                    color = ColorPalette.gray900
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = ColorPalette.gray200, thickness = 0.5.dp)

            if (roomInfo?.isDefaultRoom != true) {
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = stringResource(R.string.chat_room_owner),
                    style = ExoTypo.typo14.copy(fontWeight = FontWeight.Bold),
                    color = ColorPalette.gray900
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.padding(start = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isAnonymous && ownerMember != null) {
                        ExoProfileImage(
                            imageUrl = ownerMember.imageUrl,
                            type = ProfileImageType.SMALL,
                            contentDescription = "방장 프로필"
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Icon(
                            painter = painterResource(getLevelDrawable(ownerMember.level)),
                            contentDescription = null,
                            modifier = Modifier.height(15.dp),
                            tint = Color.Unspecified
                        )

                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    // Old 로직: 익명방에서도 방장 닉네임은 실제 닉네임으로 표시
                    Text(
                        text = ownerMember?.nickname ?: "",
                        style = ExoTypo.typo11.copy(fontWeight = FontWeight.Bold),
                        color = ColorPalette.gray580
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = ColorPalette.gray200, thickness = 0.5.dp)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .background(ColorPalette.gray80)
                .clickable(onClick = onLeaveRoom)
                .padding(end = 20.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.chat_room_leave),
                style = ExoTypo.typo14.copy(fontWeight = FontWeight.Bold),
                color = ColorPalette.gray900
            )
            Spacer(modifier = Modifier.width(7.dp))
            Icon(
                painter = painterResource(R.drawable.btn_navigation_back),
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer(rotationZ = 180f),
                tint = ColorPalette.gray900
            )
        }
    }
}

// ============= Helper Functions =============

private fun formatMessageTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("a h:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatDateHeader(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy. MM. dd.", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

/**
 * 분단위가 다른지 체크 (old 로직: 분단위가 같으면 시간 생략)
 * 예: 10:30과 10:30 → false (같음, 시간 생략)
 *     10:30과 10:31 → true (다름, 시간 표시)
 */
private fun isTimeGapLarge(timestamp1: Long, timestamp2: Long): Boolean {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    val time1 = sdf.format(Date(timestamp1))
    val time2 = sdf.format(Date(timestamp2))
    return time1 != time2
}

private fun getLevelDrawable(level: Int): Int {
    return when (level.coerceIn(0, 40)) {
        0 -> R.drawable.icon_level_0
        1 -> R.drawable.icon_level_1
        2 -> R.drawable.icon_level_2
        3 -> R.drawable.icon_level_3
        4 -> R.drawable.icon_level_4
        5 -> R.drawable.icon_level_5
        6 -> R.drawable.icon_level_6
        7 -> R.drawable.icon_level_7
        8 -> R.drawable.icon_level_8
        9 -> R.drawable.icon_level_9
        10 -> R.drawable.icon_level_10
        11 -> R.drawable.icon_level_11
        12 -> R.drawable.icon_level_12
        13 -> R.drawable.icon_level_13
        14 -> R.drawable.icon_level_14
        15 -> R.drawable.icon_level_15
        16 -> R.drawable.icon_level_16
        17 -> R.drawable.icon_level_17
        18 -> R.drawable.icon_level_18
        19 -> R.drawable.icon_level_19
        20 -> R.drawable.icon_level_20
        21 -> R.drawable.icon_level_21
        22 -> R.drawable.icon_level_22
        23 -> R.drawable.icon_level_23
        24 -> R.drawable.icon_level_24
        25 -> R.drawable.icon_level_25
        26 -> R.drawable.icon_level_26
        27 -> R.drawable.icon_level_27
        28 -> R.drawable.icon_level_28
        29 -> R.drawable.icon_level_29
        30 -> R.drawable.icon_level_30
        31 -> R.drawable.icon_level_31
        32 -> R.drawable.icon_level_32
        33 -> R.drawable.icon_level_33
        34 -> R.drawable.icon_level_34
        35 -> R.drawable.icon_level_35
        36 -> R.drawable.icon_level_36
        37 -> R.drawable.icon_level_37
        38 -> R.drawable.icon_level_38
        39 -> R.drawable.icon_level_39
        40 -> R.drawable.icon_level_40
        else -> R.drawable.icon_level_0
    }
}

/**
 * 신고 사유 입력 다이얼로그
 */
@Composable
private fun ReportReasonDialog(
    reason: String,
    onReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(ColorPalette.background100)
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.report_description),
                style = ExoTypo.typo16Bold,
                color = ColorPalette.textDefault
            )

            Spacer(modifier = Modifier.height(16.dp))

            androidx.compose.material3.OutlinedTextField(
                value = reason,
                onValueChange = onReasonChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = {
                    Text(
                        text = stringResource(R.string.quiz_report_hint),
                        style = ExoTypo.typo14,
                        color = ColorPalette.textDimmed
                    )
                },
                textStyle = ExoTypo.typo14,
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ColorPalette.main,
                    unfocusedBorderColor = ColorPalette.gray200
                )
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = stringResource(R.string.btn_cancel),
                    style = ExoTypo.typo14,
                    color = ColorPalette.textDimmed,
                    modifier = Modifier
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.report),
                    style = ExoTypo.typo14,
                    color = ColorPalette.main,
                    modifier = Modifier
                        .clickable(onClick = onConfirm)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}
