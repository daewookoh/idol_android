package net.ib.mn.viewholder

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import net.ib.mn.databinding.ItemBoardBinding
import net.ib.mn.databinding.ItemBoardNoticeBinding
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.DateFormat
import java.util.Calendar

class BoardNoticeViewHolder(
    val binding: ItemBoardNoticeBinding,
    private val onItemClicked: (NoticeModel) -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        item: NoticeModel,
        isShowDivider: Boolean = true,
    ) = with(binding) {
        layoutContainer.setOnClickListener {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                onItemClicked(item)
            }
        }

        viewDivider.visibility =if (isShowDivider) {
            View.VISIBLE
        } else {
            View.GONE
        }

        tvTitle.text = item.title
    }
}