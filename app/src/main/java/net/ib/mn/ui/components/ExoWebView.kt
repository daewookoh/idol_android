package net.ib.mn.ui.components

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.viewinterop.AndroidView
import net.ib.mn.R

/**
 * 앱 전체에서 사용하는 공통 WebView 컴포넌트
 *
 * @param url 로드할 URL (htmlContent가 없을 때 사용)
 * @param htmlContent 직접 로드할 HTML 콘텐츠 (우선 적용)
 * @param baseUrl HTML 콘텐츠 로드 시 기본 URL
 * @param modifier Modifier
 */
@Composable
fun ExoWebView(
    url: String? = null,
    htmlContent: String? = null,
    baseUrl: String? = null,
    modifier: Modifier = Modifier
) {
    var isLoading by remember { mutableStateOf(true) }

    val wrappedHtml = remember(htmlContent) {
        if (htmlContent.isNullOrEmpty()) null
        else wrapHtmlContent(htmlContent)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = false
                        displayZoomControls = false
                        useWideViewPort = true
                        loadWithOverviewMode = true
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onReceivedError(
                            view: WebView?,
                            errorCode: Int,
                            description: String?,
                            failingUrl: String?
                        ) {
                            super.onReceivedError(view, errorCode, description, failingUrl)
                            isLoading = false
                        }
                    }

                    setBackgroundColor(android.graphics.Color.WHITE)

                    when {
                        !wrappedHtml.isNullOrEmpty() -> {
                            loadDataWithBaseURL(baseUrl, wrappedHtml, "text/html", "UTF-8", null)
                        }
                        !url.isNullOrEmpty() -> {
                            loadUrl(url)
                        }
                        else -> {
                            isLoading = false
                        }
                    }
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = colorResource(id = R.color.main)
            )
        }
    }
}

/**
 * HTML 콘텐츠를 적절한 CSS 스타일과 함께 래핑
 */
private fun wrapHtmlContent(content: String): String {
    return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                * { box-sizing: border-box; }
                html, body {
                    margin: 0;
                    padding: 16px;
                    background-color: #FFFFFF;
                    color: #000000;
                    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                    font-size: 16px;
                    line-height: 1.6;
                    word-wrap: break-word;
                    overflow-wrap: break-word;
                }
                img { max-width: 100%; height: auto; }
                a { color: #007AFF; }
                p { margin: 0 0 16px 0; }
            </style>
        </head>
        <body>$content</body>
        </html>
    """.trimIndent()
}
