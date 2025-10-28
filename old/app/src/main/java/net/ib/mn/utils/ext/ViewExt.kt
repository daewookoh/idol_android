package net.ib.mn.utils.ext

import androidx.core.graphics.Insets
import android.os.SystemClock
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.doOnAttach

fun View.setOnSingleClickListener(interval: Long = 600L, onSingleClick: (View) -> Unit) {
    var lastClickTime = 0L

    setOnClickListener {
        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastClickTime >= interval) {
            lastClickTime = currentTime
            onSingleClick(it)
        }
    }
}

fun View.setVisibilityIfNeeded(newVisibility: Int) {
    if (visibility != newVisibility) visibility = newVisibility
}

// TODO Activity 생성 시 꼭 추가 해야함
fun View.applySystemBarInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.setPadding(
            0,
            systemBarsInsets.top,
            0,
            systemBarsInsets.bottom
        )
        insets
    }
}

fun View.applySystemBarInsets(includeIme: Boolean = false) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val sys = insets.getInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars())

        // Insets.NONE 대신 직접 생성 (minSdk 26 호환)
        val ime = if (includeIme) {
            insets.getInsets(WindowInsetsCompat.Type.ime())
        } else {
            Insets.of(0, 0, 0, 0)
        }

        val bottomInset = maxOf(sys.bottom, ime.bottom)

        v.setPadding(
            initialLeft + sys.left,
            initialTop + sys.top,
            initialRight + sys.right,
            initialBottom + bottomInset
        )
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applySystemBarInsetsAndRequest() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
        v.setPadding(0, bars.top, 0, bars.bottom)
        insets
    }
    // attach 직후 한 프레임 뒤 요청하면 더 안전
    if (isAttachedToWindow) {
        ViewCompat.requestApplyInsets(this)
    } else {
        doOnAttach { ViewCompat.requestApplyInsets(it) }
    }
}

fun View.applySystemBarAndActionBarInsets(activity: AppCompatActivity) {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

        // ActionBar height
        val ta = activity.theme.obtainStyledAttributes(intArrayOf(androidx.appcompat.R.attr.actionBarSize))
        val abHeight = ta.getDimensionPixelSize(0, 0)
        ta.recycle()

        val showActionBar = activity.supportActionBar?.isShowing == true
        val topPadding = bars.top + if (showActionBar) abHeight else 0

        v.setPadding(0, topPadding, 0, bars.bottom)
        insets
    }
    if (isAttachedToWindow) ViewCompat.requestApplyInsets(this) else doOnAttach { ViewCompat.requestApplyInsets(it) }
}

fun View.applyNavigationBarInset() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val navBar = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        view.setPadding(
            view.paddingLeft,
            view.paddingTop,
            view.paddingRight,
            navBar
        )
        insets
    }
}

fun View.applyStatusBarInsetOnly() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val top = insets.getInsets(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.displayCutout()
        ).top
        v.updatePadding(top = top) // 기존 좌/우/하단 패딩 유지
        insets
    }
    if (isAttachedToWindow) ViewCompat.requestApplyInsets(this)
    else doOnAttach { ViewCompat.requestApplyInsets(it) }
}

fun View.applyTopInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val top = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
        v.updatePadding(top = v.paddingTop + top)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}

fun View.applyBottomInsets() {
    ViewCompat.setOnApplyWindowInsetsListener(this) { v, insets ->
        val bottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
        v.updatePadding(bottom = v.paddingBottom + bottom)
        insets
    }
    ViewCompat.requestApplyInsets(this)
}


fun View.updatePadding(
    left: Int = paddingLeft, top: Int = paddingTop,
    right: Int = paddingRight, bottom: Int = paddingBottom
) = setPadding(left, top, right, bottom)