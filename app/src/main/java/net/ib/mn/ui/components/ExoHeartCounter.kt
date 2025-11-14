package net.ib.mn.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import java.text.NumberFormat
import java.util.Locale

/**
 * ExoHeartCounter
 *
 * 숫자가 변경될 때 애니메이션 효과를 주는 컴포넌트
 * 주로 투표수(heartCount) 표시에 사용됩니다.
 *
 * @param count 표시할 숫자
 * @param modifier Modifier
 * @param style 텍스트 스타일
 * @param color 텍스트 색상
 * @param fontWeight 폰트 굵기
 * @param formatWithComma 천단위 콤마 표시 여부 (기본값: true)
 * @param animationDuration 애니메이션 지속 시간 (밀리초, 기본값: 800ms)
 */
@Composable
fun ExoHeartCounter(
    count: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    formatWithComma: Boolean = true,
    animationDuration: Int = 800
) {
    // 애니메이션 가능한 Float 값으로 count 관리
    val animatedCount = remember { Animatable(count.toFloat()) }

    // count가 변경될 때마다 애니메이션 실행
    LaunchedEffect(count) {
        animatedCount.animateTo(
            targetValue = count.toFloat(),
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = FastOutSlowInEasing
            )
        )
    }

    // 현재 표시할 값 (애니메이션 중인 값)
    val displayValue = animatedCount.value.toLong()

    // 포맷팅
    val formattedText = if (formatWithComma) {
        NumberFormat.getNumberInstance(Locale.US).format(displayValue)
    } else {
        displayValue.toString()
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = formattedText,
            style = style,
            color = color,
            fontWeight = fontWeight
        )
    }
}

/**
 * ExoHeartCounter - String 버전
 *
 * 이미 포맷팅된 문자열을 받아서 표시합니다.
 * 내부적으로 숫자로 변환 후 애니메이션 처리합니다.
 *
 * @param countString 숫자 문자열 (콤마 포함 가능, 예: "1,234")
 * @param modifier Modifier
 * @param style 텍스트 스타일
 * @param color 텍스트 색상
 * @param fontWeight 폰트 굵기
 * @param formatWithComma 천단위 콤마 표시 여부 (기본값: true)
 * @param animationDuration 애니메이션 지속 시간 (밀리초, 기본값: 800ms)
 */
@Composable
fun ExoHeartCounter(
    countString: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    formatWithComma: Boolean = true,
    animationDuration: Int = 800
) {
    // 문자열을 숫자로 변환 (콤마 제거)
    val count = remember(countString) {
        try {
            countString.replace(",", "").toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    ExoHeartCounter(
        count = count,
        modifier = modifier,
        style = style,
        color = color,
        fontWeight = fontWeight,
        formatWithComma = formatWithComma,
        animationDuration = animationDuration
    )
}
