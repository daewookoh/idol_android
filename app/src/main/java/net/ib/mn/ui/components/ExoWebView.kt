package net.ib.mn.ui.components

import android.view.ViewGroup
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

    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        setSupportZoom(true)
                        builtInZoomControls = true
                        displayZoomControls = false
                        textZoom = 103
                    }
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            isLoading = false
                        }
                    }
                    when {
                        !htmlContent.isNullOrEmpty() -> {
                            loadDataWithBaseURL(baseUrl, htmlContent, "text/html; charset=utf-8", "UTF-8", null)
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
