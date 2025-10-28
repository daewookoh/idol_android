package feature.common.exodusimagepicker.util

import android.content.Context

object Util {

    fun convertDpToPixel(context: Context, dp: Float): Float {
        val resources = context.resources
        val metrics = resources.displayMetrics
        return dp * (metrics.densityDpi / 160f)
    }
}