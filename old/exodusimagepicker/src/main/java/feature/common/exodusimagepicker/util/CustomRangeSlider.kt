package feature.common.exodusimagepicker.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import feature.common.exodusimagepicker.R
import kotlin.math.abs
import androidx.core.graphics.drawable.toDrawable

class CustomRangeSlider @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) :
    ViewGroup(context, attrs, defStyleAttr) {
    private val mLinePaint: Paint
    private val mBgPaint: Paint
    private val mLeftThumb: CustomRangeSliderThumbView
    private val mRightThumb: CustomRangeSliderThumbView
    private val mTouchSlop: Int
    private var mOriginalX = 0
    private var mLastX = 0
    private var mThumbWidth: Int
    private val mTickStart = 0
    private var mTickEnd = 5
    private val mTickInterval = 1
    private var mTickCount: Int
    private var mLineSize: Float
    private var mIsDragging = false
    private var mRangeChangeListener: OnRangeChangeListener? = null
    private var maxDifference = 0

    //중앙이  눌렸는지 체크한다. -> true면  중앙이 눌린것이므로 ,  이동처리
    private var isRangeSliderMiddleTouched = false


    //최대 range 차이를  지정해준다.
    fun setMaxDifference(maxDifference: Int) {
        this.maxDifference = maxDifference
    }

    init {
        this.mTickCount = (this.mTickEnd - this.mTickStart) / this.mTickInterval
        val array = context.obtainStyledAttributes(attrs, R.styleable.RangeSlider, 0, 0)
        this.mThumbWidth = array.getDimensionPixelOffset(R.styleable.RangeSlider_thumbWidth, 7)
        this.mLineSize =
            array.getDimensionPixelOffset(R.styleable.RangeSlider_lineHeight, 1).toFloat()
        this.mBgPaint = Paint()
        mBgPaint.color = array.getColor(
            R.styleable.RangeSlider_maskColor,
            0xA0000000.toInt()
        )
        this.mLinePaint = Paint()
        mLinePaint.color = array.getColor(
            R.styleable.RangeSlider_lineColor,
            0xFF000000.toInt()
        )
        this.mTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        val lDrawable = array.getDrawable(R.styleable.RangeSlider_leftThumbDrawable)
        val rDrawable = array.getDrawable(R.styleable.RangeSlider_rightThumbDrawable)
        this.mLeftThumb = CustomRangeSliderThumbView(
            context, this.mThumbWidth, (lDrawable
                ?: 0xFF000000.toInt().toDrawable())
        )
        this.mRightThumb = CustomRangeSliderThumbView(
            context, this.mThumbWidth, (rDrawable
                ?: 0xFF000000.toInt().toDrawable())
        )
        this.setTickCount(array.getInteger(R.styleable.RangeSlider_tickCount, 5))
        this.setRangeIndex(
            array.getInteger(R.styleable.RangeSlider_leftThumbIndex, 0), array.getInteger(
                R.styleable.RangeSlider_rightThumbIndex,
                this.mTickCount
            )
        )
        array.recycle()
        this.addView(this.mLeftThumb)
        this.addView(this.mRightThumb)
        this.setWillNotDraw(false)
    }

    fun setThumbWidth(thumbWidth: Int) {
        this.mThumbWidth = thumbWidth
        mLeftThumb.setThumbWidth(thumbWidth)
        mRightThumb.setThumbWidth(thumbWidth)
    }

    fun setLeftThumbDrawable(drawable: Drawable) {
        mLeftThumb.setThumbDrawable(drawable)
    }

    fun setRightThumbDrawable(drawable: Drawable) {
        mRightThumb.setThumbDrawable(drawable)
    }

    fun setLineColor(color: Int) {
        mLinePaint.color = color
    }

    fun setLineSize(lineSize: Float) {
        this.mLineSize = lineSize
    }

    fun setMaskColor(color: Int) {
        mBgPaint.color = color
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var widthMeasureSpec = widthMeasureSpec
        widthMeasureSpec =
            MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        mLeftThumb.measure(widthMeasureSpec, heightMeasureSpec)
        mRightThumb.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val lThumbWidth = mLeftThumb.measuredWidth
        val lThumbHeight = mLeftThumb.measuredHeight
        mLeftThumb.layout(0, 0, lThumbWidth, lThumbHeight)
        mRightThumb.layout(0, 0, lThumbWidth, lThumbHeight)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        this.moveThumbByIndex(this.mLeftThumb, mLeftThumb.rangeIndex)
        this.moveThumbByIndex(
            this.mRightThumb,
            mRightThumb.rangeIndex
        )
    }

    override fun onDraw(canvas: Canvas) {
        val width = this.measuredWidth
        val height = this.measuredHeight
        val lThumbWidth = mLeftThumb.measuredWidth
        val lThumbOffset = mLeftThumb.x
        val rThumbOffset = mRightThumb.x
        val lineTop = this.mLineSize
        val lineBottom = height.toFloat() - this.mLineSize
        canvas.drawRect(
            lThumbWidth.toFloat() + lThumbOffset,
            0.0f,
            rThumbOffset,
            lineTop,
            this.mLinePaint
        )
        canvas.drawRect(
            lThumbWidth.toFloat() + lThumbOffset, lineBottom, rThumbOffset, height.toFloat(),
            this.mLinePaint
        )
        if (lThumbOffset > mThumbWidth.toFloat()) {
            canvas.drawRect(
                mThumbWidth.toFloat(), 0.0f, lThumbOffset + mThumbWidth.toFloat(), height.toFloat(),
                this.mBgPaint
            )
        }

        if (rThumbOffset < (width - this.mThumbWidth).toFloat()) {
            canvas.drawRect(rThumbOffset, 0.0f, width.toFloat(), height.toFloat(), this.mBgPaint)
        }
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!this.isEnabled) {
            return false
        } else {
            var handle = false
            val x: Int
            when (event.action) {
                0 -> {
                    x = event.x.toInt()
                    val y = event.y.toInt()

                    //오른쪽 thumb x 좌표와  left thumb 왼쪽 좌표 사이 클릭을 체크
                    if (((mRightThumb.x - 10) > x) && (x > (mLeftThumb.x + 10))) {
                        isRangeSliderMiddleTouched = true
                        this.mOriginalX = x
                        this.mLastX = this.mOriginalX
                        this.mIsDragging = true

                        //양옆 thumb 가운데를 누르면  양옆 thumb 가 눌린 상태로 체크되어 같이 이동하게 해준다.
                        mLeftThumb.isPressed = true
                        mRightThumb.isPressed = true

                        handle = true
                    } else {
                        isRangeSliderMiddleTouched = false
                        mLeftThumb.isPressed = false
                        mRightThumb.isPressed = false

                        this.mOriginalX = x
                        this.mLastX = this.mOriginalX
                        this.mIsDragging = false

                        if (!mLeftThumb.isPressed && mLeftThumb.inInTarget(x, y)) {
                            mLeftThumb.isPressed = true
                            handle = true
                        } else if (!mRightThumb.isPressed && mRightThumb.inInTarget(x, y)) {
                            mRightThumb.isPressed = true
                            handle = true
                        }
                    }
                }

                1, 3 -> {
                    this.mIsDragging = false
                    run {
                        this.mLastX = 0
                        this.mOriginalX = this.mLastX
                    }
                    this.parent.requestDisallowInterceptTouchEvent(false)
                    if (mLeftThumb.isPressed) {
                        this.releaseLeftThumb()
                        this.invalidate()
                        handle = true
                    } else if (mRightThumb.isPressed) {
                        this.releaseRightThumb()
                        this.invalidate()
                        handle = true
                    }
                }

                2 -> {
                    x = event.x.toInt()
                    if (!this.mIsDragging && abs((x - this.mOriginalX).toDouble()) > this.mTouchSlop) {
                        this.mIsDragging = true
                    }

                    if (this.mIsDragging) {
                        val moveX = x - this.mLastX
                        if (mLeftThumb.isPressed) {
                            if (isRangeSliderMiddleTouched) {
                                this.parent.requestDisallowInterceptTouchEvent(true)
                                this.moveRightThumbByPixel(moveX)
                                this.moveLeftThumbByPixel(moveX)
                                handle = true
                                this.invalidate()
                            } else {
                                //왼쪽 thumb는  movex가 0또는  양수일때   오른쪽으로  이동한다.
                                //이렇게 오른쪽으로 이동은  difference를 줄이는 행위로 간주하여  thumb 이동 가능
                                //왼쪽으로 이동하여  difference를 늘리는 행위를 할때는 -> maxdifference를 넘었는지 체크되어,  thumb 이동을 막는다.
                                //아래  오른쪽 thumb도 같은 로직
                                if ((maxDifference >= (mRightThumb.rangeIndex - mLeftThumb.rangeIndex)) || moveX >= 0) {
                                    this.parent.requestDisallowInterceptTouchEvent(true)
                                    this.moveLeftThumbByPixel(moveX)
                                    handle = true
                                    this.invalidate()
                                }
                            }
                        } else if (mRightThumb.isPressed) {
                            if (isRangeSliderMiddleTouched) {
                                this.parent.requestDisallowInterceptTouchEvent(true)
                                this.moveRightThumbByPixel(moveX)
                                this.moveLeftThumbByPixel(moveX)
                                handle = true
                                this.invalidate()
                            } else {
                                if ((maxDifference >= (mRightThumb.rangeIndex - mLeftThumb.rangeIndex)) || moveX <= 0) {
                                    this.parent.requestDisallowInterceptTouchEvent(true)
                                    this.moveRightThumbByPixel(moveX)
                                    handle = true
                                    this.invalidate()
                                }
                            }
                        }
                    }

                    this.mLastX = x
                }
            }

            return handle
        }
    }

    private fun isValidTickCount(tickCount: Int): Boolean {
        return tickCount > 1
    }

    private fun indexOutOfRange(leftThumbIndex: Int, rightThumbIndex: Int): Boolean {
        return leftThumbIndex < 0 || leftThumbIndex > this.mTickCount || rightThumbIndex < 0 || rightThumbIndex > this.mTickCount
    }

    private val rangeLength: Float
        get() {
            val width = this.measuredWidth
            return if (width < this.mThumbWidth) 0.0f else (width - this.mThumbWidth).toFloat()
        }

    private val intervalLength: Float
        get() = this.rangeLength / mTickCount.toFloat()

    fun getNearestIndex(x: Float): Int {
        return Math.round(x / this.intervalLength)
    }

    val leftIndex: Int
        get() = mLeftThumb.rangeIndex

    val rightIndex: Int
        get() = mRightThumb.rangeIndex

    private fun notifyRangeChange() {
        if (this.mRangeChangeListener != null) {
            mRangeChangeListener!!.onRangeChange(
                this,
                mLeftThumb.rangeIndex, mRightThumb.rangeIndex
            )
        }
    }

    fun setRangeChangeListener(rangeChangeListener: OnRangeChangeListener) {
        this.mRangeChangeListener = rangeChangeListener
    }

    fun setTickCount(count: Int) {
        val tickCount = (count - this.mTickStart) / this.mTickInterval
        if (this.isValidTickCount(tickCount)) {
            this.mTickEnd = count
            this.mTickCount = tickCount
            mRightThumb.setTickIndex(this.mTickCount)
        } else {
            throw IllegalArgumentException("tickCount less than 2; invalid tickCount.")
        }
    }

    fun setRangeIndex(leftIndex: Int, rightIndex: Int) {
        require(
            !this.indexOutOfRange(
                leftIndex,
                rightIndex
            )
        ) { "Thumb index left " + leftIndex + ", or right " + rightIndex + " is out of bounds. Check that it is greater than the minimum (" + this.mTickStart + ") and less than the maximum value (" + this.mTickEnd + ")" }
        if (mLeftThumb.rangeIndex != leftIndex) {
            mLeftThumb.setTickIndex(leftIndex)
        }

        if (mRightThumb.rangeIndex != rightIndex) {
            mRightThumb.setTickIndex(rightIndex)
        }
    }

    private fun moveThumbByIndex(view: CustomRangeSliderThumbView, index: Int): Boolean {
        view.x = index.toFloat() * this.intervalLength
        if (view.rangeIndex != index) {
            view.setTickIndex(index)
            return true
        } else {
            return false
        }
    }

    private fun moveLeftThumbByPixel(pixel: Int) {
        val x = mLeftThumb.x + pixel.toFloat()
        val interval = this.intervalLength
        val start = (this.mTickStart / this.mTickInterval).toFloat() * interval
        val end = (this.mTickEnd / this.mTickInterval).toFloat() * interval
        if (x > start && x < end && x < mRightThumb.x - mThumbWidth.toFloat()) {
            mLeftThumb.x = x
            val index = this.getNearestIndex(x)
            if (mLeftThumb.rangeIndex != index) {
                mLeftThumb.setTickIndex(index)
                this.notifyRangeChange()
            }
        }
    }

    private fun moveRightThumbByPixel(pixel: Int) {
        val x = mRightThumb.x + pixel.toFloat()
        val interval = this.intervalLength
        val start = (this.mTickStart / this.mTickInterval).toFloat() * interval
        val end = (this.mTickEnd / this.mTickInterval).toFloat() * interval
        if (x > start && x < end && x > mLeftThumb.x + mThumbWidth.toFloat()) {
            mRightThumb.x = x
            val index = this.getNearestIndex(x)
            if (mRightThumb.rangeIndex != index) {
                mRightThumb.setTickIndex(index)
                this.notifyRangeChange()
            }
        }
    }

    private fun releaseLeftThumb() {
        var index = this.getNearestIndex(mLeftThumb.x)
        val endIndex = mRightThumb.rangeIndex
        if (index >= endIndex) {
            index = endIndex - 1
        }

        if (this.moveThumbByIndex(this.mLeftThumb, index)) {
            this.notifyRangeChange()
        }

        mLeftThumb.isPressed = false
    }

    private fun releaseRightThumb() {
        var index = this.getNearestIndex(mRightThumb.x)
        val endIndex = mLeftThumb.rangeIndex
        if (index <= endIndex) {
            index = endIndex + 1
        }

        if (this.moveThumbByIndex(this.mRightThumb, index)) {
            this.notifyRangeChange()
        }

        mRightThumb.isPressed = false
    }

    interface OnRangeChangeListener {
        fun onRangeChange(var1: CustomRangeSlider, var2: Int, var3: Int)
    }
}
