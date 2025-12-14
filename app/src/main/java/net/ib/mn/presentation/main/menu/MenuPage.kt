package net.ib.mn.presentation.main.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.domain.model.IconMenuItem
import net.ib.mn.domain.model.IconMenuType
import net.ib.mn.domain.model.InAppBanner
import net.ib.mn.domain.model.TextMenuItem
import net.ib.mn.domain.model.TextMenuType
import net.ib.mn.presentation.common.InAppBannerSection
import net.ib.mn.presentation.main.menu.components.MenuIconItem
import net.ib.mn.presentation.main.menu.components.MenuTextItem
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.ui.components.ExoTutorialHeart

/**
 * Menu 페이지
 * Old 프로젝트의 MyInfoFragment 메뉴 리스트 부분을 Compose로 변환
 */
@Composable
fun MenuPage(
    modifier: Modifier = Modifier,
    viewModel: MenuPageViewModel = hiltViewModel(),
    onMenuItemClick: (String) -> Unit = {},
    onBannerClick: (String?) -> Unit = {},
    onNavigateToFriendInvite: (token: String, language: String) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()
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
        },
        onTextMenuItemClick = { item ->
            when (item.type) {
                TextMenuType.INVITE_FRIEND -> {
                    scope.launch {
                        val inviteInfo = viewModel.getInviteInfo()
                        if (inviteInfo != null) {
                            onNavigateToFriendInvite(inviteInfo.first, inviteInfo.second)
                        }
                    }
                }
                else -> {}
            }
        },
        onTutorialComplete = { tutorialIndex ->
            viewModel.updateTutorial(tutorialIndex)
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
    onTextMenuItemClick: (TextMenuItem) -> Unit = {},
    onTutorialComplete: (Int) -> Unit = {}
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
                InAppBannerSection(
                    bannerList = bannerList,
                    modifier = Modifier.padding(start = 10.dp, top = 10.dp, end = 10.dp),
                    onBannerClick = { banner ->
                        onBannerClick(banner.link)
                    }
                )
            }

            Spacer(modifier= Modifier.height(10.dp))

            // 아이콘 메뉴 그리드 (4열)
            IconMenuGrid(
                items = iconMenuItems,
                onItemClick = onMenuItemClick,
                onTutorialComplete = onTutorialComplete
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
                onItemClick = onTextMenuItemClick,
                onTutorialComplete = onTutorialComplete
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
    onItemClick: (IconMenuItem) -> Unit,
    onTutorialComplete: (Int) -> Unit = {}
) {
    val rows = items.chunked(4)  // 4개씩 잘라서 행으로 만들기
    val currentTutorialIndex by TutorialManager.currentTutorialIndex.collectAsState()

    // IconMenuType을 TutorialBit으로 매핑
    fun getTutorialBit(type: IconMenuType): Int? = when (type) {
        IconMenuType.SUPPORT -> TutorialBits.MENU_SUPPORT
        IconMenuType.FREE_CHARGE -> TutorialBits.MENU_FREE_HEART
        IconMenuType.ATTENDANCE -> TutorialBits.MENU_DAILY_STAMP
        IconMenuType.EVENT -> TutorialBits.MENU_EVENT
        IconMenuType.STORE -> TutorialBits.MENU_HEART_SHOP
        IconMenuType.NOTICE -> TutorialBits.MENU_NOTICE
        else -> null
    }

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
                    val tutorialBit = getTutorialBit(item.type)
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        MenuIconItem(
                            item = item,
                            onClick = { onItemClick(item) }
                        )

                        // 튜토리얼 하트 오버레이
                        tutorialBit?.let { bit ->
                            if (currentTutorialIndex == bit) {
                                ExoTutorialHeart(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .offset(y = (-20).dp),
                                    tutorialBit = bit,
                                    animationSize = 28.dp,
                                    onTutorialComplete = {
                                        onTutorialComplete(bit)
                                        onItemClick(item)
                                    }
                                )
                            }
                        }
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
    onItemClick: (TextMenuItem) -> Unit,
    onTutorialComplete: (Int) -> Unit = {}
) {
    val currentTutorialIndex by TutorialManager.currentTutorialIndex.collectAsState()

    // TextMenuType을 TutorialBit으로 매핑
    fun getTutorialBit(type: TextMenuType): Int? = when (type) {
        TextMenuType.VOTE_CERTIFICATE -> TutorialBits.MENU_CERTIFICATE
        TextMenuType.INVITE_FRIEND -> TutorialBits.MENU_FRIEND_INVITE
        TextMenuType.HISTORY -> TutorialBits.MENU_STATS
        TextMenuType.STORE -> TutorialBits.MENU_HEART_SHOP
        TextMenuType.NOTICE -> TutorialBits.MENU_NOTICE
        else -> null
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        items.forEach { item ->
            val tutorialBit = getTutorialBit(item.type)
            Box(contentAlignment = Alignment.CenterStart) {
                MenuTextItem(
                    item = item,
                    onClick = { onItemClick(item) }
                )

                // 튜토리얼 하트 오버레이
                tutorialBit?.let { bit ->
                    if (currentTutorialIndex == bit) {
                        ExoTutorialHeart(
                            modifier = Modifier
                                .padding(start = 35.dp)
                                .align(Alignment.CenterStart),
                            tutorialBit = bit,
                            animationSize = 28.dp,
                            onTutorialComplete = {
                                onTutorialComplete(bit)
                                onItemClick(item)
                            }
                        )
                    }
                }
            }
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
