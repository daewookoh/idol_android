package net.ib.mn.utils

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide

const val MAX_BITMAP_BYTES = 100 * 1024 * 1024 // RecordingCanvas.java getPanelFrameSize()

/**
 * 100MB가 넘는 비트맵을 보여주려할 때 크래시나지 않게 안전하게 보여주는 함수
 * Canvas: trying to draw too large(~~~bytes) bitmap. Fatal Exception
 */
fun ImageView.safeSetImageBitmap(
    activity: Activity?,
    bitmap: Bitmap,
) {
    val activity = activity ?: return
    val requestManager = Glide.with(activity.applicationContext)

    if(bitmap.byteCount >= MAX_BITMAP_BYTES) {
        activity.runOnUiThread {
            requestManager
                .load(bitmap)
                .override(Const.MAX_IMAGE_WIDTH)
                .into(this)
        }
        return
    }

    setImageBitmap(bitmap)
}

fun ImageView.safeSetImageDrawable(
activity: Activity?,
drawable: Drawable
) {
    val activity = activity ?: return
    val requestManager = Glide.with(activity.applicationContext)

    activity.runOnUiThread {
        requestManager
            .load(drawable)
            .override(Const.MAX_IMAGE_WIDTH) // Optional, use if needed
            .into(this)
    }

    setImageDrawable(drawable)
}