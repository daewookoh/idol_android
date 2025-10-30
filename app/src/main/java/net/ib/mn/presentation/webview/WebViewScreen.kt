package net.ib.mn.presentation.webview

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoWebView
import net.ib.mn.ui.theme.ExodusTheme

/**
 * WebView 화면
 *
 * @param url 로드할 URL (필수)
 * @param title AppBar 타이틀 (옵션) - 있으면 AppBar 표시, 없으면 전체 화면 WebView
 * @param onNavigateBack 뒤로 가기 콜백
 */
@Composable
fun WebViewScreen(
    url: String,
    title: String? = null,
    onNavigateBack: () -> Unit
) {
    // 백버튼 처리
    BackHandler {
        onNavigateBack()
    }

    ExoScaffold(
        topBar = {
            // 타이틀이 있으면 AppBar 표시
            if (title != null) {
                ExoAppBar(
                    title = title,
                    onNavigationClick = onNavigateBack
                )
            }
        }
    ) {
        // WebView
        ExoWebView(
            url = url,
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(
    name = "Light Mode with Title",
    showSystemUi = true,
    showBackground = true,
    locale = "ko"
)
@Composable
fun WebViewScreenPreviewLightWithTitle() {
    ExodusTheme(darkTheme = false) {
        WebViewScreen(
            url = "https://www.google.com",
            title = "Google",
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Dark Mode with Title",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ko"
)
@Composable
fun WebViewScreenPreviewDarkWithTitle() {
    ExodusTheme(darkTheme = true) {
        WebViewScreen(
            url = "https://www.google.com",
            title = "Google",
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Light Mode without Title",
    showSystemUi = true,
    showBackground = true,
    locale = "ko"
)
@Composable
fun WebViewScreenPreviewLightWithoutTitle() {
    ExodusTheme(darkTheme = false) {
        WebViewScreen(
            url = "https://www.google.com",
            title = null,
            onNavigateBack = {}
        )
    }
}

@Preview(
    name = "Dark Mode without Title",
    showSystemUi = true,
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    locale = "ko"
)
@Composable
fun WebViewScreenPreviewDarkWithoutTitle() {
    ExodusTheme(darkTheme = true) {
        WebViewScreen(
            url = "https://www.google.com",
            title = null,
            onNavigateBack = {}
        )
    }
}
