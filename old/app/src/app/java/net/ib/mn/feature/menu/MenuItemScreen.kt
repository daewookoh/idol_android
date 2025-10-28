package net.ib.mn.feature.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import net.ib.mn.R
import net.ib.mn.core.designsystem.R.*
import net.ib.mn.feature.menu.component.MenuIconItem
import net.ib.mn.feature.menu.component.MenuIconTextItem
import net.ib.mn.feature.menu.listener.MenuIconItemClickListener
import net.ib.mn.feature.menu.listener.MenuTextItemClickListener
import net.ib.mn.model.ConfigModel
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.SupportedLanguage

@Composable
fun IconMenuList(
    isBasicUI: Boolean,
    showGame: Boolean,
    menuIconItemClickListener: MenuIconItemClickListener,
    menuTextItemClickListener: MenuTextItemClickListener,
    onUpdateTutorial: () -> Unit = { /* No-op */ },
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp

    val itemWidth = (screenWidth.value * 0.2).dp
    val paddingHorizontal = (screenWidth.value * 0.04).dp
    val itemSpacing = (screenWidth.value * 0.04).dp

    val isShowFreeBoard = ConfigModel.getInstance(context).showLiveStreamingTab

    // 메뉴 항목 리스트 필터링
    val iconMenuItems = IconMenu.values().toList().filter { item ->
        when (item) {
            IconMenu.NOTICE -> ConfigModel.getInstance(context).menuNoticeMain != "N"
            IconMenu.STORE -> ConfigModel.getInstance(context).menuStoreMain != "N"
            IconMenu.FREE_BOARD -> ConfigModel.getInstance(context).menuFreeBoardMain != "N" && isShowFreeBoard
            else -> true
        }
    }

    val textMenuItems = IconTextMenu.entries.filter { item ->
        when (item) {
            IconTextMenu.NOTICE -> ConfigModel.getInstance(context).menuNoticeMain != "Y"
            IconTextMenu.STORE -> ConfigModel.getInstance(context).menuStoreMain != "Y"
            IconTextMenu.FREE_BOARD -> ConfigModel.getInstance(context).menuFreeBoardMain != "Y" && isShowFreeBoard
            IconTextMenu.QUIZ -> LocaleUtil.isExistCurrentLocale(context, SupportedLanguage.BOARD_KIN_QUIZZES_TOP100_LOCALES)
            IconTextMenu.GAME -> showGame
            else -> true
        }
    }


    val iconTutorialItem = when(TutorialManager.getTutorialIndex()) {
        TutorialBits.MENU_SUPPORT -> IconMenu.SUPPORT
        TutorialBits.MENU_FREE_HEART -> IconMenu.FREE_CHARGE
        TutorialBits.MENU_DAILY_STAMP -> IconMenu.ATTENDANCE
        TutorialBits.MENU_EVENT -> IconMenu.EVENT
        TutorialBits.MENU_NOTICE -> IconMenu.NOTICE
        TutorialBits.MENU_HEART_SHOP -> IconMenu.STORE
        else -> null
    }

    val textTutorialItem = when(TutorialManager.getTutorialIndex()) {
        TutorialBits.MENU_CERTIFICATE -> IconTextMenu.VOTE_CERTIFICATE
        TutorialBits.MENU_HEART_SHOP -> IconTextMenu.STORE
        TutorialBits.MENU_NOTICE -> IconTextMenu.NOTICE
        TutorialBits.MENU_FRIEND_INVITE -> IconTextMenu.INVITE_FRIEND
        TutorialBits.MENU_STATS -> IconTextMenu.HISTORY
        TutorialBits.MENU_QUIZ -> IconTextMenu.QUIZ
        else -> null
    }

    Column {
        // TODO 기존 코드 정리 후 적용 (UI는 완성 해놓음)
//        ProfileComponent(context)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = paddingHorizontal),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            iconMenuItems.chunked(4).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(itemSpacing)
                ) {
                    rowItems.forEach { item ->
                        var showTutorial by remember { mutableStateOf(item == iconTutorialItem) }
                        var isTouchingTutorial by remember { mutableStateOf(false) }

                        val tutorialComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(raw.tutorial_heart))
                        val tutorialTouchComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(raw.tutorial_heart_touch))

                        val currentComposition = if (isTouchingTutorial) tutorialTouchComposition else tutorialComposition

                        val progress by animateLottieCompositionAsState(
                            composition = currentComposition,
                            iterations = if (isTouchingTutorial) 1 else LottieConstants.IterateForever
                        )

                        LaunchedEffect(isTouchingTutorial, progress) {
                            if (isTouchingTutorial && progress >= 1f) {
                                showTutorial = false
                                isTouchingTutorial = false

                                onUpdateTutorial()

                                when (item) {
                                    IconMenu.SUPPORT -> menuIconItemClickListener.onSupportClick()
                                    IconMenu.NOTICE -> menuIconItemClickListener.onNoticeClick()
                                    IconMenu.STORE -> menuIconItemClickListener.onStoreClick()
                                    IconMenu.EVENT -> menuIconItemClickListener.onEventClick()
                                    IconMenu.ATTENDANCE -> menuIconItemClickListener.onAttendanceClick()
                                    IconMenu.FREE_CHARGE -> menuIconItemClickListener.onFreeChargeClick()
                                    IconMenu.FREE_BOARD -> menuIconItemClickListener.onFreeBoardClick()
                                }
                            }
                        }

                        Box() {
                            MenuIconItem(
                                Modifier
                                    .width(itemWidth)
                                    .wrapContentHeight(),
                                item,
                                isPreview = false,
                                onClick = {
                                    if (showTutorial) {
                                        // 튜토리얼 터치 애니메이션 재생
                                        isTouchingTutorial = true
                                    } else {
                                        // 일반 탭 동작
                                        when (item) {
                                            IconMenu.SUPPORT -> menuIconItemClickListener.onSupportClick()
                                            IconMenu.NOTICE -> menuIconItemClickListener.onNoticeClick()
                                            IconMenu.STORE -> menuIconItemClickListener.onStoreClick()
                                            IconMenu.EVENT -> menuIconItemClickListener.onEventClick()
                                            IconMenu.ATTENDANCE -> menuIconItemClickListener.onAttendanceClick()
                                            IconMenu.FREE_CHARGE -> menuIconItemClickListener.onFreeChargeClick()
                                            IconMenu.FREE_BOARD -> menuIconItemClickListener.onFreeBoardClick()
                                        }
                                    }
                                }
                            )
                            if (showTutorial) {
                                LottieAnimation(
                                    composition = currentComposition,
                                    progress = { progress },
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.TopCenter)
                                        .offset(y = (15).dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .background(Color(ContextCompat.getColor(LocalContext.current, R.color.gray50)))
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            textMenuItems.forEach { item ->
                var showTutorial by remember { mutableStateOf(item == textTutorialItem) }
                var isTouchingTutorial by remember { mutableStateOf(false) }

                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(
                        if (isTouchingTutorial) raw.tutorial_heart_touch else raw.tutorial_heart
                    )
                )
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    iterations = if (isTouchingTutorial) 1 else LottieConstants.IterateForever
                )

                LaunchedEffect(isTouchingTutorial, progress) {
                    if (isTouchingTutorial && progress >= 1f) {
                        showTutorial = false
                        isTouchingTutorial = false

                        onUpdateTutorial()

                        when (item) {
                            IconTextMenu.VOTE_CERTIFICATE -> menuTextItemClickListener.onVoteCertificateClick()
                            IconTextMenu.FACE -> menuTextItemClickListener.onFaceClick()
                            IconTextMenu.NOTICE -> menuTextItemClickListener.onNoticeClick()
                            IconTextMenu.INVITE_FRIEND -> menuTextItemClickListener.onInviteFriendClick()
                            IconTextMenu.STORE -> menuTextItemClickListener.onStoreClick()
                            IconTextMenu.QUIZ -> menuTextItemClickListener.onQuizClick()
                            IconTextMenu.HISTORY -> menuTextItemClickListener.onHistoryClick()
                            IconTextMenu.FREE_BOARD -> menuTextItemClickListener.onFreeBoardClick()
                            IconTextMenu.GAME -> menuTextItemClickListener.onGameClick()
                        }
                    }
                }

                Box {
                    MenuIconTextItem(
                        context = context,
                        item = item,
                        onClick = {
                            if (showTutorial) {
                                // 튜토리얼 터치 애니메이션 재생
                                isTouchingTutorial = true
                            } else {
                                when (item) {
                                    IconTextMenu.VOTE_CERTIFICATE -> menuTextItemClickListener.onVoteCertificateClick()
                                    IconTextMenu.FACE -> menuTextItemClickListener.onFaceClick()
                                    IconTextMenu.NOTICE -> menuTextItemClickListener.onNoticeClick()
                                    IconTextMenu.INVITE_FRIEND -> menuTextItemClickListener.onInviteFriendClick()
                                    IconTextMenu.STORE -> menuTextItemClickListener.onStoreClick()
                                    IconTextMenu.QUIZ -> menuTextItemClickListener.onQuizClick()
                                    IconTextMenu.HISTORY -> menuTextItemClickListener.onHistoryClick()
                                    IconTextMenu.FREE_BOARD -> menuTextItemClickListener.onFreeBoardClick()
                                    IconTextMenu.GAME -> menuTextItemClickListener.onGameClick()
                                }
                            }
                        }
                    )

                    if (showTutorial) {
                        LottieAnimation(
                            composition = composition,
                            progress = { progress },
                            modifier = Modifier
                                .size(28.dp)
                                .align(Alignment.CenterStart)
                                .offset(x = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun PreviewIconMenuList() {
    IconMenuList(
        isBasicUI = true,
        showGame = false,
        object : MenuIconItemClickListener {
            override fun onSupportClick() {}
            override fun onFreeChargeClick() {}
            override fun onAttendanceClick() {}
            override fun onEventClick() {}
            override fun onStoreClick() {}
            override fun onNoticeClick() {}
            override fun onFreeBoardClick() {}
        },
        object : MenuTextItemClickListener {
            override fun onVoteCertificateClick() {}
            override fun onStoreClick() {}
            override fun onNoticeClick() {}
            override fun onInviteFriendClick() {}
            override fun onHistoryClick() {}
            override fun onQuizClick() {}
            override fun onFaceClick() {}
            override fun onFreeBoardClick() {}
            override fun onGameClick() {}
        },
    )
}