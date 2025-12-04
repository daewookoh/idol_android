package net.ib.mn.presentation.webview

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.components.ExoWebView
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.ui.theme.ExodusTheme

/**
 * WebView 화면
 *
 * @param url 로드할 URL (htmlContent가 없을 때 사용)
 * @param htmlContent 직접 로드할 HTML 콘텐츠 (우선 적용)
 * @param baseUrl HTML 콘텐츠 로드 시 기본 URL
 * @param screenTitle AppBar 타이틀 (옵션) - 있으면 AppBar 표시
 * @param contentTitle 콘텐츠 상단 헤더 타이틀 (옵션) - WebView 위에 표시
 * @param onNavigateBack 뒤로 가기 콜백
 */
@Composable
fun WebViewScreen(
    url: String? = null,
    htmlContent: String? = null,
    baseUrl: String? = null,
    screenTitle: String? = null,
    contentTitle: String? = null,
    onNavigateBack: () -> Unit
) {
    BackHandler {
        onNavigateBack()
    }

    ExoScaffold(
        topBar = {
            if (screenTitle != null) {
                ExoAppBar(
                    title = screenTitle,
                    onNavigationClick = onNavigateBack
                )
            }
        }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 콘텐츠 상단 헤더 타이틀
            if (!contentTitle.isNullOrEmpty()) {
                Text(
                    text = contentTitle,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = ColorPalette.textDefault,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                )
            }

            // WebView
            key(htmlContent ?: url) {
                ExoWebView(
                    url = url,
                    htmlContent = htmlContent,
                    baseUrl = baseUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

// 기존 호출 호환을 위한 오버로드 함수
@Composable
fun WebViewScreen(
    url: String? = null,
    htmlContent: String? = null,
    baseUrl: String? = null,
    title: String? = null,
    onNavigateBack: () -> Unit
) {
    WebViewScreen(
        url = url,
        htmlContent = htmlContent,
        baseUrl = baseUrl,
        screenTitle = title,
        contentTitle = null,
        onNavigateBack = onNavigateBack
    )
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
            screenTitle = "공지사항",
            contentTitle = "공지 제목입니다",
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
            screenTitle = "공지사항",
            contentTitle = "공지 제목입니다",
            onNavigateBack = {}
        )
    }
}
