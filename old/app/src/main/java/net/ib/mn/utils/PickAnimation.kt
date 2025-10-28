package net.ib.mn.utils

import android.view.View
import android.view.animation.Animation
import android.view.animation.Transformation


open class PickAnimation(val view: View, val target: View) : Animation() {

    override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
        view.x = view.x * (1 - interpolatedTime)
        view.y = view.y * (1 - interpolatedTime)
        view.layoutParams.width =
                (view.width * (1 - interpolatedTime) + target.width * interpolatedTime).toInt()
        view.layoutParams.height =
                (view.height * (1 - interpolatedTime) + target.height * interpolatedTime).toInt()
        view.requestLayout()
    }

    override fun willChangeBounds(): Boolean {
        return true
    }
}