/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.onepick.viewholder.imagepick

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.databinding.ItemImagePickRankBinding
import net.ib.mn.model.OnepickIdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.vote.VotePercentage
import java.text.NumberFormat
import java.util.Locale

/**
 * @see
 * */

class ImagePickRankingViewHolder(
    val binding: ItemImagePickRankBinding,
    private val date: String,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(
        onePickIdolModel: OnepickIdolModel,
        position: Int,
    ) =
        with(binding) {
            val idolId = onePickIdolModel.idol?.getId() ?: 0

            val imageUrl = UtilK.onePickImageUrl(
                itemView.context,
                onePickIdolModel.id,
                date,
                Const.IMAGE_SIZE_LOWEST,
            )

            Glide.with(itemView.context).load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(ivPhoto)

            inRankingPickNameAndGroup.setNameAndGroup(
                idol = onePickIdolModel.idol,
                maxLine = 2,
            )

            tvRank.text = onePickIdolModel.rank.toString()

            setVote(onePickIdolModel)
            view.visibility = if (position == 2) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }

    private fun setVote(
        onePickIdolModel: OnepickIdolModel?,
    ) =
        with(binding.inGradientProgressBar) {
            onePickIdolModel ?: return@with

            val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
            tvVote.text = numberFormat.format(onePickIdolModel.vote)

            val vote = onePickIdolModel.vote
            val lastPlaceVoteCount = onePickIdolModel.lastPlaceVoteCount
            val firstPlaceVoteCount = onePickIdolModel.firstPlaceVoteCount
            val minPercent = onePickIdolModel.minPercent

            val progressPercent = VotePercentage.getVotePercentage(
                minPercentage = minPercent.toInt(),
                currentPlaceVote = vote,
                firstPlaceVote = firstPlaceVoteCount,
                lastPlaceVote = lastPlaceVoteCount,
            )

            progress.setWidthRatio(progressPercent)
            progress.requestLayout()
        }

    companion object {
        const val MIN_PERCENT_PROGRESS = 25
        const val MAX_PERCENT_PROGRESS = 100
    }
}