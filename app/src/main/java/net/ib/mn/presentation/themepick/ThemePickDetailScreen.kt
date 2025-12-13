package net.ib.mn.presentation.themepick

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import net.ib.mn.domain.model.ThemePickIdol
import net.ib.mn.domain.model.ThemePickModel
import net.ib.mn.data.local.PreferencesManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import kotlin.math.absoluteValue
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoConfirmDialog
import net.ib.mn.ui.components.ExoLoading
import net.ib.mn.ui.components.ExoNameWithGroup
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoTitleDialog
import net.ib.mn.ui.components.RankingItem
import net.ib.mn.ui.components.ThemePickRankingItem
import net.ib.mn.util.IdolImageUtil.toSecureUrl
import net.ib.mn.ad.RewardAdManager
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

/**
 * 테마픽 상세 화면 (통합)
 *
 * old 프로젝트의 ThemePickRankActivity, ThemePickResultActivity를 Compose로 재구현.
 *
 * 세 가지 상태를 모두 대응:
 * - PREPARING: 투표 예정 - 후보 목록(셔플), 알림 설정 버튼
 * - PROGRESS: 투표 중 - 후보 선택 + 투표 버튼 / 현재 순위 보기
 * - FINISHED: 투표 종료 - 최종 결과 순위 표시
 */
@Composable
fun ThemePickDetailScreen(
    themePickId: Int,
    modifier: Modifier = Modifier,
    viewModel: ThemePickDetailViewModel = hiltViewModel(),
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
    LaunchedEffect(themePickId) {
        viewModel.sendIntent(ThemePickDetailContract.Intent.LoadThemePick(themePickId))
    }

    // Effect 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is ThemePickDetailContract.Effect.ShowToast -> {
                    Toast.makeText(context, effect.message, Toast.LENGTH_SHORT).show()
                }
                is ThemePickDetailContract.Effect.ShareThemePick -> {
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, effect.shareText)
                        type = "text/plain"
                    }
                    context.startActivity(Intent.createChooser(sendIntent, null))
                }
                is ThemePickDetailContract.Effect.NavigateToResult -> {
                    onNavigateToResult?.invoke(effect.themePickId)
                }
                is ThemePickDetailContract.Effect.ShowVoteCompleteDialog -> {
                    voteCompleteDialogData = VoteCompleteDialogData(
                        candidateName = effect.candidateName,
                        rank = effect.rank,
                        voteGapFromFirst = effect.voteGapFromFirst
                    )
                    showVoteCompleteDialog = true
                }
                is ThemePickDetailContract.Effect.ShowNotifyEnabledToast -> {
                    Toast.makeText(context, context.getString(R.string.vote_alert_after), Toast.LENGTH_SHORT).show()
                }
                is ThemePickDetailContract.Effect.NavigateBack -> {
                    onBackClick()
                }
                is ThemePickDetailContract.Effect.ShowVideoAd -> {
                    // 광고 로드 및 표시
                    if (activity != null && !isAdLoading) {
                        isAdLoading = true
                        rewardAdManager.loadAd(
                            context = context,
                            onLoaded = {
                                // 광고 로드 완료 → 표시
                                rewardAdManager.showAd(
                                    activity = activity,
                                    onRewarded = {
                                        // 광고 시청 완료 → 투표
                                        isAdLoading = false
                                        viewModel.sendIntent(ThemePickDetailContract.Intent.VoteAfterAd)
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
                is ThemePickDetailContract.Effect.ShowAlreadyVotedDialog -> {
                    Toast.makeText(context, context.getString(R.string.themepick_already_vote), Toast.LENGTH_SHORT).show()
                }
                is ThemePickDetailContract.Effect.ShowNoParticipantsDialog -> {
                    Toast.makeText(context, context.getString(R.string.onepick_no_votes), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // 투표 완료 다이얼로그
    if (showVoteCompleteDialog && voteCompleteDialogData != null) {
        val data = voteCompleteDialogData!!
        val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

        val title = if (data.rank == 1) {
            stringResource(R.string.vote_themepick_finished_first, data.candidateName, stringResource(R.string.title_share))
        } else {
            stringResource(R.string.vote_themepick_finished, data.candidateName, numberFormat.format(data.voteGapFromFirst), stringResource(R.string.title_share))
        }

        ExoConfirmDialog(
            title = "",
            message = title,
            confirmButtonText = stringResource(R.string.title_share),
            dismissButtonText = stringResource(R.string.button_close),
            onConfirm = {
                showVoteCompleteDialog = false
                viewModel.sendIntent(ThemePickDetailContract.Intent.Share)
            },
            onDismiss = {
                showVoteCompleteDialog = false
            }
        )
    }

    // 타이틀 결정
    val title = when (state.status) {
        ThemePickDetailContract.ThemePickStatus.PREPARING -> stringResource(R.string.themepick)
        ThemePickDetailContract.ThemePickStatus.FINISHED -> "${stringResource(R.string.themepick)} ${stringResource(R.string.lable_final_result)}"
        ThemePickDetailContract.ThemePickStatus.PROGRESS -> stringResource(R.string.themepick)
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

    // 알림 권한 설정 다이얼로그
    if (showNotificationPermissionDialog) {
        ExoConfirmDialog(
            title = stringResource(R.string.push_induct_head),
            message = stringResource(R.string.push_induct_body),
            confirmButtonText = stringResource(R.string.push_induct_button),
            dismissButtonText = stringResource(R.string.btn_cancel),
            onConfirm = {
                showNotificationPermissionDialog = false
                // 알림 설정 화면으로 이동
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
                                viewModel.sendIntent(ThemePickDetailContract.Intent.Share)
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
                    when (state.status) {
                        ThemePickDetailContract.ThemePickStatus.PREPARING -> {
                            PreparingContent(
                                state = state,
                                onNotifyClick = {
                                    // 알림 권한 체크
                                    val isNotificationEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
                                    if (!isNotificationEnabled) {
                                        // 알림 권한이 꺼져 있으면 설정 다이얼로그 표시
                                        showNotificationPermissionDialog = true
                                    } else {
                                        // 알림 권한이 켜져 있으면 API 호출
                                        viewModel.sendIntent(ThemePickDetailContract.Intent.ToggleNotification)
                                    }
                                },
                                onRewardToggle = {
                                    viewModel.sendIntent(ThemePickDetailContract.Intent.ToggleRewardExpand)
                                }
                            )
                        }
                        ThemePickDetailContract.ThemePickStatus.PROGRESS -> {
                            ProgressContent(
                                state = state,
                                onCandidateSelect = { candidate ->
                                    viewModel.sendIntent(ThemePickDetailContract.Intent.SelectCandidate(candidate))
                                },
                                onVoteClick = {
                                    viewModel.sendIntent(ThemePickDetailContract.Intent.Vote)
                                },
                                onShowRankingClick = {
                                    viewModel.sendIntent(ThemePickDetailContract.Intent.GoToResult)
                                },
                                onRewardToggle = {
                                    viewModel.sendIntent(ThemePickDetailContract.Intent.ToggleRewardExpand)
                                }
                            )
                        }
                        ThemePickDetailContract.ThemePickStatus.FINISHED -> {
                            FinishedContent(
                                state = state,
                                onRewardToggle = {
                                    viewModel.sendIntent(ThemePickDetailContract.Intent.ToggleRewardExpand)
                                }
                            )
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
    val rank: Int,
    val voteGapFromFirst: Long
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

// ============================================================
// PREPARING (투표 예정) Content
// ============================================================

@Composable
private fun PreparingContent(
    state: ThemePickDetailContract.State,
    onNotifyClick: () -> Unit,
    onRewardToggle: () -> Unit
) {
    val themePick = state.themePick ?: return
    val candidates = themePick.candidates ?: emptyList()

    // 후보 셔플 (메모이제이션)
    val shuffledCandidates = remember(candidates) { candidates.shuffled() }

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
            // 배너 이미지
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ThemePickBanner(
                    imageUrl = state.secureImageUrl,
                )
            }

            // 제목
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp).padding(top=20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = themePick.title,
                        color = colorResource(R.color.text_default),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 1위 리워드
            item {
                RewardSection(
                    themePick = themePick,
                    isExpanded = state.isRewardExpanded,
                    onToggle = onRewardToggle
                )
            }

            // 알림 설정 버튼
            item {
                NotifyButton(
                    isNotifyEnabled = state.isNotifyEnabled,
                    onClick = onNotifyClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 후보 Swiper (HorizontalPager)
            item {
                if (shuffledCandidates.isNotEmpty()) {
                    CandidateSwiper(
                        candidates = shuffledCandidates,
                        type = themePick.type,
                        imageRatio = themePick.imageRatio
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
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
            .padding(horizontal = 16.dp)
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

/**
 * 후보 Swiper (HorizontalPager)
 * Old 프로젝트의 ViewPager2 + CompositePageTransformer 정확히 재구현
 *
 * Old 핵심:
 * - ViewPager2: clipChildren=false, clipToPadding=false → 양옆 페이지 보임
 * - 아이템: match_parent 크기, CardView 안에 1:1 이미지
 * - pageMarginPx = -(imageWidth / 1.7)
 * - offsetPx = screenWidth - pageMarginPx - pagerWidth
 * - translationX = -offsetPx * position (양옆 페이지를 가운데로 당김)
 * - translationZ로 가운데가 맨 앞
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CandidateSwiper(
    candidates: List<ThemePickIdol>,
    type: String,
    imageRatio: String
) {
    if (candidates.isEmpty()) return

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // 카드 높이 (Old: 화면너비 * 1/2 또는 3/4)
    val pagerHeight = if (imageRatio == "S") {
        screenWidthDp * 0.5f
    } else {
        screenWidthDp * 0.75f
    }

    // 카드 크기 (1:1 비율)
    val cardSize = pagerHeight - 20.dp

    // 가운데 정렬을 위한 padding (카드가 정중앙에 오도록)
    val horizontalPadding = (screenWidthDp - cardSize) / 2

    // 무한 스크롤
    val startIndex = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = startIndex) {
        Int.MAX_VALUE
    }

    // 현재 인덱스
    val currentIndex = pagerState.currentPage % candidates.size
    val currentCandidate = candidates[currentIndex]

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(pagerHeight),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = horizontalPadding),
            // 음수 spacing으로 카드 겹침 (50% 겹침 = 50% 보임)
            pageSpacing = (-cardSize * 0.5f),
            // 양쪽에 1개씩만 미리 로드 (총 3개 보임)
            beyondViewportPageCount = 1
        ) { page ->
            val actualIndex = page % candidates.size
            val candidate = candidates[actualIndex]

            // pageOffset: 현재 페이지에서 얼마나 떨어져 있는지 (-1 ~ 0 ~ 1)
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val offsetAbs = pageOffset.absoluteValue

            // 가운데인지 확인
            val isCenter = offsetAbs < 0.5f

            // zIndex: 가운데가 맨 앞 (높은 값)
            val zIndex = 1f - offsetAbs

            // 3개만 보이도록: 1개 이상 떨어진 카드는 숨김
            val alpha = if (offsetAbs > 1.5f) 0f else 1f

            Box(
                modifier = Modifier
                    .zIndex(zIndex)
                    .graphicsLayer {
                        // 크기: 중앙 1.0, 멀어질수록 0.85
                        val scale = lerp(1f, 0.85f, offsetAbs.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale

                        // Y축 회전: 3D 휠 효과 (양옆이 뒤로 돌아감)
                        rotationY = pageOffset * -30f

                        // 카메라 거리 (입체감)
                        cameraDistance = 12 * this.density

                        // 투명도 (3개만 보이도록)
                        this.alpha = alpha
                    }
                    .size(cardSize)
                    .clip(RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 기본 하얀 배경 (이미지 로드 실패 시 표시)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White)
                )

                AsyncImage(
                    model = candidate.imageUrl.toSecureUrl(),
                    contentDescription = candidate.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // 가운데가 아니면 회색 오버레이
                if (!isCenter) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0x99BDBDBD))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 이름 + 부제목 (ExoNameWithGroup 사용)
        val fullName = if (currentCandidate.subtitle.isNotEmpty()) {
            "${currentCandidate.title}_${currentCandidate.subtitle}"
        } else {
            currentCandidate.title
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            ExoNameWithGroup(fullName = fullName)
        }
    }
}

/**
 * PROGRESS 상태 캐로셀 (체크박스 포함)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProgressCandidateSwiper(
    candidates: List<ThemePickIdol>,
    selectedCandidate: ThemePickIdol?,
    type: String,
    imageRatio: String,
    onCandidateSelect: (ThemePickIdol) -> Unit
) {
    if (candidates.isEmpty()) return

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.dp

    // 카드 높이 (Old: 화면너비 * 1/2 또는 3/4)
    val pagerHeight = if (imageRatio == "S") {
        screenWidthDp * 0.5f
    } else {
        screenWidthDp * 0.75f
    }

    // 카드 크기 (1:1 비율)
    val cardSize = pagerHeight - 20.dp

    // 가운데 정렬을 위한 padding (카드가 정중앙에 오도록)
    val horizontalPadding = (screenWidthDp - cardSize) / 2

    // 무한 스크롤
    val startIndex = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = startIndex) {
        Int.MAX_VALUE
    }

    // 현재 인덱스
    val currentIndex = pagerState.currentPage % candidates.size
    val currentCandidate = candidates[currentIndex]

    // 현재 보이는 후보가 선택되었는지
    val isCurrentSelected = selectedCandidate?.id == currentCandidate.id

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(pagerHeight),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = horizontalPadding),
            // 음수 spacing으로 카드 겹침 (50% 겹침 = 50% 보임)
            pageSpacing = (-cardSize * 0.5f),
            // 양쪽에 1개씩만 미리 로드 (총 3개 보임)
            beyondViewportPageCount = 1
        ) { page ->
            val actualIndex = page % candidates.size
            val candidate = candidates[actualIndex]

            // pageOffset: 현재 페이지에서 얼마나 떨어져 있는지 (-1 ~ 0 ~ 1)
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val offsetAbs = pageOffset.absoluteValue

            // 가운데인지 확인
            val isCenter = offsetAbs < 0.5f

            // zIndex: 가운데가 맨 앞 (높은 값)
            val zIndex = 1f - offsetAbs

            // 3개만 보이도록: 1개 이상 떨어진 카드는 숨김
            val alpha = if (offsetAbs > 1.5f) 0f else 1f

            // 이 후보가 선택되었는지
            val isSelected = selectedCandidate?.id == candidate.id

            Box(
                modifier = Modifier
                    .zIndex(zIndex)
                    .graphicsLayer {
                        // 크기: 중앙 1.0, 멀어질수록 0.85
                        val scale = lerp(1f, 0.85f, offsetAbs.coerceIn(0f, 1f))
                        scaleX = scale
                        scaleY = scale

                        // Y축 회전: 3D 휠 효과 (양옆이 뒤로 돌아감)
                        rotationY = pageOffset * -30f

                        // 카메라 거리 (입체감)
                        cameraDistance = 12 * this.density

                        // 투명도 (3개만 보이도록)
                        this.alpha = alpha
                    }
                    .size(cardSize),
                contentAlignment = Alignment.Center
            ) {
                // 이미지 영역 (clip 적용)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (isCenter) {
                                onCandidateSelect(candidate)
                            }
                        }
                ) {
                    // 기본 하얀 배경 (이미지 로드 실패 시 표시)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )

                    AsyncImage(
                        model = candidate.imageUrl.toSecureUrl(),
                        contentDescription = candidate.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

                    // 가운데가 아니면 회색 오버레이
                    if (!isCenter) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x99BDBDBD))
                        )
                    }
                }

                // 체크 버튼 (우측 상단) - 가운데 이미지만 표시, 이미지 영역 밖으로 나옴
                if (isCenter) {
                    AsyncImage(
                        model = if (isSelected) R.drawable.btn_themapick_vote_selected
                        else R.drawable.btn_themapick_vote,
                        contentDescription = if (isSelected) "Selected" else "Not selected",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 7.dp, y = (-7).dp)
                            .size(32.dp)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                onCandidateSelect(candidate)
                            }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 이름 + 부제목 (ExoNameWithGroup 사용)
        val fullName = if (currentCandidate.subtitle.isNotEmpty()) {
            "${currentCandidate.title}_${currentCandidate.subtitle}"
        } else {
            currentCandidate.title
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            ExoNameWithGroup(fullName = fullName)
        }
    }
}

// ============================================================
// PROGRESS (투표 중) Content
// ============================================================

@Composable
private fun ProgressContent(
    state: ThemePickDetailContract.State,
    onCandidateSelect: (ThemePickIdol) -> Unit,
    onVoteClick: () -> Unit,
    onShowRankingClick: () -> Unit,
    onRewardToggle: () -> Unit
) {
    val themePick = state.themePick ?: return
    val candidates = themePick.candidates ?: emptyList()

    // 후보 셔플 (메모이제이션)
    val shuffledCandidates = remember(candidates) { candidates.shuffled() }

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
            // 배너 이미지
            item {
                Spacer(modifier = Modifier.height(16.dp))
                ThemePickBanner(
                    imageUrl = state.secureImageUrl,
                )
            }

            // 제목 (main 컬러)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp).padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = themePick.title,
                        color = colorResource(R.color.main_light),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 1위 리워드
            item {
                RewardSection(
                    themePick = themePick,
                    isExpanded = state.isRewardExpanded,
                    onToggle = onRewardToggle
                )
            }

            // 캐로셀 (체크박스 포함)
            item {
                Spacer(modifier = Modifier.height(24.dp))
                if (shuffledCandidates.isNotEmpty()) {
                    ProgressCandidateSwiper(
                        candidates = shuffledCandidates,
                        selectedCandidate = state.selectedCandidate,
                        type = themePick.type,
                        imageRatio = themePick.imageRatio,
                        onCandidateSelect = onCandidateSelect
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        // 하단 투표 버튼
        VoteButton(
            selectedCandidate = state.selectedCandidate,
            canVote = state.canVote,
            needsVideoAd = state.needsVideoAd,
            hasVotedToday = state.hasVotedToday,
            onVoteClick = onVoteClick
        )
    }
}

@Composable
private fun ProgressInfoSection(
    dDayText: String,
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

        Spacer(modifier = Modifier.height(6.dp))

        // D-Day
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.remaining_time),
                color = colorResource(R.color.text_gray),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = dDayText,
                color = colorResource(R.color.main_light),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun ProgressCandidateItem(
    candidate: ThemePickIdol,
    isSelected: Boolean,
    type: String,
    onClick: () -> Unit
) {
    val bgColor = if (isSelected) {
        colorResource(R.color.main100)
    } else {
        colorResource(R.color.background_200)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .background(
                color = bgColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 프로필 이미지
        AsyncImage(
            model = candidate.imageUrl,
            contentDescription = candidate.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        // 이름
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = candidate.title,
                color = colorResource(R.color.text_default),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (candidate.subtitle.isNotEmpty()) {
                Text(
                    text = candidate.subtitle,
                    color = colorResource(R.color.text_gray),
                    fontSize = 12.sp
                )
            }
        }

        // 선택 아이콘 (선택 시만 표시)
        Box(
            modifier = Modifier.size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    painter = painterResource(R.drawable.icon_check),
                    contentDescription = null,
                    tint = colorResource(R.color.main_light),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun VoteButton(
    selectedCandidate: ThemePickIdol?,
    canVote: Boolean,
    needsVideoAd: Boolean,
    hasVotedToday: Boolean,
    onVoteClick: () -> Unit
) {
    // hasVotedToday면 버튼 비활성화 (오늘 투표 완료)
    // 그렇지 않으면 체크박스 선택되면 버튼 활성화
    val isSelected = selectedCandidate != null
    val isEnabled = !hasVotedToday && isSelected

    val btnColor = when {
        hasVotedToday -> colorResource(R.color.gray110)
        isSelected -> colorResource(R.color.main_light)
        else -> colorResource(R.color.gray110)
    }

    val btnText = when {
        hasVotedToday -> stringResource(R.string.themepick_today_voted)   // "오늘 투표 완료"
        needsVideoAd -> stringResource(R.string.themepick_vote_with_ad)   // "투표하기 [AD]"
        else -> stringResource(R.string.guide_vote_title)                 // "투표하기"
    }

    // 오늘 투표 완료시 안내 문구 숨김
    val showGuide = !hasVotedToday

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

        // 안내 문구 (투표 가능할 때만 표시)
        if (showGuide) {
            Text(
                text = stringResource(R.string.themepick_candidate_vote),
                color = colorResource(R.color.text_gray),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 8.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

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
// FINISHED (투표 종료) Content
// ============================================================

@Composable
private fun FinishedContent(
    state: ThemePickDetailContract.State,
    onRewardToggle: () -> Unit
) {
    val themePick = state.themePick ?: return
    val candidates = themePick.candidates ?: emptyList()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colorResource(R.color.background_200))
    ) {
        // 배너 이미지
        item {
            Spacer(modifier = Modifier.height(16.dp))
            ThemePickBanner(
                imageUrl = state.secureImageUrl,
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
                    color = colorResource(R.color.text_default),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        // 정보 섹션 (전체 투표수, 기간, 종료 상태)
        item {
            FinishedInfoSection(
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
        itemsIndexed(candidates, key = { _, candidate -> candidate.id }) { index, candidate ->
            val rankingItem = candidate.toRankingItem(
                totalVote = themePick.count,
                firstPlaceVote = themePick.getFirstPlaceVote()
            )

            ThemePickRankingItem(
                item = rankingItem,
                isFirstItem = index == 0,
                type = themePick.type,
                onClick = { }
            )
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun FinishedInfoSection(
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

// ============================================================
// Common Sections
// ============================================================

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
