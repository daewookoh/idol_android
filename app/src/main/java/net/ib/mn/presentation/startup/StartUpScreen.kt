package net.ib.mn.presentation.startup

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.R
import net.ib.mn.ui.theme.ExodusTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * 스타트업 화면.
 * safe area 내에서 전체 화면을 사용합니다.
 *
 * UI 구성:
 * - 배경: @color/text_white_black (라이트: #ffffff, 다크: #121212)
 * - 로고 이미지: startup_logo.xml (variant별 다른 XML drawable), width=150dp, 비율 유지
 * - 프로그레스바: 하단 중앙, width=160dp, marginBottom=60dp
 *   - color: @color/main (라이트: #ff4444, 다크: #E24848)
 *   - trackColor: @color/gray150 (라이트: #dddddd, 다크: #404040)
 */
@Composable
fun StartUpScreen(
    onNavigateToMain: () -> Unit,
    onNavigateToLogin: () -> Unit,
    viewModel: StartUpViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Side effects 처리
    LaunchedEffect(Unit) {
        viewModel.effect.collectLatest { effect ->
            when (effect) {
                is StartUpContract.Effect.NavigateToMain -> {
                    onNavigateToMain()
                }
                is StartUpContract.Effect.NavigateToLogin -> {
                    onNavigateToLogin()
                }
                is StartUpContract.Effect.ShowError -> {
                    // TODO: 에러 처리 (필요시 Toast 또는 Dialog)
                }
            }
        }
    }

    StartUpContent(state = state)
}

/**
 * 스타트업 화면의 UI 컨텐츠 (Stateless).
 * 프리뷰 및 테스트를 위한 stateless composable.
 */
@Composable
private fun StartUpContent(
    state: StartUpContract.State
) {

    Scaffold(
        containerColor = colorResource(id = R.color.text_white_black),
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            // 스타트업 로고 (variant별로 다른 XML drawable 사용)
            Image(
                painter = painterResource(id = R.drawable.startup_logo),
                contentDescription = "Startup Logo",
                modifier = Modifier.width(150.dp),
                contentScale = ContentScale.FillWidth
            )

            // 하단 프로그레스바
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
            ) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier
                        .width(160.dp)
                        .height(1.dp),
                    color = colorResource(id = R.color.main),
                    trackColor = colorResource(id = R.color.gray150),
                    strokeCap = StrokeCap.Butt,
                )
            }
        }
    }
}

@Preview(
    name = "Light Mode",
    showSystemUi = true,
    showBackground = true
)
@Composable
fun StartUpScreenPreviewLight() {
    ExodusTheme(darkTheme = false) {
        StartUpContent(
            state = StartUpContract.State()
        )
    }
}

@Preview(
    name = "Dark Mode",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun StartUpScreenPreviewDark() {
    ExodusTheme(darkTheme = true) {
        StartUpContent(
            state = StartUpContract.State()
        )
    }
}
