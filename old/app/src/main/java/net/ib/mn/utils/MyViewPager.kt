package net.ib.mn.utils

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.viewpager.widget.ViewPager

// DrawerLayout 위에 ViewPager를 얹고 viewPager 스크롤시 DrawerLayout이 스크롤 이벤트를 처리하지 않도록 한다
class MyViewPager : ViewPager {
    // click 이벤트가 먹혀서 시뮬레이션 해준다
    private val CLICK_ACTION_THRESHOLD = 200
    private var startX = 0f
    private var startY = 0f

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        super.onTouchEvent(event)
        try {
            if (event.action == MotionEvent.ACTION_DOWN) {
                //Request parent to do not intercept touch event.
                requestDisallowInterceptTouchEvent(true)
                startX = event.x
                startY = event.y
                return true
            }
            if (event.action == MotionEvent.ACTION_UP) {
                val endX = event.x
                val endY = event.y
                if (isAClick(startX, endX, startY, endY)) {
                    performClick()
                }
            }
            if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL || event.action == MotionEvent.ACTION_OUTSIDE) {
                //Request parent to treat touch event.
                requestDisallowInterceptTouchEvent(false)
                return true
            }
        } catch (e: Throwable) {
        }
        return true
    }

    private fun isAClick(startX: Float, endX: Float, startY: Float, endY: Float): Boolean {
        val differenceX = Math.abs(startX - endX)
        val differenceY = Math.abs(startY - endY)
        return !(differenceX > CLICK_ACTION_THRESHOLD /* =5 */ || differenceY > CLICK_ACTION_THRESHOLD)
    }
}
