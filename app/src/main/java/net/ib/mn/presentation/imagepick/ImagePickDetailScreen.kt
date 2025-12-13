package net.ib.mn.presentation.imagepick

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import net.ib.mn.ad.RewardAdManager
import net.ib.mn.data.local.PreferencesManager
import net.ib.mn.domain.model.ImagePickIdolModel
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.components.ExoLoading
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoTitleDialog
import net.ib.mn.util.IdolImageUtil.toSecureUrl

/**
 * 이미지픽 상세/투표 화면
 *
 * old 프로젝트의 OnepickMatchActivity, OnepickResultActivity를 Compose로 재구현.
 *
 * 세 가지 상태를 대응:
 * - PREPARING: 투표 예정 - 알림 설정 버튼
 * - PROGRESS: 투표 중 - 토너먼트 형식 투표 (3x3 그리드에서 선택)
 * - FINISHED: 투표 종료 - 결과 화면으로 이동
 */
@Composable
fun ImagePickDetailScreen(
    imagePickId: Int,
    modifier: Modifier = Modifier,
    viewModel: ImagePickDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit = {},
    onNavigateToResult: ((Int) -> Unit)? = null
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 안내 다이얼로그 상태
    var showInfoDialog by remember { mutableStateOf(false) }

    // 투표 완료 다이얼로그 상태
    var showVoteCompleteDialog by remember { mutableStateOf(false) }
    var voteCompleteDialogData by remember { mutableStateOf<VoteCompleteDialogData?>(null) }

    // 알림 권한 설정 다이얼로그 상태
    var showNotificationPermissionDialog by remember { mutableStateOf(false) }

    // 광고 로딩 상태
    var isAdLoading by remember { mutableStateOf(false) }

    // RewardAdManager
    val rewardAdManager = remember { RewardAdManager.getInstance() }

    // Activity 참조 (광고 표시용)
    val activity = LocalContext.current as? android.app.Activity

    // 초기 데이터 로드
    LaunchedEffect(imagePickId) {
        viewModel.sendIntent(ImagePickDetailContract.Intent.LoadImagePick(imagePickId))
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ImagePickDetailContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ImagePickDetailContract.Effect.ShareImagePick -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
                is ImagePickDetailContract.Effect.NavigateToResult -> {
                    onNavigateToResult?.invoke(effect.imagePickId)
                }
                is ImagePickDetailContract.Effect.ShowVoteCompleteDialog -> {
                    voteCompleteDialogData = VoteCompleteDialogData(
                        candidateName = effect.candidateName,
                        rank = effect.rank
                    )
                    showVoteCompleteDialog = true
                }
                is ImagePickDetailContract.Effect.ShowNotifyEnabledToast -> {
                    Toast.makeText(context, context.getString(R.string.vote_alert_after), Toast.LENGTH_SHORT).show()
                }
                is ImagePickDetailContract.Effect.NavigateBack -> {
                    onBackClick()
                }
                is ImagePickDetailContract.Effect.ShowVideoAd -> {
                    if (activity != null && !isAdLoading) {
                        isAdLoading = true
                        rewardAdManager.loadAd(
                            context = context,
                            onLoaded = {
                                rewardAdManager.showAd(
                                    activity = activity,
                                    onRewarded = {
                                        isAdLoading = false
                                        viewModel.sendIntent(ImagePickDetailContract.Intent.VoteAfterAd)
                                    },
                                    onFailed = { error ->
                                        isAdLoading = false
                                        Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                    },
                                    onDismissed = {
                                        isAdLoading = false
                                    }
                                )
                            },
                            onFailed = { error ->
                                isAdLoading = false
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
                is ImagePickDetailContract.Effect.ShowAlreadyVotedDialog -> {
                    Toast.makeText(context, context.getString(R.string.onepick_already_voted), Toast.LENGTH_SHORT).show()
                }
                is ImagePickDetailContract.Effect.ShowNoParticipantsDialog -> {
                    Toast.makeText(context, context.getString(R.string.onepick_no_votes), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 투표 완료 다이얼로그
    if (showVoteCompleteDialog && voteCompleteDialogData != null) {
        val data = voteCompleteDialogData!!

        ExoConfirmDialog(
            title = "",
            message = "${stringResource(R.string.my_pick)}: ${data.candidateName}",
            confirmButtonText = stringResource(R.string.see_result),
            dismissButtonText = stringResource(R.string.button_close),
            onConfirm = {
                showVoteCompleteDialog = false
                viewModel.sendIntent(ImagePickDetailContract.Intent.GoToResult)
            },
            onDismiss = {
                showVoteCompleteDialog = false
                onBackClick()
            }
        )
    }

    // 타이틀 결정
    val title = when {
        state.tournamentRound == ImagePickDetailContract.TournamentRound.FINAL ->
            "${stringResource(R.string.imagepick)} ${stringResource(R.string.final_round)}"
        state.status == ImagePickDetailContract.ImagePickStatus.FINISHED ->
            "${stringResource(R.string.imagepick)} ${stringResource(R.string.lable_final_result)}"
        else -> stringResource(R.string.imagepick)
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

    // 알림 권한 설정 다이얼로그
    if (showNotificationPermissionDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.push_induct_head),
            message = stringResource(R.string.push_induct_body),
            confirmButtonText = stringResource(R.string.push_induct_button),
            dismissButtonText = stringResource(R.string.btn_cancel),
            onConfirm = {
                showNotificationPermissionDialog = false
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                }
                context.startActivity(intent)
            },
            onDismiss = {
                showNotificationPermissionDialog = false
            }
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
                                viewModel.sendIntent(ImagePickDetailContract.Intent.Share)
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
                    when (state.status) {
                        ImagePickDetailContract.ImagePickStatus.PREPARING -> {
                            PreparingContent(
                                state = state,
                                onNotifyClick = {
                                    val isNotificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                                    if (!isNotificationEnabled) {
                                        showNotificationPermissionDialog = true
                                    } else {
                                        viewModel.sendIntent(ImagePickDetailContract.Intent.ToggleNotification)
                                    }
                                }
                            )
                        }
                        ImagePickDetailContract.ImagePickStatus.PROGRESS -> {
                            ProgressContent(
                                state = state,
                                onImageSelect = { candidate ->
                                    viewModel.sendIntent(ImagePickDetailContract.Intent.SelectImage(candidate))
                                },
                                onVoteClick = {
                                    viewModel.sendIntent(ImagePickDetailContract.Intent.StartVote)
                                },
                                onShowRankingClick = {
                                    viewModel.sendIntent(ImagePickDetailContract.Intent.GoToResult)
                                }
                            )
                        }
                        ImagePickDetailContract.ImagePickStatus.FINISHED -> {
                            // 종료 상태면 결과 화면으로 이동
                            LaunchedEffect(Unit) {
                                onNavigateToResult?.invoke(imagePickId)
                            }
                        }
                    }
                }
            }

            // 투표 중 로딩 오버레이
            if (state.isVoting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x80000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorResource(R.color.main))
                }
            }

            // 광고 로딩 중 오버레이
            ExoLoading(isLoading = isAdLoading)
        }
    }
}

// ============================================================
// Data Classes
// ============================================================

private data class VoteCompleteDialogData(
    val candidateName: String,
    val rank: Int
)

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
// PREPARING (투표 예정) Content
// ============================================================

@Composable
private fun PreparingContent(
    state: ImagePickDetailContract.State,
    onNotifyClick: () -> Unit
) {
    val imagePick = state.imagePick ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // 제목
        Text(
            text = imagePick.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.text_default),
            textAlign = TextAlign.Center
        )

        if (imagePick.subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = imagePick.subtitle,
                fontSize = 14.sp,
                color = colorResource(R.color.text_gray),
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 투표 기간
        Text(
            text = "${stringResource(R.string.onepick_period)} : ${state.periodText}",
            fontSize = 14.sp,
            color = colorResource(R.color.text_gray),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        // 알림 설정 버튼
        NotifyButton(
            isNotifyEnabled = state.isNotifyEnabled,
            onClick = onNotifyClick
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun NotifyButton(
    isNotifyEnabled: Boolean,
    onClick: () -> Unit
) {
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
            .height(50.dp)
            .background(color = btnColor, shape = RoundedCornerShape(25.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                enabled = !isNotifyEnabled
            ) { onClick() }
    ) {
        Text(
            text = btnText,
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

// ============================================================
// PROGRESS (투표 중) Content
// ============================================================

@Composable
private fun ProgressContent(
    state: ImagePickDetailContract.State,
    onImageSelect: (ImagePickIdolModel) -> Unit,
    onVoteClick: () -> Unit,
    onShowRankingClick: () -> Unit
) {
    val imagePick = state.imagePick ?: return

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_100))
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 제목
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 라운드 표시
                    val roundText = if (state.tournamentRound == ImagePickDetailContract.TournamentRound.FINAL) {
                        stringResource(R.string.final_round)
                    } else {
                        stringResource(R.string.qualifying_round)
                    }

                    Text(
                        text = roundText,
                        fontSize = 14.sp,
                        color = colorResource(R.color.main_light),
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = imagePick.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colorResource(R.color.text_default),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 진행 상황 표시 (예선일 때만)
            if (state.tournamentRound == ImagePickDetailContract.TournamentRound.QUALIFYING && state.totalRounds > 1) {
                item {
                    ProgressIndicator(
                        currentRound = state.currentRoundIndex,
                        totalRounds = state.totalRounds
                    )
                }
            }

            // 이미지 그리드
            item {
                ImageGrid(
                    candidates = state.currentRoundCandidates,
                    dimension = state.dimension,
                    date = state.date,
                    onImageSelect = onImageSelect
                )
            }
        }

        // 하단 버튼 영역 (투표 시작 전에만 표시)
        if (state.currentRoundIndex == 0 && state.selectedPicks.isEmpty()) {
            VoteButton(
                canVote = state.canVote,
                needsVideoAd = state.needsVideoAd,
                hasVotedToday = state.hasVotedToday,
                onVoteClick = onVoteClick,
                onShowRankingClick = onShowRankingClick
            )
        }
    }
}

@Composable
private fun ProgressIndicator(
    currentRound: Int,
    totalRounds: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(totalRounds) { index ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(
                        color = if (index <= currentRound) colorResource(R.color.main_light) else colorResource(R.color.gray150),
                        shape = CircleShape
                    )
            )
            if (index < totalRounds - 1) {
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
private fun ImageGrid(
    candidates: List<ImagePickIdolModel>,
    dimension: Int,
    date: String,
    onImageSelect: (ImagePickIdolModel) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(dimension),
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .height((120 * dimension).dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        userScrollEnabled = false
    ) {
        items(candidates) { candidate ->
            ImageGridItem(
                candidate = candidate,
                date = date,
                onClick = { onImageSelect(candidate) }
            )
        }
    }
}

@Composable
private fun ImageGridItem(
    candidate: ImagePickIdolModel,
    date: String,
    onClick: () -> Unit
) {
    val isEmpty = candidate.isEmpty

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isEmpty) colorResource(R.color.background_200) else Color.White)
            .clickable(enabled = !isEmpty) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (!isEmpty) {
            // 이미지 URL 생성 (old 프로젝트의 onePickImageUrl 로직)
            val imageUrl = candidate.imageUrl?.toSecureUrl()
                ?: candidate.idol?.imageUrl?.toSecureUrl()

            AsyncImage(
                model = imageUrl,
                contentDescription = candidate.idol?.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun VoteButton(
    canVote: Boolean,
    needsVideoAd: Boolean,
    hasVotedToday: Boolean,
    onVoteClick: () -> Unit,
    onShowRankingClick: () -> Unit
) {
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

        // 현재 순위 보기 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(colorResource(R.color.main200))
                    .clickable { onShowRankingClick() }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.see_current_ranking),
                    fontSize = 13.sp,
                    color = colorResource(R.color.main_light)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    painter = painterResource(R.drawable.arrow_left_to_right),
                    contentDescription = null,
                    modifier = Modifier.size(8.dp),
                    tint = colorResource(R.color.main_light)
                )
            }
        }

        // 투표하기 버튼
        val btnColor = when {
            hasVotedToday -> ColorPalette.fixGray900
            else -> colorResource(R.color.main_light)
        }

        val btnTextColor = when {
            hasVotedToday -> ColorPalette.fixWhite
            else -> Color.White
        }

        val btnText = when {
            hasVotedToday -> stringResource(R.string.themepick_today_voted)   // "오늘 투표 완료"
            needsVideoAd -> stringResource(R.string.imagepick_vote_with_ad)   // "추가 투표하기 [AD]"
            else -> stringResource(R.string.guide_vote_title)                 // "투표하기"
        }

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
                    enabled = !hasVotedToday
                ) { onVoteClick() }
        ) {
            Text(
                text = btnText,
                color = btnTextColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }
    }
}
