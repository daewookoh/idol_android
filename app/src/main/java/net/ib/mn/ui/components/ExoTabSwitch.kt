package net.ib.mn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.ui.theme.ColorPalette

/**
 * Exo 탭 스위치 컴포넌트
 *
 * 세그먼트 컨트롤 스타일의 탭 스위처
 * 애니메이션으로 선택 영역이 좌우로 슬라이드
 *
 * @param tabs 탭 목록 (2개만 지원)
 * @param selectedIndex 선택된 탭 인덱스 (0 또는 1)
 * @param onTabSelected 탭 선택 콜백
 * @param modifier Modifier
 */
@Composable
fun ExoTabSwitch(
    tabs: List<String>,
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    require(tabs.size == 2) { "ExoTabSwitch only supports 2 tabs" }
    require(selectedIndex in 0..1) { "selectedIndex must be 0 or 1" }

    // 애니메이션을 위한 bias 값 (-1.0f: 왼쪽, 1.0f: 오른쪽)
    val animatedBias by animateFloatAsState(
        targetValue = if (selectedIndex == 0) -1f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "tab_animation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 15.dp)
            .height(50.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(ColorPalette.gray120)
            .padding(4.dp)
    ) {
        // 애니메이션되는 선택 배경
        Box(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .align(BiasAlignment(animatedBias, 0f))
                .clip(RoundedCornerShape(18.dp))
                .background(ColorPalette.white)
        )

        // 탭 버튼들
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            tabs.forEachIndexed { index, tabText ->
                TabItem(
                    text = tabText,
                    isSelected = selectedIndex == index,
                    onClick = { onTabSelected(index) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * 탭 아이템 (Segmented Button 스타일)
 */
@Composable
private fun TabItem(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 15.sp,
            fontWeight = FontWeight.Normal,
            color = if (isSelected) ColorPalette.textDefault else ColorPalette.textGray
        )
    }
}
