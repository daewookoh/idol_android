package net.ib.mn.link

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebChromeClient
import androidx.databinding.DataBindingUtil
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.databinding.ActivityWebViewBinding
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.ext.applySystemBarInsets

open class BaseWebViewActivity : BaseActivity() {

    protected lateinit var binding: ActivityWebViewBinding

    protected var myTitle: String? = null // hilt 적용시 Hilt_WebViewActivity.getTitle() overrides final method in class Landroid/app/Activity; 오류 발생하여 변경
    protected var isShowShare: Boolean = true

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_web_view)
        binding.clContainer.applySystemBarInsets()

        myTitle = intent.extras?.getString(PARAM_TITLE)
        isShowShare = intent.extras?.getBoolean(PARAM_IS_SHOW_SHARE) ?: true

        val actionbar = supportActionBar
        actionbar!!.title = myTitle

        with(binding) {
            webview.apply {
                settings.javaScriptEnabled = true
                settings.textZoom = 80
                settings.loadsImagesAutomatically = true
                settings.domStorageEnabled = true
                webChromeClient = WebChromeClient()
                webViewClient = customWebViewClient()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (isShowShare) {
            menuInflater.inflate(R.menu.share_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btn_share -> {
                shareLink()
            }

            else -> {
                return super.onOptionsItemSelected(item)
            }
        }

        return super.onOptionsItemSelected(item)
    }

    open fun shareLink() = Unit

    open fun customWebViewClient() = CustomWebViewClient(this)

    override fun onPause() {
        super.onPause()
        binding.webview.onPause()
    }

    override fun onResume() {
        super.onResume()
        binding.webview.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.webview.destroy()
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.CLOSE_WEB_VIEW.actionValue,
                GaAction.CLOSE_WEB_VIEW.label,
            )
            super.onBackPressed()
        }
    }

    companion object {
        const val PARAM_TITLE = "title" // 웹뷰 상단 제목.
        const val PARAM_IS_SHOW_SHARE = "isShowShare"
    }
}