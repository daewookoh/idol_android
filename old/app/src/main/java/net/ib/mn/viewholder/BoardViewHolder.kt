package net.ib.mn.viewholder

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import net.ib.mn.databinding.ItemBoardBinding
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.DateUtil
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.DateFormat
import java.util.Calendar

class BoardViewHolder(
    val binding: ItemBoardBinding,
    val now: Calendar,
    private val onItemClicked: (ArticleModel, Int) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(item: ArticleModel) = with(binding) {
        layoutContainer.setOnClickListener {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClicked(item, position)
            }
        }

        tvTitle.text = item.title
        tvName.text = item.user?.nickname

        tvContent.apply {
            text = item.content
            visibility = if (item.content.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        val context = itemView.context
        val locale = LocaleUtil.getAppLocale(context)

        tvDate.text = DateUtil.formatCreatedAtRelativeToNow(
            now = now,
            created = Calendar.getInstance(),
            articleCreatedAt = item.createdAt,
            locale = locale,
        )

        tvLike.text = UtilK.countWithLocale(context, item.likeCount)
        tvComment.text = UtilK.countWithLocale(context, item.commentCount)
        tvViewer.text = UtilK.countWithLocale(context, item.viewCount)

        binding.ivPopular.visibility = if (item.isPopular) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.ivMostOnly.visibility = if (item.isMostOnly == "Y") {
            View.VISIBLE
        } else {
            View.GONE
        }

        if (!item.thumbnailUrl.isNullOrEmpty()) {
            val userId = item.user?.id ?: 0
            ivThumb.visibility = View.VISIBLE
            Glide.with(itemView.context)
                .load(item.thumbnailUrl)
                .transform(
                    CenterCrop(),
                    RoundedCorners(Util.convertDpToPixel(itemView.context, 12f).toInt()),
                )
                .error(Util.noProfileImage(userId))
                .fallback(Util.noProfileImage(userId))
                .placeholder(Util.noProfileImage(userId))
                .dontAnimate()
                .into(ivThumb)

            tvImageCount.apply {
                val count = item.files?.size ?: 0
                if (count > 1) {
                    text = "+${count - 1}"
                    visibility = View.VISIBLE
                } else {
                    visibility = View.GONE
                }
            }
        } else {
            ivThumb.visibility = View.GONE
            tvImageCount.visibility = View.GONE
        }
    }
}