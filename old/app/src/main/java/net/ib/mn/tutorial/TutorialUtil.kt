package net.ib.mn.tutorial

import android.animation.Animator
import android.view.View
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

fun setupLottieTutorial(view: LottieAnimationView, toInVisible: Boolean = false, onEnd: () -> Unit) {
    view.apply {
        visibility = View.VISIBLE
        setAnimation("tutorial_heart.json")
        repeatCount = LottieDrawable.INFINITE
        playAnimation()

        setOnClickListener {
            setAnimation("tutorial_heart_touch.json")
            repeatCount = 0
            playAnimation()

            removeAllAnimatorListeners()
            addAnimatorListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}

                override fun onAnimationEnd(animation: Animator) {
                    view.removeAllAnimatorListeners()
                    view.cancelAnimation()
                    view.progress = 0f
                    view.visibility = if (toInVisible) View.INVISIBLE else View.GONE

                    onEnd()
                }

                override fun onAnimationCancel(animation: Animator) {}

                override fun onAnimationRepeat(animation: Animator) {}
            })
        }
    }
}