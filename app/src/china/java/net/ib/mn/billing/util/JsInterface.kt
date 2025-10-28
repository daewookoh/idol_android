package net.ib.mn.billing.util

import android.webkit.JavascriptInterface

class JsInterface(var mWebStateListener: WebStateListener) {
    @JavascriptInterface
    fun closeWindow() {
        mWebStateListener.onCloseWindow()
    }
}
