package net.ib.mn.presentation.friend

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import net.ib.mn.R
import net.ib.mn.domain.model.FriendModel
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen
import net.ib.mn.presentation.overlay.friendinvite.FriendInviteScreen
import net.ib.mn.presentation.overlay.profile.ProfileScreen
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoDialog
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType
import java.text.NumberFormat
import java.util.Locale

/**
 * 친구 화면
 *
 * old 프로젝트의 FriendsActivity를 Compose로 재구현
 * - 친구 목록
 * - 친구 요청자 알림
 * - 하트 보내기/받기
 * - 친구 초대 배너
 *
 * Navigation 3 활용:
 * - LocalAppNavigator를 통해 네비게이션 직접 처리
 * - ViewModel에서 네비게이션 Intent/Effect 제거
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 툴팁 표시 여부를 SharedPreferences에서 읽어옴
    val tooltipPrefs = remember { context.getSharedPreferences("friend_tooltip", Context.MODE_PRIVATE) }

    // 하트 쿨타임 SharedPreferences
    val heartPrefs = remember { context.getSharedPreferences("heart", Context.MODE_PRIVATE) }

    // 타이머 갱신을 위한 tick (1초마다 증가)
    var timerTick by remember { mutableStateOf(0L) }

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showBannerTooltip by remember {
        mutableStateOf(!tooltipPrefs.getBoolean("banner_tooltip_dismissed", false))
    }
    var showHeartTooltip by remember {
        mutableStateOf(!tooltipPrefs.getBoolean("heart_tooltip_dismissed", false))
    }

    // FriendInviteScreen overlay 상태
    var showFriendInviteScreen by remember { mutableStateOf(false) }
    var inviteToken by remember { mutableStateOf("") }
    var inviteLanguage by remember { mutableStateOf("") }

    // ProfileScreen overlay 상태
    var selectedFriend by remember { mutableStateOf<FriendModel?>(null) }

    // 화면이 다시 보일 때마다 데이터 리로드 (FriendRequestScreen에서 돌아올 때 등)
    LifecycleResumeEffect(Unit) {
        viewModel.sendIntent(FriendContract.Intent.LoadFriends)
        onPauseOrDispose { }
    }

    // 1초마다 타이머 갱신 (old 프로젝트의 mRefreshTimer와 동일)
    LaunchedEffect(state.friends) {
        if (state.friends.isNotEmpty()) {
            while (isActive) {
                delay(1000L)
                timerTick++
            }
        }
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FriendContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is FriendContract.Effect.ShowDialog -> {
                    dialogTitle = effect.title
                    dialogMessage = effect.message
                    showDialog = true
                }
                is FriendContract.Effect.NavigateToInvite -> {
                    inviteToken = effect.token
                    inviteLanguage = effect.language
                    showFriendInviteScreen = true
                }
            }
        }
    }

    // 다이얼로그
    if (showDialog) {
        ExoDialog(
            title = dialogTitle,
            message = dialogMessage,
            confirmButtonText = stringResource(R.string.confirm),
            onConfirm = { showDialog = false },
            onDismiss = { showDialog = false }
        )
    }

    ExoScaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.friend),
                onNavigationClick = { navigator.popBackStack() },
                actions = {
                    // 뉴프렌즈 (새 친구 추가)
                    IconButton(
                        onClick = { navigator.navigate(Screen.FriendAdd) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_navigation_friend_add),
                            contentDescription = "뉴프렌즈",
                            tint = colorResource(R.color.text_default)
                        )
                    }
                    // 친구 신청 관리
                    IconButton(
                        onClick = { navigator.navigate(Screen.FriendRequest()) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_navigation_friend_waiting),
                            contentDescription = "친구 신청",
                            tint = colorResource(R.color.text_default)
                        )
                    }
                    // 친구 삭제
                    IconButton(
                        onClick = { navigator.navigate(Screen.FriendDelete) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_navigation_friend_delete),
                            contentDescription = "친구 삭제",
                            tint = colorResource(R.color.text_default)
                        )
                    }
                }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 검색창
            SearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { /* TODO: 검색 */ },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 0.dp)
            )

            // 메인 컨텐츠 (친구 목록)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> {
                        LoadingView()
                    }
                    state.friends.isEmpty() && !state.isLoading -> {
                        EmptyView()
                    }
                    else -> {
                        FriendListContent(
                            friends = state.friends,
                            requesters = state.requesters,
                            sendingHeartIds = state.sendingHeartIds,
                            heartPrefs = heartPrefs,
                            timerTick = timerTick,
                            onSendHeart = { friend ->
                                viewModel.sendIntent(FriendContract.Intent.SendHeart(friend.user.id, friend.user.nickname))
                            },
                            onSendHeartToAll = {
                                viewModel.sendIntent(FriendContract.Intent.SendHeartToAll)
                            },
                            onReceiveHeart = {
                                viewModel.sendIntent(FriendContract.Intent.ReceiveHeart)
                            },
                            onFriendClick = { friend ->
                                selectedFriend = friend
                            },
                            onRequestClick = {
                                navigator.navigate(Screen.FriendRequest(initialTab = 0))
                            }
                        )
                    }
                }

                // HeartTooltip을 LazyColumn 외부에 배치하여 모든 아이템 위에 표시
                if (showHeartTooltip && state.friends.isNotEmpty()) {
                    HeartTooltip(
                        text = stringResource(R.string.label_tooltip_friend_heart),
                        onDismiss = {
                            showHeartTooltip = false
                            tooltipPrefs.edit().putBoolean("heart_tooltip_dismissed", true).apply()
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 68.dp)
                            .offset(y = if (state.requesters.isNotEmpty()) 130.dp else 60.dp)
                    )
                }
            }

            // 하단 초대 배너 + 툴팁
            // old 프로젝트와 동일: 툴팁이 배너 바로 위에 붙음 (constraintBottom_toTopOf="@id/ll_invitation")
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                InviteBanner(
                    onClick = { viewModel.sendIntent(FriendContract.Intent.Invite) }
                )

                // 툴팁 (배너 위에 표시, 가운데 정렬)
                if (showBannerTooltip) {
                    BannerTooltip(
                        text = stringResource(R.string.friend_tooltip_invite_friend),
                        onDismiss = {
                            showBannerTooltip = false
                            tooltipPrefs.edit().putBoolean("banner_tooltip_dismissed", true).apply()
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .offset(y = (-40).dp)
                    )
                }
            }
        }
    }

    // FriendInviteScreen overlay
    if (showFriendInviteScreen) {
        FriendInviteScreen(
            token = inviteToken,
            language = inviteLanguage,
            onBackClick = { showFriendInviteScreen = false }
        )
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
 * 검색창
 */
@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(
                color = colorResource(R.color.background_300),
                shape = RoundedCornerShape(9.dp)
            )
            .padding(start = 20.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = colorResource(R.color.text_default),
                    fontSize = 12.sp
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        onSearch()
                        keyboardController?.hide()
                    }
                ),
                cursorBrush = SolidColor(colorResource(R.color.main)),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "",
                            color = colorResource(R.color.text_dimmed),
                            fontSize = 12.sp
                        )
                    }
                    innerTextField()
                }
            )

            IconButton(
                onClick = {
                    onSearch()
                    keyboardController?.hide()
                },
                modifier = Modifier.size(38.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_navigation_search),
                    contentDescription = "검색",
                    tint = colorResource(R.color.text_gray)
                )
            }
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
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = colorResource(R.color.main)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.lable_get_info),
                color = colorResource(R.color.text_gray),
                fontSize = 16.sp
            )
        }
    }
}

/**
 * 친구 없음 뷰
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
            text = stringResource(R.string.nofriend1),
            color = colorResource(R.color.text_gray),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 친구 목록 컨텐츠
 */
@Composable
private fun FriendListContent(
    friends: List<FriendModel>,
    requesters: List<FriendModel>,
    sendingHeartIds: Set<Int>,
    heartPrefs: SharedPreferences,
    timerTick: Long,
    onSendHeart: (FriendModel) -> Unit,
    onSendHeartToAll: () -> Unit,
    onReceiveHeart: () -> Unit,
    onFriendClick: (FriendModel) -> Unit,
    onRequestClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100))
    ) {
        // 친구 요청 알림
        if (requesters.isNotEmpty()) {
            item {
                FriendRequestNotice(
                    count = requesters.size,
                    onClick = onRequestClick
                )
            }
        }

        // 친구 목록 헤더 (친구 수 + 하트 버튼)
        item {
            FriendListHeader(
                friendCount = friends.size,
                heartPrefs = heartPrefs,
                timerTick = timerTick,
                onReceiveHeart = onReceiveHeart,
                onSendHeartToAll = onSendHeartToAll
            )

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 10.dp),
                thickness = 1.dp,
                color = colorResource(R.color.gray100)
            )
        }

        // 친구 목록
        items(friends, key = { it.user.id }) { friend ->
            FriendItem(
                friend = friend,
                isSendingHeart = sendingHeartIds.contains(friend.user.id),
                heartPrefs = heartPrefs,
                timerTick = timerTick,
                onSendHeart = { onSendHeart(friend) },
                onClick = { onFriendClick(friend) }
            )
        }
    }
}

/**
 * 친구 요청 알림 (배지 형태)
 */
@Composable
private fun FriendRequestNotice(
    count: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(top = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 친구 요청 아이콘 (old: variant_icon_friend_request -> icon_friend_request)
            AsyncImage(
                model = R.drawable.icon_friend_request,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(7.dp))
            // 요청 수
            Text(
                text = "$count",
                color = colorResource(R.color.main),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = stringResource(R.string.notice_new_friend_request),
            color = colorResource(R.color.gray580),
            fontSize = 12.sp
        )
    }
}

/**
 * 친구 목록 헤더 (친구 수 표시 + 하트 버튼)
 * old 프로젝트: 전체 하트 보내기 버튼 옆에 쿨타임 타이머 표시
 */
@Composable
private fun FriendListHeader(
    friendCount: Int,
    heartPrefs: SharedPreferences,
    timerTick: Long,
    onReceiveHeart: () -> Unit,
    onSendHeartToAll: () -> Unit
) {
    // timerTick을 사용하여 매초 recomposition 트리거
    @Suppress("UNUSED_EXPRESSION")
    timerTick

    // 전체 하트 보내기 쿨타임 체크
    val lastSendAllTime = heartPrefs.getLong("send_heart_all", -1)
    val currentTime = System.currentTimeMillis()
    val cooldownMs = 10 * 60 * 1000L  // 10분
    val expireTime = lastSendAllTime + cooldownMs
    val showSendAllTimer = lastSendAllTime > 0 && currentTime < expireTime

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_100))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 친구 목록 (N)
        Text(
            text = stringResource(R.string.friend_section_title) + " ($friendCount)",
            color = colorResource(R.color.gray300),
            fontSize = 13.sp
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 하트 받기 버튼
            IconButton(
                onClick = onReceiveHeart,
                modifier = Modifier.size(50.dp)
            ) {
                AsyncImage(
                    model = R.drawable.btn_take_heart,
                    contentDescription = "하트 받기",
                    modifier = Modifier.size(50.dp)
                )
            }

            // 전체 하트 보내기 버튼 또는 타이머
            if (showSendAllTimer) {
                // 타이머 표시 (old: sectionWaitTimer)
                val remainingSeconds = (expireTime - currentTime) / 1000
                val timerText = String.format(
                    java.util.Locale.getDefault(),
                    "%d:%02d",
                    remainingSeconds / 60,
                    remainingSeconds % 60
                )
                Box(
                    modifier = Modifier.size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timerText,
                        color = colorResource(R.color.main),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                IconButton(
                    onClick = onSendHeartToAll,
                    modifier = Modifier.size(50.dp)
                ) {
                    AsyncImage(
                        model = R.drawable.btn_give_heart_all_off,
                        contentDescription = "전체 하트 보내기",
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }
    }
}

/**
 * 상단 툴팁 (화살표가 위쪽, 어두운 배경)
 * 친구 목록 헤더의 하트 받기 버튼 아래에 표시
 */
@Composable
private fun HeartTooltip(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // 위쪽 화살표 (180도 회전)
        Icon(
            painter = painterResource(R.drawable.img_tooltop_bottom),
            contentDescription = null,
            tint = colorResource(R.color.text_default),
            modifier = Modifier
                .padding(end = 20.dp)
                .rotate(180f)
                .offset(y = (8).dp)
        )

        // 말풍선 본체
        Box(
            modifier = Modifier
                .offset(y = (-10).dp)
                .background(
                    color = colorResource(R.color.text_default),
                    shape = RoundedCornerShape(5.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
                .padding(horizontal = 8.dp, vertical = 1.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    color = colorResource(R.color.text_white_black),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_btn_close_24px),
                    contentDescription = "닫기",
                    tint = colorResource(R.color.text_dimmed),
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

/**
 * 하단 툴팁 (화살표가 아래쪽, 밝은 배경)
 * 배너 위에 표시
 */
@Composable
private fun BannerTooltip(
    text: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 말풍선 본체
        Box(
            modifier = Modifier
                .background(
                    color = colorResource(R.color.text_default),
                    shape = RoundedCornerShape(20.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onDismiss() }
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    color = colorResource(R.color.text_white_black),
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_btn_close_24px),
                    contentDescription = "닫기",
                    tint = colorResource(R.color.text_dimmed),
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        // 아래쪽 화살표
        Icon(
            painter = painterResource(R.drawable.img_tooltop_bottom),
            contentDescription = null,
            tint = colorResource(R.color.text_default),
            modifier = Modifier.offset(y = (-2).dp)
        )
    }
}

/**
 * 친구 아이템
 * old 프로젝트: 하트 보내기 버튼 옆에 쿨타임 타이머 표시
 */
@Composable
private fun FriendItem(
    friend: FriendModel,
    isSendingHeart: Boolean,
    heartPrefs: SharedPreferences,
    timerTick: Long,
    onSendHeart: () -> Unit,
    onClick: () -> Unit
) {
    // timerTick을 사용하여 매초 recomposition 트리거
    @Suppress("UNUSED_EXPRESSION")
    timerTick

    // 개별 하트 쿨타임 체크
    val lastSentTime = heartPrefs.getLong("send_heart_${friend.user.id}", -1)
    val currentTime = System.currentTimeMillis()
    val cooldownMs = 10 * 60 * 1000L  // 10분
    val expireTime = lastSentTime + cooldownMs
    val showTimer = lastSentTime > 0 && currentTime < expireTime

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onClick() }
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 이미지
            ExoProfileImage(
                imageUrl = friend.user.picture,
                type = ProfileImageType.SMALL
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 유저 정보
            Column(modifier = Modifier.weight(1f)) {
                // 레벨 배지 + 닉네임
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 레벨 배지 이미지
                    LevelBadge(level = friend.user.level)

                    Spacer(modifier = Modifier.width(2.dp))

                    Text(
                        text = "\u200E${friend.user.nickname}",
                        color = colorResource(R.color.main),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                // 최애 : 아이돌명_그룹명
                val mostText = friend.user.most?.let { most ->
                    stringResource(R.string.favorite_format, most.name)
                } ?: "${stringResource(R.string.most_favorite)} : ${stringResource(R.string.none)}"

                Text(
                    text = mostText,
                    color = colorResource(R.color.gray580),
                    fontSize = 13.sp, lineHeight = 17.sp
                )

                // 누적 투표
                val voteCountFormatted = NumberFormat.getNumberInstance(Locale.getDefault())
                    .format(friend.user.levelHeart)
                Text(
                    text = stringResource(R.string.level_heart_format, voteCountFormatted),
                    color = colorResource(R.color.gray580),
                    fontSize = 13.sp, lineHeight = 17.sp
                )

                // 소개글 (상태 메시지)
                friend.user.statusMessage?.takeIf { it.isNotEmpty() }?.let { statusMsg ->
                    Text(
                        text = statusMsg,
                        color = colorResource(R.color.gray300),
                        fontSize = 13.sp, lineHeight = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 하트 보내기 버튼 또는 타이머
            if (showTimer) {
                // 타이머 표시 (old: waitTimer)
                val remainingSeconds = (expireTime - currentTime) / 1000
                val timerText = String.format(
                    java.util.Locale.getDefault(),
                    "%d:%02d",
                    remainingSeconds / 60,
                    remainingSeconds % 60
                )
                Box(
                    modifier = Modifier.size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = timerText,
                        color = colorResource(R.color.main),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else if (isSendingHeart) {
                Box(
                    modifier = Modifier.size(50.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = colorResource(R.color.main)
                    )
                }
            } else {
                IconButton(
                    onClick = onSendHeart,
                    modifier = Modifier.size(50.dp)
                ) {
                    AsyncImage(
                        model = R.drawable.btn_give_heart_off,
                        contentDescription = "Send Heart",
                        modifier = Modifier.size(50.dp)
                    )
                }
            }
        }

        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 10.dp),
            thickness = 1.dp,
            color = colorResource(R.color.gray100)
        )
    }
}

/**
 * 레벨 배지
 * old 프로젝트와 동일하게 level 값을 그대로 icon_level_{level}로 매핑
 */
@Composable
private fun LevelBadge(level: Int) {
    val context = LocalContext.current
    // old 프로젝트: MAX_LEVEL 이상이면 MAX_LEVEL로 제한
    val clampedLevel = level.coerceIn(0, 50) // MAX_LEVEL = 50
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
 * 초대 배너
 * old 프로젝트와 동일한 그라데이션 배경: #FFFCED -> #FFDEF1 (45도)
 */
@Composable
private fun InviteBanner(
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFFCED),
                        Color(0xFFFFDEF1)
                    ),
                    start = Offset(0f, Float.POSITIVE_INFINITY),
                    end = Offset(Float.POSITIVE_INFINITY, 0f)
                )
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(end = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 배너 이미지 (비율 444:240 = 1.85:1, 높이 80dp일 때 너비 약 148dp)
        AsyncImage(
            model = R.drawable.img_invitefriend_bnr,
            contentDescription = null,
            modifier = Modifier
                .width(148.dp)
                .height(80.dp),
            contentScale = ContentScale.FillHeight
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 배너 텍스트 (줄바꿈 포함)
        Text(
            text = stringResource(R.string.friend_bnr_invite_friend_title),
            color = colorResource(R.color.pink_500),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
            maxLines = 2
        )

        // 화살표
        Icon(
            painter = painterResource(R.drawable.btn_go),
            contentDescription = null,
            tint = colorResource(R.color.pink_400),
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InviteBannerPreview() {
    InviteBanner(onClick = {})
}
