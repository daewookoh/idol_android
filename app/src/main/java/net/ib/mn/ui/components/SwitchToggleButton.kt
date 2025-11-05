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
 * 
 * 상태 관리:
 * - isMaleSelected: 외부에서 관리되는 선택 상태 (단일 진실 원천)
 * - 사용자 클릭 시에만 애니메이션 실행
 * - 초기 렌더링 또는 외부 상태 변경 시 애니메이션 없이 즉시 반영
 */
@Composable
fun SwitchToggleButton(
    genderList: List<Pair<String, String>>,
    isMaleSelected: Boolean,
    boxBackgroundColor: Color,
    boxTextColor: Color,
    thumbBackgroundColor: Color,
    thumbTextColor: Color,
    onCategoryChanged: (String) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    
    // 버튼 너비 측정
    var buttonWidthPx by remember { mutableStateOf(0) }
    val buttonWidthDp = remember(buttonWidthPx) {
        if (buttonWidthPx > 0) {
            with(density) { (buttonWidthPx - 2.5.dp.toPx()).toDp() }
        } else {
            0.dp
        }
    }
    
    // 사용자 클릭 여부 추적 (외부 상태 변경과 구분)
    var isUserClick by remember { mutableStateOf(false) }
    
    // 썸네일 텍스트 (현재 선택된 상태에 따라 즉시 업데이트)
    val thumbBoxText = if (isMaleSelected) genderList[0].first else genderList[1].first
    
    // 썸네일 오프셋 계산
    val thumbOffset by animateDpAsState(
        targetValue = if (buttonWidthDp == 0.dp || isMaleSelected) 0.dp else buttonWidthDp,
        animationSpec = if (isUserClick && buttonWidthDp > 0.dp) {
            tween(durationMillis = 250) // 애니메이션 속도 (250ms)
        } else {
            tween(durationMillis = 0) // 즉시 이동
        },
        label = "thumb_offset",
        finishedListener = {
            isUserClick = false // 애니메이션 완료 후 리셋
        }
    )

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(boxBackgroundColor)
            .wrapContentSize()
            .padding(2.dp)
            .clickable {
                // 사용자 클릭 시 애니메이션 실행
                isUserClick = true
                val newCategory = if (isMaleSelected) genderList[1].second else genderList[0].second
                onCategoryChanged(newCategory)
                HapticUtil.vibrate(context)
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((-5).dp),
            modifier = Modifier
                .wrapContentHeight()
                .onGloballyPositioned { layoutCoordinates ->
                    buttonWidthPx = (layoutCoordinates.size.width / 2)
                }
        ) {
            // 남자/첫 번째 옵션
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
            
            // 여자/두 번째 옵션
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
        
        // 썸네일 (움직이는 원)
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
