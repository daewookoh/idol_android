package net.ib.mn.presentation.imagepick.live

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import net.ib.mn.ad.RewardAdManager
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ImagePickIdolModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoLoading
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoSimpleDialog
import net.ib.mn.ui.components.ExoTitleDialog
import net.ib.mn.ui.components.ImagePickRankingItem
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 이미지픽 순위 결과 화면
 *
 * old 프로젝트의 OnepickResultActivity를 Compose로 재구현.
 *
 * 투표 후 현재 순위를 보여주는 화면.
 * - 헤더: 제목, 투표 정보 (총 투표수, 기간)
 * - 1위: 큰 이미지 레이아웃
 * - 2위 이하: 일반 랭킹 아이템
 * - 하단: 투표 버튼 (진행 중일 때)
 */
@Composable
fun ImagePickLiveScreen(
    imagePickId: Int,
    modifier: Modifier = Modifier,
    viewModel: ImagePickLiveViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToVote: ((Int) -> Unit)? = null,
    onNavigateToCommunity: ((Int) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 안내 다이얼로그 상태
    var showInfoDialog by remember { mutableStateOf(false) }

    // 광고 에러 다이얼로그 상태
    var showAdErrorDialog by remember { mutableStateOf(false) }

    // 광고 로딩 상태
    var isAdLoading by remember { mutableStateOf(false) }

    // RewardAdManager
    val rewardAdManager = remember { RewardAdManager.getInstance() }

    // Activity 참조 (광고 표시용)
    val activity = LocalContext.current as? android.app.Activity

    // 초기 데이터 로드
    LaunchedEffect(imagePickId) {
        viewModel.sendIntent(ImagePickLiveContract.Intent.LoadResult(imagePickId))
    }

    // Lifecycle 관찰 - 화면 복귀 시 조용히 새로고침
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            // ON_RESUME이고 이미 데이터가 있을 때만 새로고침
            if (event == Lifecycle.Event.ON_RESUME && state.imagePick != null) {
                viewModel.refreshSilently(imagePickId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ImagePickLiveContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ImagePickLiveContract.Effect.ShareImagePick -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
                is ImagePickLiveContract.Effect.NavigateToVote -> {
                    onNavigateToVote?.invoke(effect.imagePickId)
                }
                is ImagePickLiveContract.Effect.NavigateToCommunity -> {
                    onNavigateToCommunity?.invoke(effect.idolId)
                }
                is ImagePickLiveContract.Effect.NavigateBack -> {
                    onBackClick()
                }
                is ImagePickLiveContract.Effect.ShowVideoAd -> {
                    if (activity != null && !isAdLoading) {
                        isAdLoading = true
                        rewardAdManager.loadAd(
                            context = context,
                            onLoaded = {
                                rewardAdManager.showAd(
                                    activity = activity,
                                    onRewarded = {
                                        isAdLoading = false
                                        viewModel.voteAfterAd()
                                    },
                                    onFailed = { _ ->
                                        isAdLoading = false
                                        showAdErrorDialog = true
                                    },
                                    onDismissed = {
                                        isAdLoading = false
                                    }
                                )
                            },
                            onFailed = { _ ->
                                isAdLoading = false
                                showAdErrorDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // 광고 에러 다이얼로그
    if (showAdErrorDialog) {
        ExoSimpleDialog(
            message = stringResource(R.string.video_ad_unable),
            onDismiss = { showAdErrorDialog = false }
        )
    }

    // 타이틀 결정 (종료됨이면 최종결과)
    val title = if (state.isFinished) {
        "${stringResource(R.string.imagepick)} ${stringResource(R.string.lable_final_result)}"
    } else {
        stringResource(R.string.imagepick)
    }

    // 안내 다이얼로그
    if (showInfoDialog) {
        val preferencesManager = remember { PreferencesManager(context, com.google.gson.Gson()) }
        val helpText = remember { preferencesManager.getHelpInfoOnePick() }

        ExoTitleDialog(
            title = stringResource(R.string.popup_title_imagepick),
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
                            style = ExoTypo.typo20Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // i 안내 아이콘
                        Icon(
                            painter = painterResource(R.drawable.icon_info),
                            contentDescription = "Info",
                            tint = androidx.compose.ui.graphics.Color.Unspecified,
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
                                viewModel.sendIntent(ImagePickLiveContract.Intent.Share)
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
                state.imagePick != null -> {
                    ResultContent(
                        state = state,
                        onVoteClick = {
                            viewModel.sendIntent(ImagePickLiveContract.Intent.GoToVote)
                        },
                        onItemClick = { idolId ->
                            viewModel.sendIntent(ImagePickLiveContract.Intent.OnItemClick(idolId))
                        }
                    )
                }
            }

            // 광고 로딩 중 오버레이
            ExoLoading(isLoading = isAdLoading)
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
            style = ExoTypo.typo16.copy(color = colorResource(R.color.text_gray)),
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// Result Content
// ============================================================

@Composable
private fun ResultContent(
    state: ImagePickLiveContract.State,
    onVoteClick: () -> Unit,
    onItemClick: (Int?) -> Unit
) {
    val imagePick = state.imagePick ?: return
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
            // 헤더 섹션 (타이틀, 참여인원, 투표기간)
            item {
                ImagePickLiveHeader(
                    title = imagePick.title,
                    participantsCount = numberFormat.format(imagePick.count),
                    periodText = state.periodText,
                    isFinished = state.isFinished
                )
            }

            // 순위 목록
            if (rankItems.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.onepick_no_votes),
                            style = ExoTypo.typo14.copy(color = colorResource(R.color.text_gray)),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // 1위 득표수
                val firstPlaceVote = rankItems.firstOrNull()?.voteCount ?: 0L
                val totalVote = imagePick.count

                itemsIndexed(rankItems, key = { _, candidate -> candidate.id }) { index, candidate ->
                    ImagePickRankingItem(
                        item = candidate,
                        rank = index + 1,
                        isFirst = index == 0,
                        totalVoteCount = totalVote.toLong(),
                        onClick = { onItemClick(candidate.idol?.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 하단 투표 버튼
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

@Composable
private fun VoteButton(
    canVote: Boolean,
    needsVideoAd: Boolean,
    hasVotedToday: Boolean,
    onVoteClick: () -> Unit
) {
    val buttonText = when {
        hasVotedToday -> stringResource(R.string.themepick_today_voted)   // "오늘 투표 완료"
        needsVideoAd -> stringResource(R.string.imagepick_vote_with_ad)   // "추가 투표하기 [AD]"
        else -> stringResource(R.string.guide_vote_title)                 // "투표하기"
    }

    val buttonColor = if (hasVotedToday) {
        ColorPalette.fixGray900
    } else {
        colorResource(R.color.main)
    }

    val textColor = if (hasVotedToday) {
        ColorPalette.fixWhite
    } else {
        androidx.compose.ui.graphics.Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_100))
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .background(buttonColor, shape = RoundedCornerShape(8.dp))
                .clickable(enabled = !hasVotedToday) { onVoteClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = buttonText,
                style = ExoTypo.typo16Bold.copy(color = textColor)
            )
        }
    }
}

// ============================================================
// ImagePickLiveHeader - 이미지픽 결과 헤더
// ============================================================

/**
 * 이미지픽 결과 헤더 (타이틀, 참여인원, 투표기간)
 * old 프로젝트 item_image_pick_header.xml 기반
 */
@Composable
private fun ImagePickLiveHeader(
    title: String,
    participantsCount: String,
    periodText: String,
    isFinished: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_200))
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // 타이틀 (둥근 배경)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = colorResource(R.color.notice_background),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 20.dp, vertical = 17.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title,
                style = ExoTypo.typo14Bold.copy(
                    color = if (isFinished) {
                        colorResource(R.color.gray300)
                    } else {
                        colorResource(R.color.main_light)
                    }
                )
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 참여인원
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.num_participants),
                style = ExoTypo.typo14.copy(
                    color = colorResource(R.color.text_gray),
                    lineHeight = 20.sp
                )
            )
            Spacer(modifier = Modifier.width(11.dp))
            Text(
                text = stringResource(R.string.num_participants_format, participantsCount),
                style = ExoTypo.typo13.copy(
                    color = colorResource(R.color.text_default),
                    lineHeight = 20.sp
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 투표기간
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.onepick_period),
                style = ExoTypo.typo14.copy(
                    color = colorResource(R.color.text_gray),
                    lineHeight = 20.sp
                )
            )
            Spacer(modifier = Modifier.width(11.dp))
            Text(
                text = periodText,
                style = ExoTypo.typo13.copy(
                    color = colorResource(R.color.text_default),
                    lineHeight = 20.sp
                )
            )
        }
    }
}

// ============================================================
// ImagePickLiveRankingItem - 이미지픽 결과 랭킹 아이템
// ============================================================

/**
 * 이미지픽 결과 화면용 랭킹 아이템
 * ThemePickResultScreen과 동일한 형태, 프로필 이미지만 원형
 */
@Composable
private fun ImagePickLiveRankingItem(
    item: ImagePickIdolModel,
    totalVote: Int,
    firstPlaceVote: Long,
    isFirstItem: Boolean = false,
    onClick: () -> Unit = {}
) {
    if (item.rank == 1 && isFirstItem) {
        ImagePickLive1stRankingItem(
            item = item,
            totalVote = totalVote,
            firstPlaceVote = firstPlaceVote,
            onClick = onClick
        )
    } else {
        ImagePickLiveOtherRankingItem(
            item = item,
            totalVote = totalVote,
            onClick = onClick
        )
    }
}

/**
 * 이미지픽 결과 1위 랭킹 아이템
 */
@Composable
private fun ImagePickLive1stRankingItem(
    item: ImagePickIdolModel,
    totalVote: Int,
    firstPlaceVote: Long,
    onClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val idolName = item.idol?.name ?: ""
    val imageUrl = item.idol?.imageUrl?.toSecureUrl()
    val percentage = if (totalVote > 0) {
        ((100.0f * item.voteCount.toFloat() / totalVote.toFloat())).roundToInt()
    } else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colorResource(R.color.background_200))
                .padding(vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 프로필 이미지 (큰 사이즈, 원형)
                Box(
                    modifier = Modifier.size(90.dp)
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = idolName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                    // 1위 아이콘
                    AsyncImage(
                        model = R.drawable.icon_heartpick_1st,
                        contentDescription = "1st",
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = (-10).dp)
                            .size(30.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // 이름 및 투표 정보
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // 이름
                    Text(
                        text = idolName,
                        style = ExoTypo.typo16Bold.copy(color = colorResource(R.color.text_default)),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // 투표 수 및 퍼센트
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = numberFormat.format(item.voteCount),
                            style = ExoTypo.typo14.copy(
                                color = colorResource(R.color.text_default),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${percentage}%",
                            style = ExoTypo.typo12.copy(color = colorResource(R.color.text_gray))
                        )
                    }
                }

                // 보트 아이콘 (off 상태)
                AsyncImage(
                    model = R.drawable.btn_themapick_vote,
                    contentDescription = "Vote",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * 이미지픽 결과 2위 이하 랭킹 아이템
 */
@Composable
private fun ImagePickLiveOtherRankingItem(
    item: ImagePickIdolModel,
    totalVote: Int,
    onClick: () -> Unit
) {
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }
    val idolName = item.idol?.name ?: ""
    val imageUrl = item.idol?.imageUrl?.toSecureUrl()
    val percentage = if (totalVote > 0) {
        ((100.0f * item.voteCount.toFloat() / totalVote.toFloat())).roundToInt()
    } else 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorResource(R.color.background_200))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위
        Text(
            text = numberFormat.format(item.rank),
            style = ExoTypo.typo16Bold.copy(color = colorResource(R.color.text_default)),
            textAlign = TextAlign.Center,
            modifier = Modifier.width(42.dp)
        )

        // 프로필 이미지 (원형)
        AsyncImage(
            model = imageUrl,
            contentDescription = idolName,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(55.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.width(9.dp))

        // 이름 및 투표 정보
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 이름
            Text(
                text = idolName,
                style = ExoTypo.typo14.copy(
                    color = colorResource(R.color.text_default),
                    fontWeight = FontWeight.Medium
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 투표 수 및 퍼센트
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = numberFormat.format(item.voteCount),
                    style = ExoTypo.typo13.copy(color = colorResource(R.color.text_default))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${percentage}%",
                    style = ExoTypo.typo11.copy(color = colorResource(R.color.text_gray))
                )
            }
        }

        // 보트 아이콘 (off 상태)
        AsyncImage(
            model = R.drawable.btn_themapick_vote,
            contentDescription = "Vote",
            modifier = Modifier
                .padding(end = 20.dp)
                .size(32.dp)
        )
    }
}
