package net.ib.mn.presentation.community

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.R.color.main
import net.ib.mn.data.remote.dto.VoterTop100Model
import net.ib.mn.data.repository.UserCacheRepository
import net.ib.mn.data.repository.UsersRepository
import net.ib.mn.presentation.community.profile.ProfileScreen
import net.ib.mn.ui.components.ExoNameWithGroupColor
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.util.LocaleUtil
import java.text.NumberFormat
/**
 * VoterTop100Screen - 투표 Top100 유저 랭킹 화면
 *
 * old 프로젝트의 HeartVoteRankingActivity를 참고하여 Compose로 구현
 * 특정 아이돌에게 투표한 유저들의 Top100 랭킹 표시
 *
 * @param idolId 아이돌 ID
 * @param idolName 아이돌 이름
 * @param groupName 그룹 이름
 * @param onBackClick 뒤로가기 클릭 이벤트
 * @param usersRepository UsersRepository 인스턴스
 * @param userCacheRepository UserCacheRepository 인스턴스
 */
@Composable
fun VoterTop100Screen(
    idolId: Int,
    idolName: String,
    groupName: String,
    onBackClick: () -> Unit = {},
    usersRepository: UsersRepository,
    userCacheRepository: UserCacheRepository
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // ViewModel 생성
    val viewModel: VoterTop100ViewModel = viewModel(
        key = "voter_top100_$idolId",
        factory = VoterTop100ViewModelFactory(usersRepository, idolId, idolName, groupName)
    )

    val uiState by viewModel.uiState.collectAsState()

    val appLocale = remember { LocaleUtil.getAppLocale(context) }

    val numberFormat = remember(appLocale) {
        NumberFormat.getNumberInstance(appLocale)
    }

    // 선택된 유저 프로필 화면 표시 상태
    var selectedUser by remember { mutableStateOf<VoterTop100Model?>(null) }

    BackHandler {
        if (selectedUser != null) {
            selectedUser = null
        } else {
            onBackClick()
        }
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
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 뒤로가기 버튼
                    Box(
                        modifier = Modifier
                            .padding(start = 10.dp)
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

                    // 타이틀 - "Top 100 Votes - {아이돌이름}" 형식 (다국어 지원)
                    // Old: title_heart_vote_ranking__format = "%s Top 100 Votes"
                    // %s를 기준으로 split하여 뒷부분만 사용하고 " - " 추가
                    val titleFormat = stringResource(R.string.title_heart_vote_ranking__format)
                    val titlePrefix = titleFormat.split("%s").getOrElse(1) { " Top 100 Votes" }.trim() + " - "

                    // 가운데 정렬 + 하단(baseline) 정렬
                    Box(
                        // 1. Box가 내부의 Row 덩어리를 수직 중앙(Center)에 배치합니다.
                        contentAlignment = Alignment.CenterStart) {
                        Row(
                            // 2. [핵심] Row가 부모 높이를 꽉 채우지 않고, 텍스트 높이만큼만 차지하게 합니다.
                            modifier = Modifier.wrapContentHeight(),

                            // 3. verticalAlignment 속성은 제거합니다.
                            // (내부 자식들의 alignByBaseline()이 우선순위를 가지므로 불필요합니다)
                        ) {
                            // 앞부분 텍스트
                            Text(
                                text = titlePrefix,
                                fontSize = 18.sp,
                                lineHeight = 18.sp,
                                color = ColorPalette.textDefault,
                                maxLines = 1,
                                modifier = Modifier.alignByBaseline() // 텍스트끼리 줄 맞춤
                            )

                            Text(
                                text = idolName,
                                fontSize = 18.sp,
                                lineHeight = 18.sp,
                                modifier = Modifier.alignByBaseline()
                            )

                            if (groupName.isNotEmpty()) {
                                Text(
                                    text = " $groupName",
                                    fontSize = 10.sp,
                                    lineHeight = 18.sp,
                                    modifier = Modifier.alignByBaseline()
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        when (val state = uiState) {
            is VoterTop100UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = ColorPalette.main)
                }
            }

            is VoterTop100UiState.Error -> {
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

            is VoterTop100UiState.Success -> {
                if (state.items.isEmpty()) {
                    // 데이터 없음
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_heart_vote_ranking),
                            color = ColorPalette.textGray,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ColorPalette.background100),
                        state = listState
                    ) {
                        items(
                            items = state.items,
                            key = { it.id }
                        ) { item ->
                            VoterTop100UserItem(
                                item = item,
                                numberFormat = numberFormat,
                                onItemClick = { selectedUser = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // ProfileScreen (전체 화면으로 표시)
    AnimatedVisibility(
        visible = selectedUser != null,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it })
    ) {
        selectedUser?.let { user ->
            // most에서 언어별 이름 추출
            val mostIdolName = user.most?.let { most ->
                LocaleUtil.getLocalizedIdolName(context, most)
            }
            ProfileScreen(
                userId = user.id,
                userNickname = user.nickname,
                userImageUrl = user.imageUrl,
                userLevel = user.level,
                mostIdolName = mostIdolName,
                isMine = false,  // 타인의 프로필
                onBackClick = { selectedUser = null },
                usersRepository = usersRepository,
                userCacheRepository = userCacheRepository
            )
        }
    }
}

/**
 * VoterTop100UserItem - 유저 랭킹 아이템
 *
 * old 프로젝트의 VoteRankingAdapter를 참고
 */
@Composable
private fun VoterTop100UserItem(
    item: VoterTop100Model,
    numberFormat: NumberFormat,
    onItemClick: () -> Unit = {}
) {
    val context = LocalContext.current

    Column {
        // 구분선
        HorizontalDivider(
            thickness = 1.dp,
            color = ColorPalette.gray100
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onItemClick() }
                .padding(7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 순위 영역 (왕관 아이콘 + 순위 텍스트) - Old: width 45dp, height match_parent(wrap)
            Column(
                modifier = Modifier.width(45.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
            ) {
                // 1~3위 왕관 아이콘
                if (item.rank < 3) {
                    val crownIcon = when (item.rank) {
                        0 -> R.drawable.icon_rating_heart_voting_1st
                        1 -> R.drawable.icon_rating_heart_voting_2nd
                        2 -> R.drawable.icon_rating_heart_voting_3rd
                        else -> null
                    }
                    crownIcon?.let {
                        Icon(
                            painter = painterResource(it),
                            contentDescription = null,
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // 순위 텍스트 - rank_format 사용
                val rankText = stringResource(R.string.rank_format, (item.rank + 1).toString())
                Text(
                    text = rankText,
                    fontSize = 13.sp,
                    color = if (item.rank < 3) ColorPalette.main else ColorPalette.textGray,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }

            // 프로필 이미지 - ExoProfileImage XSMALL 사용 (35dp)
            ExoProfileImage(
                imageUrl = item.imageUrl,
                type = ProfileImageType.XSMALL,
                rank = item.rank,
                contentDescription = item.nickname
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 유저 정보 (이모티콘 + 레벨 + 닉네임 + 투표수)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                // 이모티콘 + 레벨 + 닉네임
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이모티콘
                    item.emoticon?.emojiUrl?.let { emojiUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(emojiUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 1.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // 레벨 아이콘
                    val levelIconRes = getLevelIconRes(item.level)
                    Icon(
                        painter = painterResource(levelIconRes),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier
                            .height(16.dp)
                            .padding(end = 2.dp)
                    )

                    // 닉네임
                    Text(
                        text = item.nickname,
                        fontSize = 13.sp,
                        color = ColorPalette.main,
                        style = TextStyle(
                            platformStyle = PlatformTextStyle(includeFontPadding = false)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                // 투표 수
                val voteCountText = numberFormat.format(item.levelHeart)
                Text(
                    text = stringResource(R.string.vote_count_format, voteCountText),
                    fontSize = 13.sp,
                    color = ColorPalette.textGray,
                    style = TextStyle(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    )
                )
            }
        }
    }
}

/**
 * 레벨에 따른 아이콘 리소스 반환
 * Old 프로젝트의 Util.getLevelResId() 참고
 * MAX_LEVEL = 40
 */
@Composable
private fun getLevelIconRes(level: Int): Int {
    val context = LocalContext.current
    val maxLevel = 40

    val clampedLevel = level.coerceIn(0, maxLevel)
    val resName = String.format(java.util.Locale.ENGLISH, "icon_level_%d", clampedLevel)

    val resId = context.resources.getIdentifier(resName, "drawable", context.packageName)
    return if (resId != 0) resId else R.drawable.icon_level_0
}

/**
 * VoterTop100ViewModel Factory
 */
class VoterTop100ViewModelFactory(
    private val usersRepository: UsersRepository,
    private val idolId: Int,
    private val idolName: String,
    private val groupName: String
) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        return VoterTop100ViewModel(usersRepository, idolId, idolName, groupName) as T
    }
}
