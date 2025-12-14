package net.ib.mn.presentation.friend.delete

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collectLatest
import net.ib.mn.R
import net.ib.mn.domain.model.FriendModel
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.presentation.profile.ProfileScreen
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoBottomSheetItem
import net.ib.mn.ui.components.ExoBottomSheetList
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoDialog
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ExoTypo
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 친구 삭제 화면
 *
 * old 프로젝트의 FriendDeleteActivity를 Compose로 재구현.
 * - 친구 목록 표시 (체크박스 선택)
 * - 정렬 기능 (이름순, 로그인 시간순, 하트순)
 * - 선택한 친구 일괄 삭제
 */
@Composable
fun FriendDeleteScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendDeleteViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 다이얼로그 상태
    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf("") }

    // 삭제 확인 다이얼로그 상태
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    // 삭제 완료 다이얼로그 상태
    var showDeleteCompleteDialog by remember { mutableStateOf(false) }

    // 정렬 바텀시트 상태
    var showSortBottomSheet by remember { mutableStateOf(false) }

    // ProfileScreen overlay 상태
    var selectedFriend by remember { mutableStateOf<FriendModel?>(null) }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FriendDeleteContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is FriendDeleteContract.Effect.ShowDialog -> {
                    dialogTitle = effect.title
                    dialogMessage = effect.message
                    showDialog = true
                }
                is FriendDeleteContract.Effect.ShowDeleteConfirmDialog -> {
                    showDeleteConfirmDialog = true
                }
                is FriendDeleteContract.Effect.ShowDeleteCompleteDialog -> {
                    showDeleteCompleteDialog = true
                }
            }
        }
    }

    // 일반 다이얼로그
    if (showDialog) {
        ExoDialog(
            title = dialogTitle,
            message = dialogMessage,
            confirmButtonText = stringResource(R.string.confirm),
            onConfirm = { showDialog = false },
            onDismiss = { showDialog = false }
        )
    }

    // 삭제 확인 다이얼로그
    if (showDeleteConfirmDialog) {
        ExoConfirmDialog(
            title = "",
            message = stringResource(R.string.friends_delete_message),
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                viewModel.sendIntent(FriendDeleteContract.Intent.ConfirmDelete)
                showDeleteConfirmDialog = false
            },
            onDismiss = { showDeleteConfirmDialog = false }
        )
    }

    // 삭제 완료 다이얼로그
    if (showDeleteCompleteDialog) {
        ExoDialog(
            title = null,
            message = stringResource(R.string.tiele_friend_delete_result),
            confirmButtonText = stringResource(R.string.confirm),
            onConfirm = { showDeleteCompleteDialog = false },
            onDismiss = { showDeleteCompleteDialog = false }
        )
    }

    // 정렬 바텀시트
    if (showSortBottomSheet) {
        val sortItems = listOf(
            ExoBottomSheetItem(
                value = FriendDeleteContract.SortType.NAME,
                label = stringResource(R.string.order_by_name)
            ),
            ExoBottomSheetItem(
                value = FriendDeleteContract.SortType.LOGIN_TIME,
                label = stringResource(R.string.order_by_login_time)
            ),
            ExoBottomSheetItem(
                value = FriendDeleteContract.SortType.HEART,
                label = stringResource(R.string.order_by_heart)
            )
        )

        ExoBottomSheetList(
            items = sortItems,
            selectedValue = state.sortType,
            onItemSelected = { sortType ->
                viewModel.sendIntent(FriendDeleteContract.Intent.ChangeSortType(sortType))
            },
            onDismissRequest = { showSortBottomSheet = false }
        )
    }

    ExoScaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.title_unfriend),
                onNavigationClick = { navigator.popBackStack() }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 상단 필터 영역 (오른쪽 정렬)
            FilterHeader(
                sortType = state.sortType,
                onClick = { showSortBottomSheet = true }
            )

            // "친구 목록" 헤더 + 삭제 아이콘
            FriendListHeader(
                hasSelection = state.selectedIds.isNotEmpty(),
                onDeleteClick = {
                    viewModel.sendIntent(FriendDeleteContract.Intent.DeleteSelectedFriends)
                }
            )

            // 메인 컨텐츠
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading || state.isDeleting -> {
                        LoadingView()
                    }
                    state.friends.isEmpty() && !state.isLoading -> {
                        EmptyView()
                    }
                    else -> {
                        FriendDeleteList(
                            friends = state.friends,
                            selectedIds = state.selectedIds,
                            onToggleSelection = { userId ->
                                viewModel.sendIntent(FriendDeleteContract.Intent.ToggleSelection(userId))
                            },
                            onProfileClick = { friend ->
                                selectedFriend = friend
                            }
                        )
                    }
                }
            }
        }
    }

    // ProfileScreen overlay
    selectedFriend?.let { friend ->
        ProfileScreen(
            userId = friend.user.id,
            userNickname = friend.user.nickname,
            userImageUrl = friend.user.picture,
            userLevel = friend.user.level,
            mostIdolName = friend.user.most?.name,
            isMine = false,
            onBackClick = { selectedFriend = null }
        )
    }
}

/**
 * 로딩 뷰
 */
@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = colorResource(R.color.main))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.lable_get_info),
                style = ExoTypo.typo16.copy(color = colorResource(R.color.text_gray))
            )
        }
    }
}

/**
 * 빈 뷰
 */
@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.empty_friends),
            style = ExoTypo.typo16.copy(color = colorResource(R.color.text_gray)),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 상단 필터 헤더 (오른쪽 정렬)
 * old: button_wrapper - 정렬 필터가 오른쪽에 있음
 */
@Composable
private fun FilterHeader(
    sortType: FriendDeleteContract.SortType,
    onClick: () -> Unit
) {
    val sortText = when (sortType) {
        FriendDeleteContract.SortType.NAME -> stringResource(R.string.order_by_name)
        FriendDeleteContract.SortType.LOGIN_TIME -> stringResource(R.string.order_by_login_time)
        FriendDeleteContract.SortType.HEART -> stringResource(R.string.order_by_heart)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(colorResource(R.color.background_100))
            .padding(end = 14.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = sortText,
                style = ExoTypo.typo12.copy(color = colorResource(R.color.text_gray))
            )

            Icon(
                painter = painterResource(R.drawable.icon_arrow_drop_down),
                contentDescription = null,
                tint = colorResource(R.color.text_gray),
                modifier = Modifier.size(24.dp)
            )
        }
    }

    HorizontalDivider(
        thickness = 1.dp,
        color = colorResource(R.color.gray110)
    )
}

/**
 * "친구 목록" 헤더 + 삭제 아이콘
 * old: header - 왼쪽에 "친구 목록", 오른쪽에 삭제 버튼
 */
@Composable
private fun FriendListHeader(
    hasSelection: Boolean,
    onDeleteClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(colorResource(R.color.background_100))
            .padding(start = 10.dp, end = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.friend_section_title),
            style = ExoTypo.typo14.copy(color = colorResource(R.color.gray300))
        )

        Icon(
            painter = painterResource(R.drawable.btn_unfriend_off),
            contentDescription = "삭제",
            tint = if (hasSelection) colorResource(R.color.main) else colorResource(R.color.gray300),
            modifier = Modifier
                .size(20.dp)
                .clickable(
                    enabled = hasSelection,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDeleteClick() }
        )
    }

    HorizontalDivider(
        thickness = 1.dp,
        color = colorResource(R.color.gray110)
    )
}

/**
 * 친구 삭제 목록
 */
@Composable
private fun FriendDeleteList(
    friends: List<FriendDeleteContract.FriendDeleteItem>,
    selectedIds: Set<Int>,
    onToggleSelection: (Int) -> Unit,
    onProfileClick: (FriendModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100))
    ) {
        // 친구 아이템 목록
        items(friends, key = { it.friendModel.user.id }) { item ->
            FriendDeleteItem(
                item = item,
                isSelected = selectedIds.contains(item.friendModel.user.id),
                onToggleSelection = { onToggleSelection(item.friendModel.user.id) },
                onProfileClick = { onProfileClick(item.friendModel) }
            )
        }
    }
}

/**
 * 친구 삭제 아이템
 *
 * old 프로젝트: friend_delete_item.xml
 * - 프로필 이미지 아래에 하트 아이콘 + 숫자
 * - 오른쪽 끝에 체크박스
 */
@Composable
private fun FriendDeleteItem(
    item: FriendDeleteContract.FriendDeleteItem,
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current
    val user = item.friendModel.user

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.background_100))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onToggleSelection() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지 + 하트 (세로 배치)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onProfileClick() }
            ) {
                ExoProfileImage(
                    imageUrl = user.picture,
                    type = ProfileImageType.SMALL
                )

                Spacer(modifier = Modifier.height(3.dp))

                // 하트 아이콘 + 숫자 (프로필 이미지 아래)
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = R.drawable.img_unfriend_heart,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = item.giveHeart.toString(),
                        style = ExoTypo.typo11.copy(color = colorResource(R.color.gray580))
                    )
                }
            }

            Spacer(modifier = Modifier.width(11.dp))

            // 유저 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 레벨 + 닉네임
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelBadge(level = user.level)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "\u200E${user.nickname}",
                        style = ExoTypo.typo15.copy(
                            color = colorResource(R.color.main),
                            lineHeight = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // 최애
                val mostText = user.most?.let { most ->
                    context.getString(R.string.favorite_format, most.name)
                } ?: "${context.getString(R.string.most_favorite)} : ${context.getString(R.string.none)}"

                Text(
                    text = mostText,
                    style = ExoTypo.typo13.copy(
                        color = colorResource(R.color.gray580),
                        lineHeight = 17.sp
                    )
                )

                // 누적 투표
                val voteCountFormatted = NumberFormat.getNumberInstance(Locale.getDefault())
                    .format(user.levelHeart)
                Text(
                    text = stringResource(R.string.level_heart_format, voteCountFormatted),
                    style = ExoTypo.typo13.copy(
                        color = colorResource(R.color.gray580),
                        lineHeight = 17.sp
                    )
                )

                // 상태 메시지 (있을 경우)
                user.statusMessage?.takeIf { it.isNotEmpty() }?.let { statusMsg ->
                    Text(
                        text = statusMsg,
                        style = ExoTypo.typo13.copy(
                            color = colorResource(R.color.gray300),
                            lineHeight = 17.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 최종 로그인 날짜
                val lastLoginText = formatLastLogin(context, item.lastAct)
                Text(
                    text = "${context.getString(R.string.last_login)}:$lastLoginText",
                    style = ExoTypo.typo12.copy(
                        color = colorResource(R.color.gray580),
                        lineHeight = 17.sp
                    )
                )
            }

            // 체크박스 (오른쪽 끝)
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggleSelection() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colorResource(R.color.main),
                    uncheckedColor = colorResource(R.color.gray300)
                ),
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(20.dp)
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = colorResource(R.color.gray110)
        )
    }
}

/**
 * 최종 로그인 날짜 포맷팅
 */
private fun formatLastLogin(context: android.content.Context, lastAct: Date?): String {
    if (lastAct == null) return ""

    return try {
        val locale = Locale.getDefault()
        val formatter = DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
        val localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()
        val format = SimpleDateFormat(localPattern, locale)
        format.format(lastAct)
    } catch (e: Exception) {
        ""
    }
}

/**
 * 레벨 배지
 */
@Composable
private fun LevelBadge(level: Int) {
    val context = LocalContext.current
    val clampedLevel = level.coerceIn(0, 50)
    val resName = String.format(Locale.ENGLISH, "icon_level_%d", clampedLevel)
    val levelIconRes = context.resources.getIdentifier(resName, "drawable", context.packageName)
        .takeIf { it != 0 } ?: R.drawable.icon_level_0

    AsyncImage(
        model = levelIconRes,
        contentDescription = "Level $level",
        modifier = Modifier.height(16.dp)
    )
}
