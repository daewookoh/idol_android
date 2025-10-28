package net.ib.mn.view

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.widget.ImageView
import net.ib.mn.utils.Util


class TalkLinkImageView : ImageView {

    private val roundRect = RectF()
    private var rectRadius = 0.75f
    private val maskPaint = Paint()
    private val zonePaint = Paint()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        rectRadius = Util.convertDpToPixel(context, 0.75f)
        init()
    }

    constructor(context: Context) : super(context) {
        rectRadius = Util.convertDpToPixel(context, 0.75f)
        init()
    }

    private fun init() {
        maskPaint.isAntiAlias = true
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        zonePaint.isAntiAlias = true
        zonePaint.color = Color.WHITE
        val density = resources.displayMetrics.density
        rectRadius *= density
    }

    fun setRectRadius(radius: Float) {
        rectRadius = radius
        invalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val w = width.toFloat()
        val h = height.toFloat()
        roundRect.set(0f, 0f, w, h + rectRadius)
    }

    override fun draw(canvas: Canvas) {
        canvas.saveLayer(roundRect, zonePaint, Canvas.ALL_SAVE_FLAG)
        canvas.drawRoundRect(roundRect, rectRadius, rectRadius, zonePaint)
        canvas.saveLayer(roundRect, maskPaint, Canvas.ALL_SAVE_FLAG)
        super.draw(canvas)
        canvas.restore()
    }
}