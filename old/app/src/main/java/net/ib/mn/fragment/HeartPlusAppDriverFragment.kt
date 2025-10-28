package net.ib.mn.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import net.ib.mn.R
import net.ib.mn.account.IdolAccount.Companion.getAccount
import net.ib.mn.databinding.FragmentHeartplusAppDriverBinding
import net.ib.mn.utils.Const
import net.ib.mn.utils.Util
import java.net.URISyntaxException

class HeartPlusAppDriverFragment : BaseFragment() {
    private var mWebViewSettings: WebSettings? = null

    private var _binding: FragmentHeartplusAppDriverBinding? = null
    private val binding get() = _binding!!

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mWebViewSettings = binding.webviewAppDriver.getSettings()
        mWebViewSettings!!.setJavaScriptEnabled(true)
        mWebViewSettings!!.setSupportMultipleWindows(true)
        mWebViewSettings!!.setJavaScriptCanOpenWindowsAutomatically(true)
        mWebViewSettings!!.setSupportZoom(true)
        mWebViewSettings!!.setDomStorageEnabled(true)
        mWebViewSettings!!.setUseWideViewPort(true)
        mWebViewSettings!!.setLoadWithOverviewMode(true)
        mWebViewSettings!!.setBuiltInZoomControls(true)
        mWebViewSettings!!.setDisplayZoomControls(false)

        //TODO 앱드라이버  웹뷰 뒤로가기 필요할때  주석 해제 하기
        WebViewBackEvent(binding.webviewAppDriver)

        //합의하에 userEmail에서 userId로 변경.
        val userId = getAccount(getActivity())!!.userId
        val encryptedEmail =
            Util.sha256modi(userId.toString() + ";" + Const.REWARD_ID + ";" + Const.APPDRIVER_KEY)
        binding.webviewAppDriver.loadUrl(Const.APPDRIVER_FRONTURL + Const.APPDRIVER_APP_ID + "?identifier=" + userId + "&media_id=" + Const.REWARD_ID + "&digest=" + encryptedEmail)
        binding.webviewAppDriver.setWebChromeClient(WebChromeClient())
        binding.webviewAppDriver.setWebViewClient(WebViewClientClass())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHeartplusAppDriverBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    public override fun onResume() {
        super.onResume()
    }

    //웹뷰 관련 뒤로가기 설정
    private fun WebViewBackEvent(webView: WebView) {
        webView.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent): Boolean {
                //This is the filter
                if (event.getAction() != KeyEvent.ACTION_DOWN) return true


                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (webView.canGoBack()) {
                        webView.goBack()
                    } else {
                        (requireActivity()).onBackPressed()
                    }

                    return true
                }

                return false
            }
        })
    }

    private inner class WebViewClientClass : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String?): Boolean {
            if (url != null && url.startsWith("intent://")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    val existPackage =
                        getActivity()!!.getPackageManager().getLaunchIntentForPackage(
                            intent.getPackage()!!
                        )
                    if (existPackage != null) {
                        startActivity(intent)
                    } else {
                        val marketIntent = Intent(Intent.ACTION_VIEW)
                        marketIntent.setData(Uri.parse("market://details?id=" + intent.getPackage()))
                        startActivity(marketIntent)
                    }
                    return true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else if (url != null && url.startsWith("market://")) {
                try {
                    val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                    if (intent != null) {
                        startActivity(intent)
                    }
                    return true
                } catch (e: URISyntaxException) {
                    e.printStackTrace()
                }
            }
            view.loadUrl(url!!)
            return true
        }

        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return super.shouldOverrideUrlLoading(view, request)
        }
    }
}
