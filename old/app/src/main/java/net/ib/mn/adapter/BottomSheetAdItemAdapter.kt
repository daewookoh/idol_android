package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.core.model.SupportAdTypeListModel
import net.ib.mn.databinding.ItemBottomSheetBinding


class BottomSheetAdItemAdapter(
    private val mItems: List<SupportAdTypeListModel>,
    private val mListener: AdItemListener
) : RecyclerView.Adapter<BottomSheetAdItemAdapter.ViewHolder>() {

    interface AdItemListener {
        fun onItemClick(adList: SupportAdTypeListModel)
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
        fun bind(adList: SupportAdTypeListModel) {
            binding.tvItemName.apply {
                text = adList.period
                setOnClickListener {
                    mListener.onItemClick(adList)
                }
            }
        }
    }
}