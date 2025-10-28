package feature.common.exodusimagepicker.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import feature.common.exodusimagepicker.R
import feature.common.exodusimagepicker.databinding.ItemThumbnailBinding
import feature.common.exodusimagepicker.model.Thumbnail
import feature.common.exodusimagepicker.viewholder.ThumbnailVH

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 썸네일 리스트가  뿌려지는  리사이클러뷰 어뎁터이다.
 *
 * @see
 * */
class ThumbNailRvAdapter : ListAdapter<Thumbnail, RecyclerView.ViewHolder>(diffUtil) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemThumbnail: ItemThumbnailBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_thumbnail,
                parent,
                false,
            )
        return ThumbnailVH(itemThumbnail)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as ThumbnailVH).apply {
            bind(currentList[bindingAdapterPosition], position)
            binding.executePendingBindings()
        }
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<Thumbnail>() {
            override fun areItemsTheSame(
                oldItem: Thumbnail,
                newItem: Thumbnail,
            ): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(
                oldItem: Thumbnail,
                newItem: Thumbnail,
            ): Boolean {
                return oldItem.bitmap.hashCode() == newItem.bitmap.hashCode()
            }
        }
    }
}