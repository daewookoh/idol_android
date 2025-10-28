package net.ib.mn.feature.generic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.BuildConfig
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.utils.setMargins

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: chartId 사용하는 누적 순위 아이템 어댑터
 *
 * */

class GenericAggregatedAdapter(
    private var items: ArrayList<AggregateRankModel>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_RANK -> {
                val binding = AggregatedHofItemBinding.inflate(inflater, parent, false)
                GenericAggregatedRankViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = TYPE_RANK

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (items.isEmpty()) return

        when (holder) {
            is GenericAggregatedRankViewHolder -> {
                holder.bind(items[position])

                if (!BuildConfig.CELEB) {
                    val margin = if (position == items.size - 1) {
                        9f
                    } else {
                        0f
                    }
                    holder.itemView.setMargins(bottom = margin)
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return if (position in items.indices) {
            items[position].idolId.toLong()
        } else {
            0L
        }
    }

    fun setItems(newItems: ArrayList<AggregateRankModel>) {
        items.apply {
            clear()
            addAll(newItems)
        }
        notifyDataSetChanged()
    }

    companion object {
        const val TYPE_RANK = 1
    }
}