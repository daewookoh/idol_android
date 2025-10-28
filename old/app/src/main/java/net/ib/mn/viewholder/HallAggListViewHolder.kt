/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 한 아이돌의 명예전당 누적순위 변화 리스트 보여주는 ViewHolder
 *
 * */

package net.ib.mn.viewholder

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.activity.HallOfFameAggHistoryLeagueActivity
import net.ib.mn.adapter.HallAggHistoryLeagueAdapter
import net.ib.mn.databinding.HallAggTopItemBinding
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.lang.Exception
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class HallAggListViewHolder(val binding : HallAggTopItemBinding, val mGlideRequestManager: RequestManager, val mContext : Context, val mListener : HallAggHistoryLeagueAdapter.OnClickListener, val sourceApp: String?) : RecyclerView.ViewHolder(binding.root){
    fun bind(item : HallAggHistoryModel){
        binding.llRanking.setOnClickListener{
            mListener.onItemClickListener(item)
        }
        val idolRank = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.rank.toLong())
        val rank = if (item.rank == 999) "-" else String.format(mContext.getString(R.string.rank_count_format), idolRank)
        binding.textRanking.text = rank

        if (mContext.javaClass.simpleName.contains(HallOfFameAggHistoryLeagueActivity::class.java.simpleName)) {    //명예전당에서 들어왔을 때만 화살표 보이게
            binding.ivArrowGo.visibility = View.VISIBLE
        }

        if (item.status.equals(RANKING_NEW, ignoreCase = true)) {
            binding.ranking.visibility = View.GONE
            binding.newRanking.visibility = View.VISIBLE
        } else {
            binding.ranking.visibility = View.VISIBLE
            binding.newRanking.visibility = View.GONE
            when {
                item.status.equals(RANKING_INCREASE, ignoreCase = true) -> {
                    binding.iconRanking.setImageResource(R.drawable.icon_change_ranking_up)
                }
                item.status.equals(RANKING_DECREASE, ignoreCase = true) -> {
                    binding.iconRanking.setImageResource(R.drawable.icon_change_ranking_down)
                }
                else -> {
                    binding.iconRanking.setImageResource(R.drawable.icon_change_ranking_no_change)
                }
            }
            val changeRankingCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.difference.toLong())
            binding.changeRanking.text = changeRankingCount
        }

        val idolId = item.idol?.getId() ?: 0

        if (item.resource_uri != null) {
            val imageUrl = UtilK.trendImageUrl(mContext, item.getResourceId(), sourceApp)
            Util.log("HallAggHistory:: $imageUrl")
            mGlideRequestManager
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(binding.photo)
        } else {
            mGlideRequestManager.clear(binding.photo)
            binding.photo.setImageResource(Util.noProfileImage(idolId))
        }

        val scoreCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(
            if (item.rank < ConfigModel.getInstance(mContext).cutLine + 1) {
                ConfigModel.getInstance(mContext).cutLine + 1 - item.rank
            }
            else {
                0.toLong()
            }
        )
        var scoreText = String.format(mContext.getString(R.string.score_format), scoreCount)
        if (item.rank < ConfigModel.getInstance(mContext).cutLine + 1) {
            scoreText = "+$scoreText"
        }
        binding.score.text = scoreText
        val date = item.getRefdate(mContext)
        binding.date.text = date
        val voteCount = item.heart
        val voteCountComma = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(voteCount.toLong())
        val voteCountText = String.format(mContext.getString(R.string.vote_count_format), voteCountComma)
        binding.count.text = voteCountText

    }

    companion object {
        private const val RANKING_INCREASE = "increase"
        private const val RANKING_DECREASE = "decrease"
        private const val RANKING_SAME = "same"
        private const val RANKING_NEW = "new"
    }
}