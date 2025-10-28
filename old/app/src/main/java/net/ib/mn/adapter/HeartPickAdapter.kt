package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.ItemHeartPick1stBinding
import net.ib.mn.databinding.ItemHeartPickHeaderBinding
import net.ib.mn.databinding.ItemHeartPickRankBinding
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.viewholder.HeartPick1stViewHolder
import net.ib.mn.viewholder.HeartPickHeaderViewHolder
import net.ib.mn.viewholder.HeartPickRankViewHolder

class HeartPickAdapter(
    private var heartPickModel: HeartPickModel?,
    private val heartPickListener: HeartPickListener,
    private val commentClickListener: CommentClickListener,
    private val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    init {
        setHasStableIds(true)
    }

    interface HeartPickListener {
        fun onVote(item: HeartPickIdol?, id:Int)
        fun onTimer(view: ConstraintLayout, position: Int)
        fun goCommunity(heartPickModel: HeartPickIdol)
    }

    interface CommentClickListener {
        fun onComment(id: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val binding: ItemHeartPickHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_heart_pick_header,
                    parent,
                    false,
                )
                HeartPickHeaderViewHolder(binding, commentClickListener, lifecycleScope)
            }
            TYPE_1ST -> {
                val binding: ItemHeartPick1stBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_heart_pick_1st,
                    parent,
                    false,
                )
                HeartPick1stViewHolder(binding, heartPickListener, lifecycleScope)
            }
            else -> {
                val binding: ItemHeartPickRankBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_heart_pick_rank,
                    parent,
                    false,
                )
                HeartPickRankViewHolder(binding, heartPickListener, heartPickModel?.id ?: 0, lifecycleScope)
            }
        }
    }

    override fun getItemCount(): Int {
        if (heartPickModel == null) return 0
        return 1 + (heartPickModel?.heartPickIdols?.size ?: 0)
    }

    override fun getItemViewType(position: Int): Int {
        return when(position) {
            0 -> TYPE_HEADER
            1 -> TYPE_1ST
            else -> TYPE_RANK
        }
    }

    override fun getItemId(position: Int): Long {
        return when (position) {
            0 -> TYPE_HEADER.toLong() // 헤더 ID
            else -> heartPickModel?.heartPickIdols?.get(position - 1)?.idol_id?.toLong() ?: RecyclerView.NO_ID
        }
    }

    fun setHeartPickModel(newHeartPickModel: HeartPickModel) {
        val diffCallback = HeartPickDiffCallback(heartPickModel, newHeartPickModel)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        this.heartPickModel = newHeartPickModel
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            TYPE_HEADER -> {
                (holder as HeartPickHeaderViewHolder).bind(heartPickModel)
            }
            TYPE_1ST -> {
                (holder as HeartPick1stViewHolder).bind(heartPickModel)
            }
            TYPE_RANK -> {
                (holder as HeartPickRankViewHolder).bind(
                    heartPickModel = heartPickModel,
                    type = heartPickModel?.type,
                    position = position - 2
                )
            }
        }
    }

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_1ST = 1
        const val TYPE_RANK = 2
    }
}

class HeartPickDiffCallback(private val oldModel: HeartPickModel?, private val newModel: HeartPickModel?) : DiffUtil.Callback() {

    private val oldList = oldModel?.heartPickIdols ?: emptyList()
    private val newList = newModel?.heartPickIdols ?: emptyList()

    // getItemCount는 헤더를 포함하므로 +1
    override fun getOldListSize(): Int = 1 + oldList.size
    override fun getNewListSize(): Int = 1 + newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldItemPosition == 0 && newItemPosition == 0) return true // Header
        if (oldItemPosition == 0 || newItemPosition == 0) return false // Header vs Idol

        // Idol item 비교
        val oldIdol = oldList.getOrNull(oldItemPosition - 1)
        val newIdol = newList.getOrNull(newItemPosition - 1)
        return oldIdol?.idol_id == newIdol?.idol_id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        if (oldItemPosition == 0 && newItemPosition == 0) {
            // Header 내용 비교 (예: 타이틀, 서브타이틀)
            return oldModel?.title == newModel?.title && oldModel?.subtitle == newModel?.subtitle
        }

        val oldIdol = oldList.getOrNull(oldItemPosition - 1)
        val newIdol = newList.getOrNull(newItemPosition - 1)

        // Idol 내용 비교 (투표수, 순위 등)
        return oldIdol == newIdol
    }
}