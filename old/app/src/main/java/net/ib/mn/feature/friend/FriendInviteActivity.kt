package net.ib.mn.feature.friend

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.common.util.logD
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment.Companion.newInstance
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.utils.AppConst
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Toast
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.setUiActionFirebaseGoogleAnalyticsActivity

@AndroidEntryPoint
class FriendInviteActivity : BaseActivity() {
    private lateinit var webView: WebView

    private val viewModel: FriendInviteViewModel by viewModels()
    private lateinit var headers: Map<String, String>

    @OptIn(UnstableApi::class)
    @SuppressLint("SetJavaScriptEnabled", "AddJavascriptInterface")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_friend_invite)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayShowTitleEnabled(true)
            setDisplayHomeAsUpEnabled(true)
            setHomeButtonEnabled(true)
            title = getString(R.string.invite_friend_detail_title)
        }

        onBackPressedDispatcher.addCallback(this) {
            if (isTaskRoot) {
                val intent = Intent(this@FriendInviteActivity, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } else {
                finish()
            }
        }

        observeVM()

        toolbar.setNavigationOnClickListener {
            if (isTaskRoot) {
                val intent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
            } else {
                finish()
            }
        }

        val invitePayload = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(INVITE_PAYLOAD, InvitePayload::class.java)
        } else {
            intent.getParcelableExtra<InvitePayload>(INVITE_PAYLOAD)
        }

        val root = findViewById<View>(R.id.root)

        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            insets
        }

        webView = findViewById(R.id.webView)

        val baseUa = WebSettings.getDefaultUserAgent(this)
        val customUa =
            "$baseUa (${applicationInfo.packageName}/${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})"
        logD("invite friend ua :: $customUa")

        // WebView 설정
        val settings: WebSettings = webView.settings
        settings.userAgentString = customUa
        settings.textZoom = 100
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

        webView.addJavascriptInterface(WebBridge(this), "Exodus")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }

            override fun onReceivedHttpError(
                view: WebView,
                request: android.webkit.WebResourceRequest,
                errorResponse: android.webkit.WebResourceResponse
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                    Toast.makeText(
                        this@FriendInviteActivity,
                        R.string.error_abnormal_default,
                        Toast.LENGTH_SHORT
                    ).show()

                    if (isTaskRoot) {
                        val intent = Intent(this@FriendInviteActivity, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        startActivity(intent)
                    } else {
                        finish()
                    }
                }
            }
        }
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
        webView.webChromeClient = WebChromeClient()

        headers = mapOf(
            "exodus-signature" to "c51336216ccead42682bbe020058fc3d141697ee0ad289ada5730111dd6fd18a",
            "X-HTTP-APPID" to AppConst.APP_ID,
            "X-HTTP-VERSION" to getString(R.string.app_version),
            "X-HTTP-NATION" to invitePayload?.language!!,
        )
        
        val urlPrefix = if (ServerUrl.HOST == ServerUrl.HOST_TEST) {
            ServerUrl.HOST_BBB_TEST
        } else {
            ServerUrl.HOST_REAL
        }

        val baseUrl = "$urlPrefix/webview/invite/${invitePayload.language}"
        val url = baseUrl.toUri().buildUpon()
            .appendQueryParameter("token", invitePayload.token)
            .build()
            .toString()

        webView.loadUrl(url, headers)
    }

    inner class WebBridge(private val context: Context) {
        @JavascriptInterface
        fun onEvent(message: String) {
            logD("invite friend event msg :: $message")
            if (message.contains("ERROR")) {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

                if (isTaskRoot) {
                    val intent = Intent(this@FriendInviteActivity, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                } else {
                    finish()
                }
                return
            }

            when (message) {
                GaAction.FRIEND_INVITE_CODE.label -> {
                    setUiActionFirebaseGoogleAnalyticsActivity(
                        context,
                        GaAction.FRIEND_INVITE_CODE.actionValue,
                        GaAction.FRIEND_INVITE_CODE.label
                    )

                    viewModel.getInviteMsg()
                }

                GaAction.FRIEND_INVITE_BTN.label -> {
                    setUiActionFirebaseGoogleAnalyticsActivity(
                        context,
                        GaAction.FRIEND_INVITE_BTN.actionValue,
                        GaAction.FRIEND_INVITE_BTN.label
                    )

                    viewModel.getInviteMsg()
                }
            }
        }

        private fun shareText(context: Context, title: String, text: String) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, text)
            }
            val chooser = Intent.createChooser(intent, title)
            // Activity 컨텍스트가 아니면 FLAG 필요하지만 여기선 Activity로 호출됨
            context.startActivity(chooser)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webView.apply {
            loadUrl("about:blank")
            removeAllViews()
            destroy()
        }
    }

    private fun observeVM() = with(viewModel) {
        inviteMsg.observe(this@FriendInviteActivity, SingleEventObserver { inviteMsg ->
            val url = LinkUtil.getAppLinkUrlForWebView(
                context = this@FriendInviteActivity,
                params = listOf("webview", LinkStatus.INVITE_SHARE.status)
            )
            UtilK.linkStart(context = this@FriendInviteActivity, url = url, msg = inviteMsg)
        })

        reward.observe(this@FriendInviteActivity, SingleEventObserver { rewardPair ->
            val rewardBottomSheetDialogFragment = newInstance(
                resId = RewardBottomSheetDialogFragment.FLAG_INVITE_REWARD,
                bonusHeart = rewardPair.first,
                plusHeart = rewardPair.second,
            ) {
                webView.post {
                    val urlToLoad = webView.url ?: return@post
                    webView.loadUrl(urlToLoad, headers)
                }
            }
            rewardBottomSheetDialogFragment.show(supportFragmentManager, "reward_invite")
        })
    }

    companion object {
        const val INVITE_PAYLOAD = "invite_payload"
    }
}