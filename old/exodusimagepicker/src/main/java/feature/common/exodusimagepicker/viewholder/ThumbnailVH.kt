package feature.common.exodusimagepicker.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import feature.common.exodusimagepicker.databinding.ItemThumbnailBinding
import feature.common.exodusimagepicker.model.Thumbnail

/**
 * Create Date: 2023/12/04
 *
 * @author jungSangMin
 * Description: 썸네일이 담길  뷰홀더
 *
 * @see
 * */
class ThumbnailVH(
    val binding: ItemThumbnailBinding,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(thumbnailModel: Thumbnail, position: Int) {
        Glide.with(itemView.context)
            .load(thumbnailModel.bitmap)
            .into(binding.ivThumbnail)
    }
}