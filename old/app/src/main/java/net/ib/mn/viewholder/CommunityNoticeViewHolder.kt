/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewholder

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.databinding.ItemBoardNoticeBinding
import net.ib.mn.model.NoticeModel

class CommunityNoticeViewHolder(
    val binding: ItemBoardNoticeBinding,
    val context: Context,
    private val onItemClicked: (NoticeModel) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(
        noticeItem: NoticeModel,
        isShowDivider: Boolean = true,
    ) {
        with(binding) {
            layoutContainer.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClicked(noticeItem)
                }
            }

            viewDivider.visibility = if (isShowDivider) {
                View.VISIBLE
            } else {
                View.GONE
            }

            tvTitle.text = noticeItem.title
        }
    }
}