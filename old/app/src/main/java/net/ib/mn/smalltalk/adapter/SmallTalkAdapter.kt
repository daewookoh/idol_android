/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 잡담 게시판 리스트 어댑터입니다.
 *
 * */

package net.ib.mn.smalltalk.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.ItemBoardNoticeBinding
import net.ib.mn.databinding.ItemEmptyViewBinding
import net.ib.mn.databinding.ItemLoadingBinding
import net.ib.mn.databinding.ItemSmallTalkBinding
import net.ib.mn.databinding.ItemSmallTalkHeaderBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.NoticeModel
import net.ib.mn.smalltalk.viewholder.EmptyVH
import net.ib.mn.smalltalk.viewholder.LoadingVH
import net.ib.mn.smalltalk.viewholder.SmallTalkHeaderVH
import net.ib.mn.smalltalk.viewholder.SmallTalkVH
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.viewholder.BoardNoticeViewHolder
import java.util.Calendar

class SmallTalkAdapter(
    private val context: Context,
    private val orderBy: String,
    private val locale: String?
) : ListAdapter<ArticleModel, RecyclerView.ViewHolder>(diffUtil) {

    private val calendar = Calendar.getInstance()
    private val appLocale = LocaleUtil.getAppLocale(context)
    private var noticeCount = 0

    private var itemListener: ItemListener? = null

    interface ItemListener {
        fun filterSetCallBack(bottomSheetFragment: BottomSheetFragment)
        fun smallTalkItemClicked(articleModel: ArticleModel, position: Int, isTutorial: Boolean = false)
        fun filterLocaleCallback(bottomSheetFragment: BottomSheetFragment)
        fun filterClickCallBack(orderBy: String, keyWord: String?, locale: String?, orderByText: String)
        fun localeClickCallBack(orderBy: String, keyWord: String?, locale: String?, langText: String)
        fun searchCallBack(orderBy: String, keyWord: String?, locale: String?)
        fun noticeClickCallback(noticeModel: NoticeModel)
    }

    fun setItemEventListener(itemListener: ItemListener) {
        this.itemListener = itemListener
    }

    fun setNoticeCount(count: Int) {
        this.noticeCount = count
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val itemSmallTalkHeader: ItemSmallTalkHeaderBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_small_talk_header,
                parent,
                false
            )

        val itemSmallTalk: ItemSmallTalkBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_small_talk,
            parent,
            false
        )

        val itemEmpty: ItemEmptyViewBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context),
            R.layout.item_empty_view,
            parent,
            false
        )

        val itemLoading: ItemLoadingBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_loading,
                parent,
                false
            )

        val itemNotice: ItemBoardNoticeBinding =
            DataBindingUtil.inflate(
                LayoutInflater.from(parent.context),
                R.layout.item_board_notice,
                parent,
                false
            )

        return when (viewType) {
            SMALL_TALK_HEADER -> {
                SmallTalkHeaderVH(itemSmallTalkHeader, orderBy, locale, {
                    itemListener?.filterSetCallBack(it)
                }, {
                    itemListener?.filterLocaleCallback(it)
                }, { orderBy, keyWord, locale, orderByText ->
                    itemListener?.filterClickCallBack(orderBy, keyWord, locale, orderByText)
                }, { orderBy, keyWord, locale ->
                    itemListener?.searchCallBack(orderBy, keyWord, locale)
                }, { orderBy, keyWord, locale, langText ->
                    itemListener?.localeClickCallBack(orderBy, keyWord, locale, langText)
                })
            }
            SMALL_TALK_ITEM -> {
                SmallTalkVH(itemSmallTalk, calendar, appLocale)
            }
            EMPTY_ITEM -> {
                EmptyVH(itemEmpty)
            }
            NOTICE_ITEM -> {
                BoardNoticeViewHolder(itemNotice) { noticeItem ->
                    itemListener?.noticeClickCallback(noticeItem)
                }
            }
            else -> {
                LoadingVH(itemLoading)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            SMALL_TALK_HEADER -> {
                (holder as SmallTalkHeaderVH).apply {
                    bind(currentList[bindingAdapterPosition])
                    binding.executePendingBindings()
                }
            }
            EMPTY_ITEM -> {
                (holder as EmptyVH)
            }
            SMALL_TALK_ITEM -> {
                (holder as SmallTalkVH).apply {
                    bind(articleModel = currentList[bindingAdapterPosition],
                        isFirstItem = bindingAdapterPosition == noticeCount + 1) { isTutorial ->
                        itemListener?.smallTalkItemClicked(
                            currentList[bindingAdapterPosition],
                            bindingAdapterPosition,
                            isTutorial
                        )
                    }
                    binding.executePendingBindings()
                }
            }
            NOTICE_ITEM -> {
                (holder as BoardNoticeViewHolder).apply {
                    val item = currentList[bindingAdapterPosition]
                    val noticeItem = NoticeModel()
                    noticeItem.id = item.id
                    noticeItem.title = item.title
                    bind(noticeItem)
                }
            }
            else -> {
                (holder as LoadingVH)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val item = currentList[position]

        return when {
            item.isLoading -> LOADING_ITEM

            position == 0 -> SMALL_TALK_HEADER

            item.id == EMPTY_ITEM.toString() -> EMPTY_ITEM

            position in 1..noticeCount -> NOTICE_ITEM  // ✅ 헤더 다음부터 noticeCount개까지

            else -> SMALL_TALK_ITEM
        }
    }

    //로딩 지워줌.
    fun deleteLoading() {
        try {
            if (currentList[currentList.lastIndex].isLoading) {
                val lastIndex = currentList.lastIndex
                val newList = currentList.toMutableList()
                newList.removeAt(lastIndex)// 로딩이 완료되면 프로그레스바를 지움
                submitList(newList.map { it.clone() as ArticleModel })
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].hashCode().toLong()
    }

    companion object {

        const val EMPTY_ITEM = -2
        const val SMALL_TALK_HEADER = -1
        const val LOADING_ITEM = -3
        const val SMALL_TALK_ITEM = 1
        const val NOTICE_ITEM = 2

        val diffUtil = object : DiffUtil.ItemCallback<ArticleModel>() {
            override fun areItemsTheSame(oldItem: ArticleModel, newItem: ArticleModel): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: ArticleModel, newItem: ArticleModel): Boolean {
                return (oldItem.id == newItem.id && oldItem.isEdit == newItem.isEdit &&
                    // 좋아요/댓글수/조회수 변경되면 다시 그려줌
                    oldItem.commentCount == newItem.commentCount &&
                    oldItem.likeCount == newItem.likeCount &&
                    oldItem.viewCount == newItem.viewCount)
            }
        }
    }
}