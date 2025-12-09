package net.ib.mn.presentation.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.navigation.LocalAppNavigator
import net.ib.mn.navigation.Screen
import net.ib.mn.util.IntentUtil

/**
 * InAppBanner 섹션 (자동 슬라이드 배너)
 *
 * MenuPage, SearchScreen 등에서 공통으로 사용
 *
 * 배너 클릭 시 동작:
 * - "idol:{idolId}" 형식: 해당 아이돌 커뮤니티 페이지로 이동
 * - "http://" 또는 "https://" 형식: 외부 브라우저로 열기
 *
 * @param bannerList 배너 리스트
 * @param aspectRatio 배너 이미지 비율 (기본 17:3)
 * @param autoSlideDelayMillis 자동 슬라이드 딜레이 (기본 5초)
 * @param indicatorSpacing 인디케이터 간격
 * @param indicatorSize 인디케이터 크기
 * @param onBannerClick 배너 클릭 콜백 (커스텀 처리가 필요한 경우)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InAppBannerSection(
    bannerList: List<InAppBanner>,
    modifier: Modifier = Modifier,
    aspectRatio: Float = 17f / 3f,
    autoSlideDelayMillis: Long = 5000L,
    indicatorSpacing: Dp = 4.dp,
    indicatorSize: Dp = 6.dp,
    onBannerClick: (InAppBanner) -> Unit = {}
) {
    if (bannerList.isEmpty()) return

    val context = LocalContext.current
    val navigator = LocalAppNavigator.current
    val pagerState = rememberPagerState(0, pageCount = { bannerList.size })

    // 배너 클릭 처리 함수
    val handleBannerClick: (InAppBanner) -> Unit = { banner ->
        val link = banner.link
        when {
            link.isNullOrBlank() -> {
                // 링크가 없으면 콜백만 호출
                onBannerClick(banner)
            }
            link.startsWith("idol:") -> {
                // 아이돌 커뮤니티 페이지로 이동
                val idolId = link.removePrefix("idol:").toIntOrNull()
                if (idolId != null) {
                    navigator.navigate(Screen.Community(idolId = idolId))
                }
                onBannerClick(banner)
            }
            link.startsWith("http://") || link.startsWith("https://") -> {
                // 외부 브라우저로 열기
                IntentUtil.openUrl(context, link)
                onBannerClick(banner)
            }
            else -> {
                // 기타 링크는 콜백으로 처리
                onBannerClick(banner)
            }
        }
    }

    val selectedColor = Color(ContextCompat.getColor(context, R.color.main))
    val unselectedColor = Color(ContextCompat.getColor(context, R.color.gray150))

    val coroutineScope = rememberCoroutineScope()

    // 자동 슬라이드 (배너가 2개 이상일 때만)
    LaunchedEffect(key1 = pagerState.currentPage, key2 = bannerList.size > 1) {
        if (bannerList.size <= 1) return@LaunchedEffect
        while (true) {
            delay(autoSlideDelayMillis)
            val nextPage = if (pagerState.currentPage == bannerList.size - 1) {
                0
            } else {
                pagerState.currentPage + 1
            }
            coroutineScope.launch {
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        HorizontalPager(state = pagerState) { page ->
            InAppBannerItem(
                item = bannerList[page],
                aspectRatio = aspectRatio,
                onClick = handleBannerClick
            )
        }

        // 인디케이터 (배너가 2개 이상일 때만)
        if (bannerList.size > 1) {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                Modifier
                    .height(indicatorSize)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(bannerList.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) selectedColor else unselectedColor
                    Box(
                        modifier = Modifier
                            .background(color, CircleShape)
                            .size(indicatorSize)
                    )
                    if (iteration != bannerList.size - 1) {
                        Spacer(modifier = Modifier.padding(end = indicatorSpacing))
                    }
                }
            }
        }
    }
}

/**
 * 개별 배너 아이템
 */
@Composable
private fun InAppBannerItem(
    item: InAppBanner,
    aspectRatio: Float,
    onClick: (InAppBanner) -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(8.dp))
            .clickable(
                onClick = { onClick(item) },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = "banner",
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InAppBannerSectionPreview() {
    InAppBannerSection(
        bannerList = listOf(
            InAppBanner(
                id = 1,
                imageUrl = "https://example.com/banner1.jpg",
                link = "https://example.com/1"
            ),
            InAppBanner(
                id = 2,
                imageUrl = "https://example.com/banner2.jpg",
                link = "https://example.com/2"
            )
        )
    )
}
