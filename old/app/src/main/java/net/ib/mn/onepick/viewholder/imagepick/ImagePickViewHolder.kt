/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 이미지픽 목록 리스트 아이템 뷰홀더.
 *
 * */

package net.ib.mn.onepick.viewholder.imagepick

import android.annotation.SuppressLint
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.adapter.OnePickTopicAdapter
import net.ib.mn.databinding.ItemImagePickBinding
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.onepick.viewholder.themepick.OnePickVoteStatus
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.isWithin48Hours
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.floor

/**
 * @see
 * */

class ImagePickViewHolder(
    val binding: ItemImagePickBinding,
    private val clickListener : OnePickTopicAdapter.ClickListener,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: OnepickTopicModel, isNew: Boolean) = with(binding) {
        // 이게 있어야 이미지픽에서 좌우 페이징이 됨
        itemView.setOnClickListener {  }

        val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))

        tvTopicTitle.text = item.title
        if (!item.subtitle.isNullOrEmpty()) tvTopicSubTitle.text = item.subtitle
        tvVotingPeriod.text = (
            itemView.context.getString(R.string.onepick_period) +
                " : " +
                dateFormat.format(item.createdAt) +
                " ~ " +
                dateFormat.format(item.expiredAt)
            )

        if (item.status == TYPE_TODO) {
            tvParticipation.text = "D-${getDday(item.createdAt)}"
            clParticipation.background =
                ContextCompat.getDrawable(itemView.context, R.drawable.bg_round_result_btn)
            tvNumberOfParticipation.visibility = View.GONE
            viShadow.visibility = View.GONE
            inShowCurrentRanking.root.visibility = View.VISIBLE
            return@with
        }

        if (item.status == TYPE_DOING) {
            viShadow.visibility = View.GONE
            inShowCurrentRanking.root.visibility = View.VISIBLE
        }

        ivNew.visibility = if (item.status == TYPE_DONE) {
            View.GONE
        } else {
            if (isNew && isWithin48Hours(item.createdAt)) View.VISIBLE else View.GONE
        }

        tvNumberOfParticipation.text = (
            itemView.context.getString(R.string.num_participants) +
                " : " +
                String.format(
                    itemView.context.getString(R.string.num_participants_format),
                    NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.count),
                )
            )

        inShowCurrentRanking.tvRealtimeResult.text = itemView.context.getString(R.string.see_current_ranking)
        inShowCurrentRanking.root.setOnClickListener {
            if (item.count == 0) {
                Util.showDefaultIdolDialogWithBtn1(
                    itemView.context,
                    null,
                    itemView.context.getString(R.string.onepick_no_votes),
                    { Util.closeIdolDialog() },
                    true,
                )
            } else {
                clickListener.goOnePickResult(item)
            }
        }
        tvParticipation.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_white_black))

        if (item.status == TYPE_DONE) {
            viShadow.visibility = View.VISIBLE
            clParticipation.bringToFront()
            clParticipation.background = ContextCompat.getDrawable(itemView.context, R.drawable.bg_round_result_btn)
            tvParticipation.setTextColor(ContextCompat.getColor(itemView.context, R.color.fix_white))
            tvParticipation.text = itemView.context.getString(R.string.see_result)
            inShowCurrentRanking.root.visibility = View.GONE
            clParticipation.setOnClickListener {
                clickListener.goOnePickResult(item)
            }
            return@with
        }

        when (item.vote) {
            OnePickVoteStatus.IMPOSSIBLE.code -> { // 더이상 투표 불가
                tvParticipation.setTextColor(ContextCompat.getColor(itemView.context, R.color.fix_white))
                clParticipation.setBackgroundResource(R.drawable.bg_round_result_btn)
                tvParticipation.text = itemView.context.getString(R.string.onepick_already_voted)
                clParticipation.setOnClickListener(null)
            }
            OnePickVoteStatus.SEE_VIDEOAD.code -> { // 비광 시청 후 투표할 수 있는 상태
                clParticipation.setBackgroundResource(R.drawable.bg_round_active_btn)
                tvParticipation.text = itemView.context.getString(R.string.imagepick_vote_with_ad)
                clParticipation.setOnClickListener {
                    clickListener.showVideoAd(item)
                }
            }
            else -> { // 투표 한번도 안한 상태
                clParticipation.setBackgroundResource(R.drawable.bg_round_active_btn)
                tvParticipation.text = itemView.context.getString(R.string.onepick_vote)
                clParticipation.setOnClickListener {
                    item.vote = "A"
                    item.count += 1
                    item.voteType = "N"
                    clickListener.goOnePickMatch(item)
                }
            }
        }
    }

    private fun getDday(dueDate: Date): Int {
        return try {
            val today = Calendar.getInstance()
            val dDay = Calendar.getInstance()
            dDay.time = dueDate
            floor(((dDay.timeInMillis - today.timeInMillis) / (1000 * 60 * 60 * 24)).toDouble()).toInt() + 1
        } catch (e: Exception) {
            -1
        }
    }

    companion object {
        const val TYPE_TODO = 0
        const val TYPE_DOING = 1
        const val TYPE_DONE = 2
    }
}