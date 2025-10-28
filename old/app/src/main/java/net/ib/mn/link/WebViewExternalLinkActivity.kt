package net.ib.mn.link

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import net.ib.mn.R

class WebViewExternalLinkActivity : BaseWebViewActivity() {

    private var uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initSet()
    }

    private fun initSet() {
        val dataString = intent.dataString

        uri = Uri.parse(dataString)

        with(binding) {
            header.visibility = View.GONE
            emptyView.visibility = View.GONE

            webview.loadUrl(uri.toString())
        }
    }

    override fun shareLink() {
        val shareIntent =
            Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"

        shareIntent.putExtra(Intent.EXTRA_TEXT, uri.toString())

        startActivity(
            Intent.createChooser(
                shareIntent,
                resources.getString(R.string.title_share),
            ),
        )
    }

    override fun customWebViewClient() =
        object : CustomWebViewClient(this@WebViewExternalLinkActivity) {

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                supportActionBar?.title = view?.title
            }
        }

    companion object {

        @JvmStatic
        fun createIntent(context: Context, title: String?): Intent {
            return Intent(context, WebViewExternalLinkActivity::class.java)
                .putExtra(PARAM_TITLE, title)
        }
    }
}