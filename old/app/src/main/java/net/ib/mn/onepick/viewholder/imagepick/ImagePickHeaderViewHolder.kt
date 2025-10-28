/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.onepick.viewholder.imagepick

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.ItemImagePickHeaderBinding
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.utils.LocaleUtil
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * @see
 * */

class ImagePickHeaderViewHolder(
    private val binding: ItemImagePickHeaderBinding,
    private val topPickModel: OnepickTopicModel?,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind() = with(binding) {
        if (topPickModel == null) return@with

        tvTitle.text = topPickModel.title

        tvParticipantsCount.text =
            String.format(
                itemView.context.getString(R.string.num_participants_format),
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(topPickModel.count),
            )

        val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))
        tvPeriodDate.text =
            dateFormat.format(topPickModel.createdAt) +
            " ~ " +
            dateFormat.format(topPickModel.expiredAt)
    }
}