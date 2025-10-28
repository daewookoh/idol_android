package feature.common.exodusimagepicker.util

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.View

@SuppressLint("ViewConstructor")
internal class CustomRangeSliderThumbView(
    context: Context,
    private var mThumbWidth: Int,
    private var mThumbDrawable: Drawable
) :
    View(context) {
    private val mExtendTouchSlop = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        15.0f,
        context.resources.displayMetrics
    ).toInt()
    private var mPressed = false
    var rangeIndex: Int = 0
        private set

    init {
        this.setBackgroundDrawable(this.mThumbDrawable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(
            MeasureSpec.makeMeasureSpec(this.mThumbWidth, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(heightMeasureSpec), MeasureSpec.EXACTLY)
        )
        mThumbDrawable.setBounds(0, 0, this.mThumbWidth, this.measuredHeight)
    }

    fun setThumbWidth(thumbWidth: Int) {
        this.mThumbWidth = thumbWidth
    }

    fun setThumbDrawable(thumbDrawable: Drawable) {
        this.mThumbDrawable = thumbDrawable
    }

    fun inInTarget(x: Int, y: Int): Boolean {
        val rect = Rect()
        this.getHitRect(rect)
        rect.left -= this.mExtendTouchSlop
        rect.right += this.mExtendTouchSlop
        rect.top -= this.mExtendTouchSlop
        rect.bottom += this.mExtendTouchSlop
        return rect.contains(x, y)
    }

    fun setTickIndex(tickIndex: Int) {
        this.rangeIndex = tickIndex
    }

    override fun isPressed(): Boolean {
        return this.mPressed
    }

    override fun setPressed(pressed: Boolean) {
        this.mPressed = pressed
    }

    companion object {
        private const val EXTEND_TOUCH_SLOP = 15
    }
}
