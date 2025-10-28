/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 누적순위 변화 화면( 그래프 하단 리스트 관리하는 Adapter
 *
 * */

package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import net.ib.mn.R
import net.ib.mn.databinding.HallAggChartItemBinding
import net.ib.mn.databinding.HallAggTopItemBinding
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.viewholder.HallAggChartViewHolder
import net.ib.mn.viewholder.HallAggListViewHolder

class HallAggHistoryLeagueAdapter(
    private val mContext: Context,
    private val mListener: OnClickListener,
    private val sourceApp: String?
    ) : ListAdapter<HallAggHistoryModel, RecyclerView.ViewHolder>(
    diffUtil
) {
    private val mGlideRequestManager: RequestManager = Glide.with(mContext)
    private var averageHeart = 0L

    interface OnClickListener{
        fun onItemClickListener(item: HallAggHistoryModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when(viewType) {
            TYPE_TOP -> {
                val binding : HallAggChartItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.hall_agg_chart_item, parent, false)
                HallAggChartViewHolder(binding, mContext, averageHeart)
            }else -> {
                val binding : HallAggTopItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.hall_agg_top_item, parent, false)
                HallAggListViewHolder(binding, mGlideRequestManager, mContext, mListener, sourceApp)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {

        if(holder.itemViewType == TYPE_TOP){
            (holder as HallAggChartViewHolder).apply {
                bind(currentList)
                binding.executePendingBindings()
            }
        }
        else{
            (holder as HallAggListViewHolder).apply {
                bind(currentList[bindingAdapterPosition])
                binding.executePendingBindings()
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_TOP
        }
        return TYPE_RANK
    }

    fun updateAverageHeart(averageHeart: Long) {
        this.averageHeart = averageHeart
    }

    companion object {

        const val TYPE_TOP = 0
        const val TYPE_RANK = 1
        val diffUtil = object : DiffUtil.ItemCallback<HallAggHistoryModel>() {
            override fun areItemsTheSame(oldItem: HallAggHistoryModel, newItem: HallAggHistoryModel): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: HallAggHistoryModel, newItem: HallAggHistoryModel): Boolean {
                return oldItem.idol?.getId() == newItem.idol?.getId()
            }

        }
    }
}
