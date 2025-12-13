package net.ib.mn.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import net.ib.mn.ui.theme.ColorPalette

/**
 * ExoOverlay - 전체화면 오버레이 컴포넌트
 *
 * 프로젝트 전체에서 사용하는 통일된 전체화면 오버레이 UI
 * - AnimatedVisibility + fadeIn/fadeOut 애니메이션
 * - fillMaxSize 자동 적용
 * - BackHandler 자동 처리
 *
 * @param visible 오버레이 표시 여부
 * @param onDismiss 오버레이 닫기 콜백 (백버튼 포함)
 * @param enableBackHandler 백버튼으로 닫기 활성화 (기본: true)
 * @param enter 진입 애니메이션 (기본: fadeIn)
 * @param exit 퇴장 애니메이션 (기본: fadeOut)
 * @param content 오버레이 콘텐츠
 */
@Composable
fun ExoOverlay(
    visible: Boolean,
    onDismiss: () -> Unit,
    enableBackHandler: Boolean = true,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    content: @Composable () -> Unit
) {
    if (enableBackHandler && visible) {
        BackHandler { onDismiss() }
    }

    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorPalette.background100)
        ) {
            content()
        }
    }
}

/**
 * ExoOverlay - 제네릭 버전 (nullable 상태용)
 *
 * nullable 데이터를 기반으로 오버레이를 표시하는 버전
 * data가 null이 아닐 때 오버레이를 표시하고, content에 non-null 데이터를 전달
 *
 * @param data 오버레이에 표시할 데이터 (null이면 오버레이 숨김)
 * @param onDismiss 오버레이 닫기 콜백 (백버튼 포함)
 * @param enableBackHandler 백버튼으로 닫기 활성화 (기본: true)
 * @param enter 진입 애니메이션 (기본: fadeIn)
 * @param exit 퇴장 애니메이션 (기본: fadeOut)
 * @param content 오버레이 콘텐츠 (non-null 데이터 전달)
 */
@Composable
fun <T : Any> ExoOverlay(
    data: T?,
    onDismiss: () -> Unit,
    enableBackHandler: Boolean = true,
    enter: EnterTransition = fadeIn(),
    exit: ExitTransition = fadeOut(),
    content: @Composable (T) -> Unit
) {
    if (enableBackHandler && data != null) {
        BackHandler { onDismiss() }
    }

    AnimatedVisibility(
        visible = data != null,
        enter = enter,
        exit = exit,
        modifier = Modifier.fillMaxSize()
    ) {
        data?.let {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(ColorPalette.background100)
            ) {
                content(it)
            }
        }
    }
}
