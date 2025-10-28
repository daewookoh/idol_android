/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.onepick.viewholder.imagepick

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.databinding.ItemImagePick1stBinding
import net.ib.mn.model.OnepickIdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.Locale

/**
 * @see
 * */

class ImagePick1stViewHolder(
    val binding: ItemImagePick1stBinding,
    private val date: String,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(onePickIdolModel: OnepickIdolModel) = with(binding) {
        val idolId = onePickIdolModel.idol?.getId() ?: 0
        val imageUrl = UtilK.onePickImageUrl(itemView.context, onePickIdolModel.id, date, Const.IMAGE_SIZE_LOWEST)

        Glide.with(itemView.context).load(imageUrl)
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(idolId))
            .fallback(Util.noProfileImage(idolId))
            .placeholder(Util.noProfileImage(idolId))
            .dontAnimate()
            .into(ivPhoto)

        inRankingPickNameAndGroup.setNameAndGroup(
            idol = onePickIdolModel.idol,
            maxLine = 1,
        )
        inRankingPickNameAndGroup.setTitleTextSize(titleSize = 16f, subTitleSize = 12f)
        setVote(onePickIdolModel)
    }

    private fun setVote(onePickIdolModel: OnepickIdolModel?) =
        with(binding.inGradientProgressBar) {
            onePickIdolModel ?: return@with

            val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
            tvVote.text = numberFormat.format(onePickIdolModel.vote)
            progress.setWidthRatio(100)
            progress.requestLayout()
        }
}