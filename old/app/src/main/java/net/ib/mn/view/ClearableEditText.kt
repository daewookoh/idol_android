package net.ib.mn.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.TextUtils
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import net.ib.mn.R
import net.ib.mn.view.TextWatcherAdapter.TextWatcherListener

/**
 * Copyright 2016 Alex Yanchenko
 *
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * To clear icon can be changed via
 *
 *
 * <pre>
 * android:drawable(Right|Left)="@drawable/custom_icon"
</pre> *
 */
class ClearableEditText : AppCompatEditText, View.OnTouchListener, View.OnFocusChangeListener,
    TextWatcherListener {
    enum class Location(val idx: Int) {
        LEFT(0), RIGHT(2)
    }

    interface Listener {
        fun didClearText()
    }

    constructor(context: Context?) : super(context!!) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(
        context!!, attrs
    ) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : super(
        context!!, attrs, defStyle
    ) {
        init()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    /**
     * null disables the icon
     */
    fun setIconLocation(loc: Location?) {
        this.loc = loc
        initIcon()
    }

    override fun setOnTouchListener(l: OnTouchListener) {
        this.l = l
    }

    override fun setOnFocusChangeListener(f: OnFocusChangeListener) {
        this.f = f
    }

    private var loc: Location? = Location.RIGHT
    private var xD: Drawable? = null
    private var listener: Listener? = null
    private var l: OnTouchListener? = null
    private var f: OnFocusChangeListener? = null
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        if (displayedDrawable != null) {
            val x = event.x.toInt()
            val y = event.y.toInt()
            val left = if (loc == Location.LEFT) 0 else width - paddingRight - xD!!.intrinsicWidth
            val right = if (loc == Location.LEFT) paddingLeft + xD!!.intrinsicWidth else width
            val tappedX = x in left..right && y >= 0 && y <= bottom - top
            if (tappedX) {
                if (event.action == MotionEvent.ACTION_UP) {
                    setText("")
                    if (listener != null) {
                        listener!!.didClearText()
                    }
                }
                return true
            }
        }
        return if (l != null) {
            l!!.onTouch(v, event)
        } else false
    }

    override fun onFocusChange(v: View, hasFocus: Boolean) {
        if (hasFocus) {
            setClearIconVisible(!TextUtils.isEmpty(text))
        } else {
            setClearIconVisible(false)
        }
        if (f != null) {
            f!!.onFocusChange(v, hasFocus)
        }
    }

    override fun onTextChanged(view: EditText?, text: String?) {
        if (isFocused) {
            setClearIconVisible(!TextUtils.isEmpty(text))
        }
    }

    override fun setCompoundDrawables(
        left: Drawable?,
        top: Drawable?,
        right: Drawable?,
        bottom: Drawable?
    ) {
        super.setCompoundDrawables(left, top, right, bottom)
        initIcon()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        super.setOnTouchListener(this)
        super.setOnFocusChangeListener(this)
        addTextChangedListener(TextWatcherAdapter(this, this))
        initIcon()
        setClearIconVisible(false)
    }

    private fun initIcon() {
        xD = null
        if (loc != null) {
            xD = compoundDrawables[loc!!.idx]
        }
        if (xD == null) {
            xD = ResourcesCompat.getDrawable(resources, R.drawable.btn_del, null)
        }
        if (xD != null) {
            xD!!.setBounds(0, 0, xD!!.intrinsicWidth, xD!!.intrinsicHeight)
            val min = paddingTop + xD!!.intrinsicHeight + paddingBottom
            if (suggestedMinimumHeight < min) {
                minimumHeight = min
            }
        }
    }

    private val displayedDrawable: Drawable?
        get() = if (loc != null) compoundDrawables[loc!!.idx] else null

    private fun setClearIconVisible(visible: Boolean) {
        val cd = compoundDrawables
        val displayed = displayedDrawable
        val wasVisible = displayed != null
        if (visible != wasVisible) {
            val x = if (visible) xD else null
            super.setCompoundDrawables(
                if (loc == Location.LEFT) x else cd[0], cd[1],
                if (loc == Location.RIGHT) x else cd[2],
                cd[3]
            )
        }
    }

    // Custom view has setOnTouchListener called on it but does not override performClick
    // 경고 방지
    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
