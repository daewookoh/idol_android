package net.ib.mn.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout


class VoteBarLayout : ConstraintLayout {

    private var mPercent = 100
    private var isApply = false
    private var isLastPlace = false

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle)

    fun interface OnLayoutCompleteListener {
        fun onLayoutCompleted(percentWidth : Float)
    }

    private var onLayoutCompleteListener: OnLayoutCompleteListener? = null

    fun setOnLayoutCompleteListener(listener: OnLayoutCompleteListener) {
        onLayoutCompleteListener = listener
    }

    fun setWidthRatio(percent: Int , isApply : Boolean) {
        this.isApply = isApply
        if (!isApply) {
            return
        }
        mPercent = percent
    }

    fun updateIsLastPlace(isLastPlace: Boolean) {
        this.isLastPlace = isLastPlace
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!isApply) { // 프로그래스바 resize를 시켜주고 싶지 않다면 기존 사이즈대로.
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        var parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)

        parentWidth = parentWidth * mPercent / 100
        this.setMeasuredDimension(parentWidth, parentHeight)
        super.onMeasure(MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY), heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        // 마지막 등수의 아이템만 계산하면 되서 넣어줌.
        if (!isLastPlace) {
            return
        }

        val parentView = parent as? View
        val parentWidth = parentView?.width ?: 0
        val percentWidth = (width.toFloat() / parentWidth.toFloat()) * 100

        onLayoutCompleteListener?.onLayoutCompleted(percentWidth)
    }

}
