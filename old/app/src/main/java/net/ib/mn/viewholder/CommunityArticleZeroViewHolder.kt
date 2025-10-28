package net.ib.mn.viewholder

import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.NonDataBinding

class CommunityArticleZeroViewHolder(
    val binding: NonDataBinding,
): RecyclerView.ViewHolder(binding.root) {
    fun bind(isWallpaper: Boolean) = with(binding) {
        tvNonItem.text = if(isWallpaper) {
            itemView.context.getString(R.string.msg_empty_background_image)
        } else {
            itemView.context.getString(R.string.no_search_result_community)
        }
    }
}