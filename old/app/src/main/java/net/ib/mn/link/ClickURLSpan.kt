package net.ib.mn.link

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.style.ClickableSpan
import android.view.View

class ClickURLSpan(private val url: String) : ClickableSpan() {
    override fun onClick(view: View) {
        val context = view.context
        val uri = Uri.parse(url)

        try {
            val intent = Intent(context, AppLinkActivity::class.java).apply {
                data = uri
            }
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            e.printStackTrace()
        }
    }
}