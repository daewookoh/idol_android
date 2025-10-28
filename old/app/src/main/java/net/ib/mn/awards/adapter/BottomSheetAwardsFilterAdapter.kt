/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 어워즈 바텀 다이얼로그 어댑터.
 *
 * */

package net.ib.mn.awards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.core.model.AwardChartsModel
import net.ib.mn.databinding.SmallTalkLocaleItemBinding

class BottomSheetAwardsFilterAdapter(
    private val chartCodes: List<AwardChartsModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var onConfirmClick: OnClickListener? = null

    interface OnClickListener {
        fun onItemClicked(chartModel: AwardChartsModel)
    }

    fun setOnClickListener(onConfirmClick: OnClickListener) {
        this.onConfirmClick = onConfirmClick
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val binding = SmallTalkLocaleItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = chartCodes.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ViewHolder).bind(chartCodes[position])
    }

    inner class ViewHolder(val binding: SmallTalkLocaleItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chartModel: AwardChartsModel) {
            binding.tvSmallTalkLanguage.text = chartModel.subname

            binding.tvSmallTalkLanguage.setOnClickListener {
                onConfirmClick?.onItemClicked(chartModel)
            }
        }
    }
}