package net.ib.mn.view

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.View.MeasureSpec.getMode
import android.view.ViewGroup
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.os.Build.VERSION_CODES.LOLLIPOP
import android.widget.FrameLayout


class FullDrawerLayout : DrawerLayout {

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = getMode(widthMeasureSpec)
        val heightMode = getMode(heightMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode != MeasureSpec.EXACTLY || heightMode != MeasureSpec.EXACTLY) {
            throw IllegalArgumentException(
                    "DrawerLayout must be measured with MeasureSpec.EXACTLY.")
        }

        setMeasuredDimension(widthSize, heightSize)

        // for support Android 5+
        if (Build.VERSION.SDK_INT >= LOLLIPOP) {
            val params = layoutParams as FrameLayout.LayoutParams
            params.topMargin = getStatusBarHeight()
            layoutParams = params
        }

        // Gravity value for each drawer we've seen. Only one of each permitted.
        val foundDrawers = 0
        val childCount = childCount
        for (i in 0 until childCount) {
            val child = getChildAt(i)

            if (child.visibility == View.GONE) {
                continue
            }

            val lp = child.layoutParams as LayoutParams

            when {
                isContentView(child) -> {
                    // Content views get measured at exactly the layout's size.
                    val contentWidthSpec = MeasureSpec.makeMeasureSpec(
                            widthSize - lp.leftMargin - lp.rightMargin, MeasureSpec.EXACTLY)
                    val contentHeightSpec = MeasureSpec.makeMeasureSpec(
                            heightSize - lp.topMargin - lp.bottomMargin, MeasureSpec.EXACTLY)
                    child.measure(contentWidthSpec, contentHeightSpec)
                }
                isDrawerView(child) -> {
                    val childGravity = getDrawerViewGravity(child) and Gravity.HORIZONTAL_GRAVITY_MASK
                    if (foundDrawers and childGravity != 0) {
                        throw IllegalStateException("Child drawer has absolute gravity " +
                                gravityToString(childGravity) + " but this already has a " +
                                "drawer view along that edge")
                    }
                    val drawerWidthSpec = ViewGroup.getChildMeasureSpec(widthMeasureSpec,
                            MIN_DRAWER_MARGIN + lp.leftMargin + lp.rightMargin,
                            lp.width)
                    val drawerHeightSpec = ViewGroup.getChildMeasureSpec(heightMeasureSpec,
                            lp.topMargin + lp.bottomMargin,
                            lp.height)
                    child.measure(drawerWidthSpec, drawerHeightSpec)
                }
                else -> throw IllegalStateException("Child " + child + " at index " + i +
                        " does not have a valid layout_gravity - must be Gravity.LEFT, " +
                        "Gravity.RIGHT or Gravity.NO_GRAVITY")
            }
        }
    }

    private fun isContentView(child: View): Boolean {
        return (child.layoutParams as LayoutParams).gravity == Gravity.NO_GRAVITY
    }

    private fun isDrawerView(child: View): Boolean {
        val gravity = (child.layoutParams as LayoutParams).gravity
        val absGravity = Gravity.getAbsoluteGravity(gravity,
                child.layoutDirection)
        return absGravity and (Gravity.START or Gravity.END) != 0
    }

    private fun getDrawerViewGravity(drawerView: View): Int {
        val gravity = (drawerView.layoutParams as LayoutParams).gravity
        return GravityCompat.getAbsoluteGravity(gravity, ViewCompat.getLayoutDirection(this))
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        var result = 0

        if (resourceId > 0) {
            result = resources.getDimensionPixelOffset(resourceId)
        }

        return result
    }

    companion object {

        private const val MIN_DRAWER_MARGIN = 0 // dp

        internal fun gravityToString(gravity: Int): String {
            if (gravity and Gravity.START == Gravity.START) {
                return "START"
            }
            return if (gravity and Gravity.END == Gravity.END) {
                "END"
            } else Integer.toHexString(gravity)
        }
    }

}