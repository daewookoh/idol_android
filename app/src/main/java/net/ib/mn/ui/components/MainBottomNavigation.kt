package net.ib.mn.ui.components

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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import net.ib.mn.R
import net.ib.mn.util.HapticUtil
import kotlin.coroutines.cancellation.CancellationException

/**
 * Old 프로젝트의 MainBottomNavigation과 동일한 UI와 애니메이션 효과를 제공하는 컴포저블
 */
@Composable
fun MainBottomNavigation(
    menus: List<String>,
    iconsOfSelected: List<Painter>,
    iconsOfUnSelected: List<Painter>,
    initialSelectedIndex: Int = 0,
    defaultBackgroundColor: Color,
    defaultBorderColor: Color,
    defaultTextColor: Color,
    onTabSelected: (Int) -> Unit = {}
) {
    var selectedIndex by remember { mutableStateOf(initialSelectedIndex) }

    // 외부 값이 변경되면 selectedIndex값을 변경해줍니다.
    LaunchedEffect(initialSelectedIndex) {
        selectedIndex = initialSelectedIndex
    }

    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
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

            val animatedSize by animateDpAsState(
                targetValue = if(clickedIndex == index) 25.dp else 20.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ), label = ""
            )

            // 선택된 탭의 아이콘은 텍스트 컬러와 동일한 색상으로 표시
            val iconColorFilter = if (isSelected) {
                ColorFilter.tint(defaultTextColor)
            } else {
                null
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
                        HapticUtil.vibrate(context)
                        selectedIndex = index
                        onTabSelected(index)
                        clickedIndex = index
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
                            colorFilter = iconColorFilter,
                            modifier = Modifier
                                .size(animatedSize) // 아이콘 크기 조정
                        )
                    }
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = item,
                        color = defaultTextColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        modifier = Modifier.padding(0.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(13.dp))
                }
            }
        }
    }
}

