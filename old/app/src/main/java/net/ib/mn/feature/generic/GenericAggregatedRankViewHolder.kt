package net.ib.mn.feature.generic

import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.Locale

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: chartId 사용하는 누적 순위 아이템 ViewHolder
 *
 * */

class GenericAggregatedRankViewHolder(
    val binding: AggregatedHofItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    val context = binding.root.context!!

    fun bind(
        idol: AggregateRankModel
    ) = with(binding) {
        ivArrowGo.visibility = View.GONE

        // 이게 있어야 기적-누적순위에서 좌우 페이징이 됨
        itemView.setOnClickListener {  }

        val idolRank = idol.scoreRank

        if (idolRank < 4) {
            iconRanking.visibility = View.VISIBLE
            when (idolRank) {
                1 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                2 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                3 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
            }
            rank.setTextColor(ContextCompat.getColor(context, R.color.main))
        } else {
            iconRanking.visibility = View.GONE
            rank.setTextColor(ContextCompat.getColor(context, R.color.gray580))
        }

        val nameSplit = Util.nameSplit(idol.name)
        name.text = nameSplit.first()
        group.text = if (nameSplit.size > 1) {
           nameSplit.last()
        } else {
            ""
        }

        val scoreCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(idol.score.toLong()).replace(",", "")
        val scoreText: String = String.format(context.getString(R.string.score_format), scoreCount)
        score.text = scoreText

        rank.text = String.format(context.getString(R.string.rank_format), idolRank.toString())

        val idolId = idol.idolId
        val glide = Glide.with(binding.root)

        if (idol.trendId != 0) {
            val imageUrl = UtilK.trendImageUrl(context, idol.trendId)
            glide
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(photo)
        } else {
            glide.clear(photo)
            photo.setImageResource(Util.noProfileImage(idolId))
        }
    }
}