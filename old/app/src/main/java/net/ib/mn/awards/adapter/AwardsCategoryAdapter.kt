package net.ib.mn.awards.adapter

import android.annotation.SuppressLint
import android.app.ActionBar
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.ItemTagBinding
import net.ib.mn.utils.Util

class AwardsCategoryAdapter(
    private val context: Context,
    private val items: List<TagModel>,
    private val onClickListener: OnClickListener,
) : RecyclerView.Adapter<AwardsCategoryAdapter.ViewHolder>() {

    interface OnClickListener {
        fun onItemClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tagItemBinding: ItemTagBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_tag,
            parent,
            false,
        )
        return ViewHolder(tagItemBinding)
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val tag = items[position]

        if (position == 0) {
            val linearLayoutParam = LinearLayoutCompat.LayoutParams(
                ActionBar.LayoutParams.WRAP_CONTENT,
                ActionBar.LayoutParams.WRAP_CONTENT,
            )
            linearLayoutParam.marginStart = Util.convertDpToPixel(context, 14f).toInt()
            holder.tvTag.layoutParams = linearLayoutParam
        }

        if (tag.selected) { //선택되어있는 태그일 경우
            holder.tvTag.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
            holder.tvTag.isSelected = true
        } else { //선택되지 않은 태그일 경우
            holder.tvTag.setTextColor(ContextCompat.getColor(context, R.color.text_dimmed))
            holder.tvTag.isSelected = false
        }

        holder.tvTag.setBackgroundResource(R.drawable.bg_small_talk_tag)

        holder.tvTag.text = tag.name
        holder.llTag.setOnClickListener {
            onClickListener.onItemClicked(position)
            onClickTag(position)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun onClickTag(position: Int) {
        // 다른 태그 off
        for (i in items.indices) {
            if (i != position) {
                items[i].selected = false
            }
        }
        items[position].selected = true
        notifyDataSetChanged()
    }

    inner class ViewHolder(tagItemBinding: ItemTagBinding) :
        RecyclerView.ViewHolder(tagItemBinding.root) {
        val tvTag: AppCompatTextView = tagItemBinding.tvTag
        val llTag: LinearLayoutCompat = tagItemBinding.llTag
    }

    override fun getItemViewType(position: Int): Int {
        return when (position) {
            0 -> {
                TAG_ALL
            }
            else -> {
                TAG_REST
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return items[position].hashCode().toLong()
    }

    companion object {
        const val TAG_ALL = 0
        const val TAG_REST = 1

        // 0 : All 태그가 안켜져있는 경우, 1 : All 태그 켜져있고 나머지 태그가 꺼져있는 경우, 2 : All 태그 및 나머지 태그 모두 켜져있는 경우
        const val allTagNotSelected: Int = 0
        const val allTagOnlySelected: Int = 1
        const val tagAllSelected: Int = 2
    }
}