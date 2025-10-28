/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.adapter

import android.content.Context
import android.text.Layout
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.awards.AwardsAggregatedAdapter
import net.ib.mn.databinding.HallAggChartItemBinding
import net.ib.mn.databinding.LayoutStatsBinding
import net.ib.mn.model.AwardStatsModel
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.utils.Logger
import net.ib.mn.viewholder.HallAggChartViewHolder

class AwardsStatsAdapter(
    private val mListener: OnClickListener
) : ListAdapter<AwardStatsModel, RecyclerView.ViewHolder>(
    diffUtil
) {

    interface OnClickListener {
        fun onItemClickListener(item: AwardStatsModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding: LayoutStatsBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.layout_stats,
            parent,
            false
        )
        return AwardsStatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as AwardsStatsViewHolder).apply {
            bind(currentList[bindingAdapterPosition])
            binding.executePendingBindings()

        }
    }

    inner class AwardsStatsViewHolder(val binding: LayoutStatsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AwardStatsModel) {
            with(binding) {
                stats = item.title
                rlStats.setOnClickListener {
                    mListener.onItemClickListener(item)
                }
            }
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<AwardStatsModel>() {
            override fun areItemsTheSame(
                oldItem: AwardStatsModel,
                newItem: AwardStatsModel
            ): Boolean {
                return oldItem.name == newItem.name
            }

            override fun areContentsTheSame(
                oldItem: AwardStatsModel,
                newItem: AwardStatsModel
            ): Boolean {
                return oldItem == newItem
            }

        }
    }
}