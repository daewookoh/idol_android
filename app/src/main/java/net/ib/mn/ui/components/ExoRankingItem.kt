package net.ib.mn.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import net.ib.mn.ui.theme.ColorPalette
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import net.ib.mn.R
import net.ib.mn.ui.theme.ExoTypo
import java.text.NumberFormat

/**
 * ExoRankingItem - 랭킹 아이템 리스트 렌더링 (로우레벨 구현)
 *
 * MainRankingList의 LazyColumn 내부에서 랭킹 아이템들을 표시
 *
 * old 프로젝트 ranking_item.xml 기반 리뉴얼
 * 주요 기능:
 * 1. 프로필 이미지 + 테두리 (miracleCount, fairyCount, angelCount 기준)
 * 2. 배지 시스템 (생일, 데뷔, 컴백, 몰빵일, 올인데이)
 * 3. 순위 + 이름 + 그룹명
 * 4. 투표수 프로그레스 바
 * 5. 아이콘 배지 (Angel, Fairy, Miracle, Rookie, Super Rookie)
 * 6. 하트 투표 버튼
 * 7. 최애 하이라이트 (배경색 변경)
 * 8. 펼치기 기능 (ExoTop3 사용)
 *
 * @param items 랭킹 아이템 리스트
 * @param type 랭킹 타입 ("MAIN" = 큰 이미지, "DAILY" = 작은 이미지, "AGGREGATE" = 누적 랭킹 아이템, 기본값: "MAIN")
 * @param onItemClick 아이템 클릭 이벤트
 */
fun LazyListScope.exoRankingItems(
    items: List<RankingItemData>,
    type: String = "MAIN",
    onItemClick: (Int, RankingItemData) -> Unit = { _, _ -> },
    onVoteSuccess: (idolId: Int, voteCount: Long) -> Unit = { _, _ -> }
) {
    // 랭킹 아이템 리스트
    // key를 사용하여 아이템이 변경될 때 올바른 리컴포지션 수행
    // animateItemPlacement: 순위 변경 시 리스트 내 위치 이동 애니메이션
    itemsIndexed(
        items = items,
        key = { _, item -> item.itemKey() }
    ) { index, item ->
        // 리컴포지션 카운터 (디버그용)
        var recompositionCount by remember { mutableStateOf(0) }
        SideEffect {
            recompositionCount++
            // 5회 이상 리컴포지션되면 경고 로그 (최적화 필요 신호)
            if (recompositionCount == 5) {
                android.util.Log.w("Recomposition", "⚠️ Item ${item.id} (${item.name}) recomposed $recompositionCount times")
            } else if (recompositionCount > 10) {
                android.util.Log.e("Recomposition", "🔴 Item ${item.id} (${item.name}) recomposed $recompositionCount times - Optimization needed!")
            }
        }

        // AGGREGATE 타입: 완전히 다른 UI 구조 (누적 랭킹용)
        if (type == "AGGREGATE") {
            AggregatedRankingItem(
                index = index,
                item = item,
                totalItems = items.size,
                onItemClick = onItemClick
            )
            return@itemsIndexed
        }

        // 타입에 따른 프로필 이미지 사이즈
        val (profileAreaWidth, borderSize, imageSize) = remember(type) {
            when (type) {
                "DAILY" -> Triple(60.dp, 40.dp, 32.dp)  // DAILY: 작은 사이즈
                else -> Triple(81.dp, 55.dp, 45.dp)  // MAIN: 기본 사이즈
            }
        }

        var isExpanded by remember { mutableStateOf(false) }

        // 최애 여부에 따른 배경색
        val backgroundColor = if (item.isFavorite) ColorPalette.main100 else ColorPalette.background100

        Column(
            modifier = Modifier
                .animateItem(
                    fadeInSpec = null,
                    fadeOutSpec = null,
                    placementSpec = tween(
                        durationMillis = 500,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                )
                .fillMaxWidth()
                .background(backgroundColor)
        ) {
            // 메인 랭킹 아이템 (old: line 16-337)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(index, item) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 프로필 영역 (old: line 30-135, width: 81dp + marginStart: 20dp)
                Spacer(modifier = Modifier.width(20.dp))

                Box(
                    modifier = Modifier.size(borderSize),
                    contentAlignment = Alignment.Center
                ) {
                    // 프로필 테두리 + 이미지 (old PNG 사용)
                    Box(
                        modifier = Modifier.size(borderSize),
                        contentAlignment = Alignment.Center
                    ) {
                        // 테두리 이미지 결정 (old 프로젝트의 flag 기반 로직)
                        val borderDrawable = remember(item.miracleCount, item.fairyCount, item.angelCount) {
                            var flag = 0
                            if (item.miracleCount >= 1) flag += 1
                            if (item.fairyCount >= 1) flag += 2
                            if (item.angelCount >= 1) flag += 4

                            when (flag) {
                                0 -> R.drawable.profile_round_off
                                1 -> R.drawable.profile_round_miracle
                                2 -> R.drawable.profile_round_fairy
                                3 -> R.drawable.profile_round_fairy_miracle
                                4 -> R.drawable.profile_round_angel
                                5 -> R.drawable.profile_round_angel_miracle
                                6 -> R.drawable.profile_round_angel_fairy
                                7 -> R.drawable.profile_round_angel_fairy_miracle
                                else -> R.drawable.profile_round_off
                            }
                        }

                        // 테두리 PNG 이미지
                        Icon(
                            painter = painterResource(borderDrawable),
                            contentDescription = "Profile border",
                            modifier = Modifier.size(borderSize),
                            tint = Color.Unspecified
                        )

                        // 프로필 이미지
                        ExoProfileImage(
                            imageUrl = item.photoUrl,
                            rank = item.rank,
                            contentDescription = "프로필 이미지",
                            modifier = Modifier
                                .size(imageSize)
                                .clickable { isExpanded = !isExpanded }
                        )
                    }

                    // 기념일 배지
                    Box(modifier = Modifier.size(borderSize)) {
                        when (item.anniversary) {
                            "BIRTH" -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 5.dp, top = 3.dp)
                                        .size(16.dp)
                                        .background(ColorPalette.badgeBirth, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "B",
                                        style = ExoTypo.label8
                                    )
                                }
                            }
                            "DEBUT" -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 5.dp, top = 3.dp)
                                        .size(16.dp)
                                        .background(ColorPalette.badgeDebut, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "D",
                                        style = ExoTypo.label8
                                    )
                                }
                            }
                            "COMEBACK" -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 3.dp)
                                        .size(16.dp)
                                        .background(ColorPalette.badgeComeback, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "C",
                                        style = ExoTypo.label8
                                    )
                                }
                            }
                            "MEMORIAL_DAY" -> {
                                // 몰빵일 배지 - 다국어 처리
                                // lable_day: ko=일, ja=日, zh=日, en=""
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .background(
                                            color = ColorPalette.badgeMemorialDay,
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    val dayLabel = stringResource(R.string.lable_day)
                                    Text(
                                        text = remember(item.anniversaryDays, dayLabel) {
                                            "${item.anniversaryDays}$dayLabel"
                                        },
                                        style = ExoTypo.label7.copy(color = ColorPalette.white)
                                    )
                                }
                            }
                            "ALL_IN_DAY" -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 15.dp, top = 5.dp)
                                        .size(16.dp)
                                        .background(ColorPalette.badgeAllinDay, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A",
                                        style = ExoTypo.label8
                                    )
                                }
                            }
                        }
                    }
                }

                // 정보 영역 (old: line 137-323)
                // paddingStart: 15dp (old: line 140)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 15.dp)
                ) {
                    // 순위 + 이름 + 그룹명
                    // 순위: 세로 중앙, 이름: 순위와 세로 중앙 정렬, 그룹명: 이름의 bottom에 맞춤
                    Row(
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 순위 (세로 중앙 정렬)
                        Text(
                            text = stringResource(R.string.rank_count_format, item.rank),
                            style = ExoTypo.title15
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        // 이름 + 그룹명 (이름은 세로 중앙, 그룹명은 이름 bottom에 맞춤)
                        ExoNameWithGroup(
                            fullName = item.name,
                            nameFontSize = 15.sp,
                            groupFontSize = 10.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    // 프로그레스 바 + 투표수 + 배지 아이콘 (old: line 195-330)
                    // old: FrameLayout으로 배지를 프로그레스바 위에 겹침
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 3.dp) // old: paddingBottom="3dp"
                    ) {
                        // 프로그레스 바 + 투표수 (old: ConstraintLayout, line 208-243)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(17.dp) // old: minHeight="17dp"
                        ) {
                            // 프로그레스 바 계산: old 프로젝트와 동일한 로직
                            // 38% ~ 100% 범위, 4th root 사용 (sqrt의 sqrt)
                            val progressPercent = remember(item.heartCount, item.maxHeartCount) {
                                if (item.maxHeartCount == 0L) {
                                    0.38f // 기본값 38%
                                } else if (item.heartCount == 0L) {
                                    0.38f // 0표는 38%
                                } else {
                                    // old: 38 + (sqrt(sqrt(voteCount)) * 62 / sqrt(sqrt(maxVoteCount)))
                                    val voteRoot = kotlin.math.sqrt(kotlin.math.sqrt(item.heartCount.toDouble()))
                                    val maxRoot = kotlin.math.sqrt(kotlin.math.sqrt(item.maxHeartCount.toDouble()))
                                    val p = 38 + (voteRoot * 62 / maxRoot) // toInt() 제거하여 정확한 계산
                                    (p / 100f).toFloat().coerceIn(0.38f, 1f)
                                }
                            }

                            val animatedProgress by animateFloatAsState(targetValue = progressPercent, label = "progress")

                            // 색칠된 프로그레스 바 영역
                            // type "DAILY": a_league_progress 단색, 애니메이션 없음
                            // type "MAIN": s_league_progress → main gradient, 10초마다 반복 애니메이션
                            val isTypeDaily = type == "DAILY"

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .background(
                                        brush = if (isTypeDaily) {
                                            // DAILY: a_league_progress 단색 (Old 프로젝트 기준)
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    ColorPalette.aLeagueProgress,
                                                    ColorPalette.aLeagueProgress
                                                )
                                            )
                                        } else {
                                            // MAIN: gradient
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    ColorPalette.sLeagueProgress,
                                                    ColorPalette.main
                                                )
                                            )
                                        },
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                            ) {
                                // type에 따른 애니메이션 처리
                                // type "MAIN": 10초마다 반복 애니메이션
                                // type "DAILY": 애니메이션 없음
                                // 기타: progressPercent 변경 시에만 애니메이션
                                val isTypeMain = type == "MAIN"

                                if (!isTypeDaily) {
                                    // 애니메이션 진행도
                                    val shimmerProgress = remember { androidx.compose.animation.core.Animatable(0f) }

                                    if (isTypeMain) {
                                        // type "MAIN": 처음 렌더링 시 바로 반짝임 + 10초마다 반복 애니메이션
                                        LaunchedEffect(Unit) {
                                            // 처음 렌더링 시 즉시 반짝임 실행
                                            shimmerProgress.snapTo(0f)
                                            shimmerProgress.animateTo(
                                                targetValue = 1f,
                                                animationSpec = tween(
                                                    durationMillis = 1000,
                                                    easing = LinearEasing
                                                )
                                            )

                                            // 그 후 10초마다 반복
                                            while (true) {
                                                kotlinx.coroutines.delay(10000) // 10초 대기
                                                shimmerProgress.snapTo(0f)
                                                shimmerProgress.animateTo(
                                                    targetValue = 1f,
                                                    animationSpec = tween(
                                                        durationMillis = 1000,
                                                        easing = LinearEasing
                                                    )
                                                )
                                            }
                                        }
                                    } else {
                                        // 기타: progressPercent 변경 시 애니메이션 트리거
                                        LaunchedEffect(progressPercent) {
                                            shimmerProgress.snapTo(0f)
                                            shimmerProgress.animateTo(
                                                targetValue = 1f,
                                                animationSpec = tween(
                                                    durationMillis = 1000,
                                                    easing = LinearEasing
                                                )
                                            )
                                        }
                                    }

                                    // 반짝임 효과 Canvas
                                    Canvas(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .fillMaxHeight()
                                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                    ) {
                                        val canvasWidth = size.width
                                        val canvasHeight = size.height
                                        val progress = shimmerProgress.value

                                        if (progress > 0f && progress < 1f) {
                                            // 반짝이는 흰색 그라데이션 라인
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
                            }

                            // 투표수 텍스트 - 애니메이션 위에 오버레이
                            // 최적화: voteCount가 변경될 때만 너비 재측정
                            BoxWithConstraints(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val barWidth = maxWidth * animatedProgress

                                // 텍스트 너비 측정 - voteCount 변경 시에만 재측정
                                var textWidthPx by remember(item.voteCount) { mutableStateOf(0) }
                                val density = LocalDensity.current
                                val textWidthDp = remember(textWidthPx) {
                                    with(density) { textWidthPx.toDp() }
                                }

                                Box(
                                    modifier = Modifier
                                        .offset(x = (barWidth - textWidthDp - 6.dp).coerceAtLeast(0.dp))
                                        .wrapContentWidth()
                                        .height(17.dp)
                                        .onGloballyPositioned { coordinates ->
                                            // voteCount가 같으면 재측정하지 않음
                                            val newWidth = coordinates.size.width
                                            if (textWidthPx != newWidth) {
                                                textWidthPx = newWidth
                                            }
                                        },
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = item.voteCount,
                                        style = ExoTypo.stat11.copy(
                                            fontWeight = FontWeight.Normal,
                                            lineHeight = 17.sp
                                        )
                                    )
                                }
                            }
                        }

                        // 아이콘 배지 - 프로그레스바 위에 겹침 (old: line 247-328)
                        // old: marginStart="5dp", marginTop="-3dp"
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.Top,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .offset(y = (-3).dp) // old: marginTop="-3dp"
                        ) {
                        // Angel 배지 (old: line 253-266)
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
                                    modifier = Modifier.offset(y = 6.dp)
                                )
                            }
                        }

                        // Fairy 배지 (old: line 268-282)
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
                                    modifier = Modifier.offset(y = 6.dp)
                                )
                            }
                        }

                        // Miracle 배지 (old: line 284-297)
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
                                    modifier = Modifier.offset(y = 6.dp)
                                )
                            }
                        }

                        // Rookie 배지 (old: line 299-312)
                        if (item.rookieCount > 0) {
                            Box(
                                modifier = Modifier.size(13.dp, 16.dp),
                                contentAlignment = Alignment.TopCenter
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.charity_rookie_badge),
                                    contentDescription = null,
                                    modifier = Modifier.size(13.dp, 16.dp),
                                    tint = Color.Unspecified
                                )
                                Text(
                                    text = remember(item.rookieCount) { item.rookieCount.toString() },
                                    style = ExoTypo.label7.copy(color = ColorPalette.textRookie),
                                    modifier = Modifier.offset(y = 6.dp)
                                )
                            }
                        }

                        // Super Rookie 배지 (old: line 314-327)
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
                                    modifier = Modifier.offset(y = 6.dp)
                                )
                            }
                        }
                        }
                    }
                }

                // 하트 투표 버튼 (old: line 327-335)
                // layout_width/height: 50dp, padding: 10dp, layout_margin: 5dp
                ExoVoteIcon(
                    idolId = item.id.toIntOrNull() ?: 0,
                    fullName = item.name,  // name은 이미 "이름_그룹명" 형식
                    onVoteSuccess = { votedHeart ->
                        android.util.Log.d("ExoRankingItem", "💗 Voted $votedHeart hearts to ${item.name}")
                        // 부모 컴포넌트에 투표 성공 알림
                        val idolId = item.id.toIntOrNull() ?: 0
                        onVoteSuccess(idolId, votedHeart)
                    }
                )
            }

            // 펼치기 영역 (ExoTop3)
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ExoTop3(
                    id = remember(item.rank) { "ranking_item_${item.rank}" },
                    imageUrls = item.top3ImageUrls,
                    videoUrls = item.top3VideoUrls,
                    isVisible = isExpanded
                )
            }

            // 하단 Divider (아이템 구분선)
            // old 버전에서는 RecyclerView ItemDecoration으로 처리했지만
            // Compose에서는 아이템에 직접 추가
            if (index < items.size - 1) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = ColorPalette.gray200
                )
            }
        }
    }
}

/**
 * AggregatedRankingItem - 누적 랭킹 아이템 (old: aggregated_hof_item.xml 기반)
 *
 * 주요 차이점:
 * - 순위 아이콘 (1/2/3위 왕관, 나머지 숫자)
 * - 순위 변동 표시 (NEW, UP/DOWN)
 * - 작은 원형 프로필 이미지 (테두리 없음)
 * - 점수 표시 (하트 개수 대신)
 * - 날짜 표시
 * - 투표 버튼 없음
 * - 프로그레스 바 없음
 */
@Composable
private fun AggregatedRankingItem(
    index: Int,
    item: RankingItemData,
    totalItems: Int,
    onItemClick: (Int, RankingItemData) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick(index, item) }
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 순위 영역 (old: container_ranking, 45dp width)
            Column(
                modifier = Modifier.width(45.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)  // old: constraintTop_toBottomOf with no margin
            ) {
                // 1,2,3위 왕관 아이콘 (old: icon_ranking)
                when (item.rank) {
                    1 -> Icon(
                        painter = painterResource(R.drawable.icon_rating_heart_voting_1st),
                        contentDescription = "1st",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 24.dp, height = 18.dp)
                    )
                    2 -> Icon(
                        painter = painterResource(R.drawable.icon_rating_heart_voting_2nd),
                        contentDescription = "2nd",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 24.dp, height = 18.dp)
                    )
                    3 -> Icon(
                        painter = painterResource(R.drawable.icon_rating_heart_voting_3rd),
                        contentDescription = "3rd",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 24.dp, height = 18.dp)
                    )
                }

                // 순위 텍스트 (old: rank)
                // 1,2,3등은 main 컬러, 그 외는 gray580 컬러 (old: HallOfFameAggAdapter.kt line 263, 266)
                Text(
                    text = stringResource(R.string.rank_count_format, item.rank),
                    style = ExoTypo.body11.copy(
                        color = if (item.rank <= 3) ColorPalette.main else ColorPalette.gray580
                    )
                )

                // 순위 변동 표시 (TODO: rankChange 필드 추가 필요)
                // icon_change_ranking_new, icon_change_ranking_up, icon_change_ranking_down
            }

            // 프로필 이미지
            ExoProfileImage(
                imageUrl = item.photoUrl,
                rank = item.rank,
                contentDescription = "프로필 이미지",
                modifier = Modifier.size(41.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 이름 + 그룹 + 점수 + 날짜 영역 (old: cl_name with chainStyle="packed", marginStart="10dp")
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)  // chainStyle="packed" 재현
            ) {
                // 이름 + 그룹명 (old: name, group)
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    ExoNameWithGroup(
                        fullName = item.name,
                        nameFontSize = 14.sp,
                        groupFontSize = 10.sp
                    )
                }

                // 점수 + 날짜 (old: score, date)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 점수 (TODO: score 필드 추가 필요, 현재는 voteCount 사용)
                    Text(
                        text = "${item.voteCount}점",
                        style = ExoTypo.body11
                    )

                    // 날짜 (TODO: date 필드 추가 필요)
                    // Text(
                    //     text = item.date ?: "",
                    //     fontSize = 12.sp,
                    //     color = ColorPalette.gray200
                    // )
                }
            }

            // 우측 화살표 (old: iv_arrow_go, 8dp)
            Icon(
                painter = painterResource(R.drawable.btn_go),
                contentDescription = "Go",
                modifier = Modifier
                    .size(8.dp)
                    .padding(end = 20.dp),
                tint = Color.Unspecified
            )
        }

        // 하단 Divider
        if (index < totalItems - 1) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = ColorPalette.gray200
            )
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
                .clickable { onItemClick() }
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
                // 프로필 영역 (old: best1_profile, 180dp x 98dp)
                Box(
                    modifier = Modifier
                        .width(180.dp)
                        .height(98.dp)
                        .padding(top = 15.dp),
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
                        modifier = Modifier.padding(top = 5.dp, bottom = 26.dp)
                    ) {
                        ExoProfileImage(
                            imageUrl = imageUrl,
                            rank = item.scoreRank,
                            modifier = Modifier.size(72.dp),
                            contentDescription = "프로필 이미지"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(5.dp))

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
                                            NumberFormat.getNumberInstance().format(item.difference)
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
                        groupColor = ColorPalette.mainLight
                    )

                    Spacer(modifier = Modifier.width(3.dp))

                    // 점수 (old: score, "/ 점수점")
                    val scoreText = remember(item.score) {
                        val scoreCount = NumberFormat.getNumberInstance().format(item.score)
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
 * HofAccumulativeRankingItem - 명예의 전당 누적 랭킹 아이템
 *
 * old 프로젝트의 aggregated_hof_item.xml 및 HallAggregatedAdapter.kt 기반
 *
 * 주요 기능:
 * 1. 순위 아이콘 (1/2/3위 왕관)
 * 2. 순위 변동 표시 (NEW, UP/DOWN)
 * 3. 작은 원형 프로필 이미지
 * 4. 점수 표시
 * 5. 우측 화살표
 * 6. 급상승 1위 하이라이트
 *
 * @param item 랭킹 아이템 데이터 (AggregateRankModel을 변환한 데이터)
 * @param cdnUrl CDN 베이스 URL (PreferencesManager에서 가져온 값)
 * @param onItemClick 아이템 클릭 이벤트
 */
@Composable
fun HofAccumulativeRankingItem(
    item: net.ib.mn.data.remote.dto.AggregateRankModel,
    cdnUrl: String,
    onItemClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 급상승 여부에 따라 배경 설정 (old: bg_cumulative_best)
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 급상승일 때 좌측 띠 배경 표시
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
                    .clickable { onItemClick() }
                    .background(if (item.suddenIncrease) Color.Transparent else ColorPalette.background100)
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            // 순위 영역 (old: container_ranking, 45dp width)
            Column(
                modifier = Modifier.width(45.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
            ) {
                // 1,2,3위 왕관 아이콘 (old: icon_ranking)
                // scoreRank는 1부터 시작 (API 응답 기준)
                when (item.scoreRank) {
                    1 -> Icon(
                        painter = painterResource(R.drawable.icon_rating_heart_voting_1st),
                        contentDescription = "1st",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 24.dp, height = 18.dp)
                    )
                    2 -> Icon(
                        painter = painterResource(R.drawable.icon_rating_heart_voting_2nd),
                        contentDescription = "2nd",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 24.dp, height = 18.dp)
                    )
                    3 -> Icon(
                        painter = painterResource(R.drawable.icon_rating_heart_voting_3rd),
                        contentDescription = "3rd",
                        tint = Color.Unspecified,
                        modifier = Modifier.size(width = 24.dp, height = 18.dp)
                    )
                }

                // 순위 텍스트 (old: rank)
                // 1,2,3등은 main 컬러, 그 외는 text_default 컬러
                Text(
                    text = stringResource(R.string.rank_count_format, item.scoreRank),
                    style = ExoTypo.body11.copy(
                        color = if (item.scoreRank <= 3) ColorPalette.main else ColorPalette.textDefault
                    )
                )

                // 순위 변동 표시 (old: icon_new_ranking, ll_change_ranking)
                when (item.status.lowercase()) {
                    "new" -> {
                        // NEW 아이콘
                        Icon(
                            painter = painterResource(R.drawable.icon_change_ranking_new),
                            contentDescription = "NEW",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(width = 15.dp, height = 8.dp)
                        )
                    }
                    "increase", "decrease", "same" -> {
                        // UP/DOWN/NO_CHANGE 아이콘 + 수치
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val iconRes = when (item.status.lowercase()) {
                                "increase" -> R.drawable.icon_change_ranking_up
                                "decrease" -> R.drawable.icon_change_ranking_down
                                else -> R.drawable.icon_change_ranking_no_change
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
                                        NumberFormat.getNumberInstance().format(item.difference)
                                    },
                                    style = ExoTypo.label9.copy(color = ColorPalette.textDefault)
                                )
                            }
                        }
                    }
                }
            }

            // 프로필 이미지 (old: photo, 41dp x 41dp, container_ranking 바로 옆)
            // old: UtilK.trendImageUrl(context, trendId) → ${cdnUrl}/t/${id}.1_200x200.webp
            val imageUrl = remember(item.trendId, cdnUrl) {
                net.ib.mn.util.IdolImageUtil.getTrendImageUrl(
                    cdnUrl = cdnUrl,
                    trendId = item.trendId
                )
            }

            ExoProfileImage(
                imageUrl = imageUrl,
                rank = item.scoreRank,
                modifier = Modifier.size(41.dp),
                contentDescription = "프로필 이미지"
            )

            Spacer(modifier = Modifier.width(10.dp))

            // 이름 + 그룹 + 점수 영역 (old: cl_name)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(0.dp, Alignment.CenterVertically)
            ) {
                // 이름 + 그룹명 (old: name, group)
                ExoNameWithGroup(
                    fullName = item.name,
                    nameFontSize = 14.sp,
                    groupFontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(2.dp))

                // 점수 표시 (old: score)
                val scoreText = remember(item.score) {
                    val scoreCount = NumberFormat.getNumberInstance().format(item.score)
                        .replace(",", "")
                    "${scoreCount}점"
                }
                Text(
                    text = scoreText,
                    style = ExoTypo.body11.copy(color = ColorPalette.textGray)
                )
            }

            // 급상승 표시 (old: iv_icon_up + tv_increase_step)
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

            // 우측 화살표 (old: iv_arrow_go, layout_marginEnd="20dp")
            Icon(
                painter = painterResource(R.drawable.btn_go),
                contentDescription = "Go",
                modifier = Modifier.size(12.dp),
                tint = Color.Unspecified
            )

            // 우측 마진 (old: layout_marginEnd="20dp")
            Spacer(modifier = Modifier.width(20.dp))
            }  // Row 닫기
        }  // Box 닫기

        // 하단 Divider
        HorizontalDivider(
            thickness = 0.5.dp,
            color = ColorPalette.gray200
        )
    }  // Column 닫기
}

/**
 * HeartPickRankingItem for use in LazyRow
 * LazyRow 내부에서 사용할 수 있는 단일 ExoRankingItem
 */
@Composable
fun HeartPickRankingItem(
    item: RankingItemData,
) {
    // HeartPick용 고정 사이즈
    val imageSize = 50.dp

    // 기기 너비 가져오기
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val minWidth = screenWidth * 0.6f

    // 메인 랭킹 아이템 Row
    Row(
        modifier = Modifier
            .widthIn(min = minWidth)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 순위 번호 (왼쪽 큰 숫자)
        Text(
            text = "${item.rank}",
            style = ExoTypo.title20,
            modifier = Modifier.width(24.dp)
        )

        // 프로필 이미지
        ExoProfileImage(
            imageUrl = item.photoUrl,
            rank = item.rank,
            contentDescription = "프로필 이미지",
            modifier = Modifier.size(imageSize)
        )

        Spacer(modifier = Modifier.width(8.dp))

        // 정보 영역
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // 이름
            ExoNameWithGroup(
                fullName = item.name,
                nameFontSize = 12.sp,
                groupFontSize = 10.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 프로그레스 바 (MAIN 스타일 gradient)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
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

                // 배경 (회색)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = ColorPalette.gray100,
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

                // 퍼센트 계산
                val percentage = remember(item.heartCount, item.maxHeartCount) {
                    android.util.Log.d("HeartPickPercentage", "Rank ${item.rank}: heartCount=${item.heartCount}, maxHeartCount=${item.maxHeartCount}")
                    if (item.maxHeartCount > 0) {
                        val percent = (100.0 * item.heartCount / item.maxHeartCount).toInt()
                        android.util.Log.d("HeartPickPercentage", "Rank ${item.rank}: Calculated percentage=${percent}%")
                        "${percent}%"
                    } else {
                        "0%"
                    }
                }

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
                        Text(
                            text = item.voteCount,
                            style = ExoTypo.stat10.copy(lineHeight = 20.sp),
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }

                    // 퍼센트: 나머지 영역의 우측에 배치
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Text(
                            text = percentage,
                            style = ExoTypo.stat10.copy(lineHeight = 20.sp),
                            modifier = Modifier.padding(end = 8.dp)
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
    onItemClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ColorPalette.background100)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onItemClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 프로필 영역 (old: ConstraintLayout, 70dp width, height는 40dp + margin 15dp * 2)
            Box(
                modifier = Modifier.size(70.dp),  // 70dp x 70dp
                contentAlignment = Alignment.Center
            ) {
                // 프로필 이미지 (old: iv_photo, 40dp x 40dp, margin 15dp)
                // Center 정렬이므로 자동으로 좌우 15dp margin 효과
                val imageUrl = remember(item.trendId, cdnUrl) {
                    net.ib.mn.util.IdolImageUtil.getTrendImageUrl(
                        cdnUrl = cdnUrl,
                        trendId = item.trendId
                    )
                }

                ExoProfileImage(
                    imageUrl = imageUrl,
                    rank = item.rank,
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
                                    .size(36.dp)
                                    .align(Alignment.TopStart)
                                    .padding(top = 7.dp)
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.icon_anniversary_debut_medium),
                                contentDescription = "데뷔일",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(36.dp)
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
                                .size(36.dp)
                                .align(Alignment.TopStart)
                                .padding(top = 7.dp)
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
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(end = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 이름 + 그룹명
                    ExoNameWithGroup(
                        fullName = item.name,
                        modifier = Modifier.weight(1f, fill = false),
                        nameFontSize = 15.sp,
                        groupFontSize = 10.sp
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    // 순위 아이콘 (1,2,3위 왕관, old: iv_rank_icon)
                    if (item.rank in 1..3) {
                        val iconRes = when (item.rank) {
                            1 -> R.drawable.icon_rating_heart_voting_1st
                            2 -> R.drawable.icon_rating_heart_voting_2nd
                            3 -> R.drawable.icon_rating_heart_voting_3rd
                            else -> null
                        }
                        iconRes?.let {
                            Icon(
                                painter = painterResource(it),
                                contentDescription = "${item.rank}위",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(width = 24.dp, height = 18.dp)
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
                    Text(
                        text = stringResource(R.string.vote_count_format, item.heart),
                        style = ExoTypo.body13.copy(color = ColorPalette.gray580),
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(5.dp))

                    // 날짜 (old: tv_date)
                    Text(
                        text = item.createdAt,
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
