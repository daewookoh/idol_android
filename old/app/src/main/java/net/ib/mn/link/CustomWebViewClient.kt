package net.ib.mn.link

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient

open class CustomWebViewClient(
    val activity: Activity?,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        try {
            val i = Intent(Intent.ACTION_VIEW, request?.url)
            activity?.startActivity(i)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return true
    }

    companion object {
        const val SCHEME_INTENT = "intent"
    }
}