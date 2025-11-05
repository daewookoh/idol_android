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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
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
 * @param type 랭킹 타입 ("S" = 큰 이미지, "A" = 작은 이미지, 기본값: "S")
 * @param onItemClick 아이템 클릭 이벤트
 */
fun LazyListScope.exoRankingItem(
    items: List<RankingItemData>,
    type: String = "S",
    onItemClick: (Int, RankingItemData) -> Unit = { _, _ -> }
) {
    // 랭킹 아이템 리스트
    // key를 사용하여 아이템이 변경될 때 올바른 리컴포지션 수행
    // animateItemPlacement: 순위 변경 시 리스트 내 위치 이동 애니메이션
    itemsIndexed(
        items = items,
        key = { _, item -> item.itemKey() }
    ) { index, item ->
        // 타입에 따른 프로필 이미지 사이즈
        val (profileAreaWidth, borderSize, imageSize) = remember(type) {
            when (type) {
                "A" -> Triple(60.dp, 40.dp, 32.dp)  // A급: 작은 사이즈
                else -> Triple(81.dp, 55.dp, 45.dp)  // S급: 기본 사이즈
            }
        }

        var isExpanded by remember { mutableStateOf(false) }

        // 최애 여부에 따른 배경색
        val backgroundColor = remember(item.isFavorite) {
            if (item.isFavorite) R.color.main100 else R.color.background_100
        }

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
                .background(colorResource(backgroundColor))
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

                        // 프로필 이미지 - 24시간 캐싱
                        val context = LocalContext.current
                        val imageRequest = remember(item.photoUrl) {
                            ImageRequest.Builder(context)
                                .data(item.photoUrl)
                                .memoryCacheKey(item.photoUrl)
                                .diskCacheKey(item.photoUrl)
                                .diskCachePolicy(CachePolicy.ENABLED)
                                .memoryCachePolicy(CachePolicy.ENABLED)
                                .crossfade(true)
                                .build()
                        }

                        AsyncImage(
                            model = imageRequest,
                            contentDescription = "프로필 이미지",
                            modifier = Modifier
                                .size(imageSize)
                                .clip(CircleShape)
                                .background(colorResource(R.color.gray100))
                                .clickable { isExpanded = !isExpanded },
                            contentScale = ContentScale.Crop
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
                                        .background(colorResource(R.color.badge_birth), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "B",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(R.color.white)
                                    )
                                }
                            }
                            "DEBUT" -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 5.dp, top = 3.dp)
                                        .size(16.dp)
                                        .background(colorResource(R.color.badge_debut), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "D",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(R.color.white)
                                    )
                                }
                            }
                            "COMEBACK" -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .padding(start = 3.dp)
                                        .size(16.dp)
                                        .background(colorResource(R.color.badge_comeback), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "C",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(R.color.white)
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
                                            color = colorResource(R.color.badge_memorial_day),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    val dayLabel = stringResource(R.string.lable_day)
                                    Text(
                                        text = remember(item.anniversaryDays, dayLabel) {
                                            "${item.anniversaryDays}$dayLabel"
                                        },
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(R.color.white)
                                    )
                                }
                            }
                            "ALL_IN_DAY" -> {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 15.dp, top = 5.dp)
                                        .size(16.dp)
                                        .background(colorResource(R.color.badge_allin_day), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "A",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colorResource(R.color.white)
                                    )
                                }
                            }
                        }
                    }
                }

                // 정보 영역 (old: line 137-323)
                // marginStart: 10dp (old: line 142)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                ) {
                    // 순위 + 이름 + 그룹명 (old: line 145-184)
                    Row(
                        horizontalArrangement = Arrangement.Start
                    ) {
                        // 순위 (old: line 145-154, @color/main, 15sp, bold)
                        Text(
                            text = stringResource(R.string.rank_count_format, item.rank),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.main),
                            modifier = Modifier.alignByBaseline()
                        )

                        Spacer(modifier = Modifier.width(5.dp))

                        // 이름 (old: line 156-168, @color/text_default, 15sp, bold)
                        Text(
                            text = item.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorResource(R.color.text_default),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.alignByBaseline()
                        )

                        // 그룹명 (old: line 170-184, @color/text_dimmed, 10sp, bold, baseline aligned to name)
                        if (!item.groupName.isNullOrEmpty()) {
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = item.groupName,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colorResource(R.color.text_dimmed),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.alignByBaseline()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(1.dp))

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
                            // type "A"일 때만 gradient와 애니메이션 제거
                            val isTypeA = type == "A"

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(animatedProgress)
                                    .fillMaxHeight()
                                    .background(
                                        brush = if (isTypeA) {
                                            // type A: 단색
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    colorResource(R.color.main),
                                                    colorResource(R.color.main)
                                                )
                                            )
                                        } else {
                                            // 기타: gradient
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    colorResource(R.color.s_league_progress),
                                                    colorResource(R.color.main)
                                                )
                                            )
                                        },
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                                    )
                            ) {
                                // type A가 아닐 때만 반짝이는 애니메이션 효과
                                // 최적화: progressPercent 변경 시에만 애니메이션 실행 (10초 타이머 제거)
                                if (!isTypeA) {
                                    // 애니메이션 진행도
                                    val shimmerProgress = remember { androidx.compose.animation.core.Animatable(0f) }

                                    // progressPercent 변경 시 애니메이션 트리거
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
                                                        Color.White.copy(alpha = 0f),
                                                        Color.White.copy(alpha = 0.3f),
                                                        Color.White.copy(alpha = 0f)
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
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Normal,
                                        color = colorResource(R.color.background_100),
                                        lineHeight = 17.sp
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
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(R.color.text_angel),
                                    modifier = Modifier.offset(y = (-3).dp)
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
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(R.color.text_fairy),
                                    modifier = Modifier.offset(y = (-3).dp)
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
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(R.color.text_miracle),
                                    modifier = Modifier.offset(y = (-3).dp)
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
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(R.color.text_rookie),
                                    modifier = Modifier.offset(y = (-3).dp)
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
                                    fontSize = 7.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colorResource(R.color.text_super_rookie),
                                    modifier = Modifier.offset(y = (-3).dp)
                                )
                            }
                        }
                        }
                    }
                }

                // 하트 투표 버튼 (old: line 327-335)
                // layout_width/height: 50dp, padding: 10dp, layout_margin: 5dp
                Box(
                    modifier = Modifier.padding(5.dp)
                ) {
                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(50.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.btn_ranking_vote_heart),
                            contentDescription = "투표",
                            tint = colorResource(R.color.main),
                            modifier = Modifier
                                .size(50.dp)
                                .padding(10.dp)
                        )
                    }
                }
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
                    color = colorResource(R.color.gray200)
                )
            }
        }
    }
}
