package net.ib.mn.presentation.main.menu

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.R
import net.ib.mn.domain.model.IconMenuItem
import net.ib.mn.domain.model.IconMenuType
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.TextMenuItem
import net.ib.mn.domain.model.TextMenuType
import net.ib.mn.presentation.common.InAppBanner as InAppBannerComponent
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
    val context = LocalContext.current

    MenuContent(
        modifier = modifier,
        bannerList = bannerList,
        iconMenuItems = iconMenuItems,
        textMenuItems = textMenuItems,
        onBannerClick = onBannerClick,
        onMenuItemClick = { item ->
            // TODO: 실제 네비게이션 처리
            Toast.makeText(context, "Clicked: ${item.id}", Toast.LENGTH_SHORT).show()
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
                    InAppBannerComponent(
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
