package net.ib.mn.presentation.community.profile

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.presentation.community.profile.subpage.ProfileCommentPage
import net.ib.mn.presentation.community.profile.subpage.ProfilePhotoPage
import net.ib.mn.presentation.community.profile.subpage.ProfilePostPage
import net.ib.mn.ui.components.ExoNameWithGroupColor
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ColorPalette
import android.content.Intent
import android.net.Uri

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
 * @param isMine 본인 프로필 여부 (true면 댓글 탭 표시)
 * @param onBackClick 뒤로가기 클릭 이벤트
 * @param usersRepository UsersRepository 인스턴스
 */
@Composable
fun ProfileScreen(
    userId: Int,
    userNickname: String,
    userImageUrl: String? = null,
    userLevel: Int = 0,
    mostIdolName: String? = null,
    isMine: Boolean = false,
    onBackClick: () -> Unit = {},
    usersRepository: UsersRepository,
    userCacheRepository: UserCacheRepository
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // ViewModel 생성
    val viewModel: ProfileViewModel = viewModel(
        key = "user_profile_$userId",
        factory = ProfileViewModelFactory(
            context = context,
            usersRepository = usersRepository,
            userCacheRepository = userCacheRepository,
            userId = userId,
            userNickname = userNickname,
            userImageUrl = userImageUrl,
            userLevel = userLevel,
            mostIdolName = mostIdolName,
            isMine = isMine
        )
    )

    val uiState by viewModel.uiState.collectAsState()

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
                                    // TODO: 신고 기능
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

                        // 친구 추가 버튼
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    // TODO: 친구 추가 기능
                                },
                            contentAlignment = Alignment.Center
                        ) {
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
                            ProfileTab.PHOTO -> ProfilePhotoPage(userId = userId)
                            ProfileTab.ACTIVITY -> ProfilePostPage(userId = userId)
                            ProfileTab.COMMENT -> ProfileCommentPage(userId = userId)
                        }
                    }
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

                    Spacer(modifier = Modifier.height(4.dp))

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
