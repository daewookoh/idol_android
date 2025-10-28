/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.onepick.viewholder.themepick

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.ItemThemePickRankHeaderBinding
import net.ib.mn.model.ThemepickModel
import net.ib.mn.model.ThemepickRankModel
import net.ib.mn.utils.LocaleUtil
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat

/**
 * @see
 * */
class ThemePickHeaderViewHolder(
    private val binding: ItemThemePickRankHeaderBinding,
    private val mTheme: ThemepickModel,
    private val glideRequestManager: RequestManager,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: ThemepickRankModel, position: Int) = with(binding) {
        // 상단 이미지.
        glideRequestManager
            .load(mTheme.imageUrl)
            .transform(CenterCrop(), RoundedCorners(26))
            .into(binding.ivThemePickInner)

        // 빨간색 제목.
        tvHeadTitle.text = mTheme.title

        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))

        tvTotalVoteCount.text = String.format(itemView.context.getString(R.string.vote_count_format), numberFormat.format(mTheme.count))

        val dateFormat = SimpleDateFormat.getDateInstance(
            DateFormat.MEDIUM,
            LocaleUtil.getAppLocale(itemView.context)
        )
        tvVotePeriodCount.text = "${dateFormat.format(mTheme.beginAt)} ~ ${
            dateFormat.format(mTheme.expiredAt)
        }"

        // 결과화면일땐 최종결과 보이게하기.
        if (mTheme.status == ThemepickModel.STATUS_FINISHED) {
            tvHeadTitle.setTextColor(ContextCompat.getColor(itemView.context, R.color.gray300))
        } else {
            tvHeadTitle.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (BuildConfig.CELEB) R.color.main else R.color.main_light,
                ),
            )
        }

        accordionMenu.setUI(
            title = mTheme.prize?.name,
            subtitle = mTheme.prize?.location,
            imageUrl = mTheme.prize?.image_url,
        )
    }
}