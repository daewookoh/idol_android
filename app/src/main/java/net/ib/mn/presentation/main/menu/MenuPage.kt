package net.ib.mn.presentation.main.menu

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.IconMenuItem
import net.ib.mn.domain.model.IconMenuType
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.TextMenuItem
import net.ib.mn.domain.model.TextMenuType
import net.ib.mn.presentation.main.menu.components.MenuIconItem
import net.ib.mn.presentation.main.menu.components.MenuTextItem

/**
 * Menu 페이지
 * Old 프로젝트의 MyInfoFragment 메뉴 리스트 부분을 Compose로 변환
 */
@Composable
fun MenuPage(
    modifier: Modifier = Modifier,
    viewModel: MenuPageViewModel = hiltViewModel(),
    onMenuItemClick: (String) -> Unit = {},
    onBannerClick: (String?) -> Unit = {}
) {
    val iconMenuItems by viewModel.iconMenuItems.collectAsStateWithLifecycle()
    val textMenuItems by viewModel.textMenuItems.collectAsStateWithLifecycle()
    val bannerList by viewModel.bannerList.collectAsStateWithLifecycle()

    MenuContent(
        modifier = modifier,
        bannerList = bannerList,
        iconMenuItems = iconMenuItems,
        textMenuItems = textMenuItems,
        onBannerClick = onBannerClick,
        onMenuItemClick = { item ->
            onMenuItemClick(item.id)
        }
    )
}

@Composable
private fun MenuContent(
    modifier: Modifier = Modifier,
    bannerList: List<InAppBanner>,
    iconMenuItems: List<IconMenuItem>,
    textMenuItems: List<TextMenuItem>,
    onBannerClick: (String?) -> Unit = {},
    onMenuItemClick: (IconMenuItem) -> Unit = {},
    onTextMenuItemClick: (TextMenuItem) -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorResource(id = R.color.background_100))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // 배너 영역 (10dp 패딩)
            if (bannerList.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 10.dp, end = 10.dp)
                ) {
                    InAppBannerSection(
                        bannerList = bannerList,
                        clickBanner = { banner ->
                            onBannerClick(banner.link)
                        }
                    )
                }
            }

            Spacer(modifier= Modifier.height(10.dp))

            // 아이콘 메뉴 그리드 (4열)
            IconMenuGrid(
                items = iconMenuItems,
                onItemClick = onMenuItemClick
            )

            // 구분선 (10dp 높이, gray50 배경)
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(colorResource(id = R.color.gray50))
            )

            // 텍스트 메뉴 리스트
            TextMenuList(
                items = textMenuItems,
                onItemClick = onTextMenuItemClick
            )
        }
    }
}

/**
 * 아이콘 메뉴 그리드 (4열)
 */
@Composable
private fun IconMenuGrid(
    items: List<IconMenuItem>,
    onItemClick: (IconMenuItem) -> Unit
) {
    val rows = items.chunked(4)  // 4개씩 잘라서 행으로 만들기

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                rowItems.forEach { item ->
                    Box(modifier = Modifier.weight(1f)) {
                        MenuIconItem(
                            item = item,
                            onClick = { onItemClick(item) }
                        )
                    }
                }
                // 빈 공간 채우기 (4개 미만인 경우)
                repeat(4 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * 텍스트 메뉴 리스트
 */
@Composable
private fun TextMenuList(
    items: List<TextMenuItem>,
    onItemClick: (TextMenuItem) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            MenuTextItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MenuContentPreview() {
    MenuContent(
        bannerList = emptyList(),
        iconMenuItems = listOf(
            IconMenuItem(
                id = "support",
                labelResId = R.string.support,
                iconResId = R.drawable.icon_menu_support_1,
                type = IconMenuType.SUPPORT
            ),
            IconMenuItem(
                id = "free_charge",
                labelResId = R.string.btn_free_heart_charge,
                iconResId = R.drawable.icon_menu_freeshop,
                type = IconMenuType.FREE_CHARGE,
                hasBadge = true,
                badgeIconResId = R.drawable.icon_menu_up
            ),
            IconMenuItem(
                id = "attendance",
                labelResId = R.string.attendance_check,
                iconResId = R.drawable.icon_menu_attendance,
                type = IconMenuType.ATTENDANCE
            ),
            IconMenuItem(
                id = "event",
                labelResId = R.string.menu_menu00,
                iconResId = R.drawable.icon_menu_event_1,
                type = IconMenuType.EVENT
            )
        ),
        textMenuItems = listOf(
            TextMenuItem(
                id = "vote_certificate",
                labelResId = R.string.certificate_title,
                iconResId = R.drawable.icon_sidemenu_votingcertificate,
                type = TextMenuType.VOTE_CERTIFICATE
            ),
            TextMenuItem(
                id = "notice",
                labelResId = R.string.setting_menu01,
                iconResId = R.drawable.icon_sidemenu_notice,
                type = TextMenuType.NOTICE,
                hasBadge = true
            )
        )
    )
}

/**
 * InAppBanner 섹션 (자동 슬라이드 배너)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun InAppBannerSection(
    bannerList: List<InAppBanner>,
    clickBanner: (InAppBanner) -> Unit = {},
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(0, pageCount = { bannerList.size })

    val selectedColor = Color(ContextCompat.getColor(context, R.color.main))
    val unselectedColor = Color(ContextCompat.getColor(context, R.color.gray150))

    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(key1 = pagerState.currentPage, key2 = bannerList.size > 1) {
        val delayMillis = 5000L // 5초
        while (true) {
            delay(delayMillis)
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
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        HorizontalPager(state = pagerState) {
            InAppBannerItem(
                item = bannerList[it],
                clickBanner = clickBanner
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        if (bannerList.size > 1) {
            Row(
                Modifier
                    .height(6.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(bannerList.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) selectedColor else unselectedColor
                    Box(
                        modifier = Modifier
                            .background(color, CircleShape)
                            .size(6.dp)
                    )
                    if (iteration != bannerList.size - 1 || iteration == 0) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .size(0.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InAppBannerItem(
    item: InAppBanner,
    clickBanner: (InAppBanner) -> Unit = {},
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(17f / 3f)
            .clickable(
                onClick = { clickBanner(item) },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        AsyncImage(
            model = item.imageUrl,
            contentDescription = "banner",
            modifier = Modifier.fillMaxWidth()
        )
    }
}
