package net.ib.mn.viewholder

import android.content.Context
import android.graphics.Point
import android.view.View
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import net.ib.mn.R
import net.ib.mn.databinding.ItemCommunityImgBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.common.util.appendVersion
import net.ib.mn.utils.modelToString

// 이미지 모아보기 했을 경우 나오는 ViewHolder
class CommunityImgViewHolder (
    val binding: ItemCommunityImgBinding,
    val articlePhotoListener: ArticlePhotoListener?,
): RecyclerView.ViewHolder(binding.root) {

    val glideRequestManager = Glide.with(itemView.context)

    fun bind(feedPhotoList: List<ArticleModel>) {

        val photo = listOf(binding.eivPhoto1, binding.eivPhoto2, binding.eivPhoto3)

        for(i in feedPhotoList.indices) {
            setImage(feedPhotoList[i], photo[i], i)
        }
        onClick(feedPhotoList)
    }

    private fun setImage(article: ArticleModel, photo: AppCompatImageView, position: Int ) {

        val imgUrl: String? = if (article.umjjalUrl != null && Const.FEATURE_VIDEO) {
            article.thumbnailUrl
        } else {
            article.imageUrl?.appendVersion(article.imageVer) ?: article.thumbnailUrl
        }

        glideRequestManager.load(imgUrl)
            .override(getImageSize(), getImageSize())
            .centerCrop()
            .error(R.color.text_white_black)
            .fallback(R.color.text_white_black)
            .placeholder(R.color.text_white_black)
            .into(photo)

        when(position) {
            0 -> {
                setImgCount(binding.inPhoto1.clImgCount, binding.inPhoto1.tvCount, article)
            }
            1 -> {
                setImgCount(binding.inPhoto2.clImgCount, binding.inPhoto2.tvCount, article)
            }
            2 -> {
                setImgCount(binding.inPhoto3.clImgCount, binding.inPhoto3.tvCount, article)
            }
        }
    }

    private fun setImgCount(constraint: ConstraintLayout, textView: AppCompatTextView, article: ArticleModel) {
        if(article.files.isNullOrEmpty() || article.files.size <=1) {
            constraint.visibility = View.GONE
            return
        }
        constraint.visibility = View.VISIBLE
        textView.text = article.files.size.toString()
    }

    private fun getImageSize(): Int {
        val wm = itemView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)
        return size.x / 3
    }

    private fun onClick(feedPhotoList: List<ArticleModel>) = with(binding) {
        eivPhoto1.setOnClickListener{
            articlePhotoListener?.widePhotoClick(feedPhotoList[0])

        }
        eivPhoto2.setOnClickListener{
            articlePhotoListener?.widePhotoClick(feedPhotoList[1])
        }
        eivPhoto3.setOnClickListener{
            articlePhotoListener?.widePhotoClick(feedPhotoList[2])
        }
    }
}