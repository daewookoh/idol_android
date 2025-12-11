package net.ib.mn.presentation.friend.request

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.flow.collect
import net.ib.mn.R
import net.ib.mn.domain.model.FriendModel
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.presentation.overlay.profile.ProfileScreen
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoDialog
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType

/**
 * 친구 신청 관리 화면
 *
 * old 프로젝트의 FriendsRequestActivity를 Compose로 재구현.
 * - Tab 0: 받은 친구 요청 (AcceptFriendsRequestFragment)
 * - Tab 1: 보낸 친구 요청 (CancelFriendsRequestFragment)
 *
 * @param initialTab 초기 탭 인덱스 (0: 받은 요청, 1: 보낸 요청)
 */
@Composable
fun FriendRequestScreen(
    modifier: Modifier = Modifier,
    initialTab: Int = 1,
    viewModel: FriendRequestViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf("") }

    // 확인 다이얼로그 상태
    var showConfirmDialog by remember { mutableStateOf(false) }
    var confirmDialogTitle by remember { mutableStateOf<String?>(null) }
    var confirmDialogMessage by remember { mutableStateOf("") }
    var confirmDialogAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // ProfileScreen overlay 상태
    var selectedFriend by remember { mutableStateOf<FriendModel?>(null) }

    // 초기 탭 설정
    LaunchedEffect(initialTab) {
        viewModel.sendIntent(FriendRequestContract.Intent.SelectTab(initialTab))
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is FriendRequestContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is FriendRequestContract.Effect.ShowDialog -> {
                    dialogTitle = effect.title
                    dialogMessage = effect.message
                    showDialog = true
                }
                is FriendRequestContract.Effect.FinishWithResult -> {
                    // 결과 코드는 Navigation Compose에서 직접 처리 어려우므로 생략
                    // 필요시 SharedViewModel 또는 savedStateHandle 사용
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

    // 확인 다이얼로그 (수락/거절 전)
    if (showConfirmDialog) {
        ExoConfirmDialog(
            title = confirmDialogTitle ?: "",
            message = confirmDialogMessage,
            confirmButtonText = stringResource(R.string.yes),
            dismissButtonText = stringResource(R.string.no),
            onConfirm = {
                confirmDialogAction?.invoke()
                showConfirmDialog = false
                confirmDialogAction = null
            },
            onDismiss = {
                showConfirmDialog = false
                confirmDialogAction = null
            }
        )
    }

    ExoScaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.friends_request),
                onNavigationClick = { navigator.popBackStack() }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Layout (old: TabLayout with ViewPager)
            FriendRequestTabRow(
                selectedTabIndex = state.selectedTab,
                onTabSelected = { viewModel.sendIntent(FriendRequestContract.Intent.SelectTab(it)) }
            )

            // Tab Content
            when {
                state.isLoading -> {
                    LoadingView()
                }
                else -> {
                    when (state.selectedTab) {
                        0 -> ReceivedRequestContent(
                            requesters = state.requesters,
                            onAcceptAll = {
                                confirmDialogTitle = context.getString(R.string.title_accept_friend_request)
                                confirmDialogMessage = context.getString(R.string.desc_accept_friend_request)
                                confirmDialogAction = {
                                    viewModel.sendIntent(FriendRequestContract.Intent.AcceptAllRequests)
                                }
                                showConfirmDialog = true
                            },
                            onDecline = { userId ->
                                confirmDialogTitle = context.getString(R.string.title_decline_friend_request)
                                confirmDialogMessage = context.getString(R.string.desc_decline_friend_request)
                                confirmDialogAction = {
                                    viewModel.sendIntent(FriendRequestContract.Intent.DeclineRequest(userId))
                                }
                                showConfirmDialog = true
                            },
                            onProfileClick = { friend ->
                                selectedFriend = friend
                            }
                        )
                        1 -> SentRequestContent(
                            waiters = state.waiters,
                            onCancel = { userId ->
                                viewModel.sendIntent(FriendRequestContract.Intent.CancelRequest(userId))
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
 * Tab Row
 */
@Composable
private fun FriendRequestTabRow(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    val tabTitles = listOf(
        stringResource(R.string.title_accept_friend_request),
        stringResource(R.string.cancel_friend_request)
    )

    TabRow(
        selectedTabIndex = selectedTabIndex,
        containerColor = colorResource(R.color.background_100),
        contentColor = colorResource(R.color.main),
        indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
                modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                height = 3.dp,
                color = colorResource(R.color.main)
            )
        }
    ) {
        tabTitles.forEachIndexed { index, title ->
            Tab(
                selected = selectedTabIndex == index,
                onClick = { onTabSelected(index) },
                text = {
                    Text(
                        text = title,
                        color = if (selectedTabIndex == index) {
                            colorResource(R.color.main)
                        } else {
                            colorResource(R.color.gray150)
                        },
                        fontSize = 14.sp
                    )
                }
            )
        }
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
        CircularProgressIndicator(color = colorResource(R.color.main))
    }
}

/**
 * 빈 상태 뷰
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
            text = stringResource(R.string.empty_friend_request),
            color = colorResource(R.color.text_gray),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 받은 친구 요청 탭 (AcceptFriendsRequestFragment)
 */
@Composable
private fun ReceivedRequestContent(
    requesters: List<FriendModel>,
    onAcceptAll: () -> Unit,
    onDecline: (Int) -> Unit,
    onProfileClick: (FriendModel) -> Unit
) {
    if (requesters.isEmpty()) {
        EmptyView()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.background_100))
        ) {
            itemsIndexed(requesters, key = { _, item -> item.user.id }) { index, friend ->
                ReceivedRequestItem(
                    friend = friend,
                    isFirst = index == 0,
                    onAcceptAll = onAcceptAll,
                    onDecline = { onDecline(friend.user.id) },
                    onProfileClick = { onProfileClick(friend) }
                )
            }
        }
    }
}

/**
 * 받은 친구 요청 아이템 (item_received_request_friend.xml)
 */
@Composable
private fun ReceivedRequestItem(
    friend: FriendModel,
    isFirst: Boolean,
    onAcceptAll: () -> Unit,
    onDecline: () -> Unit,
    onProfileClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 첫 번째 아이템에만 "전체 수락" 버튼 표시 (old: requesterSection)
        if (isFirst) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorResource(R.color.background_100))
                    .padding(10.dp)
            ) {
                FriendRequestButton(
                    text = stringResource(R.string.btn_accept),
                    onClick = onAcceptAll,
                    modifier = Modifier.align(Alignment.CenterEnd)
                )
            }
            HorizontalDivider(
                thickness = 1.dp,
                color = colorResource(R.color.gray110)
            )
        }

        // 유저 정보 행
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(colorResource(R.color.background_100))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onProfileClick() }
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지
            ExoProfileImage(
                imageUrl = friend.user.picture,
                type = ProfileImageType.XSMALL
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 유저 정보 (레벨 + 닉네임)
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LevelBadge(level = friend.user.level)
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "\u200E${friend.user.nickname}",
                    color = colorResource(R.color.main),
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // 거절 버튼
            FriendRequestButton(
                text = stringResource(R.string.btn_decline),
                onClick = onDecline
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = colorResource(R.color.gray110)
        )
    }
}

/**
 * 보낸 친구 요청 탭 (CancelFriendsRequestFragment)
 */
@Composable
private fun SentRequestContent(
    waiters: List<FriendModel>,
    onCancel: (Int) -> Unit,
    onProfileClick: (FriendModel) -> Unit
) {
    if (waiters.isEmpty()) {
        EmptyView()
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(colorResource(R.color.background_100))
        ) {
            itemsIndexed(waiters, key = { _, item -> item.user.id }) { _, friend ->
                SentRequestItem(
                    friend = friend,
                    onCancel = { onCancel(friend.user.id) },
                    onProfileClick = { onProfileClick(friend) }
                )
            }
        }
    }
}

/**
 * 보낸 친구 요청 아이템 (item_sent_request_friend.xml)
 */
@Composable
private fun SentRequestItem(
    friend: FriendModel,
    onCancel: () -> Unit,
    onProfileClick: () -> Unit
) {
    val context = LocalContext.current

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
                ) { onProfileClick() }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지
            ExoProfileImage(
                imageUrl = friend.user.picture,
                type = ProfileImageType.SMALL
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 유저 정보
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 레벨 + 닉네임
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LevelBadge(level = friend.user.level)
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "\u200E${friend.user.nickname}",
                        color = colorResource(R.color.main),
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // 최애 정보
                val mostText = friend.user.most?.let { most ->
                    context.getString(R.string.favorite_format, most.name)
                } ?: "${context.getString(R.string.most_favorite)} : ${context.getString(R.string.none)}"

                Text(
                    text = mostText,
                    color = colorResource(R.color.gray580),
                    fontSize = 13.sp
                )
            }

            // 취소 버튼
            FriendRequestButton(
                text = stringResource(R.string.btn_cancel),
                onClick = onCancel
            )
        }

        HorizontalDivider(
            thickness = 1.dp,
            color = colorResource(R.color.gray110)
        )
    }
}

/**
 * 레벨 배지
 */
@Composable
private fun LevelBadge(level: Int) {
    val context = LocalContext.current
    val clampedLevel = level.coerceIn(0, 50)
    val resName = String.format(java.util.Locale.ENGLISH, "icon_level_%d", clampedLevel)
    val levelIconRes = context.resources.getIdentifier(resName, "drawable", context.packageName)
        .takeIf { it != 0 } ?: R.drawable.icon_level_0

    AsyncImage(
        model = levelIconRes,
        contentDescription = "Level $level",
        modifier = Modifier.height(16.dp)
    )
}

/**
 * 공통 버튼 스타일 (bg_btn_round_brand500 스타일: 투명 배경 + main 색상 테두리)
 */
@Composable
private fun FriendRequestButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .border(
                width = 1.dp,
                color = colorResource(R.color.main),
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 15.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = colorResource(R.color.main),
            fontSize = 14.sp
        )
    }
}
