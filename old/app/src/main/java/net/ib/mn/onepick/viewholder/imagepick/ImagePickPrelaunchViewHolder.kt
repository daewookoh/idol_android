/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 이미지픽 목록 리스트 아이템 뷰홀더.
 *
 * */

package net.ib.mn.onepick.viewholder.imagepick

import android.annotation.SuppressLint
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.adapter.OnePickTopicAdapter
import net.ib.mn.databinding.ItemImagePickPrelaunchBinding
import net.ib.mn.model.OnepickTopicModel
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.calculateDDayFromUserTimeZone
import net.ib.mn.utils.setFirebaseUIAction
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import kotlin.math.floor

/**
 * @see
 * */

class ImagePickPrelaunchViewHolder(
    val binding: ItemImagePickPrelaunchBinding,
    private val clickListener : OnePickTopicAdapter.ClickListener,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: OnepickTopicModel, position: Int) = with(binding) {
        val context = itemView.context

        // 이게 있어야 이미지픽에서 좌우 페이징이 됨
        itemView.setOnClickListener {  }

        val dateFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))

        tvTopicTitle.text = item.title
        tvOpenPeriod.text = (
            itemView.context.getString(R.string.onepick_period) +
                " : " +
                dateFormat.format(item.createdAt) +
                " ~ " +
                dateFormat.format(item.expiredAt)
            )

        clParticipation.background = if(item.alarm) {
            ContextCompat.getDrawable(context, R.drawable.bg_gray110_radius27)
        }else {
            ContextCompat.getDrawable(context, R.drawable.bg_main_light_radius27)
        }

        tvParticipation.text = if (item.alarm) {
            context.getString(R.string.vote_alert_after)
        } else {
            context.getString(R.string.vote_alert_before)
        }

        tvOpenDate.text = itemView.context.getString(
            R.string.vote_dday,
            calculateDDayFromUserTimeZone(item.createdAt).toString()
        )

        clParticipation.setOnClickListener {
            if (!item.alarm) {
                setFirebaseUIAction(GaAction.IMAGE_PICK_PRELAUNCH)
                clickListener.setNotification(item)
            }
        }
    }

    companion object {
        const val TYPE_TODO = 0
        const val TYPE_DOING = 1
        const val TYPE_DONE = 2
    }
}