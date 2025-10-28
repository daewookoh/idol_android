/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.adapter

import android.content.Context
import android.util.SparseBooleanArray
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import dagger.hilt.android.UnstableApi
import net.ib.mn.R
import net.ib.mn.databinding.CommunityAllInDayBinding
import net.ib.mn.databinding.CommunityHeaderBinding
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.databinding.ItemCommunityImgBinding
import net.ib.mn.databinding.ItemCommunityWallpaperBinding
import net.ib.mn.databinding.NonDataBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.listener.CommunityArticleListener
import net.ib.mn.listener.ImgTypeClickListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.ReportRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.ItemBoardNoticeBinding
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.viewholder.CommunityAllInDayViewHolder
import net.ib.mn.viewholder.CommunityArticleViewHolder
import net.ib.mn.viewholder.CommunityHeaderViewHolder
import net.ib.mn.viewholder.CommunityNoticeViewHolder
import net.ib.mn.viewholder.CommunityWallpaperViewHolder
import net.ib.mn.viewholder.CommunityImgViewHolder
import net.ib.mn.viewholder.CommunityArticleZeroViewHolder

@OptIn(UnstableApi::class)
class CommunityAdapter(
    private val context: Context,
    private val baseFragment: BaseFragment,
    private val mIdol: IdolModel?,
    private var mOrderBy: String,
    private val mGlideRequestManager: RequestManager,
    private var noticeList: List<NoticeModel>,
    private var articleList: MutableList<ArticleModel>,
    private var reportRepository: ReportRepositoryImpl,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val usersRepository: UsersRepository,
    private val onNoticeClick: (NoticeModel) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var onlyPhotoArticleList: Array<List<ArticleModel>>    // Header 종류가 많아 어댑터 두개 만들어서 사용할 수 없어 이미지만 보여주는 2차원배열로 저장한 값 만듬
    private var communityArticleListener: CommunityArticleListener? = null
    private var imgTypeClickListener: ImgTypeClickListener? = null
    private var articlePhotoListener: ArticlePhotoListener? = null
    private var mType: TypeListModel? = null
    private var isBurningDay: Boolean = false
    private var imageOnly: Boolean = false
    private var wallpaperOnly: Boolean = false
    private val mapExpanded = SparseBooleanArray()

    fun setItemEventListener(communityArticleListener: CommunityArticleListener) {
        this.communityArticleListener = communityArticleListener
    }

    fun setPhotoClickListener(articlePhotoListener: ArticlePhotoListener) {
        this.articlePhotoListener = articlePhotoListener
    }

    fun setImgTypeClickListener(imgTypeClickListener: ImgTypeClickListener) {
        this.imgTypeClickListener = imgTypeClickListener
    }

    init {
        isBurningDay = !mIdol?.burningDay.isNullOrEmpty()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_FILTER -> {
                val communityHeader: CommunityHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.community_header,
                    parent,
                    false,
                )
                CommunityHeaderViewHolder(communityHeader, context, mIdol, communityArticleListener, imgTypeClickListener)
            }
            TYPE_NOTICE -> {
                val itemNotice: ItemBoardNoticeBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_board_notice,
                    parent,
                    false
                )
                CommunityNoticeViewHolder(itemNotice, context, onNoticeClick)
            }
            TYPE_ALL_IN -> {
                val communityAllInDay: CommunityAllInDayBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.community_all_in_day,
                    parent,
                    false,
                )
                CommunityAllInDayViewHolder(communityAllInDay, context)
            }
            TYPE_ARTICLE -> {
                val articleItem: CommunityItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.community_item,
                    parent,
                    false,
                )
                return CommunityArticleViewHolder(
                    binding = articleItem,
                    context = context,
                    mapExpanded = mapExpanded,
                    useTranslation = ConfigModel.getInstance(context).showTranslation,
                    fragment = baseFragment,
                    mType = mType,
                    mIdol = mIdol,
                    mGlideRequestManager = mGlideRequestManager,
                    communityArticleListener = communityArticleListener,
                    articlePhotoListener = articlePhotoListener,
                    reportRepository = reportRepository,
                    lifecycleScope = lifecycleScope,
                    usersRepository = usersRepository,
                )
            }
            TYPE_IMG -> {
                val itemSquare: ItemCommunityImgBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_community_img,
                    parent,
                    false,
                )
                CommunityImgViewHolder(itemSquare, articlePhotoListener)
            }
            TYPE_WALLPAPER -> {
                val itemWallPaper: ItemCommunityWallpaperBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_community_wallpaper,
                    parent,
                    false,
                )
                CommunityWallpaperViewHolder(itemWallPaper, articlePhotoListener)
            }
            else -> {
                val nonData: NonDataBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.non_data,
                    parent,
                    false
                )
                CommunityArticleZeroViewHolder(nonData)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position <= noticeList.size - 1) {
            return TYPE_NOTICE
        }

        if (isBurningDay) {
            if (position == noticeList.size) {
                return TYPE_ALL_IN
            } else if (position == noticeList.size + 1) {
                return TYPE_FILTER
            }
        } else {
            if (position == noticeList.size) {
                return TYPE_FILTER
            }
        }
        return if(imageOnly) {
            if(onlyPhotoArticleList.isEmpty()) {
                TYPE_WALLPAPER_ZERO
            } else if(wallpaperOnly){
                TYPE_WALLPAPER
            }
            else TYPE_IMG
        } else {
            if(articleList.isEmpty()) {
                TYPE_WALLPAPER_ZERO
            } else {
                TYPE_ARTICLE
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return position.hashCode().toLong()
    }

    override fun getItemCount(): Int {
        return getItemSize()
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_FILTER -> {
                (holder as CommunityHeaderViewHolder).apply {
                    bind(mOrderBy, imageOnly, wallpaperOnly, position)
                }
            }
            TYPE_NOTICE -> {
                (holder as CommunityNoticeViewHolder).apply {
                    bind(noticeList[position], position != noticeList.size - 1,)
                }
            }
            TYPE_ALL_IN -> {
                (holder as CommunityAllInDayViewHolder).apply {
                    bind(mIdol?.burningDay)
                }
            }
            TYPE_ARTICLE -> {
                (holder as CommunityArticleViewHolder).apply {
                    bind(
                        articleModel = articleList[position - getHeaderSize()],
                        position = position,
                        headerSize = getHeaderSize()
                    )
                }
            }
            TYPE_IMG -> {
                (holder as CommunityImgViewHolder).apply {
                    bind(onlyPhotoArticleList[position - getHeaderSize()])
                }
            }
            TYPE_WALLPAPER -> {
                (holder as CommunityWallpaperViewHolder).apply {
                    bind(onlyPhotoArticleList[position - getHeaderSize()])
                }
            }
            TYPE_WALLPAPER_ZERO -> {
                (holder as CommunityArticleZeroViewHolder).apply {
                    bind(wallpaperOnly)
                }
            }
        }
    }

    fun setItems(noticeItems: List<NoticeModel>, articleItems: MutableList<ArticleModel>) {
        noticeList = noticeItems
        articleList = articleItems
        onlyPhotoArticleList = articleList.chunked(3).toTypedArray()
        notifyDataSetChanged()
    }

    fun updateItem(article: ArticleModel) {
        val position = articleList.indexOf(articleList.find { it.id == article.id })
        articleList[position] = article
        notifyItemChanged(getHeaderSize() + position)
    }

    fun removeItem(article: ArticleModel) {
        val position = articleList.indexOf(articleList.find { it.id == article.id })
        articleList.removeAt(position)
        notifyDataSetChanged()
    }

    fun removeBlockItem(userId: Int) {
        articleList.removeAll { it.user?.id == userId }
        notifyDataSetChanged()
    }

    fun updateIdolModel(burning: String) {
        if (burning.isNotEmpty()) {
            isBurningDay = true
        }
        mIdol?.burningDay = burning
        notifyDataSetChanged() // 몰빵일 뷰홀더가 끼어들어가므로 전체 갱신해줘야 함
    }

    fun setOrderBy(mOrderBy: String) {
        this.mOrderBy = mOrderBy
    }

    fun setImageOnly(imageOnly: Boolean) {
        this.imageOnly = imageOnly
        if(imageOnly) {
            onlyPhotoArticleList = articleList.chunked(3).toTypedArray()
        }
    }

    fun setWallpaperOnly(wallpaperOnly: Boolean) {
        this.wallpaperOnly = wallpaperOnly
        if(wallpaperOnly) {
            onlyPhotoArticleList = articleList.chunked(3).toTypedArray()
        }
    }

    private fun getItemSize(): Int {
        var listCount = if (imageOnly) onlyPhotoArticleList.size else articleList.size
        if(listCount == 0) listCount = 1
        return getHeaderSize() + listCount
    }

    private fun getHeaderSize(): Int {
        return if (isBurningDay) noticeList.size + BURNING_DAY_COUNT + FILTER_COUNT else noticeList.size + FILTER_COUNT
    }

    companion object {
        const val TYPE_NOTICE = 0
        const val TYPE_ALL_IN = 1
        const val TYPE_FILTER = 2
        const val TYPE_ARTICLE = 3
        const val TYPE_IMG = 4
        const val TYPE_WALLPAPER = 5
        const val TYPE_WALLPAPER_ZERO = 6
        const val TYPE_YOUTUBE = 7

        const val FILTER_COUNT = 1
        const val BURNING_DAY_COUNT = 1
    }
}