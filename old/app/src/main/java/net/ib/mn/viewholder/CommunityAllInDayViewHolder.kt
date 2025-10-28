package net.ib.mn.viewholder

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.databinding.CommunityAllInDayBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.setColorFilter
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale

class CommunityAllInDayViewHolder(
    val binding: CommunityAllInDayBinding,
    val context: Context,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(burningDay: String?) {
        setBurningDay(burningDay)
    }

    private fun setBurningDay(burningDay: String? = null) = with(binding) {
        if(burningDay.isNullOrEmpty()) return@with

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val burningDate = simpleDateFormat.parse(burningDay)
            simpleDateFormat.applyPattern(context.getString(R.string.burning_day_format))
            val day = burningDate?.let { simpleDateFormat.format(it) }
            textBurningDay.text = day

            if (BuildConfig.CELEB) {
                ivBanner.setImageResource(R.drawable.img_allinday_celeb)
            } else {
                ivBanner.setImageResource(R.drawable.img_allinday)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
    }
}
