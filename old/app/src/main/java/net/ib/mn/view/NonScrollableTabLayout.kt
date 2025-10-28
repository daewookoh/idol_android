package net.ib.mn.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import com.google.android.material.tabs.TabLayout

class NonScrollableTabLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : TabLayout(context, attrs, defStyleAttr) {
    override fun onTouchEvent(ev: MotionEvent) =
        if (ev.actionMasked == MotionEvent.ACTION_MOVE) true else super.onTouchEvent(ev)
    override fun onInterceptTouchEvent(ev: MotionEvent) =
        if (ev.actionMasked == MotionEvent.ACTION_MOVE) false else super.onInterceptTouchEvent(ev)
    override fun canScrollHorizontally(direction: Int) = false
}