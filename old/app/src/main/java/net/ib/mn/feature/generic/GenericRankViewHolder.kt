package net.ib.mn.feature.generic

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.internal.managers.ViewComponentManager.FragmentContextWrapper
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.RankingBindingProxy
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.safeActivity
import net.ib.mn.utils.ext.setOnSingleClickListener
import net.ib.mn.utils.setIdolBadgeIcon
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.sqrt

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: chartId 사용하는 화면 실시간 순위 ViewHolder
 *
 * */

class GenericRankViewHolder(
    val binding: RankingBindingProxy,
    val mGlideRequestManager: RequestManager,
) : RecyclerView.ViewHolder(binding.root) {

    val context: Context = binding.root.context
    protected val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault())

    fun bind(
        idol: IdolModel,
        maxVoteCount: Long,
        voteMap: HashMap<Int, Long>,
        animatorPool: HashMap<Int, ValueAnimator?>,
        clickListener: GenericRankingClickListener
    ) = with(binding) {
        containerPhotos.visibility = View.GONE

        setIdolBadgeIcon(iconAngel, iconFairy, iconMiracle, iconRookie, iconSuperRookie, idol)

        itemView.setOnClickListener {
            clickListener.onItemClicked(idol)
        }

        val rank = idol.rank

        UtilK.setName(context, idol, nameView, groupView)

        val rankCount = numberFormatter.format(rank + 1)
        rankView.text = String.format(
            context.getString(R.string.rank_count_format),
            rankCount
        )

        when (idol.anniversary) {
            Const.ANNIVERSARY_BIRTH -> {
                if (BuildConfig.CELEB || idol.type == "S") {
                    badgeBirth.visibility = View.VISIBLE
                    badgeDebut.visibility = View.GONE
                } else {
                    // 그룹은 데뷔뱃지로 보여주기
                    badgeBirth.visibility = View.GONE
                    badgeDebut.visibility = View.VISIBLE
                }
                badgeComeback.visibility = View.GONE
                badgeMemorialDay.visibility = View.GONE
                badgeAllInDay.visibility = View.GONE
            }

            Const.ANNIVERSARY_DEBUT -> {
                badgeBirth.visibility = View.GONE
                badgeDebut.visibility = View.VISIBLE
                badgeComeback.visibility = View.GONE
                badgeMemorialDay.visibility = View.GONE
                badgeAllInDay.visibility = View.GONE
            }

            Const.ANNIVERSARY_COMEBACK -> {
                badgeBirth.visibility = View.GONE
                badgeDebut.visibility = View.GONE
                badgeComeback.visibility = View.VISIBLE
                badgeMemorialDay.visibility = View.GONE
                badgeAllInDay.visibility = View.GONE
            }

            Const.ANNIVERSARY_MEMORIAL_DAY -> {
                badgeBirth.visibility = View.GONE
                badgeDebut.visibility = View.GONE
                badgeComeback.visibility = View.GONE
                badgeMemorialDay.visibility = View.VISIBLE
                badgeAllInDay.visibility = View.GONE
                val memorialDayCount: String
                if (Util.isRTL(context)) {
                    memorialDayCount = numberFormatter.format(idol.anniversaryDays)
                } else {
                    memorialDayCount = idol.anniversaryDays.toString()
                }
                badgeMemorialDay.text =
                    memorialDayCount.replace(("[^\\d.]").toRegex(), "").plus(context.getString(R.string.lable_day))
            }

            Const.ANNIVERSARY_ALL_IN_DAY -> {
                badgeBirth.visibility = View.GONE
                badgeDebut.visibility = View.GONE
                badgeComeback.visibility = View.GONE
                badgeMemorialDay.visibility = View.GONE
                badgeAllInDay.visibility = View.VISIBLE
            }

            else -> {
                badgeBirth.visibility = View.GONE
                badgeDebut.visibility = View.GONE
                badgeComeback.visibility = View.GONE
                badgeMemorialDay.visibility = View.GONE
                badgeAllInDay.visibility = View.GONE
            }
        }

        UtilK.profileRoundBorder(idol.miracleCount, idol.fairyCount, idol.angelCount, photoBorder)

        if (BuildConfig.CELEB) {
            voteCountView.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
        }

        val voteCount = idol.heart

        val oldVote: Long = voteMap.get(idol.getId()) ?: 0L

        // ViewHolder 멤버로 넣었더니 이전 animation이 안가져 와져서 pool에서 꺼내옴
        // 여기도 스크롤 튀는 현상 유발
        var animator = animatorPool[itemView.hashCode()]
        animator?.removeAllUpdateListeners()    // 기존 애니메이션 돌던거를 취소하고
        animator?.cancel()
        if (oldVote != voteCount && Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false)) {
            animator = ValueAnimator.ofFloat(0f, 1f)
            // 애니메이터 생성 후 풀에 넣기
            animatorPool.set(itemView.hashCode(), animator)
            animator.addUpdateListener {
                var value = (oldVote + (voteCount - oldVote) * (it.animatedValue as Float)).toLong()
                value = if (value > voteCount) voteCount else value
                val voteCountComma = numberFormatter.format(value)
                voteCountView.text = voteCountComma
            }
            animator?.addListener(object : Animator.AnimatorListener {
                override fun onAnimationRepeat(animation: Animator) {
                }

                override fun onAnimationCancel(animation: Animator) {
                }

                override fun onAnimationStart(animation: Animator) {
                }

                override fun onAnimationEnd(animation: Animator) {
                    val voteCountComma = numberFormatter.format(voteCount)
                    voteCountView.text = voteCountComma
                }
            })
            animator?.duration = 1000
            animator?.start()

        } else {
            val voteCountComma = numberFormatter.format(voteCount)
            voteCountView.text = voteCountComma
        }
        voteMap.set(idol.getId(), voteCount)

        val idolId = idol.getId()
        // 얘도 스크를 튀는 현상 유발
        mGlideRequestManager.load(UtilK.top1ImageUrl(context, idol, Const.IMAGE_SIZE_LOWEST))
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(idolId))
            .fallback(Util.noProfileImage(idolId))
            .placeholder(Util.noProfileImage(idolId))
            .dontAnimate()
            .into(imageView)

        // hilt 사용시 context가 FragmentContextWrapper로 들어와서 activity로 변경
        val activity = context.safeActivity ?: return@with
        activity.windowManager
            .defaultDisplay
            .getMetrics(DisplayMetrics())

        if (maxVoteCount == 0L) {
            progressBar.setWidthRatio(28)
        } else {
            // int 오버플로 방지
            if (voteCount == 0L) {
                progressBar.setWidthRatio(28)
            } else {
                progressBar.setWidthRatio(
                    28 + (sqrt(sqrt(voteCount.toDouble())) * 72 / sqrt(
                        sqrt(maxVoteCount.toDouble())
                    )).toInt()
                )
            }
        }

        voteBtn.setOnSingleClickListener {
            clickListener.onVote(idol)
        }
    }
}