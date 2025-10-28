/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewholder

import android.content.Context
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import net.ib.mn.R
import net.ib.mn.databinding.CommunityHeaderBinding
import net.ib.mn.fragment.BottomSheetFragment
import net.ib.mn.listener.CommunityArticleListener
import net.ib.mn.listener.ImgTypeClickListener
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.OrderByType
import net.ib.mn.utils.Util

class CommunityHeaderViewHolder(
    val binding: CommunityHeaderBinding,
    val context: Context,
    val mIdol: IdolModel?,
    private val communityArticleListener: CommunityArticleListener?,
    private val imgTypeClickListener: ImgTypeClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(mOrderBy: String, imageOnly: Boolean, wallpaperOnly: Boolean, position: Int) {
        photoFilterSetting(imageOnly, wallpaperOnly)
        filterClickListener(position)
        setFilterText(mOrderBy)
    }

    private fun photoFilterSetting(imageOnly: Boolean, wallpaperOnly: Boolean) = with(binding) {
        if(imageOnly) {
            clOnlyWallpaper.visibility = View.VISIBLE
            ivVerticalFilter.isSelected = false
            ivGridFilter.isSelected = true
        } else {
            clOnlyWallpaper.visibility = View.GONE
            ivVerticalFilter.isSelected = true
            ivGridFilter.isSelected = false
        }
        ivWallpaperCheckbox.isSelected = wallpaperOnly
    }

    private fun filterClickListener(position: Int) = with(binding) {
        val mSheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_COMMUNITY_FILTER, position)
        clSortingFilter.setOnClickListener {
            communityArticleListener?.filterSetCallBack(mSheet)
        }
        ivVerticalFilter.setOnClickListener {
            ivVerticalFilter.isSelected = true
            ivGridFilter.isSelected = false
            imgTypeClickListener?.verticalImgClickListener()
        }
        ivGridFilter.setOnClickListener {
            ivVerticalFilter.isSelected = false
            ivGridFilter.isSelected = true
            imgTypeClickListener?.gridImgClickListener()
        }

        clOnlyWallpaper.setOnClickListener {
            ivWallpaperCheckbox.isSelected = !ivWallpaperCheckbox.isSelected
            imgTypeClickListener?.wallpaperClickListener(ivWallpaperCheckbox.isSelected)
        }
    }

    //뷰 보일때 상태에 따라 스트링 재세팅
    private fun setFilterText(mOrderBy: String) = with(binding){
        tvFilter.text = when(mOrderBy){
            OrderByType.HEART.orderBy -> {
                context.getString(R.string.order_by_heart)
            }
            OrderByType.TIME.orderBy -> {
                context.getString(R.string.freeboard_order_newest)
            }
            OrderByType.COMMENTS.orderBy -> {
                context.getString(R.string.freeboard_order_comments)
            }
            else -> {
                context.getString(R.string.order_by_like)
            }
        }
    }

    fun filterByHeart() {
        communityArticleListener?.filterClickCallBack(GaAction.ORDER_HEART.label, OrderByType.HEART.orderBy)
    }

    fun filterByLatest() {
        communityArticleListener?.filterClickCallBack(GaAction.ORDER_TIME.label, OrderByType.TIME.orderBy)
    }

    fun filterByComments() {
        communityArticleListener?.filterClickCallBack(GaAction.ORDER_COMMENTS.label, OrderByType.COMMENTS.orderBy)
    }

    fun filterByLikes() {
        communityArticleListener?.filterClickCallBack(GaAction.ORDER_LIKES.label, OrderByType.LIKES.orderBy)
    }
}