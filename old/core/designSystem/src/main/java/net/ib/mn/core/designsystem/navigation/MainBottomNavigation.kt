/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.core.designsystem.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import kotlinx.coroutines.delay
import net.ib.mn.core.designsystem.R
import net.ib.mn.core.designsystem.util.Util
import kotlin.coroutines.cancellation.CancellationException


/**
 * @see
 * */

@Composable
fun MainBottomNavigation(
    // 해당 스트링은 디폴트 스트링으로 Preview 화면 확인용으로 들어간겁니다.(그리고 모듈이 달라서 다국어 처리 불가)
    menus: List<String> = listOf("랭킹", "순위", "내정보", "메뉴"),
    iconsOfSelected: List<Painter> = listOf(),
    iconsOfUnSelected: List<Painter> = listOf(),
    initialSelectedIndex: Int = 0,
    defaultBackgroundColor: Color,
    defaultBorderColor: Color,
    defaultTextColor : Color,
    packagePrefName : String,
    hapticPrefName : String,
    tutorialIndex: Int = -1,
    tabIndex: Int = -1,
    navigateToFragment: (Int) -> Unit = {},
    tutorialClick: (Int) -> Unit = {}
) {

    var selectedIndex by remember { mutableStateOf(initialSelectedIndex) }

    // 외부 값이 변경되면  selectedIndex값을 변경해줍니다.
    LaunchedEffect(initialSelectedIndex) {
        selectedIndex = initialSelectedIndex
    }

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
            .background(defaultBackgroundColor)
            .border(
                BorderStroke(0.3.dp, defaultBorderColor),
                RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
            )
            .padding(0.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        menus.forEachIndexed { index, item ->

            val isSelected = selectedIndex == index

            val icon =
                if (isSelected) iconsOfSelected.getOrNull(index) else iconsOfUnSelected.getOrNull(
                    index
                )
            var clickedIndex by remember { mutableStateOf(-1) }

            // 클릭시 터치 그림자 없애주기.
            val interactionSource = remember { MutableInteractionSource() }

            var showTutorial by remember { mutableStateOf(tabIndex == index) }
            var isTouchingTutorial by remember { mutableStateOf(false) }

            val tutorialComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.tutorial_heart))
            val tutorialTouchComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.tutorial_heart_touch))

            val currentComposition = if (isTouchingTutorial) tutorialTouchComposition else tutorialComposition

            val progress by animateLottieCompositionAsState(
                composition = currentComposition,
                iterations = if (isTouchingTutorial) 1 else LottieConstants.IterateForever
            )

            val animatedSize by animateDpAsState(
                targetValue = if(clickedIndex == index) 25.dp else 20.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ), label = ""
            )

            LaunchedEffect(isTouchingTutorial, progress) {
                if (isTouchingTutorial && progress >= 1f) {
                    showTutorial = false
                    isTouchingTutorial = false

                    Util.vibratePhone(context, hapticPrefName, packagePrefName)
                    selectedIndex = index
                    navigateToFragment(index)
                    clickedIndex = index

                    tutorialClick(tutorialIndex)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 8.dp)
                    .background(
                        defaultBackgroundColor
                    )
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                    ) {
                        if (showTutorial) {
                            // 튜토리얼 터치 애니메이션 재생
                            isTouchingTutorial = true
                        } else {
                            // 일반 탭 동작
                            Util.vibratePhone(context, hapticPrefName, packagePrefName)
                            selectedIndex = index
                            navigateToFragment(index)
                            clickedIndex = index
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LaunchedEffect(clickedIndex) {
                        try {
                            if (clickedIndex == index) {
                                delay(200)
                                clickedIndex = -1
                            }
                        } catch (e: CancellationException) {
                            clickedIndex = -1
                        }
                    }
                    if (icon != null) {
                        Image(
                            painter = icon,
                            contentDescription = item,
                            modifier = Modifier
                                .size(animatedSize) // 아이콘 크기 조정
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item,
                        color = defaultTextColor,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(0.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(13.dp))
                }

                if (showTutorial) {
                    LottieAnimation(
                        composition = currentComposition,
                        progress = { progress },
                        modifier = Modifier
                            .size(28.dp)
                            .offset(y = (-10).dp)
                    )
                }
            }
        }
    }
}



@Preview(showBackground = true)
@Composable
fun MainBottomNavigationPreview() {
    MaterialTheme {
//        MainBottomNavigation()
    }
}
