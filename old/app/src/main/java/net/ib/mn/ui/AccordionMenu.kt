/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.transition.Fade
import androidx.transition.TransitionManager
import androidx.transition.TransitionSet
import com.bumptech.glide.Glide
import net.ib.mn.R
import net.ib.mn.databinding.AccordionMenuBinding

/**
 * @see
 * */

class AccordionMenu @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : ConstraintLayout(context, attrs, defStyleAttr) {

    private var isExpanded = false
    private var targetHeight = 0

    private val binding: AccordionMenuBinding = DataBindingUtil.inflate(
        LayoutInflater.from(context),
        R.layout.accordion_menu,
        this@AccordionMenu,
        true,
    )

    fun setUI(
        title: String?,
        subtitle: String?,
        imageUrl: String?
    ) = with(binding) {
        tvRewardTitle.text = title
        tvRewardSub.text = subtitle

        Glide.with(context).load(imageUrl)
            .into(ivReward)

        ivReward.visibility = if (imageUrl.isNullOrEmpty()) View.GONE else View.VISIBLE
        tvRewardTitle.visibility = if (title.isNullOrEmpty()) View.GONE else View.VISIBLE
        tvRewardSub.visibility = if (subtitle.isNullOrEmpty()) View.GONE else View.VISIBLE

        binding.cl1stRewardHead.setOnClickListener {
            val transition = TransitionSet().apply {
                addTransition(Fade())
                // 필요 애니메이션 있으면 더 추가합니다.
                duration = 600
            }
            TransitionManager.beginDelayedTransition(binding.clRoot, transition)

            if (binding.cl1stRewardDetail.visibility == View.GONE) {
                binding.cl1stRewardDetail.visibility = View.VISIBLE
                binding.ivArrowDown.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.btn_arrow_up_gray,
                    ),
                )
            } else {
                binding.cl1stRewardDetail.visibility = View.GONE
                binding.ivArrowDown.setImageDrawable(
                    ContextCompat.getDrawable(
                        context,
                        R.drawable.btn_arrow_down_gray,
                    ),
                )
            }

        }
    }
}