/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.CommunityItemYoutubeBinding
import net.ib.mn.databinding.CommunityVpItemBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.utils.YoutubeHelper
import net.ib.mn.viewholder.CommunityArticlePagerViewHolder
import net.ib.mn.viewholder.CommunityArticleYoutubeViewHolder

@UnstableApi
class CommunityArticlePagerAdapter(
    private val context: Context,
    private val articleModel: ArticleModel,
    private val articlePhotoListener: ArticlePhotoListener?,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val lifecycle: Lifecycle?,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        when(viewType) {
            VIEW_TYPE_YOUTUBE -> {
                val binding: CommunityItemYoutubeBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.community_item_youtube,
                    parent,
                    false,
                )

                return CommunityArticleYoutubeViewHolder(binding, context, lifecycle)
            }
            else -> {
                val binding: CommunityVpItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.community_vp_item,
                    parent,
                    false,
                )

                return CommunityArticlePagerViewHolder(binding, context, articlePhotoListener, lifecycleScope)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.hashCode().toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return if( YoutubeHelper.hasYoutubeLink(articleModel)) {
            VIEW_TYPE_YOUTUBE
        } else {
            VIEW_TYPE_COMMON
        }
    }

    override fun getItemCount(): Int = if (articleModel.files.isNullOrEmpty()) 1 else articleModel.files.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when(holder.itemViewType) {
            VIEW_TYPE_YOUTUBE -> {
                (holder as CommunityArticleYoutubeViewHolder).apply {
                    bind(articleModel, position)
                }
            }
            else -> {
                (holder as CommunityArticlePagerViewHolder).apply {
                    bind(articleModel, position)
                }
            }
        }
    }

    override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
        when(holder.itemViewType) {
            VIEW_TYPE_COMMON -> {
                (holder as CommunityArticlePagerViewHolder).removeListener()
            }
            else -> {
                // do nothing
            }
        }
    }

    companion object {
        const val VIEW_TYPE_COMMON = 1
        const val VIEW_TYPE_YOUTUBE= 2
    }
}