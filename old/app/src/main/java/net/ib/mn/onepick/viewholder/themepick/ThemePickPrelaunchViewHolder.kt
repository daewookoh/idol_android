package net.ib.mn.onepick.viewholder.themepick

import android.annotation.SuppressLint
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import net.ib.mn.R
import net.ib.mn.databinding.ItemThemePickPrelaunchBinding
import net.ib.mn.model.ThemepickModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.calculateDDayFromUserTimeZone
import java.text.DateFormat
import java.text.SimpleDateFormat

class ThemePickPrelaunchViewHolder(
    val binding: ItemThemePickPrelaunchBinding,
    val onItemClick: (ThemepickModel) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        item: ThemepickModel,
        glideRequestManager: RequestManager,
    ) = with(binding) {
        itemView.setOnClickListener { }

        glideRequestManager
            .load(item.imageUrl)
            .transform(CenterCrop(), RoundedCorners(26))
            .into(themePickDoingIv)

        themePickDoingTitle.text = item.title

        val dateFormat = SimpleDateFormat.getDateInstance(
            DateFormat.MEDIUM,
            LocaleUtil.getAppLocale(itemView.context)
        )

        tvOpenDate.text = itemView.context.getString(
            R.string.vote_dday,
            calculateDDayFromUserTimeZone(item.beginAt).toString()
        )

        tvOpenPeriod.text = "${itemView.context.getString(R.string.onepick_period)} : ${
            dateFormat.format(item.beginAt)
        } ~ ${dateFormat.format(item.expiredAt)}"

        clThemePickDoingVoteParticipation.setOnClickListener { onItemClick(item) }
    }
}