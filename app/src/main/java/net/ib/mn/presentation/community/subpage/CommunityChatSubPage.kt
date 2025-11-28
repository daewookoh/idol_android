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
import androidx.compose.foundation.layout.navigationBarsPadding
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.ChatRoomModel
import net.ib.mn.presentation.community.chat.CommunityChatContract
import net.ib.mn.presentation.community.chat.CommunityChatViewModel
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityChatSubPage(
    rankingItem: RankingItem,
    viewModel: CommunityChatViewModel = hiltViewModel(key = "chat_${rankingItem.id}")
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val idolId = rankingItem.id.toIntOrNull() ?: 0

    var showLeaveDialog by remember { mutableStateOf(false) }
    var leaveDialogRoom by remember { mutableStateOf<ChatRoomModel?>(null) }
    var leaveDialogIsOwner by remember { mutableStateOf(false) }

    LaunchedEffect(idolId) {
        viewModel.sendIntent(CommunityChatContract.Intent.LoadInitialData(idolId))
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
                    // TODO: 채팅방 화면으로 이동
                    Toast.makeText(context, "채팅방 입장: ${effect.title}", Toast.LENGTH_SHORT).show()
                }
                is CommunityChatContract.Effect.NavigateToCreateRoom -> {
                    // TODO: 채팅방 생성 화면으로 이동
                    Toast.makeText(context, "채팅방 생성", Toast.LENGTH_SHORT).show()
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
                    Text(text = "채팅방이 없습니다.", style = ExoTypo.body14, color = ColorPalette.textDimmed)
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
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState()
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

        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = 0 },
            sheetState = sheetState,
            containerColor = ColorPalette.background200,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(ColorPalette.gray200, RoundedCornerShape(2.dp))
                )
            }
        ) {
            FilterBottomSheetContent(
                currentFilter = currentFilter,
                onFilterSelected = { orderBy ->
                    scope.launch {
                        sheetState.hide()
                        showFilterSheet = 0
                        if (isJoinedFilter) onJoinedFilterClick(orderBy) else onAllFilterClick(orderBy)
                    }
                }
            )
        }
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

@Composable
private fun FilterBottomSheetContent(currentFilter: Int, onFilterSelected: (Int) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(bottom = 16.dp)
    ) {
        FilterOptionItem(
            text = stringResource(R.string.freeboard_order_newest),
            onClick = { onFilterSelected(CommunityChatContract.State.ORDER_BY_RECENT) }
        )
        FilterOptionItem(
            text = stringResource(R.string.chat_many_talk_at),
            onClick = { onFilterSelected(CommunityChatContract.State.ORDER_BY_TALK_COUNT) }
        )
    }
}

@Composable
private fun FilterOptionItem(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        style = ExoTypo.title14,
        color = ColorPalette.textDefault,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 16.dp)
    )
}

@Composable
private fun ChatRoomSectionHeader(title: String, currentFilter: Int, onFilterClick: () -> Unit) {
    val filterText = when (currentFilter) {
        CommunityChatContract.State.ORDER_BY_TALK_COUNT -> stringResource(R.string.chat_many_talk_at)
        else -> stringResource(R.string.freeboard_order_newest)
    }

    Row(
        modifier = Modifier.fillMaxWidth().background(ColorPalette.background200).padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = ExoTypo.body14, color = ColorPalette.textDefault)

        Row(modifier = Modifier.clickable { onFilterClick() }, verticalAlignment = Alignment.CenterVertically) {
            Text(text = filterText, style = ExoTypo.body12, color = ColorPalette.textDimmed)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                painter = painterResource(R.drawable.icon_arrow_drop_down),
                contentDescription = null,
                tint = ColorPalette.textDimmed,
                modifier = Modifier.size(16.dp)
            )
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
                .padding(start = 15.dp, end = 0.dp, top = 15.dp, bottom = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (room.levelLimit > 0 || room.isAnonymity) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (room.levelLimit > 0) {
                            Text(
                                text = "Lv.${room.levelLimit}",
                                style = ExoTypo.caption10.copy(fontSize = 10.sp, lineHeight = 12.sp),
                                color = ColorPalette.gray50,
                                modifier = Modifier
                                    .background(ColorPalette.main, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 1.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        if (room.isAnonymity) {
                            Text(
                                text = stringResource(R.string.chat_anonymous),
                                style = ExoTypo.caption10.copy(fontSize = 10.sp),
                                color = ColorPalette.main
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }

                Text(
                    text = room.title,
                    style = ExoTypo.body14,
                    color = ColorPalette.gray1000,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!room.desc.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = room.desc,
                        style = ExoTypo.caption11,
                        color = ColorPalette.textDimmed,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.icon_arrow_right),
                contentDescription = null,
                tint = ColorPalette.gray200,
                modifier = Modifier.padding(start = 20.dp, end = 15.dp).size(24.dp)
            )
        }
    }
}
