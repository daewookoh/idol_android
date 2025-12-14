package net.ib.mn.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import net.ib.mn.tutorial.TutorialManager

/**
 * 튜토리얼 하트 컴포넌트
 *
 * 특정 위치에 반짝이는 애니메이션 하트를 표시하고,
 * 클릭하면 터치 애니메이션을 재생한 후 콜백을 실행합니다.
 *
 * @param modifier Modifier
 * @param tutorialBit 이 위치에 해당하는 튜토리얼 비트 인덱스
 * @param animationSize 애니메이션 크기 (기본값: 28dp)
 * @param onTutorialComplete 튜토리얼 클릭 완료 시 실행할 콜백
 */
@Composable
fun ExoTutorialHeart(
    modifier: Modifier = Modifier,
    tutorialBit: Int,
    animationSize: Dp = 28.dp,
    onTutorialComplete: () -> Unit
) {
    // StateFlow를 Compose에서 올바르게 관찰하여 상태 변경 시 재구성되도록 함
    val currentTutorialIndex by TutorialManager.currentTutorialIndex.collectAsState()

    // 현재 튜토리얼 인덱스가 이 비트와 일치하고, 아직 완료되지 않은 경우에만 표시
    val shouldShow = currentTutorialIndex == tutorialBit && TutorialManager.isShown(tutorialBit)

    if (shouldShow) {
        TutorialHeartAnimation(
            modifier = modifier,
            animationSize = animationSize,
            onComplete = onTutorialComplete
        )
    }
}

/**
 * 튜토리얼 하트 애니메이션 (내부 구현)
 *
 * 1. tutorial_heart.json: 반짝이는 대기 애니메이션 (무한 반복)
 * 2. tutorial_heart_touch.json: 터치 시 재생되는 애니메이션 (1회)
 */
@Composable
private fun TutorialHeartAnimation(
    modifier: Modifier = Modifier,
    animationSize: Dp,
    onComplete: () -> Unit
) {
    // 애니메이션 상태: idle(대기) -> touched(터치됨)
    var isTouched by remember { mutableStateOf(false) }

    // 대기 애니메이션 (반짝임)
    val idleComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("tutorial_heart.json")
    )

    // 터치 애니메이션
    val touchComposition by rememberLottieComposition(
        LottieCompositionSpec.Asset("tutorial_heart_touch.json")
    )

    // 터치 애니메이션 진행 상태
    val touchProgress by animateLottieCompositionAsState(
        composition = touchComposition,
        isPlaying = isTouched,
        iterations = 1,
        restartOnPlay = true
    )

    // 터치 애니메이션 완료 감지
    LaunchedEffect(touchProgress) {
        if (isTouched && touchProgress >= 1f) {
            onComplete()
        }
    }

    Box(
        modifier = modifier
            .size(animationSize)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (!isTouched) {
                    isTouched = true
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (!isTouched) {
            // 대기 애니메이션 (무한 반복)
            LottieAnimation(
                composition = idleComposition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(animationSize)
            )
        } else {
            // 터치 애니메이션 (1회)
            LottieAnimation(
                composition = touchComposition,
                progress = { touchProgress },
                modifier = Modifier.size(animationSize)
            )
        }
    }
}

/**
 * 튜토리얼 하트 오버레이
 *
 * 기존 UI 위에 오버레이로 배치할 때 사용합니다.
 * Box 안에서 기존 컨텐츠와 함께 사용하세요.
 *
 * @param tutorialBit 이 위치에 해당하는 튜토리얼 비트 인덱스
 * @param alignment Box 내 정렬 위치
 * @param animationSize 애니메이션 크기
 * @param onTutorialComplete 튜토리얼 클릭 완료 시 실행할 콜백
 */
@Composable
fun TutorialHeartOverlay(
    tutorialBit: Int,
    alignment: Alignment = Alignment.Center,
    animationSize: Dp = 28.dp,
    onTutorialComplete: () -> Unit
) {
    // StateFlow를 Compose에서 올바르게 관찰하여 상태 변경 시 재구성되도록 함
    val currentTutorialIndex by TutorialManager.currentTutorialIndex.collectAsState()

    val shouldShow = currentTutorialIndex == tutorialBit && TutorialManager.isShown(tutorialBit)

    if (shouldShow) {
        Box(
            contentAlignment = alignment
        ) {
            TutorialHeartAnimation(
                animationSize = animationSize,
                onComplete = onTutorialComplete
            )
        }
    }
}
