package net.ib.mn.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.Scroller
import androidx.viewpager.widget.PagerAdapter
import androidx.viewpager.widget.ViewPager

class InfiniteViewPager : ViewPager {
    private var mScroller: FixedSpeedScroller? = null

    private var isRightSwipeAllowable = false //오른쪽 스와이프 가능여부
    private var initialXValue = 0f // 뷰페이저에 처음 터치할때  x좌표

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    private fun init() {
        try {
            isRightSwipeAllowable = true
            val viewpager: Class<*> = ViewPager::class.java
            val scroller = viewpager.getDeclaredField("mScroller")
            scroller.isAccessible = true
            mScroller = FixedSpeedScroller(
                context,
                DecelerateInterpolator()
            )
            scroller[this] = mScroller
        } catch (ignored: Exception) {
        }
    }

    fun setScrollDuration(duration: Int) {
        mScroller!!.setScrollDuration(duration)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        //오른쪽 스와이프가 막힌 경우 false return해서 motion을 막는다.

        if (this.isRightSwipeNotALLow(event)) {
            return false
        }
        //return되면  일반적인 진행
        return super.onTouchEvent(event)
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        //오른쪽 스와이프가 막힌 경우 false return해서 motion을 막는다.

        if (this.isRightSwipeNotALLow(event)) {
            return false
        }
        //return되면  일반적인 진행
        return super.onInterceptTouchEvent(event)
    }

    //오른쪽  swipe 안막혀있는지  여부를  체크한다. 막았으면  true를 return
    private fun isRightSwipeNotALLow(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) { //처음  눌렀을때
            initialXValue = event.x //뷰페이저를  처음 눌렀을때 x좌표를 inital 값으로 저장
            return false
        }

        if (event.action == MotionEvent.ACTION_MOVE) { //누르고 움직였을때 (뷰페이져  밀었을때 와 동일)

            //현재  event의  x좌표에서  inital x좌표 뺏을때 -가 나오면  오른쪽으로 가고 있다는 뜻
            //오른쪽으로 가면서  오른쪽 swipe가능 여부가  false일때  ->  true를 리턴해서  오른쪽 swipe를 막는다.

            val diff = event.x - initialXValue
            return diff < 0 && !isRightSwipeAllowable
        }
        return false
    }

    //오른쪽 스와이프  가능 여부를 set
    fun setRightSwipeAllow(allowable: Boolean) {
        this.isRightSwipeAllowable = allowable
    }

    private inner class FixedSpeedScroller : Scroller {
        private var mDuration = 300

        constructor(context: Context?) : super(context)

        constructor(context: Context?, interpolator: Interpolator?) : super(context, interpolator)

        constructor(context: Context?, interpolator: Interpolator?, flywheel: Boolean) : super(
            context,
            interpolator,
            flywheel
        )

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int, duration: Int) {
            super.startScroll(startX, startY, dx, dy, mDuration)
        }

        override fun startScroll(startX: Int, startY: Int, dx: Int, dy: Int) {
            super.startScroll(startX, startY, dx, dy, mDuration)
        }

        fun setScrollDuration(duration: Int) {
            mDuration = duration
        }
    }

    override fun setAdapter(adapter: PagerAdapter?) {
        super.setAdapter(adapter)
        // offset first element so that we can scroll to the left
        currentItem = 0
    }

    override fun setCurrentItem(item: Int) {
        // offset the current item to ensure there is space to scroll
        setCurrentItem(item, false)
    }

    override fun setCurrentItem(item: Int, smoothScroll: Boolean) {
        if (adapter!!.count == 0) {
            super.setCurrentItem(item, smoothScroll)
            return
        }
        super.setCurrentItem(item, smoothScroll)
    }

    override fun getCurrentItem(): Int {
        if (adapter!!.count == 0) {
            return super.getCurrentItem()
        }
        val position = super.getCurrentItem()
        if (adapter is InfinitePagerAdapter) {
            val infAdapter = adapter as InfinitePagerAdapter?
            // Return the actual item position in the data backing InfinitePagerAdapter
            return (position % infAdapter!!.realCount)
        } else {
            return super.getCurrentItem()
        }
    }

    val offsetAmount: Int
        get() {
            if (adapter!!.count == 0) {
                return 0
            }
            if (adapter is InfinitePagerAdapter) {
                val infAdapter = adapter as InfinitePagerAdapter?
                return infAdapter!!.realCount * 100
            } else {
                return 0
            }
        }
}
