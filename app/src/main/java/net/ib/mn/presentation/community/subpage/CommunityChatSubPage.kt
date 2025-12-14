package net.ib.mn.presentation.community.subpage

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.domain.model.ChatRoomModel
import net.ib.mn.presentation.community.CommunityViewModel
import net.ib.mn.ui.components.ExoBottomSheetItem
import net.ib.mn.ui.components.ExoBottomSheetList
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityChatSubPage(
    idolData: CommunityViewModel.IdolData,
    shouldRefresh: Boolean = false,
    onRefreshConsumed: () -> Unit = {},
    onNavigateToChatRoom: (roomId: Int, nickname: String?, userId: Int?, role: String?, isAnonymity: Boolean, title: String) -> Unit = { _, _, _, _, _, _ -> },
    onNavigateToCreateRoom: () -> Unit = {},
    viewModel: CommunityChatViewModel = hiltViewModel(key = "chat_${idolData.id}")
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val idolId = idolData.id.toIntOrNull() ?: 0

    var showLeaveDialog by remember { mutableStateOf(false) }
    var leaveDialogRoom by remember { mutableStateOf<ChatRoomModel?>(null) }
    var leaveDialogIsOwner by remember { mutableStateOf(false) }

    LaunchedEffect(idolId) {
        viewModel.sendIntent(CommunityChatContract.Intent.LoadInitialData(idolId))
    }

    // 외부에서 새로고침 요청 시 처리
    LaunchedEffect(shouldRefresh) {
        if (shouldRefresh) {
            viewModel.sendIntent(CommunityChatContract.Intent.Refresh(idolId))
            onRefreshConsumed()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is CommunityChatContract.Effect.ShowError,
                is CommunityChatContract.Effect.ShowToast -> {
                    val message = when (effect) {
                        is CommunityChatContract.Effect.ShowError -> effect.message
                        is CommunityChatContract.Effect.ShowToast -> effect.message
                        else -> return@collectLatest
                    }
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
                is CommunityChatContract.Effect.NavigateToChatRoom -> {
                    onNavigateToChatRoom(
                        effect.roomId,
                        effect.nickname,
                        effect.userId,
                        effect.role,
                        effect.isAnonymity,
                        effect.title
                    )
                }
                is CommunityChatContract.Effect.NavigateToCreateRoom -> {
                    onNavigateToCreateRoom()
                }
                is CommunityChatContract.Effect.ShowLeaveConfirmDialog -> {
                    leaveDialogRoom = effect.room
                    leaveDialogIsOwner = effect.isOwner
                    showLeaveDialog = true
                }
                is CommunityChatContract.Effect.ShowLowLevelPopup -> {
                    Toast.makeText(context, context.getString(R.string.chat_less_level_popup, 5), Toast.LENGTH_SHORT).show()
                }
                is CommunityChatContract.Effect.ShowDifferentMostPopup -> {
                    Toast.makeText(context, "최애가 아닌 채팅방은 만들 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    leaveDialogRoom?.takeIf { showLeaveDialog }?.let { room ->
        val subtitle = when {
            leaveDialogIsOwner && room.curPeopleCount == 1 -> stringResource(R.string.chat_room_leave_desc3)
            leaveDialogIsOwner -> stringResource(R.string.chat_room_leave_desc2)
            else -> stringResource(R.string.chat_room_leave_desc1)
        }

        ExoConfirmDialog(
            title = stringResource(R.string.chat_room_leave),
            message = subtitle,
            onConfirm = {
                showLeaveDialog = false
                viewModel.leaveRoom(room.roomId)
            },
            onDismiss = { showLeaveDialog = false }
        )
    }

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { viewModel.sendIntent(CommunityChatContract.Intent.Refresh(idolId)) },
        modifier = Modifier.fillMaxSize()
    ) {
        when {
            state.isLoading && state.joinedRooms.isEmpty() && state.allRooms.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }
            state.isEmpty -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = "채팅방이 없습니다.", style = ExoTypo.typo14, color = ColorPalette.textDimmed)
                }
            }
            else -> {
                ChatRoomList(
                    state = state,
                    onRoomClick = { room ->
                        viewModel.sendIntent(CommunityChatContract.Intent.JoinRoom(room.roomId, room))
                    },
                    onRoomLongClick = { room ->
                        if (room.isJoinedRoom) {
                            viewModel.sendIntent(CommunityChatContract.Intent.LeaveRoom(room.roomId, room))
                        }
                    },
                    onJoinedFilterClick = { orderBy ->
                        viewModel.sendIntent(CommunityChatContract.Intent.ChangeJoinedFilter(orderBy, idolId))
                    },
                    onAllFilterClick = { orderBy ->
                        viewModel.sendIntent(CommunityChatContract.Intent.ChangeAllFilter(orderBy, idolId))
                    },
                    onLoadMoreJoined = { viewModel.sendIntent(CommunityChatContract.Intent.LoadMoreJoined(idolId)) },
                    onLoadMoreAll = { viewModel.sendIntent(CommunityChatContract.Intent.LoadMoreAll(idolId)) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatRoomList(
    state: CommunityChatContract.State,
    onRoomClick: (ChatRoomModel) -> Unit,
    onRoomLongClick: (ChatRoomModel) -> Unit,
    onJoinedFilterClick: (Int) -> Unit,
    onAllFilterClick: (Int) -> Unit,
    onLoadMoreJoined: () -> Unit,
    onLoadMoreAll: () -> Unit
) {
    val listState = rememberLazyListState()
    var showFilterSheet by remember { mutableIntStateOf(0) }

    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            when {
                state.joinedNextUrl != null -> onLoadMoreJoined()
                state.allNextUrl != null -> onLoadMoreAll()
            }
        }
    }

    if (showFilterSheet > 0) {
        val isJoinedFilter = showFilterSheet == 1
        val currentFilter = if (isJoinedFilter) state.joinedOrderBy else state.allOrderBy

        val filterItems = listOf(
            ExoBottomSheetItem(
                value = CommunityChatContract.State.ORDER_BY_RECENT,
                label = stringResource(R.string.freeboard_order_newest)
            ),
            ExoBottomSheetItem(
                value = CommunityChatContract.State.ORDER_BY_TALK_COUNT,
                label = stringResource(R.string.chat_many_talk_at)
            )
        )

        ExoBottomSheetList(
            items = filterItems,
            selectedValue = currentFilter,
            onItemSelected = { orderBy ->
                if (isJoinedFilter) onJoinedFilterClick(orderBy) else onAllFilterClick(orderBy)
            },
            onDismissRequest = { showFilterSheet = 0 }
        )
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize().background(ColorPalette.background100)
    ) {
        if (state.joinedRooms.isNotEmpty()) {
            item(key = "joined_header") {
                ChatRoomSectionHeader(
                    title = stringResource(R.string.chat_list_join, state.joinedTotalCount),
                    currentFilter = state.joinedOrderBy,
                    onFilterClick = { showFilterSheet = 1 }
                )
            }

            items(items = state.joinedRooms, key = { "joined_${it.roomId}" }) { room ->
                ChatRoomItem(room = room, onClick = { onRoomClick(room) }, onLongClick = { onRoomLongClick(room) })
            }
        }

        val filteredAllRooms = state.filteredAllRooms
        if (filteredAllRooms.isNotEmpty() || state.joinedRooms.isEmpty()) {
            item(key = "all_header") {
                ChatRoomSectionHeader(
                    title = stringResource(R.string.chat_list_all, state.filteredAllTotalCount),
                    currentFilter = state.allOrderBy,
                    showTopLine = state.joinedRooms.isNotEmpty(), // 참여 중이 있을 때만 상단 라인 표시
                    onFilterClick = { showFilterSheet = 2 }
                )
            }

            items(items = filteredAllRooms, key = { "all_${it.roomId}" }) { room ->
                ChatRoomItem(room = room, onClick = { onRoomClick(room) }, onLongClick = {})
            }
        }

        if (state.isLoadingMoreJoined || state.isLoadingMoreAll) {
            item(key = "loading") {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = ColorPalette.main)
                }
            }
        }
    }
}

/**
 * 채팅방 섹션 헤더
 * old 프로젝트: item_header_chatting_room_list_join.xml, item_header_chatting_room_list_entire.xml
 *
 * @param title 섹션 제목 (참여 중 N개, 전체 N개)
 * @param currentFilter 현재 필터 값
 * @param showTopLine 상단 라인 표시 여부 (전체 섹션에서만 true)
 * @param onFilterClick 필터 클릭 콜백
 */
@Composable
private fun ChatRoomSectionHeader(
    title: String,
    currentFilter: Int,
    showTopLine: Boolean = false,
    onFilterClick: () -> Unit
) {
    val filterText = when (currentFilter) {
        CommunityChatContract.State.ORDER_BY_TALK_COUNT -> stringResource(R.string.chat_many_talk_at)
        else -> stringResource(R.string.freeboard_order_newest)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        // 전체 섹션 상단 라인 (old: gray_110_solid_white_border_bottom 배경)
        if (showTopLine) {
            HorizontalDivider(thickness = 0.5.dp, color = ColorPalette.gray100)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 15.dp, end = 20.dp, top = 15.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // old: 12sp, text_dimmed
            Text(
                text = title,
                style = ExoTypo.typo12,
                color = ColorPalette.textDimmed
            )

            Row(
                modifier = Modifier.clickable { onFilterClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                // old: 12sp, text_gray
                Text(
                    text = filterText,
                    style = ExoTypo.typo12,
                    color = ColorPalette.textGray
                )
                Spacer(modifier = Modifier.width(5.dp))
                Icon(
                    painter = painterResource(R.drawable.icon_arrow_drop_down),
                    contentDescription = null,
                    tint = ColorPalette.textGray,
                    modifier = Modifier.size(width = 8.dp, height = 5.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatRoomItem(room: ChatRoomModel, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(ColorPalette.background200)) {
        HorizontalDivider(thickness = 0.5.dp, color = ColorPalette.gray100)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .pointerInput(Unit) { detectTapGestures(onTap = { onClick() }, onLongPress = { onLongClick() }) }
                .padding(start = 15.dp, end = 15.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 상단: 태그 + 시간
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (room.levelLimit > 0) {
                            Text(
                                text = "Lv.${room.levelLimit}",
                                style = ExoTypo.typo10.copy(fontSize = 10.sp, lineHeight = 12.sp),
                                color = ColorPalette.gray50,
                                modifier = Modifier
                                    .background(ColorPalette.main, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (room.isAnonymousRoom) {
                            Text(
                                text = stringResource(R.string.chat_anonymous),
                                style = ExoTypo.typo10.copy(fontSize = 10.sp),
                                color = ColorPalette.main
                            )
                        }
                    }

                    // 마지막 메시지 시간
                    if (!room.lastMessageTime.isNullOrEmpty()) {
                        Text(
                            text = formatChatTime(room.lastMessageTime),
                            style = ExoTypo.typo10,
                            color = ColorPalette.textDimmed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 제목
                Text(
                    text = room.title,
                    style = ExoTypo.typo14,
                    color = ColorPalette.gray1000,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // 채팅방 설명 (desc만 표시, 마지막 메시지는 표시 안함)
                if (!room.desc.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = room.desc,
                        style = ExoTypo.typo11,
                        color = ColorPalette.textDimmed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 읽지 않은 메시지 개수 배지 또는 화살표
            if (room.unreadCount > 0 && room.isJoinedRoom) {
                Box(
                    modifier = Modifier
                        .background(ColorPalette.main, RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (room.unreadCount > 99) "99+" else room.unreadCount.toString(),
                        style = ExoTypo.typo10,
                        color = ColorPalette.gray50
                    )
                }
            } else {
                Icon(
                    painter = painterResource(R.drawable.icon_arrow_right),
                    contentDescription = null,
                    tint = ColorPalette.gray200,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * 채팅 시간 포맷팅
 * - 오늘: 시간만 표시 (HH:mm)
 * - 어제: "어제"
 * - 올해: 월/일 (MM/dd)
 * - 이전 연도: 년/월/일 (yy/MM/dd)
 */
private fun formatChatTime(timeString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val date = inputFormat.parse(timeString) ?: return timeString

        val now = java.util.Calendar.getInstance()
        val messageTime = java.util.Calendar.getInstance().apply { time = date }

        val isToday = now.get(java.util.Calendar.YEAR) == messageTime.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) == messageTime.get(java.util.Calendar.DAY_OF_YEAR)

        val isYesterday = now.get(java.util.Calendar.YEAR) == messageTime.get(java.util.Calendar.YEAR) &&
                now.get(java.util.Calendar.DAY_OF_YEAR) - messageTime.get(java.util.Calendar.DAY_OF_YEAR) == 1

        val isThisYear = now.get(java.util.Calendar.YEAR) == messageTime.get(java.util.Calendar.YEAR)

        when {
            isToday -> java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(date)
            isYesterday -> "Yesterday"
            isThisYear -> java.text.SimpleDateFormat("MM/dd", java.util.Locale.getDefault()).format(date)
            else -> java.text.SimpleDateFormat("yy/MM/dd", java.util.Locale.getDefault()).format(date)
        }
    } catch (e: Exception) {
        timeString
    }
}
