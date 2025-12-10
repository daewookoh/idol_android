package net.ib.mn.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import net.ib.mn.R

/**
 * 오리 로딩 애니메이션 컴포넌트
 *
 * Old 프로젝트의 로딩 다이얼로그를 Compose로 구현
 * Lottie 애니메이션을 사용하여 오리가 둥둥 떠다니는 로딩 표시
 *
 * @param isLoading 로딩 상태
 * @param modifier Modifier
 * @param backgroundColor 배경색 (기본: 반투명 검정)
 * @param animationSize 애니메이션 크기 (기본: 120dp)
 */
@Composable
fun ExoLoading(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Black.copy(alpha = 0.5f),
    animationSize: Dp = 120.dp
) {
    if (isLoading) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.animation_loading_duck)
        )

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(backgroundColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* 터치 이벤트 소비 */ }
                ),
            contentAlignment = Alignment.Center
        ) {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(animationSize)
            )
        }
    }
}

/**
 * 오리 로딩 애니메이션만 표시 (배경 없음)
 *
 * @param modifier Modifier
 * @param animationSize 애니메이션 크기 (기본: 120dp)
 */
@Composable
fun ExoLoadingAnimation(
    modifier: Modifier = Modifier,
    animationSize: Dp = 120.dp
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.animation_loading_duck)
    )

    LottieAnimation(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        modifier = modifier.size(animationSize)
    )
}
