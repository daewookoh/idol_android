package net.ib.mn.feature.generic

import android.animation.ValueAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import net.ib.mn.BuildConfig
import net.ib.mn.databinding.RankingItemBinding
import net.ib.mn.databinding.TextureRankingItemBinding
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.RankingBindingProxy
import net.ib.mn.utils.Util
import net.ib.mn.utils.setMargins

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: chartId 사용하는 실시간 랭킹 화면 아이템 어댑터
 *
 * */

class GenericRankingAdapter (
    private var items: ArrayList<IdolModel>,
    private val onClickListener: GenericRankingClickListener,
    private val mGlideRequestManager: RequestManager,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var mMaxVoteCount: Long = 0L
    private val voteMap = HashMap<Int, Long>()

    // 투표수 애니메이션 pool
    private val animatorPool = HashMap<Int, ValueAnimator?>()

    init {
        setMaxVoteCount()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val viewHolder = when (viewType) {
            TYPE_RANK -> {
                if (Util.isOSNougat()) {
                    val binding = RankingItemBinding.inflate(inflater, parent, false)
                    GenericRankViewHolder(RankingBindingProxy(binding), mGlideRequestManager)
                } else {
                    val binding = TextureRankingItemBinding.inflate(inflater, parent, false)
                    GenericRankViewHolder(RankingBindingProxy(binding), mGlideRequestManager)
                }
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        val animator = animatorPool[holder.itemView.hashCode()]
        animator?.apply {
            cancel()
            removeAllUpdateListeners()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (items.isEmpty()) return

        when (holder) {
            is GenericRankViewHolder -> {
                val item = items[position]
                holder.bind(item, mMaxVoteCount, voteMap, animatorPool, onClickListener)

                if (!BuildConfig.CELEB) {
                    val margin = if (position == items.size - 1) {
                        9f
                    } else {
                        0f
                    }
                    holder.itemView.setMargins(bottom = margin)
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return if (items.isNotEmpty() && position < items.size) {
            items[position].getId().toLong()
        } else {
            0L
        }
    }


    override fun getItemViewType(position: Int): Int {
        return TYPE_RANK
    }

    fun clear() {
        items.clear()
    }

    private fun setMaxVoteCount() {
        mMaxVoteCount = if (items.isNotEmpty()) {
            items[0].heart
        } else {
            0
        }
    }

    fun setItems(list: java.util.ArrayList<IdolModel>) {
        items.clear()
        items.addAll(list)
        setMaxVoteCount()
        notifyDataSetChanged()
    }

    companion object {
        const val TYPE_RANK = 2
    }
}