package net.ib.mn.attendance.viewholder

import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.model.DailyRewardModel
import net.ib.mn.databinding.ItemAttendanceHeartMoreBinding
import net.ib.mn.utils.Util
import net.ib.mn.utils.loadSvgImage
import net.ib.mn.utils.setMargins

class AttendanceHeartMoreVH(
    val binding: ItemAttendanceHeartMoreBinding,
    val lifecycleScope: LifecycleCoroutineScope,
) : RecyclerView.ViewHolder(binding.root) {

    val heartMoreViewClickArray: Array<View> = arrayOf(
        binding.btnLevelMoreStatus,
        binding.clLevelMore,
        binding.ivLevelMore,
        binding.tvLevelMoreTitle,
        binding.tvLevelMoreSubTitle
    )

    fun bind(dailyRewardModel: DailyRewardModel, position: Int) {
        setImageUrl(dailyRewardModel)
        setTitle(dailyRewardModel)
        setButtonStatus(dailyRewardModel)
        repositionItem(position)
    }

    private fun setImageUrl(dailyRewardModel: DailyRewardModel) = with(binding) {

        val imageUrl = if (!Util.isUsingNightModeResources(itemView.context)) {
            dailyRewardModel.imageUrl
        } else {
            dailyRewardModel.imageUrlDark
        }

        ivLevelMore.loadSvgImage(imageUrl ?: return@with, lifecycleScope)
    }

    private fun setTitle(dailyRewardModel: DailyRewardModel) = with(binding) {
        tvLevelMoreTitle.text = dailyRewardModel.title

        //무료 충전소 부제목 세팅.
        if (dailyRewardModel.linkUrl != null) {

            val amount = if (dailyRewardModel.amount == 0) {
                "\u221E"
            } else {
                dailyRewardModel.amount
            }

            val formatText = if (dailyRewardModel.item != "heart") {
                itemView.context.getString(R.string.reward_offerwall_diamond)
            } else {
                itemView.context.getString(R.string.reward_offerwall_heart)
            }

            tvLevelMoreSubTitle.text = String.format(
                formatText,
                amount
            )

            return@with
        }

        if (dailyRewardModel.item == "heart") {
            tvLevelMoreSubTitle.text = String.format(
                itemView.context.getString(R.string.attendance_heart_count_format),
                dailyRewardModel.amount,
            )
            return@with
        }

        tvLevelMoreSubTitle.text = String.format(
            itemView.context.getString(R.string.dia_count_format),
            dailyRewardModel.amount,
        )
    }

    private fun repositionItem(position: Int) = with(binding) {
        when (position) {
            1 -> {
                tvAdditionalHeart.visibility = View.VISIBLE
                clLevelMore.setMargins(
                    top = 16f,
                    left = 16f,
                    right = 16f,
                )
            }

            else -> {
                tvAdditionalHeart.visibility = View.GONE
                clLevelMore.setMargins(
                    top = 10f,
                    left = 16f,
                    right = 16f,
                )
            }
        }
    }

    private fun setButtonStatus(dailyRewardModel: DailyRewardModel) = with(binding) {

        when (dailyRewardModel.status) {
            AVAILABLE_STATUS -> {
                btnLevelMoreStatus.text = itemView.context.getString(R.string.receive)
                btnLevelMoreStatus.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.bg_radius_20_main,
                    ),
                )
                btnLevelMoreStatus.isEnabled = true
                heartMoreViewClickArray.forEach { view -> view.isEnabled = true}
            }

            COMPLETE_STATUS -> {
                btnLevelMoreStatus.text = itemView.context.getString(R.string.complete)
                btnLevelMoreStatus.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.bg_radius_20_gray200,
                    ),
                )
            }

            BEFORE_OPENING_STATUS -> {
                btnLevelMoreStatus.text = itemView.context.getString(R.string.before_opening)
                btnLevelMoreStatus.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.bg_radius_20_gray200,
                    ),
                )
            }

            MOVE_STATUS -> {
                btnLevelMoreStatus.text = itemView.context.getString(R.string.attendance_transition)
                btnLevelMoreStatus.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.bg_radius_20_main,
                    ),
                )
                btnLevelMoreStatus.isEnabled = true
                heartMoreViewClickArray.forEach { view -> view.isEnabled = true }
            }
            CLOSE_STATUS -> {
                btnLevelMoreStatus.text = itemView.context.getString(R.string.support_end)
                btnLevelMoreStatus.setBackgroundDrawable(
                    ContextCompat.getDrawable(
                        itemView.context,
                        R.drawable.bg_radius_20_gray200,
                    ),
                )
            }
        }
    }

    companion object {
        const val AVAILABLE_STATUS = "Y"
        const val COMPLETE_STATUS = "D"
        const val BEFORE_OPENING_STATUS = "N"
        const val MOVE_STATUS = "M"
        const val CLOSE_STATUS = "C"
    }
}