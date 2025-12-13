package net.ib.mn.presentation.themepick.live

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
import androidx.compose.foundation.layout.aspectRatio
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
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ThemePickIdol
import net.ib.mn.domain.model.ThemePickModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoTitleDialog
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.components.ThemePickRankingItem
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 테마픽 순위 결과 화면
 *
 * old 프로젝트의 ThemePickLiveActivity를 Compose로 재구현.
 *
 * 투표 후 현재 순위를 보여주는 화면.
 * - 헤더: 배너 이미지, 제목, 투표 정보 (총 투표수, 기간)
 * - 1위: 큰 이미지 레이아웃
 * - 2위 이하: 일반 랭킹 아이템
 * - 하단: 투표 버튼 (진행 중일 때)
 */
@Composable
fun ThemePickLiveScreen(
    themePickId: Int,
    modifier: Modifier = Modifier,
    viewModel: ThemePickLiveViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToVote: ((Int) -> Unit)? = null,
    onNavigateToCommunity: ((Int) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 안내 다이얼로그 상태
    var showInfoDialog by remember { mutableStateOf(false) }

    // 초기 데이터 로드
    LaunchedEffect(themePickId) {
        viewModel.sendIntent(ThemePickLiveContract.Intent.LoadResult(themePickId))
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ThemePickLiveContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ThemePickLiveContract.Effect.ShareThemePick -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
                is ThemePickLiveContract.Effect.NavigateToVote -> {
                    onNavigateToVote?.invoke(effect.themePickId)
                }
                is ThemePickLiveContract.Effect.NavigateToCommunity -> {
                    onNavigateToCommunity?.invoke(effect.idolId)
                }
                is ThemePickLiveContract.Effect.NavigateBack -> {
                    onBackClick()
                }
            }
        }
    }

    // 타이틀 결정 (종료됨이면 최종결과)
    val title = if (state.isFinished) {
        "${stringResource(R.string.themepick)} ${stringResource(R.string.lable_final_result)}"
    } else {
        stringResource(R.string.themepick)
    }

    // 안내 다이얼로그
    if (showInfoDialog) {
        val preferencesManager = remember { PreferencesManager(context, com.google.gson.Gson()) }
        val helpText = remember { preferencesManager.getHelpInfoThemePick() }

        ExoTitleDialog(
            title = stringResource(R.string.popup_title_themepick),
            message = helpText ?: "",
            onDismiss = { showInfoDialog = false }
        )
    }

    ExoScaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100)),
        topBar = {
            ExoAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // i 안내 아이콘
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
                                viewModel.sendIntent(ThemePickLiveContract.Intent.Share)
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
                state.themePick != null -> {
                    ResultContent(
                        state = state,
                        onRewardToggle = {
                            viewModel.sendIntent(ThemePickLiveContract.Intent.ToggleRewardExpand)
                        },
                        onVoteClick = {
                            viewModel.sendIntent(ThemePickLiveContract.Intent.GoToVote)
                        },
                        onItemClick = { idolId ->
                            viewModel.sendIntent(ThemePickLiveContract.Intent.OnItemClick(idolId))
                        }
                    )
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
            .background(colorResource(R.color.background_100)),
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
            .background(colorResource(R.color.background_100)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = error,
            color = colorResource(R.color.text_gray),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// Result Content
// ============================================================

@Composable
private fun ResultContent(
    state: ThemePickLiveContract.State,
    onRewardToggle: () -> Unit,
    onVoteClick: () -> Unit,
    onItemClick: (Int?) -> Unit
) {
    val themePick = state.themePick ?: return
    val rankItems = state.rankItems
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_200))
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 헤더 섹션 (배너 이미지)
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ThemePickBanner(
                    imageUrl = state.secureImageUrl
                )
            }

            // 제목
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = themePick.title,
                        color = if (state.isFinished) {
                            colorResource(R.color.gray300)
                        } else {
                            colorResource(R.color.main_light)
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 정보 섹션 (전체 투표수, 기간)
            item {
                ResultInfoSection(
                    periodText = state.periodText,
                    totalVote = themePick.count,
                    numberFormat = numberFormat
                )
            }

            // 1위 리워드
            item {
                RewardSection(
                    themePick = themePick,
                    isExpanded = state.isRewardExpanded,
                    onToggle = onRewardToggle
                )
            }

            // 순위 목록
            if (rankItems.isEmpty()) {
                item {
                    EmptyView()
                }
            } else {
                itemsIndexed(rankItems, key = { _, candidate -> candidate.id }) { index, candidate ->
                    val rankingItem = candidate.toRankingItem(
                        totalVote = themePick.count,
                        firstPlaceVote = themePick.getFirstPlaceVote()
                    )

                    ThemePickRankingItem(
                        item = rankingItem,
                        isFirstItem = index == 0,
                        isImagePick = false,
                        isLive = true,
                        selectedItemId = state.voteId.toString(),
                        onClick = { onItemClick(candidate.idolId) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 하단 투표 버튼 (진행 중이고 종료되지 않은 경우만)
        if (!state.isFinished) {
            VoteButton(
                canVote = state.canVote,
                needsVideoAd = state.needsVideoAd,
                hasVotedToday = state.hasVotedToday,
                onVoteClick = onVoteClick
            )
        }
    }
}

/**
 * 공통 배너 이미지
 * Old 프로젝트: layout_constraintDimensionRatio="3.3:1" (너비:높이)
 */
@Composable
private fun ThemePickBanner(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .aspectRatio(3.3f / 1f)
            .clip(RoundedCornerShape(13.dp))
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun ResultInfoSection(
    periodText: String,
    totalVote: Int,
    numberFormat: NumberFormat
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        // 전체 투표수
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.themepick_total_votes),
                color = colorResource(R.color.text_gray),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${numberFormat.format(totalVote)}${stringResource(R.string.votes)}",
                color = colorResource(R.color.text_default),
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 기간
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.onepick_period),
                color = colorResource(R.color.text_gray),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = periodText,
                color = colorResource(R.color.text_default),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun EmptyView() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.onepick_no_votes),
            color = colorResource(R.color.text_gray),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * 1위 보상 섹션
 */
@Composable
private fun RewardSection(
    themePick: ThemePickModel,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    if (themePick.prize == null) return

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
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
                    color = colorResource(R.color.text_default),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
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
                    themePick.prize.name?.let { prizeName ->
                        Text(
                            text = prizeName,
                            color = colorResource(R.color.text_default),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    themePick.prize.location?.let { location ->
                        Text(
                            text = location,
                            color = colorResource(R.color.text_gray),
                            fontSize = 13.sp
                        )
                    }
                    themePick.prize.imageUrl?.let { imageUrl ->
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

@Composable
private fun VoteButton(
    canVote: Boolean,
    needsVideoAd: Boolean,
    hasVotedToday: Boolean,
    onVoteClick: () -> Unit
) {
    val isEnabled = !hasVotedToday

    val btnColor = when {
        hasVotedToday -> colorResource(R.color.gray110)
        else -> colorResource(R.color.main_light)
    }

    val btnText = when {
        hasVotedToday -> stringResource(R.string.themepick_today_voted)   // "오늘 투표 완료"
        needsVideoAd -> stringResource(R.string.themepick_vote_again)     // "다시 투표"
        else -> stringResource(R.string.guide_vote_title)                 // "투표하기"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_100))
    ) {
        // 섹션 구분선
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(colorResource(R.color.gray150))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 투표 버튼
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .height(40.dp)
                .background(color = btnColor, shape = RoundedCornerShape(8.dp))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    enabled = isEnabled
                ) { onVoteClick() }
        ) {
            Text(
                text = btnText,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ============================================================
// ThemePickIdol -> RankingItem 변환 확장 함수
// ============================================================

/**
 * ThemePickIdol을 RankingItem으로 변환
 */
private fun ThemePickIdol.toRankingItem(
    totalVote: Int,
    firstPlaceVote: Long
): RankingItem {
    val fullName = if (subtitle.isNotEmpty()) {
        "${title}_${subtitle}"
    } else {
        title
    }

    val percentage = if (totalVote > 0) {
        ((100.0f * vote.toFloat() / totalVote.toFloat())).roundToInt()
    } else 0

    return RankingItem(
        id = id.toString(),
        name = fullName,
        rank = rank,
        voteCount = vote.toString(),
        heartCount = vote,
        maxHeartCount = firstPlaceVote,
        photoUrl = imageUrl,
        percentage = percentage,
        isFavorite = false
    )
}

