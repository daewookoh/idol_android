package net.ib.mn.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import net.ib.mn.R

/**
 * 앱 전체에서 사용하는 공통 Scaffold.
 * 기본적으로 Safe Area 안에서 작동하도록 systemBars padding을 적용합니다.
 *
 * Edge-to-Edge가 필요한 화면(예: StartUp, Splash)에서는 이 Scaffold를 사용하지 않고
 * 직접 Scaffold를 사용하거나 applySystemBarsPadding = false로 설정합니다.
 *
 * @param modifier Scaffold에 적용할 Modifier
 * @param applySystemBarsPadding Safe Area 패딩 적용 여부 (기본: true)
 * @param topBar 상단 앱바
 * @param bottomBar 하단 네비게이션 바
 * @param snackbarHost 스낵바 호스트
 * @param floatingActionButton 플로팅 액션 버튼
 * @param floatingActionButtonPosition FAB 위치
 * @param containerColor 배경색
 * @param contentColor 컨텐츠 색상
 * @param content Scaffold 내부 컨텐츠
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
    // applyContentWindowInsets 값에 따라 Scaffold에 전달할 인셋 결정
    val windowInsets = if (useFullScreen) {
        WindowInsets(0.dp)
    } else {
        ScaffoldDefaults.contentWindowInsets
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
