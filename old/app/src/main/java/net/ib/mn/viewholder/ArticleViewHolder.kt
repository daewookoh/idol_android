/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 자게, 지식돌, 피드 공통 ArticleViewHolder
 *
 * */

package net.ib.mn.viewholder

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.gson.reflect.TypeToken
import net.ib.mn.R
import net.ib.mn.adapter.FeedArticleAdapter.Companion.TYPE_FEED_COMMUNITY_ARTICLE
import net.ib.mn.adapter.NewArticleAdapter.Companion.TYPE_KIN_BOARD
import net.ib.mn.adapter.SearchedAdapter.Companion.TYPE_SEARCH_COMMUNITY_ARTICLE
import net.ib.mn.adapter.SearchedAdapter.Companion.TYPE_SEARCH_FEED_KIN_BOARD
import net.ib.mn.addon.IdolGson
import net.ib.mn.base.BaseArticleViewHolder
import net.ib.mn.core.model.TagModel
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.vote.TranslateUiHelper
import java.text.NumberFormat
import java.util.HashMap

@dagger.hilt.android.UnstableApi
class ArticleViewHolder(
    val binding: CommunityItemBinding,
    private val context: Context,
    private val useTranslation: Boolean = false,
    private val itemViewType: Int,
    private val onArticleButtonClick: (ArticleModel, View, Int) -> Unit?,
    val articlePhotoListener: ArticlePhotoListener?,
    private val searchedIdolListSize: Int = 0,
    private val lifecycleScope: LifecycleCoroutineScope,
    private val mapExpandedForSearch: HashMap<Int, Boolean> = HashMap(),
    private val updateExpanded: (Int, Boolean) -> Unit = { _, _ -> }
) : BaseArticleViewHolder(binding, mGlideRequestManager = Glide.with(context), null, context, articlePhotoListener, lifecycleScope) {
    private var glideRequestManager: RequestManager = Glide.with(context)
    private val systemLanguage = Util.getSystemLanguage(context)

    // view more 에서 이미 펼친 행 기억
    private var mapExpanded = HashMap<Int, Boolean>()

    private var currentArticle: ArticleModel? = null

    override fun bind(article: ArticleModel, idolModel: IdolModel, position: Int, headerSize : Int, isViewCountVisible: Boolean) = with(binding) {
        super.bind(article, idolModel, position, headerSize, isViewCountVisible)

        if (mapExpandedForSearch.isNotEmpty()) {
            mapExpanded = mapExpandedForSearch
        }

        currentArticle = article

        ivViewMore.visibility = View.VISIBLE
        clFooterComment.visibility = View.VISIBLE
        llCategoryInfo.visibility = View.GONE
        clFooterHeart.visibility = View.GONE
        llHeartCount.visibility = View.GONE

        tvTag.visibility = View.GONE
        llTitle.visibility = View.GONE

        val userId = article.user?.id ?: 0
        if (article.user?.imageUrl != null) {
            glideRequestManager
                .load(article.user?.imageUrlCommunity + article.enterTime)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(userId))
                .fallback(Util.noProfileImage(userId))
                .placeholder(Util.noProfileImage(userId))
                .into(photo)
        } else {
            glideRequestManager.clear(photo)
            photo.setImageResource(Util.noProfileImage(userId))
        }

        if (!isViewCountVisible) {
            binding.llViewCount.visibility = View.GONE
        }

        article.isUserLikeCache = article.isUserLike
        setLikeIcon(article.likeCount, article.isUserLike)
        setCommentString(article)
        setArticleVisibility(article)
        isArticleDeleted(article)
        setArticleContent(article, position)
        setOnClickListener(article, position)
    }

    @OptIn(UnstableApi::class)
    private fun setCommentString(article: ArticleModel) = with(binding) {
        when (itemViewType) {
            TYPE_KIN_BOARD -> { // 자게, 지식돌로 들어갔을 때
                when (article.idol?.getId()) {
                    Const.IDOL_ID_KIN -> {
                        // 지식돌은 하트 갯수 안보여줌
                        llHeartCount.visibility = View.GONE
                        tvComment.text = context.getString(R.string.answer)
                        llCategoryInfo.visibility = View.GONE
                    }
                    Const.IDOL_ID_FREEBOARD -> {
                        // 카테고리
                        val gson = IdolGson.getInstance()
                        val listType = object : TypeToken<List<TagModel>>() {}.type
                        val tags: List<TagModel>? =
                            gson.fromJson(Util.getPreference(context, Const.BOARD_TAGS), listType)
                        if (tags != null) {
                            tvComment.text = context.getString(R.string.lable_community_comment)
                        }
                    }
                }
            }
            TYPE_FEED_COMMUNITY_ARTICLE -> { // 피드에서 커뮤니티 게시글 봤을 때
                tvComment.text = context.getString(R.string.lable_community_comment)
                llHeartCount.visibility = View.VISIBLE
                clFooterHeart.visibility = View.VISIBLE

                tvLabel.text = context.getString(R.string.community_post)
                tvCategory.text = article.idol?.getName(context)
                llCategoryInfo.visibility = View.VISIBLE
            }
            TYPE_SEARCH_COMMUNITY_ARTICLE -> { // 검색에서 커뮤니티 게시글 봤을 때
                tvComment.text = context.getString(R.string.lable_community_comment)
                llHeartCount.visibility = View.VISIBLE
                clFooterHeart.visibility = View.GONE

                tvLabel.text = context.getString(R.string.community_post)
                tvCategory.text = article.idol?.getName(context)
                llCategoryInfo.visibility = View.VISIBLE
            }
            TYPE_SEARCH_FEED_KIN_BOARD -> {
                when (article.idol?.getId()) {
                    Const.IDOL_ID_KIN -> {
                        // 지식돌은 하트 갯수 안보여줌
                        llHeartCount.visibility = View.GONE
                        tvComment.text = context.getString(R.string.answer)
                    }
                    Const.IDOL_ID_FREEBOARD -> {
                        tvComment.text = context.getString(R.string.lable_community_comment)
                    }
                }
            }
        }
    }

    private fun setArticleVisibility(article: ArticleModel) = with(binding) {
        // 최애만 보기 또는 뉴비. 자게는 좀 다르긴한데 일단 똑같이 넣어봄
        if (article.isMostOnly == "Y" || article.isWelcome == "Y") {
            iconSecret.visibility = if (article.isMostOnly == "Y") View.VISIBLE else View.GONE
        } else {
            iconSecret.visibility = View.GONE
        }
    }

    // 보안관에 의해 삭제된 경우 처리
    private fun isArticleDeleted(article: ArticleModel) = with(binding) {
        if (article.isViewable != null && article.isViewable == "X") {
            content.visibility = View.VISIBLE

            val deletedBy = article.deletedBy
            if (deletedBy != null) {
                val nick = deletedBy.nickname
                val msg = context.resources.getString(R.string.deleted_by)

                val ss = SpannableString("$$nick$msg")
                // 레벨 아이콘, 닉네임, 뱃지 순으로 표시
                //                Drawable d = ContextCompat.getDrawable(context, Util.getLevelResId(deletedBy.getLevel()));
                val d = BitmapDrawable(context.resources, Util.getLevelImage(context, deletedBy))
                d.setBounds(0, 0, d.intrinsicWidth, d.intrinsicHeight)
                val span = ImageSpan(d, ImageSpan.ALIGN_BASELINE)
                ss.apply {
                    setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)
                    // 닉네임에 색칠
                    setSpan(
                        ForegroundColorSpan(ContextCompat.getColor(context, R.color.main)),
                        1,
                        1 + nick.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                    setSpan(
                        StyleSpan(Typeface.BOLD),
                        1,
                        1 + nick.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
                    )
                }

                content.text = ss
            } else {
                content.setText(R.string.deleted_by_unknown)
            }
            articleAction.visibility = View.GONE
            articleResult.visibility = View.GONE
            llBtnContainer.visibility = View.GONE
        }
    }

    @OptIn(UnstableApi::class)
    private fun setArticleContent(article: ArticleModel, position: Int) = with(binding) {
        if (TextUtils.isEmpty(article.content)) {
            content.visibility = View.GONE
        } else {
            content.apply {
                text = Util.convertHashTags(context, article.content)
                movementMethod = LinkMovementMethod.getInstance()
                visibility = View.VISIBLE
            }
        }

        // 자게 게시물만 번역 -> 모든 게시물로
        TranslateUiHelper.bindTranslateButton(
            context = itemView.context,
            view = viewTranslate,
            content = if (article.idol?.getId() == Const.IDOL_ID_FREEBOARD) {
                (article.title ?: "") + (article.content ?: "")
            } else {
                article.content?: ""
            },
            systemLanguage = systemLanguage,
            nation = article.nation,
            translateState = article.translateState,
            isTranslatableCached = article.isTranslatable,
            useTranslation = useTranslation,
        ).also { canTranslate ->
            if(article.isTranslatable == null) {
                article.isTranslatable = canTranslate
            }
            viewTranslate.setOnClickListener {
                onArticleButtonClick(article, viewTranslate, position) // 번역은 해당 row를 직접 업데이트하므로 실제 row position을 전달
            }
        }

        // 지식돌, 자게의 경우 더보기 없음
        // 검색결과에는 더보기 있어야 함
        if (itemViewType != TYPE_FEED_COMMUNITY_ARTICLE && itemViewType != TYPE_SEARCH_COMMUNITY_ARTICLE) {
            return@with
        }

        // view more
        var contentString: String? = article.content
        if (contentString == null) {
            contentString = ""
        }

        heartCount.text = (NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(article.heart))

        if (content.visibility == View.VISIBLE) {
            // 이미 확장했거나 getLineCount()를 읽을 수 있는 상태에서 3줄 이하라면
            if (mapExpanded[position] != null &&
                mapExpanded[position] == true ||
                contentString.isNotEmpty() && content.lineCount > 0 &&
                content.lineCount <= 3
            ) {
                content.maxLines = 4000
                viewMore.visibility = View.GONE
            } else {
                // 텍스트뷰 스크롤 방지
                content.maxLines = 3
                viewMore.visibility = View.VISIBLE
            }

            viewMore.setOnClickListener {
                updateExpanded(position, true)
                mapExpanded[position] = true
                viewMore.visibility = View.GONE

                val animation = ObjectAnimator.ofInt(content, "maxLines", 4000)
                animation.setDuration(100).start()
                content.maxLines = 4000
            }

            // 텍스트뷰 스크롤 방지
            content.scrollTo(0, 0)
        }

        // 이미지만 있는 경우 view more가 나오는 경우가 있음
        if (content.visibility == View.GONE) {
            viewMore.visibility = View.GONE
        }

        if (content.visibility == View.VISIBLE) {
            content.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    content.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    // 4행 이상이고 펼쳐지지 않았다면
                    //                        Util.    ( _position+": lines="+mContentView.getLineCount()+ " expanded="+mapExpanded.get(_position) );
                    if (content.lineCount > 3 && (mapExpanded[position] == null || mapExpanded[position] == false)) {
                        viewMore.visibility = View.VISIBLE
                        viewMore.text = "... " + context.getString(R.string.view_more)
                        val animation = ObjectAnimator.ofInt(content, "maxLines", 3)
                        animation.setDuration(10).start()
                    } else {
                        val animation = ObjectAnimator.ofInt(content, "maxLines", 4000)
                        animation.setDuration(10).start()
                        viewMore.visibility = View.GONE
                        content.maxLines = 4000
                    }
                    // 텍스트뷰 스크롤 방지
                    content.scrollTo(0, 0)
                }
            })
        }
    }

    @OptIn(UnstableApi::class)
    private fun setOnClickListener(article: ArticleModel, position: Int) = with(binding) {
        val listener = View.OnClickListener { v ->
            onArticleButtonClick(article, v, position - searchedIdolListSize)
        }
        name.setOnClickListener(listener)
        photo.setOnClickListener(listener)
        iconSecret.setOnClickListener(listener)
        llCommentCount.setOnClickListener(listener)
        footerComment.setOnClickListener(listener)
        footerHeart.setOnClickListener(listener)
        ivViewMore.setOnClickListener(listener)

        // 검색에서 자게, 지식돌, 커뮤니티 게시글 봤을 경우 해당 커뮤니티로
//        if (itemViewType == TYPE_SEARCH_FEED_KIN_BOARD || itemViewType == TYPE_SEARCH_COMMUNITY_ARTICLE) {
//            tvCategory.setOnClickListener {
//                // 클릭한 게시물이 지식돌, 자유게시판일 경우 이동 안하고 아무 효과 없게
//                val localeStart = LocaleUtil.getAppLocale(itemView.context).toString().split("_")
//                when (article.idol.getId()) {
//                    Const.IDOL_ID_FREEBOARD -> {
//                        if (BuildConfig.CELEB) {
//                            context.startActivity(FreeboardActivity.createIntent(context))
//                            return@setOnClickListener
//                        }
//                        if (localeStart[0] == "ko" || localeStart[0] == "en" || localeStart[0] == "ja" || localeStart[0] == "zh") {
//                            context.startActivity(BoardActivity.createIntent(context))
//                        } else {
//                            context.startActivity(FreeboardActivity.createIntent(context))
//                        }
//                    }
//                    else -> {
//                        // 클릭한 게시물이 아이돌일 경우 이동
//                        (context as Activity).startActivityForResult(
//                            CommunityActivity.createIntent(
//                                context,
//                                article.idol,
//                                CommunityActivity.CATEGORY_COMMUNITY,
//                            ),
//                            Const.REQUEST_TOP3_UPDATED,
//                        )
//                    }
//                }
//            }
//        }

        val likeView = arrayListOf<View>(likeCountIcon, footerLike)
        likeView.forEach { view ->
            view.setOnClickListener {
                if (article.isUserLikeCache) {
                    article.likeCount--
                } else {
                    article.likeCount++
                }
                article.isUserLikeCache = !article.isUserLikeCache
                setLikeIcon(article.likeCount, article.isUserLikeCache)
                onArticleButtonClick(article, footerLike, position - searchedIdolListSize)
            }
        }
    }
    private fun setLikeIcon(articleLikeCount: Int, isUserLike: Boolean) {
        with(binding) {
            if(isUserLike) {
                ivLike.setImageResource(R.drawable.icon_board_like_active)
            } else {
                ivLike.setImageResource(R.drawable.icon_board_like)
            }
            likeCount.text = articleLikeCount.toString()
        }
    }

    fun updateLike(article: ArticleModel) {
        currentArticle = article

        article.isUserLikeCache = article.isUserLike
        setLikeIcon(article.likeCount, article.isUserLike)
        setCommentString(article)
        setArticleVisibility(article)
        isArticleDeleted(article)
        setArticleContent(article, position)
        setOnClickListener(article, position)
    }
}