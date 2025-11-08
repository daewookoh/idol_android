package net.ib.mn.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
 * 애니메이션으로 선택 영역이 슬라이드
 *
 * @param tabs 탭 목록 (2개 이상 지원)
 * @param selectedIndex 선택된 탭 인덱스
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
    require(tabs.size >= 2) { "ExoTabSwitch requires at least 2 tabs" }
    require(selectedIndex in tabs.indices) { "selectedIndex must be in valid range" }

    // 애니메이션을 위한 bias 값 계산
    // tabs.size가 2개일 때: -1, 1
    // tabs.size가 3개일 때: -1, 0, 1
    // tabs.size가 4개일 때: -1, -0.33, 0.33, 1
    val targetBias = if (tabs.size == 1) {
        0f
    } else {
        -1f + (2f * selectedIndex / (tabs.size - 1))
    }

    val animatedBias by animateFloatAsState(
        targetValue = targetBias,
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
                .fillMaxWidth(1f / tabs.size)
                .fillMaxHeight()
                .align(BiasAlignment(animatedBias, 0f))
                .clip(RoundedCornerShape(20.dp))
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
    // "(테섭)" 포함 여부에 따라 폰트 사이즈 결정
    val fontSize = if (text.contains("(테섭)")) 12.sp else 15.sp

    Box(
        modifier = modifier
            .clickable(
                onClick = onClick,
                indication = null,  // hover/ripple 효과 제거
                interactionSource = remember { MutableInteractionSource() }
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = FontWeight.Normal,
            color = if (isSelected) ColorPalette.textDefault else ColorPalette.textGray
        )
    }
}
