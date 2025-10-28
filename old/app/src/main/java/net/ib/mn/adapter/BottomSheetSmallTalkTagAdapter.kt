/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.SmallTalkTagItemBinding

class BottomSheetSmallTalkTagAdapter (
    private val tagList : List<TagModel>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var smallTalkTagClickListener : OnClickListener? = null

    interface OnClickListener {
        fun smallTalkTagClicked(tagModel: TagModel)
    }

    fun smallTalkClickListener(smallTalkTagClickListener : OnClickListener){
        this.smallTalkTagClickListener = smallTalkTagClickListener

    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val smallTalkItem : SmallTalkTagItemBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.small_talk_tag_item,
            parent,
            false
        )
        return SmallTalkViewHolder(smallTalkItem)
    }

    override fun getItemCount(): Int = tagList.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (tagList.isEmpty()) return
        else {
            (holder as SmallTalkViewHolder).apply {
                bind(tagList[position])
            }
        }
    }

    inner class SmallTalkViewHolder(val binding: SmallTalkTagItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(tagModel: TagModel){
            binding.tvSmallTalk.text = tagModel.name
            binding.tvSmallTalk.setOnClickListener {
                smallTalkTagClickListener?.smallTalkTagClicked(tagModel)
            }
        }
    }


}