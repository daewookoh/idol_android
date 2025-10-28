/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 잡담 상세화면 헤더뷰.
 *
 * */

package net.ib.mn.smalltalk.viewholder

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.net.Uri
import android.text.method.LinkMovementMethod
import android.view.View
import android.widget.RelativeLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.applovin.sdk.AppLovinSdkUtils
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.google.gson.reflect.TypeToken
import net.ib.mn.R
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.addon.InternetConnectivityManager
import net.ib.mn.databinding.SmallTalkCommentHeaderBinding
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.Const
import com.bumptech.glide.Glide
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.YouTubePlayerCallback
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.common.util.appendVersion
import net.ib.mn.core.model.TagModel
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.utils.DateUtil
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.YoutubeHelper
import net.ib.mn.utils.setAllOnClickListener
import net.ib.mn.utils.vote.TranslateUiHelper
import net.ib.mn.view.ExodusImageView
import java.net.MalformedURLException
import java.net.URL
import java.text.DateFormat
import java.util.*

class SmallTalkCommentHeaderViewHolder(
    val binding: SmallTalkCommentHeaderBinding,
    private val useTranslation: Boolean,
    private val mGlideRequestManager: RequestManager,
    private val tagName: String?,
    private val now: Calendar,
    private val locale: Locale,
    private var getVideoPlayView: NewCommentAdapter.GetVideoPlayView?,
    val onArticleItemClickListener: NewCommentAdapter.OnArticleItemClickListener?
) : RecyclerView.ViewHolder(binding.root) {
    private val systemLanguage = Util.getSystemLanguage(itemView.context)

    fun bind(articleModel: ArticleModel) {
        articleModel.isUserLikeCache = articleModel.isUserLike

        val isFreeBoard = articleModel.idol?.getId() == Const.IDOL_ID_FREEBOARD

        if (isFreeBoard || articleModel.type == "M") {
            val tag = tagName?.ifEmpty {
                if (isFreeBoard) {
                    val gson = IdolGson.getInstance()
                    val listType = object : TypeToken<List<TagModel>>() {}.type
                    val tags: List<TagModel> = gson.fromJson(Util.getPreference(itemView.context, Const.BOARD_TAGS), listType)
                    tags.find { it.id == articleModel.tagId }?.name ?: ""
                } else {
                    articleModel.idol?.getName(itemView.context)
                }
            }

            binding.tvSmallTalkHeaderTag.apply{
                visibility = View.VISIBLE
                text = tag
            }

            if (articleModel.title.isNullOrEmpty()) {
                binding.tvSmallTalkHeaderTitle.visibility = View.GONE
            } else {
                binding.tvSmallTalkHeaderTitle.apply {
                    visibility = View.VISIBLE
                    text = articleModel.title
                }
            }

            binding.ivPopular.visibility = if (articleModel.isPopular) {
                View.VISIBLE
            } else {
                View.GONE
            }

            binding.clSmallTalkLike.visibility = View.VISIBLE
        } else {
            binding.clSmallTalkLike.visibility = View.GONE
        }

        setLikeIcon(articleModel.likeCount, articleModel.isUserLike)
        onClick(articleModel)

        val userId = articleModel.user?.id ?: 0
        with(binding) {
            Glide.with(itemView.context)
                .load(articleModel.user?.imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(userId))
                .fallback(Util.noProfileImage(userId))
                .placeholder(Util.noProfileImage(userId))
                .into(eivSmallTalkHeaderPhoto)

            ivSmallTalkHeaderLevel.setImageDrawable(
                Util.getLevelImageDrawable(
                    itemView.context,
                    articleModel.user?.level ?: 0,
                ),
            )

            tvSmallTalkHeaderUsername.text = articleModel.user?.nickname

            binding.ivPopular.visibility = if (articleModel.isPopular) {
                View.VISIBLE
            } else {
                View.GONE
            }

            tvSmallTalkHeaderDate.text = DateUtil.formatCreatedAtRelativeToNow(
                now = now,
                created = Calendar.getInstance(),
                articleCreatedAt = articleModel.createdAt,
                locale = locale,
            )

            tvSmallTalkHeaderTitle.text = articleModel.title

            if (articleModel.isMostOnly == "Y") {
                ivSmallTalkMostOnly.visibility = View.VISIBLE
            } else {
                ivSmallTalkMostOnly.visibility = View.GONE
            }

            tvSmallTalkHeaderComment.text = articleModel.commentCount.toString()

            if (articleModel.content.isNullOrEmpty()) {
                stvSmallTalkHeaderContent.visibility = View.GONE
            } else {
                stvSmallTalkHeaderContent.visibility = View.VISIBLE
                stvSmallTalkHeaderContent.text =
                    Util.convertHashTags(itemView.context, articleModel.content)
                stvSmallTalkHeaderContent.movementMethod = LinkMovementMethod.getInstance()
                llPreviewInfo.setOnClickListener {
                    try {
                        val mIntent = Intent(itemView.context, AppLinkActivity::class.java).apply {
                            data = Uri.parse(articleModel.linkUrl)
                        }
                        itemView.context.startActivity(mIntent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }

            setLinkOrImgData(articleModel, this)

            // 번역
            TranslateUiHelper.bindTranslateButton(
                context = itemView.context,
                view = viewTranslate,
                content = (articleModel.title ?: "") + articleModel.content, // 덕게는 제목도 번역
                systemLanguage = systemLanguage,
                nation = articleModel.nation,
                translateState = articleModel.translateState,
                isTranslatableCached = articleModel.isTranslatable,
                useTranslation = useTranslation,
            ).also { canTranslate ->
                if(articleModel.isTranslatable == null) {
                    articleModel.isTranslatable = canTranslate
                }
                viewTranslate.setOnClickListener {
                    (itemView.context as? NewCommentActivity)?.clickTranslate(articleModel)
                }
            }

            tvSmallTalkHeaderViewCount.text = articleModel.viewCount.toString()

            // youtube
            if(YoutubeHelper.hasYoutubeLink(articleModel) && !articleModel.linkTitle.isNullOrEmpty() && articleModel.linkTitle != "None") {
                youtubePlayerView.visibility = View.VISIBLE
                llPreviewInfo.visibility = View.GONE

                val linkUrl = articleModel.linkUrl ?: return
                val videoId = YoutubeHelper.extractYoutubeVideoId(linkUrl) ?: return
                val startTime = YoutubeHelper.extractYoutubeVideoStartTime(linkUrl)
                youtubePlayerView.getYouTubePlayerWhenReady(object: YouTubePlayerCallback {
                    override fun onYouTubePlayer(youTubePlayer: YouTubePlayer) {
                        youTubePlayer.cueVideo(videoId, startTime.toFloat())
                    }
                })

            }
        }
    }

    private fun setLinkOrImgData(
        articleModel: ArticleModel,
        binding: SmallTalkCommentHeaderBinding,
    ) {
        with(binding) {
            // 링크가 있다면
            if (!articleModel.linkTitle.isNullOrEmpty() &&
                articleModel.linkTitle != "None"
            ) {
                llPreviewInfo.visibility = View.VISIBLE
                attachFrame.visibility = View.GONE
                tvPreviewTitle.text = articleModel.linkTitle

                // articleModel.files가 존재하고, originUrl이 있다면
                if (!articleModel.files.isNullOrEmpty() && !articleModel.files[0].originUrl.isNullOrEmpty()) {
                    ivPreviewImage.visibility = View.VISIBLE
                    ivPreviewImage.post {
                        mGlideRequestManager
                            .load(articleModel.files[0].originUrl?.appendVersion(articleModel.imageVer))
                            .into(ivPreviewImage)
                    }
                } else {
                    ivPreviewImage.visibility = View.GONE
                    mGlideRequestManager
                        .clear(ivPreviewImage)
                }
                if (!articleModel.linkDesc.isNullOrEmpty() &&
                    articleModel.linkDesc != "None"
                ) {
                    tvPreviewDescription.text = articleModel.linkDesc
                    tvPreviewDescription.visibility = View.VISIBLE
                } else {
                    tvPreviewDescription.visibility = View.GONE
                }
                if (!articleModel.linkUrl.isNullOrEmpty() &&
                    articleModel.linkUrl != "None"
                ) {
                    try {
                        val ogUrl = URL(articleModel.linkUrl)
                        val baseUrl = ogUrl.protocol + "://" + ogUrl.host
                        tvPreviewHost.text = baseUrl
                    } catch (e: MalformedURLException) {
                        tvPreviewHost.visibility = View.GONE
                    }
                } else {
                    tvPreviewHost.visibility = View.GONE
                }
                return@with
            }

            // 링크가 없다면
            // files가 존재하지 않거나, OriginUrl이 없다면
            if (articleModel.files.isNullOrEmpty() || articleModel.files[0].originUrl.isNullOrEmpty()) {
                llPreviewInfo.visibility = View.GONE
                attachFrame.visibility = View.GONE
                return@with
            }

            // files 존재할때.
            llPreviewInfo.visibility = View.GONE
            attachFrame.visibility = View.VISIBLE

            // 가로세로 길이 고정
            val lp = attachFrame.layoutParams as ConstraintLayout.LayoutParams
            lp.height = Util.getDeviceWidth(itemView.context)
            attachFrame.layoutParams = lp

            // 움짤이 아니라면
            if (articleModel.files[0].umjjalUrl.isNullOrEmpty()) {
                eivAttachPhoto.post {
                    mGlideRequestManager
                        .load(articleModel.files[0].originUrl?.appendVersion(articleModel.imageVer))
                        .disallowHardwareConfig()
                        .into(eivAttachPhoto)
                }
                return@with
            }

            // 움짤이면 썸네일이 존재해야함.
            if (articleModel.files[0].thumbnailUrl.isNullOrEmpty()) {
                return@with
            }

            // 움짤이라면
            // 와이파이 켜져있을 경우, 데이터절약모드 상관없이 false, wifi 꺼져있을 경우 데이터절약모드 상태에 따라 처리
            if (Util.getPreferenceBool(itemView.context, Const.PREF_DATA_SAVING, false) &&
                !InternetConnectivityManager.getInstance(itemView.context).isWifiConnected
            ) {
                eivAttachPhoto.setImageBitmap(null)
                eivAttachPhoto.setLoadInfo(
                    R.id.TAG_THUMBNAIL_URL,
                    articleModel.files[0].thumbnailUrl!!,
                )
                eivAttachPhoto.visibility = View.VISIBLE

                setThumbnailUrl(eivAttachPhoto, ivGif, articleModel)

                return@with
            }

            setThumbnailUrl(eivAttachPhoto, ivGif, articleModel)

            getVideoPlayView?.getExoVideoPlayView(
                attachExoplayerView,
                eivAttachPhoto,
                ivGif,
                articleModel.files[0].thumbnailUrl,
            )
        }
    }

    private fun setThumbnailUrl(photoView: ExodusImageView, gifView: View, articleModel: ArticleModel) {
        mGlideRequestManager.asBitmap()
            .load(articleModel.files[0].thumbnailUrl)
            .listener(object : RequestListener<Bitmap> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Bitmap>,
                    isFirstResource: Boolean,
                ): Boolean {
                    gifView.visibility = View.GONE
                    return false
                }

                override fun onResourceReady(
                    resource: Bitmap,
                    model: Any,
                    target: Target<Bitmap>,
                    dataSource: DataSource,
                    isFirstResource: Boolean,
                ): Boolean {
                    AppLovinSdkUtils.runOnUiThread {
                        // 네트워크가 느린 경우 뒤늦게 썸네일이 로드되는 경우 처리
                        photoView.setImageBitmap(resource)
                        photoView.post {
                            setGifPosition(
                                photoView = photoView,
                                gifView = gifView,
                                resource = resource,
                            )
                        }
                    }
                    return false
                }
            }).submit()
    }

    private fun setGifPosition(photoView: View, gifView: View, resource: Bitmap) {
        // gif icon 표시
        var width = resource.width
        var height = resource.height
        val ratio = height.toDouble() / width.toDouble()
        val dispWidth: Int =
            photoView.width
        val dispHeight: Int =
            photoView.height
        if (ratio < 1.0) { // 가로로 긴 움짤
            width = dispWidth
            height = (dispHeight * ratio).toInt()
        } else {
            width = (dispWidth / ratio).toInt()
            height = dispHeight
        }
        val lp =
            gifView.layoutParams as RelativeLayout.LayoutParams
        lp.rightMargin =
            (
                Util.convertDpToPixel(
                    itemView.context,
                    10f,
                )
                    .toInt() +
                    (dispWidth - width) / 2
                )
        lp.bottomMargin =
            (
                Util.convertDpToPixel(
                    itemView.context,
                    10f,
                )
                    .toInt() +
                    (dispHeight - height) / 2
                )
        gifView.layoutParams = lp
        gifView.visibility = View.VISIBLE
    }

    private fun onClick(articleModel: ArticleModel) {
        with(binding){
            clSmallTalkLike.setOnClickListener {
                if (articleModel.isUserLikeCache) {
                    articleModel.likeCount--
                } else {
                    articleModel.likeCount++
                }
                articleModel.isUserLikeCache = !articleModel.isUserLikeCache
                setLikeIcon(articleModel.likeCount, articleModel.isUserLikeCache)
                onArticleItemClickListener?.onArticleLikeClicked(articleModel)
            }

            clComment.setOnClickListener {
                if(articleModel.commentCount > 0) {
                    return@setOnClickListener
                }
                onArticleItemClickListener?.onCommentShowClicked()
            }
        }
    }

    private fun setLikeIcon(articleLikeCount: Int, isUserLike: Boolean) {
        with(binding) {
            if(isUserLike) {
                ivSmallTalkHeaderLike.apply {
                    setImageResource(R.drawable.icon_board_like_active)
                    colorFilter = null
                }
            } else {
                ivSmallTalkHeaderLike.apply {
                    setImageResource(R.drawable.icon_board_like)
                    setColorFilter(ContextCompat.getColor(context, R.color.text_default), PorterDuff.Mode.SRC_IN)
                }
            }
            tvSmallTalkHeaderLike.text = articleLikeCount.toString()
        }
    }
}