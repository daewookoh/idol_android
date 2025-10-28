package net.ib.mn.view

import android.content.Context
import android.util.AttributeSet
import android.widget.RelativeLayout
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util

class ProgressBarLayout : RelativeLayout {
    private var mPercent = 100

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?) : super(context)

    fun setWidthRatio(percent: Int) {
        mPercent = percent
        requestLayout() // 이걸 명시적으로 해줘야 바 길이가 이상하게 나오는 현상이 방지됨
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var parentWidth = MeasureSpec.getSize(widthMeasureSpec)
        val parentHeight = MeasureSpec.getSize(heightMeasureSpec)
        parentWidth = parentWidth * mPercent / 100

        setMeasuredDimension(parentWidth, parentHeight)
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(parentWidth, MeasureSpec.EXACTLY),
            heightMeasureSpec
        )
    }
}
