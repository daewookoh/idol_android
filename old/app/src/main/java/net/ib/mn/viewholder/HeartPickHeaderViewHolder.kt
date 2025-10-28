package net.ib.mn.viewholder

import android.annotation.SuppressLint
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.adapter.HeartPickAdapter
import net.ib.mn.databinding.ItemHeartPickHeaderBinding
import net.ib.mn.model.HeartPickModel
import net.ib.mn.model.PrizeModel
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HeartPickHeaderViewHolder(
    val binding: ItemHeartPickHeaderBinding,
    private val commentClickListener: HeartPickAdapter.CommentClickListener,
    val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var mGlideRequestManager: RequestManager
    private val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))

    fun bind(heartPickModel: HeartPickModel?) = with(binding) {
        mGlideRequestManager = Glide.with(itemView.context)

        tvTotalVoteData.text = String.format(itemView.context.getString(R.string.vote_count_format), numberFormat.format(heartPickModel?.vote?:0))
        tvCommentData.text =if(heartPickModel?.numComments != null && heartPickModel.numComments > 0) {
            String.format(itemView.context.getString(R.string.view_number_of_comments), heartPickModel.numComments)
        } else {
            itemView.context.getString(R.string.title_comment)
        }
        clComment.setOnClickListener {
            (itemView.context as BaseActivity).setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.HEARTPICK_COMMENT.actionValue,
                GaAction.HEARTPICK_COMMENT.label,
            )
            commentClickListener.onComment(heartPickModel!!.id)
        }


        if (heartPickModel != null) {
            setDate(heartPickModel)
        }

        setFirstReward(heartPickModel?.prize)
    }

    @SuppressLint("SetTextI18n")
    private fun setDate(heartPickModel: HeartPickModel) {
        val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(itemView.context))

        val periodFormat = SimpleDateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))

        val startDate = periodFormat.format(dateTimeFormat.parse(heartPickModel.beginAt) ?: Date()) ?: throw IllegalArgumentException("Invalid start time format")
        val endDate = periodFormat.format(dateTimeFormat.parse(heartPickModel.endAt) ?: Date()) ?: throw IllegalArgumentException("Invalid start time format")

        binding.tvPeriodData.text = "$startDate ~ $endDate"
    }

    private fun setFirstReward(prizeModel: PrizeModel?) = with(binding) {
        accordionMenu.setUI(
            title = prizeModel?.name,
            imageUrl = prizeModel?.image_url,
            subtitle = prizeModel?.location,
        )
    }
}