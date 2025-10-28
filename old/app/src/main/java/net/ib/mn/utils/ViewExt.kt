/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 뷰 확장함수.
 *
 * */

package net.ib.mn.utils

import android.content.Context
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.net.Uri
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.AppCompatImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.constraintlayout.widget.Group
import androidx.lifecycle.LifecycleCoroutineScope
import com.caverock.androidsvg.SVG
import com.caverock.androidsvg.SVGParseException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.TimeUnit

// constraint group 차일드뷰들  클릭 전부 클릭 동작 받음.
fun Group.setAllOnClickListener(listener: View.OnClickListener?) {
    referencedIds.forEach { id ->
        rootView.findViewById<View>(id).setOnClickListener(listener)
    }
}

fun Drawable.setColorFilter(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        colorFilter = BlendModeColorFilter(color, BlendMode.MULTIPLY)
    } else {
        setColorFilter(color, PorterDuff.Mode.MULTIPLY)
    }
}

fun Drawable.setColorFilterSrcIn(color: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        colorFilter = BlendModeColorFilter(color, BlendMode.SRC_IN)
    } else {
        setColorFilter(color, PorterDuff.Mode.SRC_IN)
    }
}

fun View.setMargins(
    left: Float? = null,
    top: Float? = null,
    right: Float? = null,
    bottom: Float? = null,
) {
    layoutParams<ViewGroup.MarginLayoutParams> {
        left?.run { leftMargin = dpToPx(this) }
        top?.run { topMargin = dpToPx(this) }
        right?.run { rightMargin = dpToPx(this) }
        bottom?.run { bottomMargin = dpToPx(this) }
    }
}

inline fun <reified T : ViewGroup.LayoutParams> View.layoutParams(block: T.() -> Unit) {
    if (layoutParams is T) block(layoutParams as T)
}
fun View.dpToPx(dp: Float): Int = context.dpToPx(dp)
fun Context.dpToPx(dp: Float): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics).toInt()

fun AppCompatImageView.loadSvgImage(imageUrl: String, lifecycleScope: LifecycleCoroutineScope) =
    lifecycleScope.launch(Dispatchers.IO) {
        try {
            val uri = Uri.parse(imageUrl)
            val url = URL(uri.toString())
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val inputStream: InputStream = connection.inputStream

            val svg: SVG = SVG.getFromInputStream(inputStream)
            val pictureDrawable = PictureDrawable(svg.renderToPicture())

            withContext(Dispatchers.Main) {
                this@loadSvgImage.setImageDrawable(pictureDrawable)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: SVGParseException) {
            e.printStackTrace()
        }
    }

fun ConstraintLayout.setConstraintVerticalBias(vertical: Float, id: Int) {
    val constraintSet = ConstraintSet()
    constraintSet.clone(this)
    constraintSet.setVerticalBias(id, vertical)
    constraintSet.applyTo(this)
}

fun View.visibilityChanged(action: (View) -> Unit) {
    this.viewTreeObserver.addOnGlobalLayoutListener {
        val newVis: Int = this.visibility
        if (this.tag as Int? != newVis) {
            this.tag = this.visibility

            // visibility has changed
            action(this)
        }
    }
}

//텍스트뷰에  timemills 를  00:00 포맷으로 바꿔줌.
fun TextView.convertTimeMillsToTimerFormat(duration: Long) {
    this.text = String.format(
        Locale.US,
        "%02d:%02d",
        TimeUnit.MILLISECONDS.toMinutes(duration),
        TimeUnit.MILLISECONDS.toSeconds(duration) -
            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(duration))
    )
}