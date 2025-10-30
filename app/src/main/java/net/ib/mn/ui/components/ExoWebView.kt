package net.ib.mn.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import net.ib.mn.R

/**
 * 앱 전체에서 사용하는 공통 WebView 컴포넌트
 *
 * 기본 설정:
 * - JavaScript 활성화
 * - DomStorage 활성화
 * - WebViewClient 설정 (앱 내에서 웹페이지 로드)
 * - 로딩 인디케이터 표시
 *
 * @param url 로드할 URL
 * @param modifier Modifier
 */
@Composable
fun ExoWebView(
    url: String,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    // WebView 기본 설정
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    // WebViewClient 설정: 앱 내에서 웹페이지 로드
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }

                        override fun onPageStarted(
                            view: WebView?,
                            url: String?,
                            favicon: android.graphics.Bitmap?
                        ) {
                            super.onPageStarted(view, url, favicon)
                            isLoading = true
                        }
                    }

                    // URL 로드
                    loadUrl(url)
                }
            },
            update = { webView ->
                // URL이 변경되면 새로운 URL 로드
                if (webView.url != url) {
                    webView.loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 로딩 인디케이터
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = colorResource(id = R.color.main)
            )
        }
    }
}
