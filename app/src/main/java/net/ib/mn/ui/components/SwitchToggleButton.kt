package net.ib.mn.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.util.HapticUtil

/**
 * 남자/여자 스위치 토글 버튼
 * old 프로젝트의 SwitchToggleButton과 동일한 UI와 애니메이션 효과
 */
@Composable
fun SwitchToggleButton(
    genderList: List<Pair<String, String>>,
    initialIsMaleSelected: Boolean = true,
    boxBackgroundColor: Color,
    boxTextColor: Color,
    thumbBackgroundColor: Color,
    thumbTextColor: Color,
    category: (String) -> Unit,
) {
    var isMaleSelected by remember { mutableStateOf(initialIsMaleSelected) }
    var buttonWidthPx by remember { mutableStateOf(0) }

    val density = LocalDensity.current
    val buttonWidthDp = with(density) { (buttonWidthPx - 2.5.dp.toPx()).toDp() }

    var thumbBoxText by remember {
        mutableStateOf(if (isMaleSelected) genderList[0].first else genderList[1].first)
    }

    val thumbOffset by animateDpAsState(
        targetValue = if (isMaleSelected) 0.dp else buttonWidthDp,
        animationSpec = tween(durationMillis = 600), label = "",
        finishedListener = {
            thumbBoxText = if (isMaleSelected) genderList[0].first else genderList[1].first
        }
    )

    val context = LocalContext.current

    // 외부 값이 변경되면 isMaleSelected 업데이트
    LaunchedEffect(initialIsMaleSelected) {
        isMaleSelected = initialIsMaleSelected
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(boxBackgroundColor)
            .wrapContentSize()
            .padding(2.dp)
            .clickable {
                isMaleSelected = !isMaleSelected
                val selectedCategory = if (isMaleSelected) genderList[0].second else genderList[1].second
                category(selectedCategory)
                HapticUtil.vibrate(context)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((-5).dp),
            modifier = Modifier
                .wrapContentHeight()
                .onGloballyPositioned { layoutCoordinates ->
                    // Row 내부에 있는 2개 버튼의 중앙 x좌표 위치를 구합니다.
                    buttonWidthPx = (layoutCoordinates.size.width / 2)
                }
        ) {
            Box(
                modifier = Modifier
                    .width(39.dp)
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = genderList[0].first,
                    textAlign = TextAlign.Center,
                    color = boxTextColor,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
            Box(
                modifier = Modifier
                    .width(39.dp)
                    .height(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = genderList[1].first,
                    color = boxTextColor,
                    textAlign = TextAlign.Center,
                    style = TextStyle(
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .clip(CircleShape)
                .background(thumbBackgroundColor)
                .width(39.dp)
                .height(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = thumbBoxText,
                color = thumbTextColor,
                textAlign = TextAlign.Center,
                style = TextStyle(
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }
}
