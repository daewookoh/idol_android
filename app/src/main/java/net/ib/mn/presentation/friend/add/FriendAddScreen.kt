package net.ib.mn.presentation.friend.add

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import net.ib.mn.domain.model.FriendUser
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.presentation.overlay.profile.ProfileScreen
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoDialog
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType
import java.text.NumberFormat
import java.util.Locale

/**
 * 뉴프렌즈 (새 친구 추가) 화면
 *
 * old 프로젝트의 NewFriendsActivity를 Compose로 재구현
 * - 뉴프렌즈 신청/취소 배너
 * - 뉴프렌즈 추천 목록
 * - 친구 요청 보내기
 */
@Composable
fun FriendAddScreen(
    modifier: Modifier = Modifier,
    viewModel: FriendAddViewModel = hiltViewModel()
) {
    val navigator = LocalAppNavigator.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf("") }

    // ProfileScreen overlay 상태
    var selectedUser by remember { mutableStateOf<FriendUser?>(null) }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is FriendAddContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is FriendAddContract.Effect.ShowDialog -> {
                    dialogTitle = effect.title
                    dialogMessage = effect.message
                    showDialog = true
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
                title = stringResource(R.string.new_friends),
                onNavigationClick = { navigator.popBackStack() }
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 뉴프렌즈 신청 배너
            NewFriendsBanner(
                isApplied = state.isNewFriendsApplied,
                onToggle = { viewModel.sendIntent(FriendAddContract.Intent.ToggleNewFriendsApply) }
            )

            // 구분선
            HorizontalDivider(
                thickness = 1.dp,
                color = colorResource(R.color.gray150)
            )

            // 메인 컨텐츠
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    state.isLoading -> {
                        LoadingView()
                    }
                    state.recommendedUsers.isEmpty() && !state.isLoading -> {
                        EmptyView()
                    }
                    else -> {
                        RecommendedUserList(
                            users = state.recommendedUsers,
                            myUserId = state.myUser?.id ?: 0,
                            sendingRequestIds = state.sendingRequestIds,
                            onSendRequest = { userId ->
                                viewModel.sendIntent(FriendAddContract.Intent.SendFriendRequest(userId))
                            },
                            onUserClick = { user ->
                                selectedUser = user
                            }
                        )
                    }
                }
            }
        }
    }

    // ProfileScreen overlay
    selectedUser?.let { user ->
        ProfileScreen(
            userId = user.id,
            userNickname = user.nickname,
            userImageUrl = user.picture,
            userLevel = user.level,
            mostIdolName = user.most?.name,
            isMine = false,
            onBackClick = { selectedUser = null }
        )
    }
}

/**
 * 뉴프렌즈 신청 배너
 * old 프로젝트: activity_new_friends.xml의 상단 배너
 * - 아이콘은 첫번째 텍스트 라인에 정렬
 */
@Composable
private fun NewFriendsBanner(
    isApplied: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_100))
            .padding(start = 40.dp, top = 30.dp, end = 10.dp, bottom = 30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 텍스트 영역 (아이콘 + 텍스트)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 첫번째 줄: 아이콘 + 뉴프렌즈 참여하기
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // NEW 아이콘 - Image 사용 (tint 적용 안됨)
                Image(
                    painter = painterResource(R.drawable.icon_new),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.participate_new_friends),
                    color = colorResource(R.color.gray900),
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // 두번째 줄: 설명 (아이콘 너비만큼 들여쓰기)
            Text(
                text = stringResource(R.string.participate_new_friends_desc),
                color = colorResource(R.color.gray580),
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 24.dp)
            )
        }

        // 신청/취소 버튼
        NewFriendsButton(
            isApplied = isApplied,
            onClick = onToggle
        )
    }
}

/**
 * 뉴프렌즈 신청/취소 버튼
 */
@Composable
private fun NewFriendsButton(
    isApplied: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isApplied) {
        colorResource(R.color.gray100)
    } else {
        colorResource(R.color.main)
    }

    val textColor = if (isApplied) {
        colorResource(R.color.gray580)
    } else {
        colorResource(R.color.text_white_black)
    }

    val text = if (isApplied) {
        stringResource(R.string.cancel_new_friends)
    } else {
        stringResource(R.string.apply_new_friends)
    }

    Box(
        modifier = Modifier
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(5.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 15.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
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
            text = stringResource(R.string.none_participation_new_friends),
            color = colorResource(R.color.text_gray),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 추천 유저 목록
 */
@Composable
private fun RecommendedUserList(
    users: List<FriendUser>,
    myUserId: Int,
    sendingRequestIds: Set<Int>,
    onSendRequest: (Int) -> Unit,
    onUserClick: (FriendUser) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100))
    ) {
        items(users, key = { it.id }) { user ->
            RecommendedUserItem(
                user = user,
                isMe = user.id == myUserId,
                isSendingRequest = sendingRequestIds.contains(user.id),
                onSendRequest = { onSendRequest(user.id) },
                onClick = { onUserClick(user) }
            )
        }
    }
}

/**
 * 추천 유저 아이템
 * old 프로젝트: item_new_friend.xml
 */
@Composable
private fun RecommendedUserItem(
    user: FriendUser,
    isMe: Boolean,
    isSendingRequest: Boolean,
    onSendRequest: () -> Unit,
    onClick: () -> Unit
) {
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
                imageUrl = user.picture,
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
                    LevelBadge(level = user.level)

                    Spacer(modifier = Modifier.width(2.dp))

                    Text(
                        text = "\u200E${user.nickname}",
                        color = colorResource(R.color.main),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 최애
                val mostText = user.most?.let { most ->
                    stringResource(R.string.favorite_format, most.name)
                } ?: "${stringResource(R.string.most_favorite)} : ${stringResource(R.string.none)}"

                Text(
                    text = mostText,
                    color = colorResource(R.color.gray580),
                    fontSize = 13.sp, lineHeight = 17.sp
                )

                // 누적 투표
                val voteCountFormatted = NumberFormat.getNumberInstance(Locale.getDefault())
                    .format(user.levelHeart)
                Text(
                    text = stringResource(R.string.level_heart_format, voteCountFormatted),
                    color = colorResource(R.color.gray580),
                    fontSize = 13.sp, lineHeight = 17.sp
                )

                // 소개글 (상태 메시지)
                user.statusMessage?.takeIf { it.isNotEmpty() }?.let { statusMsg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusMsg,
                        color = colorResource(R.color.gray300),
                        fontSize = 13.sp, lineHeight = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // 친구 추가 버튼 (내가 아닐 경우에만)
            if (!isMe) {
                if (isSendingRequest) {
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = colorResource(R.color.main)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSendRequest,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_add_friend_recommended),
                            contentDescription = "친구 추가",
                            tint = colorResource(R.color.main),
                            modifier = Modifier.size(20.dp)
                        )
                    }
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
