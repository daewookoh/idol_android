package net.ib.mn.awards

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.awards.viewHolder.RankViewHolder
import net.ib.mn.awards.viewHolder.StatsAwardFinalTopViewHolder
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.databinding.StatsAwardsResultHeaderBinding
import net.ib.mn.model.AwardStatsModel
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel

class BestChoice2023Adapter(
    private val glideRequestManager: RequestManager,
    private val awardStatsModel: AwardStatsModel?,
    private val onClickListener: OnClickListener,
    private val onItemClicked: (IdolModel?) -> Unit,
) : ListAdapter<HallModel, RecyclerView.ViewHolder>(diffUtil) {

    interface OnClickListener {
        fun onItemClicked(item: IdolModel?)

        fun onFinalResultBottomSheetClicked()
    }

    private var awardPeriod: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            AWARD_FINAL_TOP -> {
                val awardsResultHeader: StatsAwardsResultHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.stats_awards_result_header,
                    parent,
                    false,
                )
                StatsAwardFinalTopViewHolder(
                    binding = awardsResultHeader,
                    visibleBackground = true,
                    glideRequestManager = glideRequestManager,
                    awardStatsData = awardStatsModel,
                    onItemClicked = { idol ->
                        onItemClicked(idol)
                    },
                ) {
                    onClickListener.onFinalResultBottomSheetClicked()
                }
            }

            else -> {
                val aggregatedHofItem: AggregatedHofItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.aggregated_hof_item,
                    parent,
                    false,
                )
                RankViewHolder(
                    binding = aggregatedHofItem,
                    glideRequestManager = glideRequestManager,
                    votable = "A",
                    0,
                    onItemClicked = { idol ->
                        onItemClicked(idol)
                    },
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            AWARD_FINAL_TOP -> {
                (holder as StatsAwardFinalTopViewHolder).apply {
                    bind(currentList[position])
                    binding.tvAwardPeriod.visibility = View.VISIBLE
                    binding.tvAwardPeriod.text = awardPeriod
                    binding.executePendingBindings()
                }
            }

            else -> {
                (holder as RankViewHolder).apply {
                    bind(currentList[position])
                    binding.executePendingBindings()
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (currentList[position].id == EMPTY_ITEM) {
            return AwardsAggregatedAdapter.EMPTY_ITEM
        }

        if (position == 0) {
            return AWARD_FINAL_TOP
        }

        return TYPE_RANK
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].hashCode().toLong()
    }

    override fun getItemCount(): Int = currentList.size

    fun setAwardPeriod(awardPeriod: String) {
        this.awardPeriod = awardPeriod
        notifyItemChanged(0)
    }

    companion object {

        const val AWARD_FINAL_TOP = 0
        const val TYPE_RANK = 2
        const val EMPTY_ITEM = -2

        val diffUtil = object : DiffUtil.ItemCallback<HallModel>() {
            override fun areItemsTheSame(oldItem: HallModel, newItem: HallModel): Boolean {
                return oldItem == newItem
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: HallModel, newItem: HallModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}