package net.ib.mn.attendance.viewholder

import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.model.DailyRewardModel
import net.ib.mn.databinding.ItemWarningBinding

class WarningVH(
    val binding: ItemWarningBinding,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(dailyRewardModel: DailyRewardModel) {
        binding.tvWarningTitle.text = dailyRewardModel.title
        binding.tvWarningSubtitle.text = dailyRewardModel.desc
    }
}