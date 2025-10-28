package net.ib.mn.activity

import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.webkit.*
import android.widget.FrameLayout
import net.ib.mn.R
import net.ib.mn.billing.util.JsInterface
import net.ib.mn.billing.util.NativeXManager
import net.ib.mn.billing.util.WebStateListener
import net.ib.mn.utils.Util
import java.util.*

// https://doc.xplorechina.com/document/android-for-pay.html
class NativeXActivity  : BaseActivity() {
    var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nativex)

        // 구매 성공 팝업 닫힘 방지
        FLAG_CLOSE_DIALOG = false

        webView = findViewById(R.id.webView)
        var settings = webView!!.settings
        webView?.webChromeClient = MyWebChromeClient()
        val webViewClient = MyWebViewClient()
        webViewClient.context = this
        webViewClient.tag = 1
        webView?.webViewClient = webViewClient

        settings.javaScriptEnabled = true
        settings.setSupportMultipleWindows(true)
        settings.javaScriptCanOpenWindowsAutomatically = true
        settings.domStorageEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setBuiltInZoomControls(true)
        settings.displayZoomControls = false
        CookieManager.getInstance().setAcceptCookie(true)

        if (Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
        }

        val tradeNo = intent.getStringExtra(NativeXManager.EXTRA_TRADENO)
        webView?.addJavascriptInterface(JsInterface(object : WebStateListener {
            override fun onCloseWindow() {
                runOnUiThread {
                    webView!!.removeAllViews()
                    webView!!.destroy()
                    // 아래는 'This method can not be called from the main application thread' 예외 발생하여 변경
//                    val inst = Instrumentation()
//                    inst.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                    val intent = Intent().putExtra(NativeXManager.EXTRA_TRADENO, tradeNo)
                    setResult(RESULT_OK, intent)
                    finish()

                }
            }
        }), "Android")

        val url = "https://collect.h5mob.com/v3/pay"
        val params = intent.getStringExtra(NativeXManager.EXTRA_PARAMS)
        val signature = intent.getStringExtra(NativeXManager.EXTRA_SIGNATURE)
        val postData = params + "&sign="+signature
        Util.log("PWActivity webView=${webView.toString()} url=${url}")
        webView?.postUrl(url, postData.toByteArray(Charsets.UTF_8))
        Util.log(url+"?"+postData)
//        webView?.loadUrl(url+"?"+postData)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        Util.log("onActivityResult requestCode=${requestCode} resultCode=${resultCode}")
    }

//    override fun onBackPressed() {
//        if (webview_container.childCount > 1) {
//            var webView = webview_container.getChildAt(webview_container.childCount - 1) as WebView
//            if (webView.canGoBack()) {
//                webView.goBack()
//            } else {
//                webview_container.removeView(webView)
//                webView.destroy()
//            }
//        } else {
//            super.onBackPressed()
//        }
//    }

    open class MyWebChromeClient : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            Util.log("view=${view.toString()} isDialog=${isDialog} userGesture=${isUserGesture}")
            var newWebView = WebView(view!!.context)
            var settings = newWebView.getSettings()
            settings.setJavaScriptEnabled(true)
            settings.javaScriptCanOpenWindowsAutomatically = true
            settings.setSupportMultipleWindows(true)
            settings.setSupportZoom(true)
            settings.domStorageEnabled = true
            settings.useWideViewPort = true
            settings.loadWithOverviewMode = true
            settings.setBuiltInZoomControls(true)
            settings.displayZoomControls = false

            val webViewClient = MyWebViewClient()
            webViewClient.context = view!!.context as BaseActivity
            newWebView.webViewClient = MyWebViewClient()
            newWebView.webChromeClient = MyWebChromeClient()

            val a = view!!.context as NativeXActivity
            val container = a.findViewById<FrameLayout>(R.id.webview_container)
            var lp = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            newWebView.layoutParams = lp
            container.addView(newWebView)

            newWebView.setWebChromeClient(object : MyWebChromeClient() {
                override fun onCloseWindow(window: WebView?) {
                    container.removeView(window)
                    window?.destroy()
                }
            })

            (resultMsg!!.obj as WebView.WebViewTransport).webView = newWebView
            resultMsg.sendToTarget()

            return true
        }

        override fun onCloseWindow(window: WebView?) {
            super.onCloseWindow(window)
            (window?.context as BaseActivity)?.finish()
        }
    }

    open class MyWebViewClient : WebViewClient() {
        var context: BaseActivity? = null
        var tag: Int = 0

        companion object {
            var lastUrl: String? = null
        }

        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            context = view?.context as BaseActivity
            val uri: Uri = Uri.parse(url)
            return handleUri(view, uri)
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest
        ): Boolean {
            context = view?.context as BaseActivity
            val uri: Uri = request.url
            return handleUri(view, uri)
        }

        fun handleUri(view: WebView?, uri: Uri): Boolean {
            val url = uri.toString()
            Util.log("shouldOverrideUrlLoading webView=" + view.toString() + " url=" + url)
//            ApiResources.postUserLog(context, "user.nativex", url)

            try {
                // WechatPay and Alipay Scheme
                if (url.startsWith("weixin://") || url.startsWith("alipays://")) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context?.startActivity(intent)
                    return true
                }
            } catch (e: Exception) {
                return true
            }

            if (url.startsWith("https://wx.tenpay.com/")) {
                val extraHeaders: MutableMap<String, String> = HashMap()
                extraHeaders["Referer"] = "https://pay.ipaynow.cn"
                view?.loadUrl(url, extraHeaders)
            } else {
                view?.loadUrl(url)
            }
            return true
        }
    }
}