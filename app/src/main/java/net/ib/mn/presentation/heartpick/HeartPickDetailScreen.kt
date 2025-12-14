package net.ib.mn.presentation.heartpick

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import net.ib.mn.domain.model.HeartPickIdol
import net.ib.mn.domain.model.HeartPickModel
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoNameWithGroup
import net.ib.mn.ui.components.ExoProfileImage
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoHeartPickVoteDialog
import net.ib.mn.ui.components.ExoTitleDialog
import net.ib.mn.ui.components.HeartPickVoteCompleteBottomSheet
import net.ib.mn.ui.components.HeartPickDetailRankingItem
import net.ib.mn.ui.components.ProfileImageType
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.theme.ExoTypo
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 하트픽 상세 화면 (통합)
 *
 * old 프로젝트의 HeartPickActivity, HeartPickPrelaunchActivity를 Compose로 재구현.
 *
 * 세 가지 상태를 모두 대응:
 * - PRELAUNCH: 투표 예정 - 아이돌 목록만 표시, 알림 설정 버튼
 * - VOTING: 투표 중 - 순위 + 투표 버튼
 * - VOTE_FINISHED: 투표 종료 - 최종 결과 표시
 */
@Composable
fun HeartPickDetailScreen(
    heartPickId: Int,
    modifier: Modifier = Modifier,
    viewModel: HeartPickDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToFeed: ((Int, String?) -> Unit)? = null,
    onShowImageDetail: ((String) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 투표 다이얼로그 상태
    var showVoteDialog by remember { mutableStateOf(false) }
    var selectedIdol by remember { mutableStateOf<HeartPickIdol?>(null) }

    // 투표 완료 바텀시트 상태
    var showVoteCompleteSheet by remember { mutableStateOf(false) }
    var voteCompleteBonusHeart by remember { mutableStateOf(0) }
    var voteCompleteVoted by remember { mutableStateOf(0L) }
    var voteCompleteIdolName by remember { mutableStateOf("") }

    // 안내 다이얼로그 상태
    var showInfoDialog by remember { mutableStateOf(false) }

    // 댓글 화면 상태
    var showCommentScreen by remember { mutableStateOf(false) }
    var commentHeartPickId by remember { mutableStateOf(0) }

    // 초기 데이터 로드
    LaunchedEffect(heartPickId) {
        viewModel.sendIntent(HeartPickDetailContract.Intent.LoadHeartPick(heartPickId))
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is HeartPickDetailContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is HeartPickDetailContract.Effect.ShareHeartPick -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
                is HeartPickDetailContract.Effect.NavigateToCommunity -> {
                    // TODO: CommunityScreen 연동
                }
                is HeartPickDetailContract.Effect.NavigateToComment -> {
                    // CommentOnlyScreen 열기
                    commentHeartPickId = effect.heartPickId
                    showCommentScreen = true
                }
                is HeartPickDetailContract.Effect.ShowVoteDialog -> {
                    selectedIdol = effect.idol
                    showVoteDialog = true
                }
                is HeartPickDetailContract.Effect.ShowNotifyEnabledToast -> {
                    Toast.makeText(context, context.getString(R.string.vote_alert_after), Toast.LENGTH_SHORT).show()
                }
                is HeartPickDetailContract.Effect.NavigateBack -> {
                    onBackClick()
                }
                is HeartPickDetailContract.Effect.VoteSuccess -> {
                    showVoteDialog = false
                    // 투표 완료 바텀시트 표시
                    voteCompleteBonusHeart = effect.bonusHeart
                    voteCompleteVoted = effect.voted
                    voteCompleteIdolName = selectedIdol?.let { idol ->
                        if (idol.subtitle.isNotEmpty()) "${idol.title}_${idol.subtitle}" else idol.title
                    } ?: ""
                    selectedIdol = null
                    showVoteCompleteSheet = true
                }
                is HeartPickDetailContract.Effect.VoteError -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 댓글 화면 (전체 화면 오버레이)
    if (showCommentScreen) {
        net.ib.mn.presentation.comment.CommentOnlyScreen(
            heartPickId = commentHeartPickId,
            onBackClick = { showCommentScreen = false },
            onNavigateToFeed = onNavigateToFeed,
            onShowImageDetail = onShowImageDetail,
            onCommentSubmitted = { _, _ ->
                // 댓글 작성 완료 시 댓글 수 1 증가
                viewModel.sendIntent(HeartPickDetailContract.Intent.IncrementCommentCount)
            },
            onCommentDeleted = {
                // 댓글 삭제 완료 시 댓글 수 1 감소
                viewModel.sendIntent(HeartPickDetailContract.Intent.DecrementCommentCount)
            }
        )
        return // 댓글 화면이 열려있으면 아래 내용 렌더링하지 않음
    }

    // 타이틀 결정
    val title = when (state.status) {
        HeartPickDetailContract.HeartPickStatus.PRELAUNCH -> stringResource(R.string.vote_preview)
        HeartPickDetailContract.HeartPickStatus.VOTE_FINISHED -> stringResource(R.string.result_heartpick)
        HeartPickDetailContract.HeartPickStatus.VOTING -> stringResource(R.string.heartpick)
    }

    // 투표 다이얼로그가 열릴 때 유저 하트 로드
    LaunchedEffect(showVoteDialog) {
        if (showVoteDialog) {
            viewModel.sendIntent(HeartPickDetailContract.Intent.LoadUserHearts)
        }
    }

    // 투표 다이얼로그 (하트픽 전용)
    if (showVoteDialog && selectedIdol != null) {
        val idol = selectedIdol!!
        val fullName = if (idol.subtitle.isNotEmpty()) {
            "${idol.title}_${idol.subtitle}"
        } else {
            idol.title
        }
        ExoHeartPickVoteDialog(
            heartPickIdolId = idol.id,  // HeartPickIdol.id (하트픽 아이돌 ID)
            idolId = idol.idolId,
            fullName = fullName,
            totalHeart = state.totalHeart,
            freeHeart = state.freeHeart,
            isVoting = state.isVoting,
            onVote = { heartPickIdolId, idolId, heart ->
                // 투표 API 호출
                viewModel.sendIntent(
                    HeartPickDetailContract.Intent.VoteHeartPick(
                        heartPickIdolId = heartPickIdolId,
                        idolId = idolId,
                        heart = heart
                    )
                )
            },
            onDismiss = {
                showVoteDialog = false
                selectedIdol = null
            }
        )
    }

    // 투표 완료 바텀시트
    if (showVoteCompleteSheet) {
        HeartPickVoteCompleteBottomSheet(
            voteCount = voteCompleteVoted,
            bonusHeart = voteCompleteBonusHeart,
            idolName = voteCompleteIdolName,
            onShare = {
                viewModel.sendIntent(HeartPickDetailContract.Intent.Share)
            },
            onDismiss = {
                showVoteCompleteSheet = false
            }
        )
    }

    // 안내 다이얼로그 (old: DefaultDialog with popup_title_heartpick)
    if (showInfoDialog) {
        val preferencesManager = remember { PreferencesManager(context, com.google.gson.Gson()) }
        val helpText = remember { preferencesManager.getHelpInfoHeartPick() }

        ExoTitleDialog(
            title = stringResource(R.string.popup_title_heartpick),
            message = helpText ?: "",
            onDismiss = { showInfoDialog = false }
        )
    }

    ExoScaffold(
        modifier = modifier,
        topBar = {
            ExoAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = ExoTypo.typo20Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // i 안내 아이콘 - 타이틀 바로 우측
                        Icon(
                            painter = painterResource(R.drawable.icon_info),
                            contentDescription = "Info",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(16.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    showInfoDialog = true
                                }
                        )
                    }
                },
                onNavigationClick = onBackClick,
                actions = {
                    // 공유 아이콘
                    Icon(
                        painter = painterResource(R.drawable.btn_navigation_share),
                        contentDescription = "Share",
                        tint = colorResource(R.color.text_default),
                        modifier = Modifier
                            .padding(end = 10.dp)
                            .size(24.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                viewModel.sendIntent(HeartPickDetailContract.Intent.Share)
                            }
                    )
                }
            )
        }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading -> LoadingView()
                state.error != null -> ErrorView(error = state.error!!)
                state.heartPick != null -> {
                    when (state.status) {
                        HeartPickDetailContract.HeartPickStatus.PRELAUNCH -> {
                            PrelaunchContent(
                                state = state,
                                onIdolClick = { idolId ->
                                    viewModel.sendIntent(HeartPickDetailContract.Intent.GoToCommunity(idolId))
                                },
                                onCommentClick = {
                                    viewModel.sendIntent(HeartPickDetailContract.Intent.GoToComment)
                                },
                                onNotifyClick = {
                                    viewModel.sendIntent(HeartPickDetailContract.Intent.ToggleNotification)
                                },
                                onRewardToggle = {
                                    viewModel.sendIntent(HeartPickDetailContract.Intent.ToggleRewardExpand)
                                }
                            )
                        }
                        else -> {
                            VotingOrFinishedContent(
                                state = state,
                                onIdolClick = { idolId ->
                                    viewModel.sendIntent(HeartPickDetailContract.Intent.GoToCommunity(idolId))
                                },
                                onCommentClick = {
                                    viewModel.sendIntent(HeartPickDetailContract.Intent.GoToComment)
                                },
                                onVoteClick = { idol ->
                                    viewModel.sendIntent(HeartPickDetailContract.Intent.Vote(idol))
                                },
                                onRewardToggle = {
                                    viewModel.sendIntent(HeartPickDetailContract.Intent.ToggleRewardExpand)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================
// Common Components
// ============================================================

@Composable
private fun LoadingView() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_200)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = colorResource(R.color.main))
    }
}

@Composable
private fun ErrorView(error: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_200)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = error,
            style = ExoTypo.typo16.copy(color = colorResource(R.color.text_gray)),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 공통 배너
 */
@Composable
private fun HeartPickBanner(
    bannerUrl: String?,
    title: String,
    subtitle: String,
    dDayText: String,
    showOverlay: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
    ) {
        AsyncImage(
            model = bannerUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
            )
        }

        // D-Day 뱃지
        Row(
            modifier = Modifier
                .padding(start = 20.dp, top = 16.dp)
                .height(20.dp)
                .background(
                    color = Color(0xE6202020),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_heartpick_timer),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = dDayText,
                style = ExoTypo.typo12.copy(color = Color.White, lineHeight = 20.sp)
            )
        }

        // 제목 + 부제목
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 25.dp)
        ) {
            Text(
                text = title,
                style = ExoTypo.typo19.copy(color = Color.White),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = ExoTypo.typo13.copy(color = Color.White),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ============================================================
// PRELAUNCH (투표 예정) Content
// ============================================================

@Composable
private fun PrelaunchContent(
    state: HeartPickDetailContract.State,
    onIdolClick: (Int) -> Unit,
    onCommentClick: () -> Unit,
    onNotifyClick: () -> Unit,
    onRewardToggle: () -> Unit
) {
    val heartPick = state.heartPick ?: return
    val idols = heartPick.heartPickIdols ?: emptyList()
    val mostIdolId = state.myIdol?.idolId

    // 나의 최애 아이돌을 맨 위로
    val reorderedIdols = remember(idols, mostIdolId) {
        if (mostIdolId != null) {
            val (matched, others) = idols.partition { it.idolId == mostIdolId }
            matched + others
        } else {
            idols
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_200))
    ) {
        // 배너 (secureUrl 적용)
        item {
            HeartPickBanner(
                bannerUrl = state.secureBannerUrl,
                title = heartPick.title,
                subtitle = heartPick.subtitle,
                dDayText = state.dDayText
            )
        }

        // 기간 + 댓글 + 리워드 + 알림버튼
        item {
            PrelaunchInfoSection(
                heartPick = heartPick,
                periodText = state.periodText,
                isNotifyEnabled = state.isNotifyEnabled,
                isRewardExpanded = state.isRewardExpanded,
                onCommentClick = onCommentClick,
                onNotifyClick = onNotifyClick,
                onRewardToggle = onRewardToggle
            )
        }

        // 아이돌 목록 (순위 없이)
        items(reorderedIdols, key = { it.id }) { idol ->
            PrelaunchIdolItem(
                idol = idol,
                isMost = idol.idolId == mostIdolId,
                onClick = { onIdolClick(idol.idolId) }
            )
        }
    }
}

@Composable
private fun PrelaunchInfoSection(
    heartPick: HeartPickModel,
    periodText: String,
    isNotifyEnabled: Boolean,
    isRewardExpanded: Boolean,
    onCommentClick: () -> Unit,
    onNotifyClick: () -> Unit,
    onRewardToggle: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_200))
            .padding(20.dp)
    ) {
        // 기간
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.kdf_period_title),
                style = ExoTypo.typo14.copy(color = colorResource(R.color.text_gray))
            )
            Spacer(modifier = Modifier.width(11.dp))
            Text(
                text = periodText,
                style = ExoTypo.typo13.copy(color = colorResource(R.color.text_gray))
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 응원 댓글
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.cheering_comments),
                style = ExoTypo.typo14.copy(color = colorResource(R.color.text_gray))
            )
            Spacer(modifier = Modifier.width(6.dp))
            Row(
                modifier = Modifier
                    .background(
                        color = colorResource(R.color.main200),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onCommentClick() }
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (heartPick.numComments > 0) {
                        stringResource(R.string.view_number_of_comments, heartPick.numComments)
                    } else {
                        stringResource(R.string.title_comment)
                    },
                    style = ExoTypo.typo13.copy(color = colorResource(R.color.main_light))
                )
                Spacer(modifier = Modifier.width(3.dp))
                Icon(
                    painter = painterResource(R.drawable.icon_arrow_right),
                    contentDescription = null,
                    tint = colorResource(R.color.main_light),
                    modifier = Modifier.size(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(17.dp))

        // 1등 리워드
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.gray80),
                    shape = RoundedCornerShape(15.dp)
                )
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { onRewardToggle() }
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.icon_heartpick_reward),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(9.dp))
                    Text(
                        text = stringResource(R.string.first_rank_reward),
                        style = ExoTypo.typo15Bold.copy(color = colorResource(R.color.text_default)),
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        painter = painterResource(R.drawable.icon_arrow_drop_down),
                        contentDescription = null,
                        tint = colorResource(R.color.text_gray),
                        modifier = Modifier.size(22.dp)
                    )
                }

                AnimatedVisibility(
                    visible = isRewardExpanded,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        heartPick.prize?.name?.let { prizeName ->
                            Text(
                                text = prizeName,
                                style = ExoTypo.typo14Bold.copy(color = colorResource(R.color.text_default))
                            )
                        }
                        heartPick.prize?.location?.let { location ->
                            Text(
                                text = location,
                                style = ExoTypo.typo13.copy(color = colorResource(R.color.text_gray))
                            )
                        }
                        heartPick.prize?.imageUrl?.let { imageUrl ->
                            Spacer(modifier = Modifier.height(8.dp))
                            AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(133.dp)
                                    .clip(RoundedCornerShape(15.dp))
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 알림 설정 버튼
        val btnColor = if (!isNotifyEnabled) {
            colorResource(R.color.main_light)
        } else {
            colorResource(R.color.gray110)
        }
        val btnText = if (!isNotifyEnabled) {
            stringResource(R.string.vote_alert_before)
        } else {
            stringResource(R.string.vote_alert_after)
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .background(color = btnColor, shape = CircleShape)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = !isNotifyEnabled
                ) { onNotifyClick() }
        ) {
            Text(
                text = btnText,
                style = ExoTypo.typo16Bold.copy(color = colorResource(R.color.text_white_black)),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PrelaunchIdolItem(
    idol: HeartPickIdol,
    isMost: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isMost) {
        colorResource(R.color.main100)
    } else {
        colorResource(R.color.background_200)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 26.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExoProfileImage(
            imageUrl = idol.imageUrl,
            type = ProfileImageType.MEDIUM,
            contentDescription = idol.title
        )

        Spacer(modifier = Modifier.width(9.dp))

        // 이름_그룹명 형식으로 ExoNameWithGroup 사용
        val fullName = if (idol.subtitle.isNotEmpty()) {
            "${idol.title}_${idol.subtitle}"
        } else {
            idol.title
        }
        ExoNameWithGroup(
            fullName = fullName,
            nameFontSize = 14.sp,
            groupFontSize = 10.sp
        )
    }
}

// ============================================================
// VOTING / VOTE_FINISHED Content
// ============================================================

@Composable
private fun VotingOrFinishedContent(
    state: HeartPickDetailContract.State,
    onIdolClick: (Int) -> Unit,
    onCommentClick: () -> Unit,
    onVoteClick: (HeartPickIdol) -> Unit,
    onRewardToggle: () -> Unit
) {
    val heartPick = state.heartPick ?: return
    val idols = heartPick.heartPickIdols ?: emptyList()
    val isVoting = state.status == HeartPickDetailContract.HeartPickStatus.VOTING
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(colorResource(R.color.background_200))
        ) {
            // 배너 (secureUrl 적용)
            item {
                ResultBanner(
                    bannerUrl = state.secureBannerUrl,
                    title = heartPick.title,
                    subtitle = heartPick.subtitle,
                    dDayText = state.dDayText
                )
            }

            // 정보 섹션 (전체 투표수, 투표 기간, 응원 댓글)
            item {
                ResultInfoSection(
                    totalVote = heartPick.vote,
                    periodText = state.periodText,
                    numComments = heartPick.numComments,
                    onCommentClick = onCommentClick
                )
            }

            // 1위 보상 섹션
            item {
                ResultRewardSection(
                    heartPick = heartPick,
                    isExpanded = state.isRewardExpanded,
                    onToggle = onRewardToggle
                )
            }

            // 아이돌 순위 목록 - HeartPickDetailRankingItem 사용
            itemsIndexed(idols, key = { _, idol -> idol.id }) { index, idol ->
                val rankingItem = idol.toRankingItem(
                    rank = idol.rank,  // RankingUtil에서 계산된 동점자 순위 사용
                    totalVote = heartPick.vote,
                    firstPlaceVote = heartPick.getFirstPlaceVote(),
                    isFavorite = state.myIdol?.idolId == idol.idolId
                )

                // 2위, 3위에 툴팁 표시 (투표 종료가 아닐 때, 실제 순위 기준)
                val showTooltipForItem = state.showTooltip &&
                    state.status != HeartPickDetailContract.HeartPickStatus.VOTE_FINISHED &&
                    (idol.rank == 2 || idol.rank == 3) &&
                    idol.diffVote > 0

                Box {
                    HeartPickDetailRankingItem(
                        item = rankingItem,
                        isFavorite = state.myIdol?.idolId == idol.idolId,
                        showVoteButton = isVoting,
                        isFirstItem = index == 0,  // 첫번째 아이템 여부
                        onClick = { onIdolClick(idol.idolId) },
                        onVoteClick = { onVoteClick(idol) }
                    )

                    // 득표차 툴팁 (우측 상단)
                    if (showTooltipForItem) {
                        VoteDiffTooltip(
                            rankDiff = idol.rank - 1,
                            voteDiff = idol.diffVote,
                            numberFormat = numberFormat,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 10.dp)
                        )
                    }
                }
            }
        }

        // 나의 최애 아이돌 (1등이 아닐 경우만 표시)
        if (state.myIdol != null && state.myIdolPosition > 0) {
            val myIdol = idols.find { it.idolId == state.myIdol.idolId }
            val myIdolRankingItem = state.myIdol.toRankingItem(
                rank = state.myIdol.rank,
                totalVote = heartPick.vote,
                firstPlaceVote = heartPick.getFirstPlaceVote(),
                isFavorite = true
            )
            HeartPickDetailRankingItem(
                item = myIdolRankingItem,
                isFavorite = true,
                showVoteButton = isVoting,
                onClick = { onIdolClick(state.myIdol.idolId) },
                onVoteClick = { myIdol?.let { onVoteClick(it) } }
            )
        }
    }
}

/**
 * 득표차 툴팁 (2위, 3위 아이템 위에 표시)
 * old 프로젝트: item_heart_pick_rank.xml의 cl_tool_tip
 */
@Composable
private fun VoteDiffTooltip(
    rankDiff: Int,
    voteDiff: Int,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(
            R.string.heartpick_tooltip_msg,
            numberFormat.format(rankDiff),
            numberFormat.format(voteDiff)
        ),
        style = ExoTypo.typo10.copy(color = Color.White),
        modifier = modifier
            .background(
                color = Color(0xFF333333),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

/**
 * 결과 화면 배너
 */
@Composable
private fun ResultBanner(
    bannerUrl: String?,
    title: String,
    subtitle: String,
    dDayText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
    ) {
        // 배너 이미지
        AsyncImage(
            model = bannerUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 하단 그라데이션 오버레이
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xCC000000)
                        ),
                        startY = 100f
                    )
                )
        )

        // 상태 뱃지 (투표 종료 / D-Day)
        Row(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp)
                .height(20.dp)
                .background(
                    color = Color(0xE6202020),
                    shape = RoundedCornerShape(10.dp)
                )
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.icon_heartpick_timer),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = dDayText,
                style = ExoTypo.typo12.copy(color = Color.White, lineHeight = 20.sp)
            )
        }

        // 제목 + 부제목 (하단)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Text(
                text = title,
                style = ExoTypo.typo19.copy(color = Color.White),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = ExoTypo.typo14.copy(color = Color(0xCCFFFFFF)),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 정보 섹션 (전체 투표수, 투표 기간, 응원 댓글)
 */
@Composable
private fun ResultInfoSection(
    totalVote: Int,
    periodText: String,
    numComments: Int,
    onCommentClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_200))
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // 전체 투표수
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.themepick_total_votes),
                style = ExoTypo.typo14.copy(color = colorResource(R.color.text_gray))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.num_participants_format, numberFormat.format(totalVote)),
                style = ExoTypo.typo14.copy(color = colorResource(R.color.text_default))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 투표 기간
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.kdf_period_title),
                style = ExoTypo.typo14.copy(color = colorResource(R.color.text_gray))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = periodText,
                style = ExoTypo.typo14.copy(color = colorResource(R.color.text_default))
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 응원 댓글
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.cheering_comments),
                style = ExoTypo.typo14.copy(color = colorResource(R.color.text_gray))
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .background(
                        color = colorResource(R.color.main200),
                        shape = RoundedCornerShape(6.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onCommentClick() }
                    .padding(horizontal = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (numComments > 0) {
                        stringResource(R.string.view_number_of_comments, numComments)
                    } else {
                        stringResource(R.string.title_comment)
                    },
                    style = ExoTypo.typo13.copy(color = colorResource(R.color.main_light))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.icon_arrow_right),
                    contentDescription = null,
                    tint = colorResource(R.color.main_light),
                    modifier = Modifier.size(8.dp)
                )
            }
        }
    }
}

/**
 * 1위 보상 섹션
 */
@Composable
private fun ResultRewardSection(
    heartPick: HeartPickModel,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .background(
                color = colorResource(R.color.gray80),
                shape = RoundedCornerShape(15.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onToggle() }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.icon_heartpick_reward),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(9.dp))
                Text(
                    text = stringResource(R.string.first_rank_reward),
                    style = ExoTypo.typo15Bold.copy(color = colorResource(R.color.text_default)),
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.icon_arrow_drop_down),
                    contentDescription = null,
                    tint = colorResource(R.color.text_gray),
                    modifier = Modifier.size(22.dp)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    heartPick.prize?.name?.let { prizeName ->
                        Text(
                            text = prizeName,
                            style = ExoTypo.typo14Bold.copy(color = colorResource(R.color.text_default))
                        )
                    }
                    heartPick.prize?.location?.let { location ->
                        Text(
                            text = location,
                            style = ExoTypo.typo13.copy(color = colorResource(R.color.text_gray))
                        )
                    }
                    heartPick.prize?.imageUrl?.let { imageUrl ->
                        Spacer(modifier = Modifier.height(8.dp))
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(133.dp)
                                .clip(RoundedCornerShape(15.dp))
                        )
                    }
                }
            }
        }
    }
}

// ============================================================
// HeartPickIdol -> RankingItem 변환 확장 함수
// ============================================================

/**
 * HeartPickIdol을 RankingItem으로 변환
 *
 * @param rank 순위
 * @param totalVote 전체 투표수 (퍼센트 계산용)
 * @param firstPlaceVote 1위 투표수 (프로그레스바 계산용)
 * @param isFavorite 나의 최애 여부
 */
private fun HeartPickIdol.toRankingItem(
    rank: Int,
    totalVote: Int,
    firstPlaceVote: Int,
    isFavorite: Boolean = false
): RankingItem {
    // 이름 구성: title_subtitle (ExoNameWithGroup에서 _로 분리)
    val fullName = if (subtitle.isNotEmpty()) {
        "${title}_${subtitle}"
    } else {
        title
    }

    // 퍼센트 계산
    val percentage = if (totalVote > 0) {
        ((100.0f * vote.toFloat() / totalVote.toFloat())).roundToInt()
    } else 0

    return RankingItem(
        id = idolId.toString(),
        name = fullName,
        rank = rank,
        voteCount = vote.toString(),
        heartCount = vote.toLong(),
        maxHeartCount = firstPlaceVote.toLong(),
        photoUrl = imageUrl,
        percentage = percentage,
        isFavorite = isFavorite
    )
}
