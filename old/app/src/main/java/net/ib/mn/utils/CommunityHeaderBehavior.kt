package net.ib.mn.utils

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.appbar.AppBarLayout
import net.ib.mn.R
import net.ib.mn.view.CommunityHeaderToolbar
import kotlin.math.abs


class CommunityHeaderBehavior: CoordinatorLayout.Behavior<CommunityHeaderToolbar> {

    private var mContext: Context

    private var mStartMarginLeft: Int = 0
    private var mEndMarginLeft: Int = 0
    private var mMarginRight: Int = 0
    private var mStartMarginBottom: Int = 0
    private var mTitleStartSize: Float = 0.toFloat()
    private var mTitleEndSize: Float = 0.toFloat()
    private var isHide: Boolean = false

    constructor(context: Context) : super() {
        mContext = context
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mContext = context
    }

    constructor(context: Context, attrs: AttributeSet, mContext: Context) : super(context, attrs) {
        this.mContext = mContext
    }

    override fun layoutDependsOn(parent: CoordinatorLayout, child: CommunityHeaderToolbar, dependency: View): Boolean {
        return dependency is AppBarLayout
    }

    override fun onDependentViewChanged(parent: CoordinatorLayout, child: CommunityHeaderToolbar, dependency: View): Boolean {
        shouldInitProperties()

        val tabHeight = Util.convertDpToPixel(mContext, 48f)
        val maxScroll = (dependency as AppBarLayout).totalScrollRange
        val percentage = abs(dependency.y) / maxScroll.toFloat()
        var childPosition = (dependency.height
                + dependency.y
                - child.height
                - (tabHeight * percentage)
                - (getToolbarHeight(mContext) - child.height) * percentage / 2)

        childPosition -= mStartMarginBottom * (1f - percentage)

        val lp = child.layoutParams as CoordinatorLayout.LayoutParams
        if (abs(dependency.y) >= maxScroll / 2) {
            val layoutPercentage = (abs(dependency.y) - maxScroll / 2) / abs(maxScroll / 2)
            lp.leftMargin = if (mEndMarginLeft < mStartMarginLeft) {
                (mStartMarginLeft - (layoutPercentage * (mStartMarginLeft - mEndMarginLeft))).toInt()
            } else {
                mStartMarginLeft + (layoutPercentage * mEndMarginLeft).toInt()
            }
            child.setTextSize(getTranslationOffset(mTitleStartSize, mTitleEndSize, layoutPercentage))
            child.setTextColor(layoutPercentage)
        } else {
            lp.leftMargin = mStartMarginLeft
            child.setTextColor(0f)
        }
        lp.rightMargin = mMarginRight
        child.layoutParams = lp
        child.y = childPosition

        if (isHide && percentage < 1) {
            child.visibility = View.VISIBLE
            isHide = false
        } else if (!isHide && percentage == 1f) {
            child.visibility = View.GONE
            isHide = true
        }
        return true
    }

    private fun getToolbarHeight(context: Context): Int {
        var result = 0
        val tv = TypedValue()
        if (context.theme.resolveAttribute(android.R.attr.actionBarSize, tv, true)) {
            result = TypedValue.complexToDimensionPixelSize(tv.data, context.resources.displayMetrics)
        }
        return result
    }

    private fun getTranslationOffset(expandedOffset: Float, collapsedOffset: Float, ratio: Float): Float {
        return expandedOffset + ratio * (collapsedOffset - expandedOffset)
    }

    private fun shouldInitProperties() {
        if (mStartMarginLeft == 0) {
            mStartMarginLeft = mContext.resources.getDimensionPixelOffset(R.dimen.header_view_start_margin_left)
        }

        if (mEndMarginLeft == 0) {
            mEndMarginLeft = getToolbarHeight(mContext)
        }

        if (mStartMarginBottom == 0) {
            mStartMarginBottom = mContext.resources.getDimensionPixelOffset(R.dimen.header_view_start_margin_bottom)
        }

        if (mMarginRight == 0) {
            mMarginRight = mContext.resources.getDimensionPixelOffset(R.dimen.header_view_end_margin_right)
        }

        if (mTitleStartSize == 0f) {
            mTitleEndSize = mContext.resources.getDimensionPixelSize(R.dimen.header_view_end_text_size).toFloat()
        }

        if (mTitleStartSize == 0f) {
            mTitleStartSize = mContext.resources.getDimensionPixelSize(R.dimen.header_view_start_text_size).toFloat()
        }
    }
}