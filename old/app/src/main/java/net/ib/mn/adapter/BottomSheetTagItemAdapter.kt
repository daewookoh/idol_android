package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.ItemBottomSheetBinding


class BottomSheetTagItemAdapter(
    private val mItems: ArrayList<TagModel>,
    private val mListener: TagItemListener
) : RecyclerView.Adapter<BottomSheetTagItemAdapter.ViewHolder>() {

    interface TagItemListener {
        fun onItemClick(tag: TagModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemBottomSheetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false)
        )
    }

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mItems[position])
    }

    inner class ViewHolder(val binding: ItemBottomSheetBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tag: TagModel) {
            binding.tvItemName.apply {
                text = tag.name
                setOnClickListener {
                    mListener.onItemClick(tag)
                }
            }
        }
    }
}