/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.view

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import net.ib.mn.R
import net.ib.mn.viewholder.ArticleViewHolder
import net.ib.mn.viewholder.CommunityArticlePagerViewHolder
import net.ib.mn.viewholder.CommunityArticleViewHolder
import java.util.concurrent.TimeUnit


/**
 * @see
 * */

class ArticleRecyclerView constructor(
    context: Context,
    attributes: AttributeSet,
) : RecyclerView(context, attributes) {
    init {
        //recyclerview attach 되었을때 initial 세팅 진행한다.
        init()
    }

    //현재  보여지는  게시글의 포지션이다.
    var presentTargetItemPosition = 0
    var playedVideoItemPositionList = mutableListOf<Int>()

    private var compositeDisposable = CompositeDisposable()

    private fun init() {

        this.setHasFixedSize(true)
        this.setItemViewCacheSize(20)

        //스크롤 리스너
        addOnScrollListener(object : OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                try {

                    val layoutManager =
                        this@ArticleRecyclerView.layoutManager as LinearLayoutManager
                    val firstPosition = layoutManager.findFirstVisibleItemPosition()
                    val lastPosition = layoutManager.findLastVisibleItemPosition()
                    val rvRect = Rect()

                    this@ArticleRecyclerView.getGlobalVisibleRect(rvRect)

                    for (i in firstPosition..lastPosition) {

                        if ((videoArticleVisiblePercent(
                                type = currentType,
                                position = i,
                                rvRect = rvRect
                            ) ?: continue) == VIDEO_ARTICLE_VISIBLE_PERCENT
                        ) { //70퍼센트 이상이면 해당 비디오뷰  실행한다.

                            if (recyclerView.layoutManager?.isSmoothScrolling == false) {
                                val communityArticlePagerViewHolder = (getCurrentViewHolder(i) as? CommunityArticlePagerViewHolder) ?: continue

                                communityArticlePagerViewHolder.apply {
                                    initExoPlayer()
                                }

                                //현재 플레이되는 비디오뷰 포지션을 체크한다.
                                presentTargetItemPosition = i

                                //플레이 되었던  비디오 포지션을 모두 캐싱함.
                                //중복 제거를 위해 이렇게 진행함.
                                if (playedVideoItemPositionList.any { it == i }) {
                                    playedVideoItemPositionList.remove(i)
                                    playedVideoItemPositionList.add(i)
                                } else {
                                    playedVideoItemPositionList.add(i)
                                }

                                //현재 플레이하는 플레이어 이외에는 다 종료 해줌.
                                removeNonPlayedPlayer(playedPlayerIndex = i)
                            }
                        } else { //위 재생 비디오뷰이외에는 플레이어 없애줌.
                            (getCurrentViewHolder(i) as? CommunityArticlePagerViewHolder)?.cleanUp()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    removeExistPlayer()
                }
            }
        })
    }


    private var currentType = -1

    fun setType(type: Int) {
        this.currentType = type
    }

    //비디오 게시글  몇퍼센트 보이는지 체크
    private fun videoArticleVisiblePercent(type: Int, position: Int, rvRect: Rect): Int? {
        val rowRect = Rect()
        if (type == TYPE_ARTICLE_RCY) {
            (findViewHolderForAdapterPosition(position) as? CommunityArticleViewHolder)?.binding?.clMultiView?.getGlobalVisibleRect(
                rowRect
            )
        } else {
            (findViewHolderForAdapterPosition(position) as? ArticleViewHolder)?.binding?.clMultiView?.getGlobalVisibleRect(
                rowRect
            )
        }

        if(rowRect.isEmpty) {
            return null
        }

        var percentFirst = if (rowRect.bottom >= rvRect.bottom) {
            val visibleHeightFirst: Int = rvRect.bottom - rowRect.top
            visibleHeightFirst * 100 / getViewHolderHeight(type = currentType, position = position)
        } else {
            val visibleHeightFirst: Int = rowRect.bottom - rvRect.top
            visibleHeightFirst * 100 / getViewHolderHeight(type = currentType, position = position)
        }

        if (percentFirst > VIDEO_ARTICLE_VISIBLE_PERCENT) percentFirst =
            VIDEO_ARTICLE_VISIBLE_PERCENT

        return percentFirst
    }

    private fun getViewHolderHeight(type: Int, position: Int): Int {

        val height = if (type == TYPE_ARTICLE_RCY) {
            (findViewHolderForAdapterPosition(position) as? CommunityArticleViewHolder)?.binding?.clMultiView?.height ?: 0
        } else {
            (findViewHolderForAdapterPosition(position) as? ArticleViewHolder)?.binding?.clMultiView?.height ?: 0
        }

        return height
    }

    //플레이 되고있지 않은  플레이어들 모두 remove 시켜준다.
    fun removeNonPlayedPlayer(playedPlayerIndex: Int) {
        try {
            playedVideoItemPositionList.forEach {
                if (playedPlayerIndex != it) { //플레이중이 아닌  플레이어는 모두 꺼줌.
                    (getCurrentViewHolder(it) as? CommunityArticlePagerViewHolder)?.cleanUp()
                    removeListener(it)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun removeExistPlayer() {
        try {
            playedVideoItemPositionList.forEach {
                (getCurrentViewHolder(it) as? CommunityArticlePagerViewHolder)?.cleanUp()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun removeListener(index: Int) {
        (getCurrentViewHolder(index) as? CommunityArticlePagerViewHolder)?.removeListener()
    }


    private fun getCurrentViewPagerItemView(itemIndex: Int): View? {
        val currentViewPager = getCurrentViewPager(itemIndex)

        val currentIndex = currentViewPager?.currentItem ?: return null
        val viewPagerView = (currentViewPager.getChildAt(0) as? RecyclerView) ?: return null
        val viewHolder = viewPagerView.findViewHolderForAdapterPosition(currentIndex) ?: return null
        return viewHolder.itemView
    }

    private fun getCurrentViewPager(itemIndex: Int): ViewPager2? =
        findViewHolderForAdapterPosition(itemIndex)?.itemView?.findViewById(R.id.vp_community)

    private fun getCurrentViewHolder(itemIndex: Int): ViewHolder? {
        val currentViewPager = getCurrentViewPager(itemIndex)
        val viewPagerCurrentIndex = currentViewPager?.currentItem ?: return null
        val viewPagerView = (currentViewPager.getChildAt(0) as? RecyclerView)
        return viewPagerView?.findViewHolderForAdapterPosition(viewPagerCurrentIndex)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        compositeDisposable.clear()
    }

    override fun onWindowVisibilityChanged(visibility: Int) {
        super.onWindowVisibilityChanged(visibility)

        try {
            if (visibility == GONE) { //플레이어 안보이므로  전체 비디오 플레이어를   pause 시켜줌.
                playedVideoItemPositionList.forEach {
                    (getCurrentViewHolder(it) as CommunityArticlePagerViewHolder?)?.pauseVideoPlayer()
                }
            } else {
                //플레이어  타켓 포지션에 해당하는  플레이어  다시 resume 시켜줌.
                playedVideoItemPositionList.forEach { _ ->
                    (getCurrentViewHolder(presentTargetItemPosition) as CommunityArticlePagerViewHolder?)?.resumeVideoPlayer()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //화면이 attach 되었을때 다시 특정 포지션에서 새로운 플레이어를 적용해준다.
    fun setNewPlayerWhenViewAttached() {
        try {
            playedVideoItemPositionList.forEach {
                if (it == presentTargetItemPosition) {

                    //딜레이를 적용해야  attach 되고나서 플레이어가 add되었을때  플레이가 다시 됨.
                    //attach랑  플레이어 플레이 같이 실행되면  플레이어 플레이가 씹히는 경향이 있음.
                    //0.1초 딜레이를 적용 함.
                    Observable.timer(100, TimeUnit.MILLISECONDS)
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnNext {
                            try {
                                (findViewHolderForAdapterPosition(presentTargetItemPosition) as CommunityArticlePagerViewHolder?)?.initExoPlayer()
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.subscribe().addTo(compositeDisposable)
                } else {
                    (findViewHolderForAdapterPosition(it) as CommunityArticlePagerViewHolder?)?.initExoPlayer()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        const val VIDEO_ARTICLE_VISIBLE_PERCENT = 70

        const val TYPE_ARTICLE_RCY = 100
    }


}