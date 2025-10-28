/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.onepick.viewholder.themepick

import android.annotation.SuppressLint
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import net.ib.mn.R
import net.ib.mn.databinding.ItemThemePickBinding
import net.ib.mn.onepick.ThemePickAdapter
import net.ib.mn.model.ThemepickModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.isWithin48Hours
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat

/**
 * @see
 * */

class ThemePickViewHolder(
    val binding: ItemThemePickBinding,
    private val clickListener : ThemePickAdapter.ClickListener
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        item: ThemepickModel,
        glideRequestManager: RequestManager,
        isNew: Boolean
    ) = with(binding) {
        // 이게 있어야 좌우 페이징이 됨
        itemView.setOnClickListener {  }

        glideRequestManager
            .load(item.imageUrl)
            .transform(CenterCrop(), RoundedCorners(26))
            .into(themePickDoingIv)

        themePickDoingTitle.text = item.title

        val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))
        themePickVotingDoingPeriod.text = (
            itemView.context.getString(R.string.onepick_period) +
                " : " +
                dateFormat.format(item.beginAt) +
                " ~ " +
                dateFormat.format(item.expiredAt)
            )
        val count = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.count)
        themePickDoingCount.text =
            "${itemView.context.getString(R.string.themepick_total_votes)} : ${count}${
                itemView.context.getString(
                    R.string.votes,
                )
            }"
        tvThemePickDoingVoteParticipation.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_white_black))

        if (item.status == TYPE_DONE) {
            viShadow.visibility = View.VISIBLE
            clThemePickDoingVoteParticipation.bringToFront()
            clThemePickDoingVoteParticipation.background =
                ContextCompat.getDrawable(itemView.context, R.drawable.bg_round_result_btn)
            tvThemePickDoingVoteParticipation.setTextColor(ContextCompat.getColor(itemView.context, R.color.fix_white))
            tvThemePickDoingVoteParticipation.text = itemView.context.getString(R.string.see_result)
            inShowCurrentRanking.root.visibility = View.GONE
            clThemePickDoingVoteParticipation.setOnClickListener {
                clickListener.goThemePickResult(item)
            }
            ivNew.visibility = View.GONE
            return@with
        } else {
            inShowCurrentRanking.root.visibility = View.VISIBLE
            ivNew.visibility = if (isNew && isWithin48Hours(item.beginAt)) View.VISIBLE else View.GONE
        }

        viShadow.visibility = View.GONE
        when (item.vote) {
            OnePickVoteStatus.ABLE.code -> { // 한번도 투표하지 않음.
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_round_active_btn)
                tvThemePickDoingVoteParticipation.text =
                    itemView.context.getString(R.string.guide_vote_title)
            }

            OnePickVoteStatus.SEE_VIDEOAD.code -> { // 투표를 했지만 다이아 사용을 통해 추가 투표 가능함.
                // 광고 시청 후 추가 투표로 변경
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_round_active_btn)
                tvThemePickDoingVoteParticipation.text =
                    itemView.context.getString(R.string.themepick_vote_again)
            }

            else -> { // 더이상 투표할 수 없음.
                clThemePickDoingVoteParticipation.setBackgroundResource(R.drawable.bg_round_result_btn)
                tvThemePickDoingVoteParticipation.setTextColor(ContextCompat.getColor(itemView.context, R.color.fix_white))
                tvThemePickDoingVoteParticipation.text =
                    itemView.context.getString(R.string.themepick_today_voted)
            }
        }

        if (item.vote == OnePickVoteStatus.SEE_VIDEOAD.code || item.vote == OnePickVoteStatus.ABLE.code) { // 다이아투표이거나 투표안했으면 클릭가능.
            clThemePickDoingVoteParticipation.setOnClickListener {
                clickListener.goThemePickRank(item)
            }
        } else { // 투표완료했으면 클릭 불가능.
            clThemePickDoingVoteParticipation.setOnClickListener(null)
        }

        inShowCurrentRanking.root.setOnClickListener {
            if (item.count == 0) { // 참여자수가 0이면 팝업을띄워줌 아니면 결과 화면으로.
                Util.showDefaultIdolDialogWithBtn1(
                    itemView.context,
                    null,
                    itemView.context.getString(R.string.onepick_no_votes),
                    { Util.closeIdolDialog() },
                    true,
                )
            } else {
                clickListener.goThemePickResult(item)
            }
        }
    }

    companion object {
        const val TYPE_DOING = 1
        const val TYPE_DONE = 2
    }
}