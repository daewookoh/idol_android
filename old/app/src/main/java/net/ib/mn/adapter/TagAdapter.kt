package net.ib.mn.adapter

import android.app.ActionBar
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.ItemFreeBoardTagBinding
import net.ib.mn.databinding.ItemTagBinding
import net.ib.mn.utils.Util

class TagAdapter(
    private val context: Context,
    private val items: List<TagModel>,
    private val onClickListener: OnClickListener,
) : RecyclerView.Adapter<TagAdapter.ViewHolder>() {

    private var currentTag: TagModel = TagModel()

    interface OnClickListener {
        fun onItemClicked(tag: TagModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val tagItemBinding: ItemFreeBoardTagBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_free_board_tag,
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
            holder.tvTag.setBackgroundResource(R.drawable.bg_free_board_hot)
        } else {
            holder.tvTag.setBackgroundResource(R.drawable.bg_small_talk_tag)
        }

        if (tag.selected) { //선택되어있는 태그일 경우
            currentTag = tag

            holder.tvTag.setTextColor(ContextCompat.getColor(context, R.color.text_white_black))
            holder.tvTag.isSelected = true
        } else { //선택되지 않은 태그일 경우
            holder.tvTag.setTextColor(ContextCompat.getColor(context, R.color.text_dimmed))
            holder.tvTag.isSelected = false
        }

        holder.tvTag.text = tag.name
        holder.llTag.setOnClickListener {
            currentTag = tag

            onClickTag(tag, position)
            onClickListener.onItemClicked(items[position])
        }

        val leftPadding = if (position == 0) Util.convertDpToPixel(context, 15f).toInt() else 0
        val rightPadding = if (position == itemCount - 1) Util.convertDpToPixel(context, 20f).toInt() else 0
        holder.itemView.setPadding(leftPadding, holder.itemView.paddingTop, rightPadding, holder.itemView.paddingBottom)
    }

    private fun onClickTag(tag: TagModel, position: Int) {
        for (i in items.indices) {
            items[i].selected = i == position
        }

        setSelctedTagIds()

        notifyDataSetChanged()
    }

    // 프로그래매틱하게 태그를 설정
    fun selectTag(tagId: Int) {
        var position = -1
        for (i in items.indices) {
            if (items[i].id == tagId) {
                position = i
                break
            }
        }
        // 잘못된 태그가 들어오는 경우 방지
        if(position in items.indices) {
            // 해당 태그가 존재하는 경우
            currentTag = items[position]
        } else {
            Log.e("TagAdapter", "Invalid tagId $tagId")
            return
        }

        onClickTag(currentTag, position)
    }

    fun getCurrentTag() = currentTag

    private fun setSelctedTagIds() {
        var selectedTagIds = ""

        for (i in items.indices) {
            if (items[i].selected) {
                selectedTagIds += "${items[i].id},"
            }
        }
        if (selectedTagIds.isNotEmpty()) {
            // 마지막 comma 지우기
            selectedTagIds = selectedTagIds.substring(0, selectedTagIds.length - 1)
        }
    }

    inner class ViewHolder(tagItemBinding: ItemFreeBoardTagBinding) :
        RecyclerView.ViewHolder(tagItemBinding.root) {
        val tvTag: AppCompatTextView = tagItemBinding.tvTag
        val llTag: LinearLayoutCompat = tagItemBinding.llTag // 태그 클릭 영역 넓게
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