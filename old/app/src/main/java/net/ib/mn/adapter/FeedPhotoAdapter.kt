package net.ib.mn.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.appcompat.widget.AppCompatImageView
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import net.ib.mn.R
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.databinding.ItemFeedPhotoBinding
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.RemoteFileModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.MediaExtension
import net.ib.mn.utils.RequestCode
import net.ib.mn.common.util.appendVersion
import net.ib.mn.view.ExodusImageView

@OptIn(UnstableApi::class)
class FeedPhotoAdapter(
        private val context: Context,
        private val glideRequestManager: RequestManager,
        private val imageSize: Int,
        private val feedPhotoList: ArrayList<ArticleModel>
) : RecyclerView.Adapter<FeedPhotoAdapter.ViewHolder>() {

    override fun getItemCount(): Int {
        return feedPhotoList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFeedPhotoBinding.inflate(LayoutInflater.from(context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val binding = holder.binding
        val photo = feedPhotoList[position]

        val imgUrl: String? = if (photo.umjjalUrl != null && Const.FEATURE_VIDEO) {
            photo.thumbnailUrl
        } else {
            photo.imageUrl?.appendVersion(photo.imageVer) ?: photo.thumbnailUrl
        }

        val adapterType =
            if (feedPhotoList[position].type == "M") NewCommentAdapter.TYPE_SMALL_TALK else NewCommentAdapter.TYPE_ARTICLE

        glideRequestManager.load(imgUrl)
                .override(imageSize, imageSize)
                .centerCrop()
                .error(R.color.text_white_black)
                .fallback(R.color.text_white_black)
                .placeholder(R.color.text_white_black)
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                        binding.eivPhoto.setOnClickListener {
                            (context as? FeedActivity)?.startActivityForResult(
                                    NewCommentActivity.createIntent(context, photo, holder.bindingAdapterPosition, true, adapterType = adapterType),
                                    RequestCode.ARTICLE_COMMENT.value)
                        }
                        return false
                    }

                    override fun onResourceReady(resource: Drawable, model: Any, target: Target<Drawable>, dataSource: DataSource, isFirstResource: Boolean): Boolean {
                        if (photo.umjjalUrl != null && Const.FEATURE_VIDEO) {
                            iconTypeVisibility(holder, photo)
                        } else {
                            binding.ivGif.visibility = View.GONE
                            binding.ivMp4.visibility = View.GONE
                        }

                        binding.eivPhoto.setOnClickListener {
                            (context as? FeedActivity)?.startActivityForResult(
                                    NewCommentActivity.createIntent(context, photo, holder.bindingAdapterPosition, true, adapterType = adapterType),
                                    RequestCode.ARTICLE_COMMENT.value)
                        }
                        return false
                    }

                })
                .into(binding.eivPhoto)
    }

    private fun iconTypeVisibility(holder: ViewHolder, photo: ArticleModel) = with(holder.binding) {
        if (photo.files.isNullOrEmpty()) {
            ivGif.visibility = View.VISIBLE
            ivMp4.visibility = View.GONE
            return@with
        }

        if (photo.files[0].originUrl?.endsWith(MediaExtension.MP4.value) == true) {
            ivGif.visibility = View.GONE
            ivMp4.visibility = View.VISIBLE
            return
        }

        ivGif.visibility = View.VISIBLE
        ivMp4.visibility = View.GONE
    }


    inner class ViewHolder(val binding: ItemFeedPhotoBinding) : RecyclerView.ViewHolder(binding.root) {
    }
}