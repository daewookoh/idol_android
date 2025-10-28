package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.ItemHeartPickIdolBinding
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.viewholder.HeartPickMainIdolViewHolder

class HeartPickMainIdolAdapter(
    private var heartPickList: ArrayList<HeartPickIdol>?,
    private var totalVote: Int,
    private var firstPercent: Int,
    private val lifecycleScope: LifecycleCoroutineScope
    ) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemHeartPickIdol: ItemHeartPickIdolBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_heart_pick_idol,
            parent,
            false,
        )
        return HeartPickMainIdolViewHolder(itemHeartPickIdol, totalVote, lifecycleScope)
    }

    override fun getItemCount(): Int {
        return heartPickList?.size?.minus(1)?:0
    }

    override fun getItemId(position: Int): Long {
        return position.hashCode().toLong()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as HeartPickMainIdolViewHolder).apply {
            bind(heartPickList?.get(position + 1), position)
        }
    }
}