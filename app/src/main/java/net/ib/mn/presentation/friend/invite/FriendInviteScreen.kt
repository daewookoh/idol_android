package net.ib.mn.presentation.friend.invite

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.ui.components.ExoAppBar
import net.ib.mn.ui.components.ExoScaffold
import net.ib.mn.ui.theme.ColorPalette
import net.ib.mn.util.Constants
import net.ib.mn.util.LocaleUtil
import net.ib.mn.util.ServerUrl
import net.ib.mn.util.link.LinkUtil
import net.ib.mn.util.logD

/**
 * FriendInviteScreen - 친구 초대 웹뷰 화면
 *
 * old 프로젝트의 FriendInviteActivity를 Compose로 변환
 * - ExoScaffold 사용
 * - 탑바 + 웹뷰 구성
 * - 네이티브-웹뷰 통신 (WebBridge)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun FriendInviteScreen(
    token: String,
    language: String = LocaleUtil.getWikiLocale(LocalContext.current),
    onBackClick: () -> Unit = {},
    viewModel: FriendInviteViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // 백버튼 처리
    BackHandler { onBackClick() }

    // ViewModel 상태 구독
    val inviteMsg by viewModel.inviteMsg.collectAsState()
    val errorMsg by viewModel.errorMsg.collectAsState()

    // 초대 메시지가 준비되면 공유
    LaunchedEffect(inviteMsg) {
        inviteMsg?.let { msg ->
            shareInvite(context, msg)
            viewModel.consumeInviteMsg()
        }
    }

    // 에러 메시지 표시
    LaunchedEffect(errorMsg) {
        errorMsg?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.consumeErrorMsg()
        }
    }

    // WebView 헤더
    val headers = remember {
        mapOf(
            "exodus-signature" to "c51336216ccead42682bbe020058fc3d141697ee0ad289ada5730111dd6fd18a",
            "X-HTTP-APPID" to Constants.APP_ID,
            "X-HTTP-VERSION" to context.getString(R.string.app_version),
            "X-HTTP-NATION" to language,
        )
    }

    // WebView URL 생성
    val webViewUrl = remember(token, language) {
        val urlPrefix = if (ServerUrl.isTestServer()) {
            ServerUrl.HOST_TEST
        } else {
            ServerUrl.HOST_REAL
        }

        val baseUrl = "$urlPrefix/webview/invite/$language"
        baseUrl.toUri().buildUpon()
            .appendQueryParameter("token", token)
            .build()
            .toString()
    }

    // WebView cleanup
    DisposableEffect(Unit) {
        onDispose {
            webViewInstance?.apply {
                loadUrl("about:blank")
                removeAllViews()
                destroy()
            }
        }
    }

    ExoScaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ExoAppBar(
                title = stringResource(R.string.invite_friend_detail_title),
                onNavigationClick = onBackClick
            )
        }
    ) {
        // 웹뷰 배경색 (다크모드 대응)
        val webViewBackgroundColor = ColorPalette.background100.toArgb()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ColorPalette.background100)
        ) {
            AndroidView(
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewInstance = this

                        // 웹뷰 배경색 설정 (다크모드 대응)
                        setBackgroundColor(webViewBackgroundColor)

                        // UserAgent 설정 (old와 동일)
                        val baseUa = WebSettings.getDefaultUserAgent(ctx)
                        val customUa = "$baseUa (${ctx.applicationInfo.packageName}/${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
                        logD("FriendInvite", "invite friend ua :: $customUa")

                        settings.apply {
                            userAgentString = customUa
                            textZoom = 100
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        }

                        // JavaScript Interface 추가 (네이티브-웹뷰 통신)
                        addJavascriptInterface(
                            WebBridge(
                                onInviteCodeClick = { viewModel.getInviteMsg() },
                                onInviteBtnClick = { viewModel.getInviteMsg() },
                                onError = { message ->
                                    Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
                                    onBackClick()
                                }
                            ),
                            "Exodus"
                        )

                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                url: String?
                            ): Boolean {
                                return false
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                isLoading = false
                            }

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: WebResourceResponse
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                                    Toast.makeText(
                                        ctx,
                                        R.string.error_abnormal_default,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    onBackClick()
                                }
                            }
                        }

                        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
                        webChromeClient = WebChromeClient()

                        // URL 로드
                        loadUrl(webViewUrl, headers)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // 로딩 인디케이터
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = ColorPalette.main
                )
            }
        }
    }
}

/**
 * WebBridge - JavaScript Interface
 * 웹뷰에서 네이티브로 이벤트를 전달
 */
private class WebBridge(
    private val onInviteCodeClick: () -> Unit,
    private val onInviteBtnClick: () -> Unit,
    private val onError: (String) -> Unit
) {
    @JavascriptInterface
    fun onEvent(message: String) {
        logD("FriendInvite", "invite friend event msg :: $message")

        if (message.contains("ERROR")) {
            onError(message)
            return
        }

        when (message) {
            "friend_invite_code" -> {
                onInviteCodeClick()
            }
            "friend_invite_btn" -> {
                onInviteBtnClick()
            }
        }
    }
}

/**
 * 초대 메시지 공유
 */
private fun shareInvite(context: Context, msg: String) {
    val url = LinkUtil.getAppLinkUrlForWebView(
        context = context,
        params = listOf("webview", "invite_share")
    )

    val fullMsg = "$msg\n$url"

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, fullMsg)
    }
    val chooser = Intent.createChooser(intent, context.getString(R.string.title_share))
    context.startActivity(chooser)
}
