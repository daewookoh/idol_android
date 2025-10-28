package net.ib.mn.adapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Util


abstract class BaseRankingAdapter(
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    // 펼치기 애니메이션
    protected fun expand(view: View, onAnimationEnd: () -> Unit) {
        Util.log("=== expand")
        view.measure(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        val targetHeight = (view.parent as ViewGroup).width / 3

        view.layoutParams.height = targetHeight
        view.visibility = View.VISIBLE

        val animator = ValueAnimator.ofInt(0, targetHeight).apply {
            duration = 250 // 애니메이션 지속 시간
            addUpdateListener { animation ->
                view.layoutParams.height = animation.animatedValue as Int
                view.requestLayout()
            }
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd()
            }
        })
        animator.start()
    }

    // 접기 애니메이션
    protected fun collapse(view: View) {
        val initialHeight = view.measuredHeight

        val animator = ValueAnimator.ofInt(initialHeight, 0).apply {
            duration = 250 // 애니메이션 지속 시간
            addUpdateListener { animation ->
                view.layoutParams.height = animation.animatedValue as Int
                view.requestLayout()
                if (animation.animatedValue as Int == 0) {
                    view.visibility = View.GONE
                }
            }
        }
        animator.start()
    }
}
