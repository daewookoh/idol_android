package net.ib.mn.view

import android.content.Context
import android.graphics.Rect
import android.text.Spannable
import android.text.SpannableString
import android.text.style.URLSpan
import android.text.util.Linkify
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatTextView
import net.ib.mn.link.ClickURLSpan

class NoScrollingTextView : AppCompatTextView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun scrollTo(x: Int, y: Int) {
        // do nothing
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        setLinkURL()
    }

    // 피드 화면 자기소개 게시글 눌렀을때 URL세팅되었던게 풀림.
    override fun setOnClickListener(l: OnClickListener?) {
        super.setOnClickListener(l)
        setLinkURL()
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        setLinkURL()
    }

    fun setLinkURL() {
        Linkify.addLinks(this, Linkify.WEB_URLS)

        val spannableString = SpannableString(this.text)
        try {
            val urls: Array<URLSpan> =
                spannableString.getSpans(0, spannableString.length, URLSpan::class.java)
            for (url in urls) {
                val start = spannableString.getSpanStart(url)
                val end = spannableString.getSpanEnd(url)
                spannableString.removeSpan(url)
                spannableString.setSpan(
                    ClickURLSpan(url.url),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                )
            }
        } catch (e: IndexOutOfBoundsException) {
            e.printStackTrace()
        }

        this.text = spannableString
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (visibility == VISIBLE) {
            setLinkURL()
        }
    }
}