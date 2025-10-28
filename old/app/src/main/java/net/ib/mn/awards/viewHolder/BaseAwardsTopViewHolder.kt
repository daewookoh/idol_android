/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 어워즈 헤더 베이스 뷰 홀더.
 *
 * */

package net.ib.mn.awards.viewHolder

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.StatsAwardsResultHeaderBinding
import net.ib.mn.model.HallModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.Locale

/**
 * @see
 * */

open class BaseAwardsTopViewHolder(val binding: StatsAwardsResultHeaderBinding) :
    RecyclerView.ViewHolder(binding.root) {

    open fun bind(item: HallModel) = with(binding) {
        val rankCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(1)
        rank.text = String.format(itemView.context.getString(R.string.rank_format), rankCount)

        llAwardBackground.background =
            ContextCompat.getDrawable(itemView.context, R.drawable.img_award_1st)

        UtilK.setName(itemView.context, item.idol, name, group)

        val scoreCount: String =
            NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.score)
                .replace(("[^\\d.]").toRegex(), "")
        val scoreText: String = String.format(
            itemView.context.getString(R.string.score_format),
            scoreCount,
        )
        score.text = scoreText
    }
}