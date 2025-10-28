/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.viewholder

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.preference.PreferenceManager
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.StyleSpan
import android.util.SparseBooleanArray
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.UnstableApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.WriteArticleActivity
import net.ib.mn.base.BaseArticleViewHolder
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.ReportRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.dialog.ArticleRemoveDialogFragment
import net.ib.mn.dialog.ReportDialogFragment
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.listener.CommunityArticleListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.ErrorControl
import net.ib.mn.utils.Logger
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getFontColor
import net.ib.mn.utils.vote.TranslateUiHelper

@UnstableApi
class CommunityArticleViewHolder(
    val binding: CommunityItemBinding,
    val context: Context,
    val mapExpanded: SparseBooleanArray,
    private val useTranslation: Boolean = true,
    val fragment: BaseFragment,
    val mType: TypeListModel?,
    val mIdol: IdolModel?,
    val mGlideRequestManager: RequestManager,
    val communityArticleListener: CommunityArticleListener?,
    val articlePhotoListener: ArticlePhotoListener?,
    private val reportRepository: ReportRepositoryImpl,
    private val usersRepository: UsersRepository,
    private val lifecycleScope: LifecycleCoroutineScope,
) : BaseArticleViewHolder(binding, mGlideRequestManager, communityArticleListener, context, articlePhotoListener, lifecycleScope) {
    private var isClickAllowed = true
    private val systemLanguage = Util.getSystemLanguage(context)

    override fun bind(articleModel: ArticleModel, idolModel: IdolModel, position: Int, headerSize : Int, isViewCountVisible: Boolean) =
        with(binding) {
            super.bind(articleModel, idolModel, position, headerSize, isViewCountVisible)
            articleModel.isUserLikeCache = articleModel.isUserLike
            setLikeIcon(articleModel.likeCount, articleModel.isUserLike)

            ivViewMore.visibility = View.VISIBLE
            clFooterComment.visibility = View.VISIBLE

            if (BuildConfig.CELEB) {
                if (mType?.type == null) {
                    name.setTextColor(ContextCompat.getColor(context, R.color.main))
                } else {
                    name.setTextColor(
                        mType?.getFontColor(context).let {
                            Color.parseColor(it)
                        },
                    )
                }
            }

            tvTag.visibility = View.GONE
            llTitle.visibility = View.GONE
            llViewCount.visibility = View.GONE

            // 게시글 작성자 아이콘 세팅
            val userModel = articleModel.user
            val userId = userModel?.id

            photo.let { photoView ->
                mGlideRequestManager.load("${articleModel.user?.imageUrlCommunity}${articleModel.enterTime}")
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(userId ?: 0))
                    .fallback(Util.noProfileImage(userId ?: 0))
                    .placeholder(Util.noProfileImage(userId ?: 0))
                    .into(photoView)
            }

            setBadgeStatus(articleModel)

            // view more
            val contents = articleModel.content ?: ""

            if (content.visibility == View.VISIBLE) {
                // already expanded or less than 3 lines in view and can read line count
                if (mapExpanded[position] || contents.isNotEmpty() && content.lineCount in 1..3) {
                    content.maxLines = 4000
                    viewMore.visibility = View.GONE
                } else {
                    // prevent TextView from scrolling
                    content.maxLines = 3
                    viewMore.visibility = View.VISIBLE
                }

                viewMore.setOnClickListener {
                    handleViewMore(
                        viewMore = it,
                        position = position
                    )
                }

                // prevent TextView from scrolling
                content.scrollTo(0, 0)
            }

            if (content.visibility == View.GONE) {
                viewMore.visibility = View.GONE
            }

            // 이미지가 없는 경우
            if (content.visibility == View.VISIBLE) {
                content.viewTreeObserver?.addOnGlobalLayoutListener(object :
                    ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        content.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                        // 4행 이상이고 펼쳐지지 않았다면
                        if ((content.lineCount ?: 0) > 3 && !mapExpanded[position]) {
                            viewMore.visibility = View.VISIBLE
                            viewMore.text = if (Util.isRTL(context)) {
                                context.getString(R.string.view_more) + " ..."
                            } else {
                                "... " + context.getString(R.string.view_more)
                            }
                            val animation = ObjectAnimator.ofInt(content, "maxLines", 3)
                            animation.duration = 10L
                            animation.start()
                        } else {
                            val animation = ObjectAnimator.ofInt(content, "maxLines", 4000)
                            animation.duration = 10L
                            animation.start()
                            viewMore.visibility = View.GONE
                            content.maxLines = 4000
                        }
                        // 텍스트뷰 스크롤 방지
                        content.scrollTo(0, 0)
                    }
                })
            }

            if (position == headerSize) {
                if (BuildConfig.CELEB) {
                    when (val currentTutorialIndex = TutorialManager.getTutorialIndex()) {
                        CelebTutorialBits.FEED_USER_PROFILE -> {
                            setupLottieTutorial(lottieTutorialProfile, true) {
                                updateTutorial(currentTutorialIndex)
                                communityArticleListener?.feedClick(articleModel.user ?: return@setupLottieTutorial)
                            }
                        }
                        CelebTutorialBits.FEED_VOTE -> {
                            setupLottieTutorial(lottieTutorialVote, true) {
                                updateTutorial(currentTutorialIndex)
                                communityArticleListener?.heartClick(articleModel, position)
                            }
                        }
                        CelebTutorialBits.FEED_LIKE -> {
                            setupLottieTutorial(lottieTutorialLike, true) {
                                updateTutorial(currentTutorialIndex)
                                onClickLike(articleModel)
                            }
                        }
                        CelebTutorialBits.FEED_COMMENTS -> {
                            setupLottieTutorial(lottieTutorialComment, true) {
                                updateTutorial(currentTutorialIndex)
                                communityArticleListener?.commentClick(articleModel, position)
                            }
                        }
                    }
                } else {
                    when (val currentTutorialIndex = TutorialManager.getTutorialIndex()) {
                        TutorialBits.COMMUNITY_FEED_LIKES -> {
                            setupLottieTutorial(lottieTutorialLike, true) {
                                updateTutorial(currentTutorialIndex)
                                onClickLike(articleModel)
                            }
                        }
                        TutorialBits.COMMUNITY_FEED_USER_PROFILE -> {
                            setupLottieTutorial(lottieTutorialProfile, true) {
                                updateTutorial(currentTutorialIndex)
                                communityArticleListener?.feedClick(articleModel.user ?: return@setupLottieTutorial)
                            }
                        }
                        TutorialBits.COMMUNITY_FEED_VOTE -> {
                            setupLottieTutorial(lottieTutorialVote, true) {
                                updateTutorial(currentTutorialIndex)
                                communityArticleListener?.heartClick(articleModel, position)
                            }
                        }
                        TutorialBits.COMMUNITY_FEED_COMMENTS -> {
                            setupLottieTutorial(lottieTutorialComment, true) {
                                updateTutorial(currentTutorialIndex)
                                communityArticleListener?.commentClick(articleModel, position)
                            }
                        }
                    }
                }
            } else {
                lottieTutorialLike.visibility = View.GONE
                lottieTutorialProfile.visibility = View.GONE
                lottieTutorialVote.visibility = View.GONE
                lottieTutorialComment.visibility = View.GONE
            }

            // 최애만 보기 또는 뉴비
            if (articleModel.isMostOnly == "Y" || articleModel.isWelcome == "Y") {
                iconSecret.visibility = if (articleModel.isMostOnly == "Y") View.VISIBLE else View.GONE
            } else {
                iconSecret.visibility = View.GONE
            }

            TranslateUiHelper.bindTranslateButton(
                context = itemView.context,
                view = viewTranslate,
                content = articleModel.content ?: "",
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
                    // 펼친 것으로 처리
                    handleViewMore(viewMore = binding.viewMore, position = position)
                    // 번역하기
                    communityArticleListener?.translationClick(articleModel, position)
                }
            }

            setOnClick(articleModel, position)
        }

    private fun updateTutorial(tutorialIndex: Int) = GlobalScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex,
            { response ->
                if (response.optBoolean("success")) {
                    Logger.d("Tutorial updated successfully: $tutorialIndex")
                    val bitmask = response.optLong("tutorial", 0L)
                    TutorialManager.init(bitmask)
                } else {
                    UtilK.handleCommonError(context, response)
                }
            },
            { error ->
                Toast.makeText(context, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                if (Util.is_log()) {
                    Util.log(error.message ?: "Unknown error")
                }
            },
        )
    }

    private fun onClickLike(articleModel: ArticleModel) {
        isClickAllowed = false

        // 좋아요 카운트와 상태 업데이트
        if (articleModel.isUserLikeCache) {
            articleModel.likeCount--
        } else {
            articleModel.likeCount++
        }
        articleModel.isUserLikeCache = !articleModel.isUserLikeCache
        setLikeIcon(articleModel.likeCount, articleModel.isUserLikeCache)

        communityArticleListener?.likeClick(articleModel)

        // 1초 후 다시 클릭 허용
        itemView.postDelayed({
            isClickAllowed = true
        }, 1000)
    }

    // ... 더 보기 클릭 처리
    private fun handleViewMore(viewMore: View, position: Int) {
        Util.log("set MapExpanded($position) to true")
        mapExpanded.put(position, true)
        viewMore.visibility = View.GONE

        val animation = ObjectAnimator.ofInt(binding.content, "maxLines", 4000)
        animation.duration = 100L
        animation.start()
        binding.content.maxLines = 4000

    }

    private fun setOnClick(articleModel: ArticleModel, position: Int) {
        with(binding) {
            footerHeart.setOnClickListener {
                if (binding.lottieTutorialVote.isVisible) return@setOnClickListener
                communityArticleListener?.heartClick(articleModel, position)
            }

            llHeartCount.setOnClickListener {
                communityArticleListener?.heartClick(articleModel, position)
            }

            llCommentCount.setOnClickListener {
                communityArticleListener?.commentClick(articleModel, position)
            }

            llLikeCount.setOnClickListener {
                if (!isClickAllowed || binding.lottieTutorialLike.isVisible) return@setOnClickListener
                onClickLike(articleModel)
            }

            footerComment.setOnClickListener {
                if (binding.lottieTutorialComment.isVisible) return@setOnClickListener
                communityArticleListener?.commentClick(articleModel, position)
            }

            footerLike.setOnClickListener {
                if (!isClickAllowed || binding.lottieTutorialLike.isVisible) return@setOnClickListener

                isClickAllowed = false

                // 좋아요 카운트와 상태 업데이트
                if (articleModel.isUserLikeCache) {
                    articleModel.likeCount--
                } else {
                    articleModel.likeCount++
                }
                articleModel.isUserLikeCache = !articleModel.isUserLikeCache
                setLikeIcon(articleModel.likeCount, articleModel.isUserLikeCache)

                communityArticleListener?.likeClick(articleModel)

                // 1초 후 다시 클릭 허용
                itemView.postDelayed({
                    isClickAllowed = true
                }, 1000)
            }

            ivViewMore.setOnClickListener {
                communityArticleListener?.viewMoreClick(articleModel, position)
            }

            photo.setOnClickListener {
                if (binding.lottieTutorialProfile.isVisible) return@setOnClickListener
                communityArticleListener?.feedClick(articleModel.user ?: return@setOnClickListener)
            }

            name.setOnClickListener {
                communityArticleListener?.feedClick(articleModel.user ?: return@setOnClickListener)
            }
        }
    }

    fun clickEdit(activity: CommunityActivity, model: ArticleModel) {
        activity.setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "community_edit",
        )

        if (Const.FEATURE_WRITE_RESTRICTION) {
            // 집계시간에는 수정도 불가
            lifecycleScope.launch {
                usersRepository.isActiveTime(
                    { response ->
                        if (response.optBoolean("success")) {
                            if (response.optString("active") == Const.RESPONSE_Y) {
                                val intent = WriteArticleActivity.createIntent(
                                    context,
                                    mIdol,
                                )
                                intent.putExtra(Const.EXTRA_ARTICLE, model)
                                communityArticleListener?.editClick(intent)
                            } else {
                                val start = Util.convertTimeAsTimezone(
                                    response.optString("begin"),
                                )
                                val end = Util.convertTimeAsTimezone(
                                    response.optString("end"),
                                )
                                val unableUseTime = String.format(
                                    context.getString(R.string.msg_unable_use_write),
                                    start,
                                    end,
                                )

                                Util.showIdolDialogWithBtn1(
                                    context,
                                    null,
                                    unableUseTime,
                                ) { Util.closeIdolDialog() }
                            }
                        } else { // success is false!
                            UtilK.handleCommonError(context, response)
                        }
                    }, {
                        Toast.makeText(
                            context,
                            R.string.error_abnormal_exception,
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                )
            }
        } else {
            val intent = WriteArticleActivity.createIntent(activity, mIdol)
            intent.putExtra(Const.EXTRA_ARTICLE, model)
            communityArticleListener?.editClick(intent)
        }
    }

    fun clickRemove(activity: CommunityActivity, model: ArticleModel, position: Int) {
        Util.showProgress(context)
        activity.setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "community_delete",
        )
        val removeDlg = ArticleRemoveDialogFragment.getInstance(model, position)
        removeDlg.setTargetFragment(fragment, RequestCode.ARTICLE_REMOVE.value)
        removeDlg.show(activity.supportFragmentManager, "remove")
    }

    fun clickReport(activity: CommunityActivity, model: ArticleModel, position: Int) {
        val account = IdolAccount.getAccount(context)
        if (account == null && Util.mayShowLoginPopup(activity)) {
            return
        }
        activity.setUiActionFirebaseGoogleAnalyticsActivity(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            "community_report",
        )
        // 로컬에서 보여주지말고 그냥 무조건 API불려주는걸로 변경함.
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val reportedArticles = prefs.getStringSet(
            account?.email + "_did_report", HashSet(),
        ) ?: HashSet()
        if (reportedArticles.contains(model.resourceUri)) {
            doReport(activity, model)
            return
        }

        // config/self 제거하고 미리 받아놓은 값 사용
        val reportHeart = ConfigModel.getInstance(context).reportHeart
        val report = ReportDialogFragment.getInstance(model, position)
        val articleIdol = model.idol

        // 하트 차감 수가 0일 때
        if (reportHeart == 0 ||
            // 내 커뮤이면서
            (account?.userModel?.most?.getId() == articleIdol?.getId()) &&
            // 최애가 없는 사람 글과
            (
                model.user?.most == null ||
                    // 커뮤니티가 최애가 아닌 사람의 글도 무료로 신고 가능
                    (model.user?.most?.getId() != articleIdol?.getId())
                )
        ) {
            report.setMessage(
                HtmlCompat.fromHtml(
                    context.getString(R.string.warning_report_hide_article),
                    HtmlCompat.FROM_HTML_MODE_LEGACY,
                ),
            )
        } else {
            if (reportHeart > 0) {
                val color = "#" + (
                    Integer.toHexString(
                        ContextCompat.getColor(
                            context,
                            R.color.main,
                        ),
                    ).substring(2)
                    )
                val msg = String.format(
                    context.resources.getString(R.string.warning_report_lose_heart),
                    "<FONT color=$color>$reportHeart</FONT>",
                )
                val spanned = HtmlCompat.fromHtml(
                    msg,
                    HtmlCompat.FROM_HTML_MODE_LEGACY,
                )
                report.setMessage(spanned)
            }
        }

        report.setTargetFragment(fragment, RequestCode.ARTICLE_REPORT.value)
        report.show(activity.supportFragmentManager, "report")
    }

    private fun doReport(activity: CommunityActivity, model: ArticleModel) {
        MainScope().launch {
            reportRepository.doReport(
                model.id.toLong(),
                null,
                { response ->
                    if (!response.getBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(context, response)
                        if (responseMsg != null) {
                            Util.showDefaultIdolDialogWithBtn1(
                                context,
                                null,
                                responseMsg,
                            ) { Util.closeIdolDialog() }
                        }
                    }
                },
                { error ->
                    Toast.makeText(
                        context,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (Util.is_log()) {
                        activity.showMessage(error.message)
                    }
                },
            )
        }
    }

    private fun setBadgeStatus(articleModel: ArticleModel) = with(binding) {
        if (articleModel.isViewable != BADGE_STATUS_OF_MANAGER) {
            return@with
        }

        articleAction.visibility = View.GONE
        articleResult.visibility = View.GONE

        // 보안관에 의해 삭제된 경우 처리
        content.visibility = View.VISIBLE

        val deletedBy = articleModel.deletedBy

        if (deletedBy == null) {
            content.text = itemView.context.getString(R.string.deleted_by_unknown)
            return@with
        }

        val nickName = deletedBy.nickname
        val msg = context.resources.getString(R.string.deleted_by)

        val spannableString = SpannableString("$nickName$msg")
        // 레벨 아이콘, 닉네임, 뱃지 순으로 표시
        val bitmapDrawable =
            BitmapDrawable(context.resources, Util.getLevelImage(context, deletedBy))
        bitmapDrawable.setBounds(
            0,
            0,
            bitmapDrawable.intrinsicWidth,
            bitmapDrawable.intrinsicHeight,
        )
        val span = ImageSpan(bitmapDrawable, ImageSpan.ALIGN_BASELINE)
        spannableString.setSpan(span, 0, 1, Spannable.SPAN_INCLUSIVE_EXCLUSIVE)

        // 닉네임에 색칠
        spannableString.setSpan(
            ForegroundColorSpan(
                ContextCompat.getColor(context, R.color.main),
            ),
            1,
            1 + nickName.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )
        spannableString.setSpan(
            StyleSpan(Typeface.BOLD),
            1,
            1 + nickName.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
        )

        content.text = spannableString
    }

    private fun setLikeIcon(articleLikeCount: Int, isUserLike: Boolean) {
        with(binding) {
            if(isUserLike) {
                ivLike.apply {
                    setImageResource(R.drawable.icon_board_like_active)
                    colorFilter = null
                }
                likeCountIcon.apply {
                    setImageResource(R.drawable.icon_board_like)
                    setColorFilter(ContextCompat.getColor(context, R.color.text_default), PorterDuff.Mode.SRC_IN)
                }
            } else {
                ivLike.apply {
                    setImageResource(R.drawable.icon_board_like)
                    setColorFilter(ContextCompat.getColor(context, R.color.text_default), PorterDuff.Mode.SRC_IN)
                }
                likeCountIcon.apply {
                    setImageResource(R.drawable.icon_board_like)
                    setColorFilter(ContextCompat.getColor(context, R.color.text_default), PorterDuff.Mode.SRC_IN)
                }
            }
            likeCount.text = articleLikeCount.toString()
        }
    }

    companion object {
        const val BADGE_STATUS_OF_MANAGER = "X"
    }
}