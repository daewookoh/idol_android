/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: 공통 아티클 뷰홀더.
 *
 * */

package net.ib.mn.base

import android.content.Context
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import dagger.hilt.android.UnstableApi
import net.ib.mn.adapter.CommunityArticlePagerAdapter
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.listener.CommunityArticleListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.model.RemoteFileModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.viewholder.CommunityArticlePagerViewHolder
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Locale

/**
 * @see
 * */

@UnstableApi
open class BaseArticleViewHolder(
    private val binding: CommunityItemBinding,
    private val mGlideRequestManager: RequestManager,
    private val communityArticleListener: CommunityArticleListener?,
    private val context: Context,
    private val articlePhotoListener: ArticlePhotoListener?,
    private val lifecycleScope: LifecycleCoroutineScope,
) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var communityArticlePagerAdapter: CommunityArticlePagerAdapter
    private var itemPosition = 0

    open fun bind(
        articleModel: ArticleModel,
        idolModel: IdolModel = IdolModel(),
        position: Int = 0,
        headerSize :Int = 0,
        isViewCountVisible: Boolean = true,
    ) {
        setUserSection(articleModel)
        setContent(articleModel)
        setArticleBottomButton(articleModel)
        setViewPager(articleModel)
    }

    protected fun stopRestOfPlayer(
        viewPagerRootView: RecyclerView,
        excludePosition: Int = 0,
        totalSize: Int = 0,
    ) {
        for (i in 0 until totalSize) {
            if (i == excludePosition) continue
            val viewHolder = viewPagerRootView.findViewHolderForAdapterPosition(i) ?: continue
            val itemView = viewHolder.itemView
            (viewHolder as? CommunityArticlePagerViewHolder)?.cleanUp()
        }
    }

    protected fun playSelectedPlayer(
        viewPagerRootView: RecyclerView,
        selectedPosition: Int,
        adapter: CommunityArticlePagerAdapter,
    ) {
        val communityArticlePagerViewHolder = (viewPagerRootView.findViewHolderForAdapterPosition(selectedPosition) as? CommunityArticlePagerViewHolder) ?: return
        communityArticlePagerViewHolder.initExoPlayer()
    }

    private fun setReOrganizeArticleModel(articleModel: ArticleModel) {
        // 옛날에 올린 경우, files가 없어서 files 생성해서 넘기도록 처리
        if (articleModel.files.isNullOrEmpty()) {
            val thumbnail = if (articleModel.thumbnailUrl.isNullOrEmpty()) {
                articleModel.imageUrl
            } else {
                articleModel.thumbnailUrl
            }
            articleModel.files = mutableListOf(RemoteFileModel(null, thumbnail, articleModel.umjjalUrl))
        }

        articleModel.files.sortBy { it.seq }
    }

    private fun setContent(articleModel: ArticleModel) = with(binding) {
        if (articleModel.content.isNullOrEmpty()) {
            content.visibility = View.GONE
        } else {
            content.visibility = View.VISIBLE
            content.text = Util.convertHashTags(itemView.context, articleModel.content)
            content.movementMethod = LinkMovementMethod.getInstance()
        }
    }

    private fun setArticleBottomButton(articleModel: ArticleModel) = with(binding) {
        viewCount.text = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
            .format(articleModel.viewCount)
        heartCount.text = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
            .format(articleModel.heart.toLong())
        commentCount.text = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
            .format(articleModel.commentCount)
    }

    private fun setUserSection(articleModel: ArticleModel) = with(binding) {
        name.text = "\u200E" + articleModel.user?.nickname

        level.setImageBitmap(Util.getLevelImage(itemView.context, articleModel.user))

        // 게시글 작성날짜 세팅
        val createAt = articleModel.createdAt
        if (createAt == null) {
            createdAt.text = ""
        } else {
            val f = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                LocaleUtil.getAppLocale(itemView.context),
            )
            val dateString = f.format(createAt)
            createdAt.text = dateString
        }

        // /emotiocon 관련 값이 있으면 emoticon 이미지뷰에 값을 set해준다.
        // 이모티콘
        if (articleModel.user?.emoticon?.emojiUrl != null) {
            emoticon.visibility = View.VISIBLE
            emoticon.let { mGlideRequestManager.load(articleModel.user?.emoticon?.emojiUrl).into(it) }
        } else {
            emoticon.visibility = View.GONE
            emoticon.let { mGlideRequestManager.clear(it) }
        }
    }

    open fun setViewPager(articleModel: ArticleModel) = with(binding) {
        setReOrganizeArticleModel(articleModel)
        communityArticlePagerAdapter = CommunityArticlePagerAdapter(context, articleModel, articlePhotoListener, lifecycleScope, lifecycleOwner?.lifecycle)
        vpCommunity.apply {
            adapter = communityArticlePagerAdapter
            orientation = ViewPager2.ORIENTATION_HORIZONTAL
        }

        val viewPagerRootView = vpCommunity.getChildAt(0) as RecyclerView
        val communityArticlePagerAdapter =
            vpCommunity.adapter as CommunityArticlePagerAdapter

        if (articleModel.files.size >= 2) {
            val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
            val fileCount = numberFormat.format(articleModel.files.size)

            clPhotoIdx.visibility = View.VISIBLE
            tvPhotoIdx.text = numberFormat.format(itemPosition + 1).plus("/").plus(fileCount)
        } else {
            binding.clPhotoIdx.visibility = View.GONE
            return@with
        }

        vpCommunity.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                val fileCount = numberFormat.format(articleModel.files.size)
                itemPosition = position
                tvPhotoIdx.text =
                    numberFormat.format(itemPosition + 1).plus("/").plus(fileCount)

                stopRestOfPlayer(
                    viewPagerRootView = viewPagerRootView,
                    excludePosition = position,
                    totalSize = articleModel.files.size,
                )
                playSelectedPlayer(
                    viewPagerRootView = viewPagerRootView,
                    selectedPosition = position,
                    communityArticlePagerAdapter,
                )
            }
        })
    }
}