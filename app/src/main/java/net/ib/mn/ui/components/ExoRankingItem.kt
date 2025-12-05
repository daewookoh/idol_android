package net.ib.mn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.R
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExoTypo
import net.ib.mn.util.NumberFormatUtil
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * 랭킹 아이템 타입
 */
object RankingItemType {
    const val MAIN = "MAIN"                 // 메인 랭킹 (그룹/솔로/글로벌)
    const val DAILY = "DAILY"               // 기적/루키 일일 랭킹
    const val CUMULATIVE = "CUMULATIVE"     // 기적/루키/명예의전당 누적 랭킹
    const val HEARTPICK = "HEARTPICK"       // 하트픽 랭킹
}

// ==================== 공통 유틸리티 함수 ====================

/**
 * 프로그레스 퍼센트 계산
 * old 프로젝트와 동일한 로직: 38% ~ 100% 범위, 4th root 사용
 */
private fun calculateProgressPercent(heartCount: Long, maxHeartCount: Long): Float {
    return if (maxHeartCount == 0L || heartCount == 0L) {
        0.38f
    } else {
        val voteRoot = kotlin.math.sqrt(kotlin.math.sqrt(heartCount.toDouble()))
        val maxRoot = kotlin.math.sqrt(kotlin.math.sqrt(maxHeartCount.toDouble()))
        val p = 38 + (voteRoot * 62 / maxRoot)
        (p / 100f).toFloat().coerceIn(0.38f, 1f)
    }
}

// ==================== 공통 UI 컴포넌트 ====================

/**
 * 순위 왕관 아이콘 (1,2,3위)
 */
@Composable
private fun RankCrownIcon(rank: Int) {
    when (rank) {
        1 -> Icon(
            painter = painterResource(R.drawable.icon_rating_heart_voting_1st),
            contentDescription = "1st",
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 18.dp, height = 12.dp)
        )
        2 -> Icon(
            painter = painterResource(R.drawable.icon_rating_heart_voting_2nd),
            contentDescription = "2nd",
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 18.dp, height = 12.dp)
        )
        3 -> Icon(
            painter = painterResource(R.drawable.icon_rating_heart_voting_3rd),
            contentDescription = "3rd",
            tint = Color.Unspecified,
            modifier = Modifier.size(width = 18.dp, height = 12.dp)
        )
    }
}

/**
 * 순위 텍스트 (색상 옵션)
 */
@Composable
private fun RankText(rank: Int, useMainColorForTop3: Boolean = true) {
    val textColor = if (useMainColorForTop3 && rank <= 3) ColorPalette.main else ColorPalette.gray580
    Text(
        text = stringResource(R.string.rank_count_format, rank),
        style = ExoTypo.body11.copy(color = textColor)
    )
}

/**
 * 투표수 텍스트 오버레이
 */
@Composable
private fun VoteCountOverlay(
    item: RankingItem,
    animatedProgress: Float
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val barWidth = maxWidth * animatedProgress
        var textWidthPx by remember(item.voteCount) { mutableStateOf(0) }
        val density = LocalDensity.current
        val textWidthDp = remember(textWidthPx) { with(density) { textWidthPx.toDp() } }

        Box(
            modifier = Modifier
                .offset(x = (barWidth - textWidthDp - 6.dp).coerceAtLeast(0.dp))
                .wrapContentWidth()
                .height(17.dp)
                .onGloballyPositioned { coordinates ->
                    val newWidth = coordinates.size.width
                    if (textWidthPx != newWidth) textWidthPx = newWidth
                },
            contentAlignment = Alignment.CenterStart
        ) {
            ExoHeartCounter(
                count = item.heartCount,
                style = ExoTypo.stat11.copy(fontWeight = FontWeight.Normal, lineHeight = 17.sp)
            )
        }
    }
}

/**
 * 순위 + 이름 영역 (공통 레이아웃)
 */
@Composable
private fun RankAndNameRow(
    rank: Int,
    name: String,
    nameFontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    groupFontSize: androidx.compose.ui.unit.TextUnit = 10.sp
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (rank == 0) "-" else stringResource(R.string.rank_count_format, rank),
            style = ExoTypo.title15
        )
        Spacer(modifier = Modifier.width(5.dp))
        ExoNameWithGroup(fullName = name, nameFontSize = nameFontSize, groupFontSize = groupFontSize)
    }
}

/**
 * 펼치기 영역 (ExoTop3)
 * 클릭 시 해당 아이돌의 CommunityScreen으로 이동
 */
@Composable
private fun ExpandableTop3(
    isExpanded: Boolean,
    item: RankingItem,
    imageUrls: List<String?>,
    videoUrls: List<String?>
) {
    val onItemClick = LocalRankingItemClick.current
    AnimatedVisibility(visible = isExpanded, enter = expandVertically(), exit = shrinkVertically()) {
        ExoTop3(
            id = remember(item.rank) { "ranking_item_${item.rank}" },
            idolId = item.id.toIntOrNull() ?: 0,
            imageUrls = imageUrls,
            videoUrls = videoUrls,
            isVisible = isExpanded,
            onClick = { _ -> onItemClick(item) }
        )
    }
}

/**
 * 하단 구분선
 */
@Composable
private fun ItemDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = ColorPalette.gray200)
}

/**
 * 반짝임 효과 (10초마다 반복)
 */
@Composable
private fun ShimmerEffect() {
    val shimmerProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(Unit) {
        shimmerProgress.snapTo(0f)
        shimmerProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
        )
        while (true) {
            kotlinx.coroutines.delay(10000)
            shimmerProgress.snapTo(0f)
            shimmerProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1000, easing = LinearEasing)
            )
        }
    }

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
    ) {
        val canvasWidth = size.width
        val progress = shimmerProgress.value

        if (progress > 0f && progress < 1f) {
            val shimmerWidth = canvasWidth * 0.3f
            val shimmerPosition = (canvasWidth + shimmerWidth) * progress - shimmerWidth

            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        ColorPalette.fixWhite.copy(alpha = 0f),
                        ColorPalette.fixWhite.copy(alpha = 0.3f),
                        ColorPalette.fixWhite.copy(alpha = 0f)
                    ),
                    startX = shimmerPosition,
                    endX = shimmerPosition + shimmerWidth
                ),
                size = size
            )
        }
    }
}

/**
 * ExoRankingItem - 랭킹 아이템 리스트 렌더링 라우터
 *
 * 타입에 따라 적절한 랭킹 아이템 함수로 분기
 *
 * @param items 랭킹 아이템 리스트
 * @param type 랭킹 타입 (RankingItemType 참조)
 * @param onItemClick 아이템 클릭 이벤트
 * @param onVoteSuccess 투표 성공 이벤트
 * @param disableAnimation 애니메이션 비활성화 (기본값: false)
 * @param expandedItemIds 확장된 아이템 ID 목록 (스크롤 시에도 상태 유지)
 * @param onExpandedChange 확장 상태 변경 콜백
 */
fun LazyListScope.exoRankingItems(
    items: List<RankingItem>,
    type: String = RankingItemType.MAIN,
    onItemClick: (Int, RankingItem) -> Unit = { _, _ -> },
    onVoteSuccess: (idolId: Int, voteCount: Long) -> Unit = { _, _ -> },
    disableAnimation: Boolean = false,
    expandedItemIds: Set<String> = emptySet(),
    onExpandedChange: (String, Boolean) -> Unit = { _, _ -> }
) {
    when (type) {
        RankingItemType.MAIN -> mainRankingItems(items, onItemClick, onVoteSuccess, disableAnimation, expandedItemIds, onExpandedChange)
        RankingItemType.DAILY -> dailyRankingItems(items, onItemClick, onVoteSuccess, disableAnimation, expandedItemIds, onExpandedChange)
        RankingItemType.CUMULATIVE -> cumulativeRankingItems(items, onItemClick)
        RankingItemType.HEARTPICK -> heartPickRankingItems(items, onItemClick)
        else -> mainRankingItems(items, onItemClick, onVoteSuccess, disableAnimation, expandedItemIds, onExpandedChange)
    }
}

/**
 * MainRankingItem - 메인 랭킹 아이템 (그룹/솔로)
 *
 * old 프로젝트 ranking_item.xml 기반
 * - 큰 프로필 이미지 (77dp, 테두리 포함)
 * - 프로그레스 바 + 반짝임 애니메이션
 * - 배지 시스템 (Angel, Fairy, Miracle, Rookie)
 * - 하트 투표 버튼
 * - 펼치기 기능 (ExoTop3)
 */
fun LazyListScope.mainRankingItems(
    items: List<RankingItem>,
    onItemClick: (Int, RankingItem) -> Unit = { _, _ -> },
    onVoteSuccess: (idolId: Int, voteCount: Long) -> Unit = { _, _ -> },
    disableAnimation: Boolean = false,
    expandedItemIds: Set<String> = emptySet(),
    onExpandedChange: (String, Boolean) -> Unit = { _, _ -> }
) {
    itemsIndexed(
        items = items,
        key = { _, item -> item.itemKey() }
    ) { index, item ->
        // LocalRankingItemClick 사용 (Composable 컨텍스트 내부)
        val localOnItemClick = LocalRankingItemClick.current

        val itemKey = item.itemKey()
        val isExpanded = expandedItemIds.contains(itemKey)
        val backgroundColor = if (item.isFavorite) ColorPalette.main100 else ColorPalette.background100

        val itemModifier = if (disableAnimation) {
            Modifier.fillMaxWidth().background(backgroundColor)
        } else {
            Modifier
                .animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
                .fillMaxWidth()
                .background(backgroundColor)
        }

        Column(modifier = itemModifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 79.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // 외부에서 전달된 onItemClick 호출 후 LocalRankingItemClick도 호출
                        onItemClick(index, item)
                        localOnItemClick(item)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(10.dp))

                ExoProfileImage(
                    imageUrl = item.photoUrl,
                    type = ProfileImageType.LARGE_CIRCLE,
                    rank = item.rank,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier.clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onExpandedChange(itemKey, !isExpanded) },
                    anniversary = item.anniversary ?: "N",
                    anniversaryDays = item.anniversaryDays,
                    miracleCount = item.miracleCount,
                    fairyCount = item.fairyCount,
                    angelCount = item.angelCount
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp)
                ) {
                    RankAndNameRow(rank = item.rank, name = item.name)
                    Spacer(modifier = Modifier.height(3.dp))
                    MainProgressBarWithBadges(item = item, useShimmer = true)
                }

                ExoVoteIcon(
                    idolId = item.id.toIntOrNull() ?: 0,
                    fullName = item.name,
                    idolHeart = item.heartCount,
                    onVoteSuccess = { votedHeart ->
                        onVoteSuccess(item.id.toIntOrNull() ?: 0, votedHeart)
                    }
                )
            }

            ExpandableTop3(isExpanded, item, item.top3ImageUrls, item.top3VideoUrls)

            if (index < items.size - 1) {
                ItemDivider()
            }
        }
    }
}

/**
 * DailyRankingItem - 기적/루키 일일 랭킹 아이템
 *
 * - 중간 프로필 이미지 (62dp, 테두리 포함)
 * - 프로그레스 바 (단색, 애니메이션 없음)
 * - 배지 시스템
 * - 하트 투표 버튼
 * - 펼치기 기능
 */
fun LazyListScope.dailyRankingItems(
    items: List<RankingItem>,
    onItemClick: (Int, RankingItem) -> Unit = { _, _ -> },
    onVoteSuccess: (idolId: Int, voteCount: Long) -> Unit = { _, _ -> },
    disableAnimation: Boolean = false,
    expandedItemIds: Set<String> = emptySet(),
    onExpandedChange: (String, Boolean) -> Unit = { _, _ -> }
) {
    itemsIndexed(
        items = items,
        key = { _, item -> item.itemKey() }
    ) { index, item ->
        // LocalRankingItemClick 사용 (Composable 컨텍스트 내부)
        val localOnItemClick = LocalRankingItemClick.current

        val itemKey = item.itemKey()
        val isExpanded = expandedItemIds.contains(itemKey)
        val backgroundColor = if (item.isFavorite) ColorPalette.main100 else ColorPalette.background100

        val itemModifier = if (disableAnimation) {
            Modifier.fillMaxWidth().background(backgroundColor)
        } else {
            Modifier
                .animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
                .fillMaxWidth()
                .background(backgroundColor)
        }

        Column(modifier = itemModifier) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 67.dp)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        // 외부에서 전달된 onItemClick 호출 후 LocalRankingItemClick도 호출
                        onItemClick(index, item)
                        localOnItemClick(item)
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(10.dp))

                ExoProfileImage(
                    imageUrl = item.photoUrl,
                    type = ProfileImageType.MEDIUM_CIRCLE,
                    rank = item.rank,
                    contentDescription = "프로필 이미지",
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { localOnItemClick(item) },
                    anniversary = item.anniversary ?: "N",
                    anniversaryDays = item.anniversaryDays,
                    miracleCount = item.miracleCount,
                    fairyCount = item.fairyCount,
                    angelCount = item.angelCount
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 5.dp)
                ) {
                    RankAndNameRow(rank = item.rank, name = item.name)
                    Spacer(modifier = Modifier.height(3.dp))
                    DailyProgressBarWithBadges(item = item)
                }

                ExoVoteIcon(
                    idolId = item.id.toIntOrNull() ?: 0,
                    fullName = item.name,
                    idolHeart = item.heartCount,
                    onVoteSuccess = { votedHeart ->
                        onVoteSuccess(item.id.toIntOrNull() ?: 0, votedHeart)
                    }
                )
            }

            ExpandableTop3(isExpanded, item, item.top3ImageUrls, item.top3VideoUrls)

            if (index < items.size - 1) {
                ItemDivider()
            }
        }
    }
}

/**
 * CumulativeRankingItem - 기적/루키/명예의전당 누적 랭킹 아이템 (CUMULATIVE 타입)
 *
 * old 프로젝트의 AggregatedRankingItem 기반
 * - 프로필 이미지 (41dp, 테두리 없음)
 * - 순위 + 이름 + 점수
 * - 순위 변동 표시 (status 필드가 있을 때만)
 * - 급상승 하이라이트 (suddenIncrease 필드가 true일 때만)
 */
fun LazyListScope.cumulativeRankingItems(
    items: List<RankingItem>,
    onItemClick: (Int, RankingItem) -> Unit = { _, _ -> },
    showArrow: Boolean = true,
    clickEnabled: Boolean = true,
    countSuffix: String = "점"
) {
    itemsIndexed(
        items = items,
        key = { _, item -> item.itemKey() }
    ) { index, item ->
        // LocalIdolRankingHistoryClick 사용 (모든 페이지에서 IdolRankingHistory 스크린으로 이동)
        val localOnItemClick = LocalIdolRankingHistoryClick.current

        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 급상승 여부에 따라 배경 설정
            Box(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 급상승일 때 배경 이미지 표시
                if (item.suddenIncrease) {
                    Image(
                        painter = painterResource(R.drawable.bg_cumulative_best),
                        contentDescription = null,
                        modifier = Modifier.matchParentSize(),
                        contentScale = ContentScale.FillBounds
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (clickEnabled) {
                                Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    onItemClick(index, item)
                                    localOnItemClick(item)
                                }
                            } else {
                                Modifier
                            }
                        )
                        .background(if (item.suddenIncrease) Color.Transparent else ColorPalette.background100)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 순위 영역 (왕관 + 텍스트 + 순위변동)
                    Column(
                        modifier = Modifier.width(45.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
                    ) {
                        RankCrownIcon(item.rank)
                        RankText(item.rank)

                        // 순위 변동 표시 (status가 있을 때만)
                        item.status?.let { status ->
                            when (status.lowercase()) {
                                "new" -> {
                                    Icon(
                                        painter = painterResource(R.drawable.icon_change_ranking_new),
                                        contentDescription = "NEW",
                                        tint = Color.Unspecified,
                                        modifier = Modifier.size(width = 15.dp, height = 8.dp)
                                    )
                                }
                                "increase", "decrease", "same" -> {
                                    Row(
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val iconRes = when (status.lowercase()) {
                                            "increase" -> R.drawable.icon_change_ranking_up
                                            "decrease" -> R.drawable.icon_change_ranking_down
                                            else -> R.drawable.icon_change_ranking_no_change
                                        }
                                        Icon(
                                            painter = painterResource(iconRes),
                                            contentDescription = status,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(8.dp)
                                        )
                                        if (item.difference != 0) {
                                            Text(
                                                text = remember(item.difference) {
                                                    NumberFormatUtil.formatWithComma(item.difference)
                                                },
                                                style = ExoTypo.label9.copy(color = ColorPalette.textDefault)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 프로필 이미지 (41dp, 테두리 없음) - old 버전과 동일
                    ExoProfileImage(
                        imageUrl = item.photoUrl,
                        modifier = Modifier.size(41.dp),
                        rank = item.rank,
                        contentDescription = "프로필 이미지"
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
                    ) {
                        ExoNameWithGroup(fullName = item.name, nameFontSize = 14.sp, groupFontSize = 10.sp)

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ExoHeartCounter(count = item.heartCount, style = ExoTypo.body11.copy(color = ColorPalette.textGray))
                            Text(text = countSuffix, style = ExoTypo.body11.copy(color = ColorPalette.textGray))
                        }
                    }

                    // 급상승 표시 (suddenIncrease가 true일 때만)
                    if (item.suddenIncrease) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.icon_up),
                                contentDescription = "급상승",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(width = 30.dp, height = 23.dp)
                            )
                            Text(
                                text = stringResource(R.string.label_rising, item.difference),
                                style = ExoTypo.label10.copy(
                                    color = ColorPalette.mainLight,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(7.dp))
                    }

                    if (showArrow) {
                        Icon(
                            painter = painterResource(R.drawable.btn_go),
                            contentDescription = "Go",
                            modifier = Modifier.size(12.dp),
                            tint = Color.Unspecified
                        )

                        Spacer(modifier = Modifier.width(if (item.suddenIncrease) 20.dp else 14.dp))
                    }
                }
            }

            // 하단 Divider
            HorizontalDivider(
                thickness = 0.5.dp,
                color = ColorPalette.gray200
            )
        }
    }
}

/**
 * HeartPickRankingItem - 하트픽 랭킹 아이템
 *
 * LazyRow에서 사용
 * - 중간 프로필 이미지 (55dp, 테두리 없음)
 * - 프로그레스 바 + 퍼센트
 * - 투표 버튼 없음
 */
fun LazyListScope.heartPickRankingItems(
    items: List<RankingItem>,
    onItemClick: (Int, RankingItem) -> Unit = { _, _ -> }
) {
    itemsIndexed(
        items = items,
        key = { _, item -> item.itemKey() }
    ) { index, item ->
        HeartPickRankingItem(item = item)
    }
}

/**
 * MainProgressBarWithBadges - MAIN 타입용 프로그레스바 + 배지
 *
 * - gradient 프로그레스 바
 * - 10초마다 반짝임 애니메이션
 * - 배지 아이콘
 */
@Composable
private fun MainProgressBarWithBadges(
    item: RankingItem,
    useShimmer: Boolean = true
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(17.dp)
        ) {
            val progressPercent = remember(item.heartCount, item.maxHeartCount) {
                calculateProgressPercent(item.heartCount, item.maxHeartCount)
            }
            val animatedProgress by animateFloatAsState(targetValue = progressPercent, label = "progress")

            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(ColorPalette.sLeagueProgress, ColorPalette.main)
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
            ) {
                if (useShimmer) {
                    ShimmerEffect()
                }
            }

            VoteCountOverlay(item = item, animatedProgress = animatedProgress)
        }

        RankingBadges(item = item)
    }
}

/**
 * DailyProgressBarWithBadges - DAILY 타입용 프로그레스바 + 배지
 *
 * - 단색 프로그레스 바
 * - 애니메이션 없음
 * - 배지 아이콘
 */
@Composable
private fun DailyProgressBarWithBadges(item: RankingItem) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(17.dp)
        ) {
            val progressPercent = remember(item.heartCount, item.maxHeartCount) {
                calculateProgressPercent(item.heartCount, item.maxHeartCount)
            }
            val animatedProgress by animateFloatAsState(targetValue = progressPercent, label = "progress")

            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .background(
                        color = ColorPalette.aLeagueProgress,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                    )
            )

            VoteCountOverlay(item = item, animatedProgress = animatedProgress)
        }

        RankingBadges(item = item)
    }
}

/**
 * RankingBadges - 배지 아이콘 공통 컴포넌트
 */
@Composable
private fun RankingBadges(item: RankingItem) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .padding(start = 4.dp)
            .offset(y = (-3).dp)
    ) {
        // Angel 배지
        if (item.angelCount > 0) {
            Box(
                modifier = Modifier.size(13.dp, 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    painter = painterResource(R.drawable.charity_angel_badge),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = remember(item.angelCount) { item.angelCount.toString() },
                    style = ExoTypo.label7.copy(color = ColorPalette.textAngel),
                    modifier = Modifier.offset(y = 5.dp)
                )
            }
        }

        // Fairy 배지
        if (item.fairyCount > 0) {
            Box(
                modifier = Modifier.size(13.dp, 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    painter = painterResource(R.drawable.charity_fairy_badge),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = remember(item.fairyCount) { item.fairyCount.toString() },
                    style = ExoTypo.label7.copy(color = ColorPalette.textFairy),
                    modifier = Modifier.offset(y = 5.dp)
                )
            }
        }

        // Miracle 배지
        if (item.miracleCount > 0) {
            Box(
                modifier = Modifier.size(13.dp, 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    painter = painterResource(R.drawable.charity_miracle_badge),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = remember(item.miracleCount) { item.miracleCount.toString() },
                    style = ExoTypo.label7.copy(color = ColorPalette.textMiracle),
                    modifier = Modifier.offset(y = 5.dp)
                )
            }
        }

        // Rookie 배지
        if (item.rookieCount > 0) {
            val isSuper = item.rookieCount >= 3
            Box(
                modifier = Modifier.size(13.dp, 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    painter = painterResource(
                        if (isSuper) R.drawable.charity_super_rookie_badge
                        else R.drawable.charity_rookie_badge
                    ),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = remember(item.rookieCount, isSuper) {
                        if (isSuper) "S" else item.rookieCount.toString()
                    },
                    style = ExoTypo.label7.copy(
                        color = if (isSuper) ColorPalette.textSuperRookie else ColorPalette.textRookie
                    ),
                    modifier = Modifier.offset(y = 5.dp)
                )
            }
        }

        // Super Rookie 배지
        if (item.superRookieCount > 0) {
            Box(
                modifier = Modifier.size(13.dp, 16.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Icon(
                    painter = painterResource(R.drawable.charity_super_rookie_badge),
                    contentDescription = null,
                    modifier = Modifier.size(13.dp, 16.dp),
                    tint = Color.Unspecified
                )
                Text(
                    text = "S",
                    style = ExoTypo.label7.copy(color = ColorPalette.textSuperRookie),
                    modifier = Modifier.offset(y = 5.dp)
                )
            }
        }
    }
}


/**
 * HofAccumulativeTop1RankingItem - 명예의 전당 누적 랭킹 1위 전용 아이템
 *
 * old 프로젝트의 aggregated_hof_header.xml 및 HallOfFameAggAdapter TopViewHolder 기반
 *
 * 주요 기능:
 * 1. 기간 표시 영역 (우측 info 버튼)
 * 2. 1위 프로필 영역 (날개 배경 + 큰 프로필 이미지)
 * 3. 급상승 표시 (우측 상단)
 * 4. 순위 변동 표시 (NEW, UP/DOWN)
 * 5. 1위 텍스트 + 이름 + 그룹 + 점수
 *
 * @param item 1위 랭킹 아이템 데이터
 * @param cdnUrl CDN 베이스 URL (PreferencesManager에서 가져온 값)
 * @param period 기간 텍스트 (예: "2024.01.01 ~ 2024.01.30")
 * @param onItemClick 아이템 클릭 이벤트
 * @param onInfoClick info 버튼 클릭 이벤트
 */
@Composable
fun HofAccumulativeTop1RankingItem(
    item: net.ib.mn.data.remote.dto.AggregateRankModel,
    cdnUrl: String,
    period: String,
    onItemClick: () -> Unit = {},
    onInfoClick: () -> Unit = {}
) {
    val localOnItemClick = LocalIdolRankingHistoryClick.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 기간 표시 영역 (old: ConstraintLayout, height: 40dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .background(ColorPalette.gray50),
            contentAlignment = Alignment.Center
        ) {
            // 기간 텍스트
            Text(
                text = period,
                style = ExoTypo.body14.copy(color = ColorPalette.textDimmed),
                modifier = Modifier.align(Alignment.Center)
            )

            // Info 버튼 (우측)
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(36.dp)
                    .padding(9.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.btn_info_black),
                    contentDescription = "Info",
                    tint = Color.Unspecified
                )
            }
        }

        // 1위 프로필 영역 (old: btn_idol)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ColorPalette.background100)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onItemClick()
                    localOnItemClick(
                        RankingItem(
                            id = item.idolId.toString(),
                            name = item.name,
                            rank = item.scoreRank,
                            voteCount = item.score.toString(),
                            heartCount = item.score.toLong()
                        )
                    )
                }
        ) {
            // 배경 이미지 (old: bg_cumulative_voting, 78dp height)
            Icon(
                painter = painterResource(R.drawable.bg_cumulative_voting),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(78.dp)
                    .align(Alignment.TopCenter),
                tint = Color.Unspecified
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(15.dp))

                // 프로필 영역 (old: best1_profile, 180dp x 98dp, marginTop=15dp)
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(110.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 날개 배경 (old: bg_cumulative_voting_wing_2)
                    Icon(
                        painter = painterResource(R.drawable.bg_cumulative_voting_wing_2),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        tint = Color.Unspecified
                    )

                    // 프로필 이미지 (old: 72dp x 72dp, marginTop=5dp, marginBottom=26dp)
                    // old: UtilK.trendImageUrl(context, trendId) → ${cdnUrl}/t/${id}.1_200x200.webp
                    val imageUrl = remember(item.trendId, cdnUrl) {
                        net.ib.mn.util.IdolImageUtil.getTrendImageUrl(
                            cdnUrl = cdnUrl,
                            trendId = item.trendId
                        )
                    }

                    // padding을 size 안에 넣으면 찌그러지므로 Box로 감싸서 margin 효과
                    Box(
                        modifier = Modifier.padding(top = 5.dp, bottom = 20.dp)
                    ) {
                        ExoProfileImage(
                            imageUrl = imageUrl,
                            rank = item.scoreRank,
                            type = ProfileImageType.LARGE,
                            contentDescription = "프로필 이미지"
                        )
                    }
                }

                // 하단 정보 영역 (old: ll_best1_info, marginTop=120dp - 98dp - 15dp = 7dp 추가 여백)
                Spacer(modifier = Modifier.height(7.dp))

                // 하단 정보 영역 (old: ll_best1_info)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 5.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 순위 변동 표시 (old: icon_new_ranking, ll_change_ranking)
                    when (item.status.lowercase()) {
                        "new" -> {
                            Icon(
                                painter = painterResource(R.drawable.icon_change_ranking_new),
                                contentDescription = "NEW",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(width = 15.dp, height = 8.dp)
                                    .padding(end = 5.dp)
                            )
                        }
                        "increase", "decrease" -> {
                            Row(
                                modifier = Modifier.padding(end = 5.dp, top = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val iconRes = when (item.status.lowercase()) {
                                    "increase" -> R.drawable.icon_change_ranking_up
                                    else -> R.drawable.icon_change_ranking_down
                                }
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = item.status,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(8.dp)
                                )
                                if (item.difference != 0) {
                                    Text(
                                        text = remember(item.difference) {
                                            NumberFormatUtil.formatWithComma(item.difference)
                                        },
                                        style = ExoTypo.label9.copy(color = ColorPalette.gray580)
                                    )
                                }
                            }
                        }
                    }

                    // "1위" 텍스트 (old: title_rank)
                    Text(
                        text = stringResource(R.string.first_rank),
                        style = ExoTypo.title15.copy(
                            fontSize = 17.sp,
                            color = ColorPalette.mainLight,
                            fontWeight = FontWeight.Bold
                        )
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    // 이름 + 그룹명
                    ExoNameWithGroupColor(
                        fullName = item.name,
                        nameFontSize = 17.sp,
                        groupFontSize = 11.sp,
                        nameColor = ColorPalette.mainLight,
                        groupColor = ColorPalette.mainLight,
                    )

                    Spacer(modifier = Modifier.width(3.dp))

                    // 점수 (old: score, "/ 점수점")
                    val scoreText = remember(item.score) {
                        val scoreCount = NumberFormatUtil.formatWithComma(item.score)
                            .replace(",", "")
                        "/ ${scoreCount}점"
                    }
                    Text(
                        text = scoreText,
                        style = ExoTypo.title15.copy(
                            fontSize = 16.sp,
                            color = ColorPalette.mainLight,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // 급상승 표시 (우측 상단, old: iv_icon_up + tv_increase_step)
            if (item.suddenIncrease) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(end = 60.dp, top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(R.drawable.icon_up),
                        contentDescription = "급상승",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 35.dp, height = 28.dp)
                    )
                    Text(
                        text = stringResource(R.string.label_rising, item.difference),
                        style = ExoTypo.label10.copy(
                            color = ColorPalette.mainLight,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }
        }

        // 하단 Divider
        HorizontalDivider(
            thickness = 1.dp,
            color = ColorPalette.gray200
        )
    }
}

/**
 * HeartPickRankingItem for use in LazyRow
 * LazyRow 내부에서 사용할 수 있는 단일 ExoRankingItem
 *
 * old 프로젝트의 item_heart_pick_idol.xml 기준:
 * - 전체 크기: 250dp x 79dp
 * - 프로그레스바 높이: 17dp
 * - 투표율 글자색: text_dimmed
 */
@Composable
fun HeartPickRankingItem(
    item: RankingItem,
) {
    // 메인 랭킹 아이템 Row (old: 250dp -> 줄여서 180dp)
    Row(
        modifier = Modifier
            .width(200.dp)
            .padding(top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위 번호 (왼쪽 큰 숫자)
        Text(
            text = "${item.rank}",
            style = ExoTypo.title20,
            modifier = Modifier.width(24.dp)
        )

        // 프로필 이미지 (2위 이하 표시용 - MEDIUM, 55dp)
        ExoProfileImage(
            imageUrl = item.photoUrl,
            type = ProfileImageType.MEDIUM,
            rank = item.rank,
            contentDescription = "프로필 이미지"
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 정보 영역
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 이름 (old: name 15dp, group 10dp)
            ExoNameWithGroup(
                fullName = item.name,
                nameFontSize = 15.sp,
                groupFontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 프로그레스 바 (old: minHeight 17dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(17.dp)
            ) {
                // 프로그레스 계산: 20% ~ 80% 범위, 4th root 사용
                val progressPercent = remember(item.heartCount, item.maxHeartCount) {
                    if (item.maxHeartCount == 0L) {
                        0.2f
                    } else if (item.heartCount == 0L) {
                        0.2f
                    } else {
                        val voteRoot = kotlin.math.sqrt(kotlin.math.sqrt(item.heartCount.toDouble()))
                        val maxRoot = kotlin.math.sqrt(kotlin.math.sqrt(item.maxHeartCount.toDouble()))
                        val p = 20 + (voteRoot * 60 / maxRoot)
                        (p / 100f).toFloat().coerceIn(0.2f, 0.8f)
                    }
                }

                // 배경 (회색 - old: progressbar_ranking_background_400)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = ColorPalette.background400,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                        )
                )

                // 프로그레스 바 (MAIN 스타일 - gradient)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressPercent)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    ColorPalette.sLeagueProgress,
                                    ColorPalette.main
                                )
                            ),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                )

                // 퍼센트 표시 (ViewModel에서 계산된 percentage 사용)
                val percentageText = "${item.percentage}%"

                // 투표수와 퍼센트를 함께 배치
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 투표수: 색상바 우측에 배치
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressPercent)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        ExoHeartCounter(
                            count = item.heartCount,
                            style = ExoTypo.stat10.copy(lineHeight = 17.sp),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    // 퍼센트: 나머지 영역의 우측에 배치 (old: text_dimmed 색상)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = percentageText,
                            style = ExoTypo.stat10.copy(
                                lineHeight = 17.sp,
                                color = ColorPalette.textDimmed
                            ),
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * HofDailyRankingItem - 명예의 전당 일일 랭킹 아이템
 *
 * old 프로젝트의 hall_item.xml 및 HallOfFameDayAdapter 기반
 *
 * 주요 기능:
 * 1. 프로필 이미지 (40x40dp)
 * 2. Anniversary badges (생일, 데뷔, 컴백, 기념일)
 * 3. 이름 + 그룹명 + 순위 아이콘 (1/2/3위 왕관)
 * 4. 투표수 + 날짜
 *
 * @param item 일일 랭킹 아이템 데이터
 * @param cdnUrl CDN 베이스 URL
 * @param onItemClick 아이템 클릭 이벤트
 */
@Composable
fun HofDailyRankingItem(
    item: net.ib.mn.data.remote.dto.DailyRankModel,
    cdnUrl: String,
    chartCode: String = "",
    onItemClick: () -> Unit = {}
) {
    // LocalHofDailyItemClick 사용
    val localOnItemClick = LocalHofDailyItemClick.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onItemClick()
                    localOnItemClick(item, chartCode)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 영역 (old: ConstraintLayout, 70dp width, height는 40dp + margin 15dp * 2)
            Box(
                modifier = Modifier.size(70.dp),  // 70dp x 70dp
                contentAlignment = Alignment.Center
            ) {
                // 프로필 이미지 (old: iv_photo, 40dp x 40dp, margin 15dp)
                // old: ${cdnUrl}/h/${resourceId}.1_200x200.webp
                val hofId: Int = remember(item.id, item.resourceUri) {
                    item.getHofId()
                }
                val imageUrl: String = remember(hofId, cdnUrl) {
                    net.ib.mn.util.IdolImageUtil.getHofImageUrl(
                        cdnUrl = cdnUrl,
                        hofId = hofId
                    )
                }

                ExoProfileImage(
                    imageUrl = imageUrl,
                    rank = 0,  // Daily ranking에서는 rank badge 표시 안 함
                    modifier = Modifier.size(40.dp),
                    contentDescription = "프로필 이미지"
                )

                // Anniversary badges (old 프로젝트 ConstraintLayout 기준)
                when (item.idol?.anniversary) {
                    "Y" -> {  // ANNIVERSARY_BIRTH (생일)
                        // 솔로는 생일 배지, 그룹은 데뷔 배지
                        if (item.idol?.type == "S") {
                            Icon(
                                painter = painterResource(R.drawable.icon_anniversary_birth_medium),
                                contentDescription = "생일",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(44.dp)
                                    .align(Alignment.TopStart)
                                    .padding(top = 7.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.icon_anniversary_debut_medium),
                                contentDescription = "데뷔일",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(44.dp)
                                    .align(Alignment.TopStart)
                                    .padding(top = 7.dp)
                            )
                        }
                    }
                    "E" -> {  // ANNIVERSARY_DEBUT (데뷔일)
                        Icon(
                            painter = painterResource(R.drawable.icon_anniversary_debut_medium),
                            contentDescription = "데뷔일",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(44.dp)
                                .align(Alignment.TopStart)
                        )
                    }
                    "C" -> {  // ANNIVERSARY_COMEBACK (컴백일)
                        Icon(
                            painter = painterResource(R.drawable.icon_anniversary_comeback_medium),
                            contentDescription = "컴백일",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .size(width = 66.dp, height = 56.dp)
                                .align(Alignment.TopStart)
                                .padding(top = 12.dp)
                        )
                    }
                    "D" -> {  // ANNIVERSARY_MEMORIAL_DAY (기념일)
                        item.idol?.anniversaryDays?.let { days ->
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(end = 7.dp, bottom = 11.dp)
                                    .background(
                                        color = ColorPalette.main,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            10.dp
                                        )
                                    )
                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${days}${stringResource(R.string.lable_day)}",
                                    style = ExoTypo.body7.copy(
                                        color = ColorPalette.textWhiteBlack,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 우측 정보 영역 (old: ll_container)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 15.dp),
                verticalArrangement = Arrangement.Center
            ) {
                // 이름 + 그룹명 + 순위 아이콘
                Row(
                    modifier = Modifier.fillMaxWidth().padding(end=11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이름 + 그룹명
                    ExoNameWithGroup(
                        fullName = item.idol?.name ?: "",
                        modifier = Modifier,  // weight 제거 - 컨텐츠 크기만큼만 차지
                        nameFontSize = 15.sp,
                        groupFontSize = 10.sp
                    )

                    // 왕관을 우측 끝으로 밀어주는 Spacer
                    Spacer(modifier = Modifier.weight(1f))

                    // 순위 아이콘 (1,2,3위 왕관, old: iv_rank_icon)
                    // rank는 0부터 시작 (0 = 1위, 1 = 2위, 2 = 3위)
                    val rank = item.idol?.rank ?: -1
                    if (rank in 0..2) {
                        val iconRes = when (rank) {
                            0 -> R.drawable.icon_rating_heart_voting_1st
                            1 -> R.drawable.icon_rating_heart_voting_2nd
                            2 -> R.drawable.icon_rating_heart_voting_3rd
                            else -> null
                        }
                        iconRes?.let {
                            Icon(
                                painter = painterResource(it),
                                contentDescription = "${rank + 1}위",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(width = 18.dp, height = 12.dp).offset(y=(5).dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // 투표수 + 날짜 (old: tv_count + tv_date)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 투표수 (old: tv_count)
                    val voteCountText = remember(item.heart) {
                        NumberFormatUtil.formatWithComma(item.heart)
                    }
                    Text(
                        text = stringResource(R.string.vote_count_format, voteCountText),
                        style = ExoTypo.body13.copy(color = ColorPalette.gray580),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    val dateText = remember(item.createdAt) {
                        try {
                            // 1. 원본 문자열 형식을 정의합니다. (T가 있으므로 'T'를 추가)
                            val isoParser = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

                            // 2. 문자열을 'LocalDateTime' (시간대 정보가 없는) 객체로 파싱합니다.
                            val localDateTime = LocalDateTime.parse(item.createdAt, isoParser)

                            // 3. 이 시간이 "Asia/Seoul" 시간대임을 명시적으로 지정합니다.
                            val kstZoneId = ZoneId.of("Asia/Seoul")
                            val zonedDateTime = localDateTime.atZone(kstZoneId)

                            // 4. 사용자의 현재 로케일에 맞는 중간 길이(MEDIUM) 날짜 형식으로 포매터를 만듭니다.
                            val userLocale = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
                            val displayFormatter = DateTimeFormatter
                                .ofLocalizedDate(FormatStyle.MEDIUM)
                                .withLocale(userLocale)
                                .withZone(kstZoneId) // 포매터에도 KST를 지정 (명확성을 위해)

                            // 5. 포맷합니다.
                            displayFormatter.format(zonedDateTime)

                        } catch (e: Exception) {
                            // 파싱 실패 시 원본 문자열 반환
                            item.createdAt
                        }
                    }
                    Text(
                        text = dateText,
                        style = ExoTypo.body12.copy(color = ColorPalette.gray580)
                    )
                }
            }
        }

        // 하단 Divider
        HorizontalDivider(
            thickness = 0.5.dp,
            color = ColorPalette.gray200
        )
    }
}
