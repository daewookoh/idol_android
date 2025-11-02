package net.ib.mn.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.FabPosition
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import net.ib.mn.R

/**
 * 앱 전체에서 사용하는 공통 Scaffold.
 * 기본적으로 전체 화면을 사용하고 Safe Area만큼 패딩을 적용합니다.
 *
 * @param modifier Scaffold에 적용할 Modifier
 * @param useFullScreen true일 경우 위쪽 패딩 없이 화면 사용 (기본: false)
 * @param topBar 상단 앱바
 * @param bottomBar 하단 네비게이션 바
 * @param snackbarHost 스낵바 호스트
 * @param floatingActionButton 플로팅 액션 버튼
 * @param floatingActionButtonPosition FAB 위치
 * @param containerColor 배경색
 * @param contentColor 컨텐츠 색상
 * @param content Scaffold 내부 컨텐츠 (자동으로 SafeArea 패딩이 적용됩니다)
 */
@Composable
fun ExoScaffold(
    modifier: Modifier = Modifier,
    useFullScreen: Boolean = false,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = colorResource(id = R.color.background_100),
    contentColor: Color = colorResource(id = R.color.text_default),
    content: @Composable () -> Unit
) {
    val windowInsets = if (useFullScreen) {
        WindowInsets(0.dp)
    } else {
        WindowInsets.systemBars
    }

    Scaffold(
        modifier = modifier.fillMaxSize(), // Scaffold는 항상 전체 화면 차지
        topBar = topBar,
        bottomBar = bottomBar,
        snackbarHost = snackbarHost,
        floatingActionButton = floatingActionButton,
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = windowInsets,
        content = { paddingValues ->
            // SafeArea 패딩을 자동으로 적용
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                content()
            }
        }
    )
}
