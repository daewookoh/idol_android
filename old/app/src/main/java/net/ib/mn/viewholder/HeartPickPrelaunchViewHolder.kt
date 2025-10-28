package net.ib.mn.viewholder

import android.content.Intent
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import net.ib.mn.R
import net.ib.mn.activity.HeartPickPrelaunchActivity
import net.ib.mn.databinding.ItemHeartPickPrelaunchBinding
import net.ib.mn.model.HeartPickModel
import net.ib.mn.utils.formatDateForDisplay
import net.ib.mn.utils.getDdayString
import net.ib.mn.utils.parseDateStringToMillis
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class HeartPickPrelaunchViewHolder(
    val binding: ItemHeartPickPrelaunchBinding,
    private val currentDateTime: Long,
    val onItemClick: () -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var mGlideRequestManager: RequestManager

    fun bind(heartPickModel: HeartPickModel) = with(binding) {
        mGlideRequestManager = Glide.with(itemView.context)

        tvTitle.text = heartPickModel.title
        tvSubTitle.text = heartPickModel.subtitle

        val startMillis = parseDateStringToMillis(heartPickModel.beginAtUtc)
        val endMillis = parseDateStringToMillis(heartPickModel.endAtUtc)
        val dday = getDdayString(itemView.context, currentDateTime, startMillis)

        tvOpenDate.text = dday

        val period = "${formatDateForDisplay(startMillis)}~${formatDateForDisplay(endMillis)}"
        tvOpenPeriod.text = period

        mGlideRequestManager.load(heartPickModel.bannerUrl)
            .transform(CenterCrop())
            .into(ivTop)

        clDoVote.setOnClickListener {
            val intent = Intent(itemView.context, HeartPickPrelaunchActivity::class.java)
            intent.putExtra(HeartPickPrelaunchActivity.HEART_PICK_ID, heartPickModel.id)
            startActivity(itemView.context, intent, null)
        }
    }
}