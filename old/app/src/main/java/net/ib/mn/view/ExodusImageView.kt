package net.ib.mn.view

import android.content.Context
import android.util.AttributeSet
import android.util.SparseArray
import androidx.appcompat.widget.AppCompatImageView
import net.ib.mn.R


class ExodusImageView : AppCompatImageView {

    enum class FixedAlong {
        width, height
    }

    var loadInfo: Any? = null

    private var mKeyedLoadInfo: SparseArray<Any>? = null
    private var fixedAlong = FixedAlong.width

    constructor(context: Context) : super(context)

    constructor(context: Context,
                attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context,
                attrs: AttributeSet?,
                defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    fun getLoadInfo(key: Int): Any? {
        return if (mKeyedLoadInfo != null) mKeyedLoadInfo!!.get(key) else null
    }

    fun setLoadInfo(key: Int, loadInfo: Any) {
        if (key.ushr(24) < 2) {
            throw IllegalArgumentException("The key must be an application-specific " + "resource id.")
        }

        setKeyedLoadInfo(key, loadInfo)
    }

    private fun setKeyedLoadInfo(key: Int, loadInfo: Any) {
        if (mKeyedLoadInfo == null) {
            mKeyedLoadInfo = SparseArray(2)
        }

        mKeyedLoadInfo!!.put(key, loadInfo)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val h = this.measuredHeight
        val w = this.measuredWidth
        val curSquareDim = if (fixedAlong == FixedAlong.width) w else h

        setMeasuredDimension(curSquareDim, curSquareDim)
    }
}
