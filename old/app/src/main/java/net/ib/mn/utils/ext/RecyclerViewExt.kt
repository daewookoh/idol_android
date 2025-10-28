package net.ib.mn.utils.ext

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.utils.Util
import kotlin.math.abs

@SuppressLint("ClickableViewAccessibility")
fun RecyclerView.preventUnwantedHorizontalScroll() {
    // 막다른 곳에서 스크롤할 때 삐딱하게 스크롤하면 좌우로 넘어감 방지
    this.setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                v.parent.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
        }
        false
    }
}

@SuppressLint("ClickableViewAccessibility")
fun RecyclerView.preventParentHorizontalScrollInViewPager2(swipeRefresh: ViewGroup) {
    var initialX = 0f
    var initialY = 0f
    var gestureDecided = false

    this.setOnTouchListener { view, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                initialX = event.x
                initialY = event.y
                gestureDecided = false
                view.parent.requestDisallowInterceptTouchEvent(false)
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - initialX
                val dy = event.y - initialY

                if (!gestureDecided) {
                    if (abs(dy) > abs(dx)) {
                        // 세로 스크롤로 판단. swipeRefresh와 viewpager2가 가로스크롤을 못하게 함
                        view.parent.requestDisallowInterceptTouchEvent(true)
                        swipeRefresh.parent.requestDisallowInterceptTouchEvent(true)
                    } else {
                        // 가로 스크롤로 판단
                        view.parent.requestDisallowInterceptTouchEvent(false)
                    }
                    gestureDecided = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                view.parent.requestDisallowInterceptTouchEvent(false)
                swipeRefresh.parent.requestDisallowInterceptTouchEvent(false)
                gestureDecided = false
            }
        }
        false // 리사이클러뷰 자체 터치는 계속 처리하게 함
    }
}