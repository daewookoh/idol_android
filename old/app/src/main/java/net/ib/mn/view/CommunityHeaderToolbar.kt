package net.ib.mn.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.TypedValue
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import net.ib.mn.R
import java.lang.Exception
import kotlin.math.ceil
import kotlin.math.roundToInt


class CommunityHeaderToolbar : LinearLayoutCompat {
    var name: AppCompatTextView? = null
    var group: AppCompatTextView? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet)
            : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet, defStyleAttr: Int)
            : super(context!!, attrs, defStyleAttr)

    override fun onFinishInflate() {
        super.onFinishInflate()

        name = findViewById(R.id.tv_name)
        group = findViewById(R.id.tv_group)
    }

    fun bindTo(name: String) {
        this.name?.text = name
    }

    fun bindTo(name: String, group: String) {
        this.name?.text = name
        this.group?.text = group
    }

    fun setTextSize(size: Float) {
        name?.setTextSize(TypedValue.COMPLEX_UNIT_PX, size)
    }

    fun setTextColor(ratio: Float) {
        // color 변경 코드가 간헐적으로 에러를 뱉어냄.. 그래서 0일 때랑, 1일 때는 강제적으로 바꾸게 함
        when (ratio) {
            0f -> {
                name?.setTextColor(ContextCompat.getColor(context, R.color.gray1000))
                group?.setTextColor(ContextCompat.getColor(context, R.color.gray300))
            }
            1f -> {
                name?.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
                group?.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
            }
            else -> try {
                name?.setTextColor(Color.parseColor(getHexColorCode(0, ratio)))
                group?.setTextColor(Color.parseColor(getHexColorCode(170, ratio)))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getHexColorCode(initialColor: Int, ratio: Float): String {
        val hex = ceil(initialColor + (255 - initialColor) * ratio).roundToInt().toString(16)
        return "#${hex}${hex}${hex}"
    }
}