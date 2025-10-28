package net.ib.mn.viewholder

import android.content.Context
import android.graphics.PorterDuff
import android.util.Log
import android.view.View
import android.widget.RelativeLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.media3.common.util.UnstableApi
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.base.BaseArticleViewHolder
import net.ib.mn.databinding.CommunityItemBinding
import net.ib.mn.listener.ArticlePhotoListener
import net.ib.mn.listener.CommunityArticleListener
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.DateUtil
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.vote.TranslateUiHelper
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

@dagger.hilt.android.UnstableApi
class ArticleCommentViewHolder(
    private val context: Context,
    private val useTranslation: Boolean,
    val binding: CommunityItemBinding,
    private var getVideoPlayView: NewCommentAdapter.GetVideoPlayView?,
    private val mGlideRequestManager: RequestManager,
    private val tagName: String?,
    private val now: Calendar,
    private val locale: Locale,
    val articlePhotoListener: ArticlePhotoListener?,
    private val lifecycleScope: LifecycleCoroutineScope,
    val onArticleItemClickListener: NewCommentAdapter.OnArticleItemClickListener?
) : BaseArticleViewHolder(binding, mGlideRequestManager, null, context, articlePhotoListener, lifecycleScope) {

    private var isClickAllowed = true
    private val systemLanguage = Util.getSystemLanguage(context)

    override fun bind(articleModel: ArticleModel, idolModel: IdolModel, position: Int,
                      headerSize: Int, isViewCountVisible: Boolean): Unit =
        with(binding) {
            super.bind(articleModel, idolModel, position, headerSize, isViewCountVisible)

            if (articleModel.idol?.getId() == 99990 || articleModel.type == "M") {
                if (tagName.isNullOrEmpty()) {
                    tvTag.visibility = View.GONE
                } else {
                    tvTag.visibility = View.VISIBLE
                    tvTag.text = tagName
                }

                if (articleModel.title.isNullOrEmpty()) {
                    llTitle.visibility = View.GONE
                } else {
                    llTitle.visibility = View.VISIBLE
                    tvTitle.text = articleModel.title
                }

                ivPopular.visibility = if (articleModel.isPopular) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                llHeartCount.visibility = View.GONE
                articleAction.visibility = View.GONE
            } else {
                tvTag.visibility = View.GONE
                llTitle.visibility = View.GONE
                llViewCount.visibility = View.GONE
            }

            val f = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM,
                DateFormat.SHORT,
                LocaleUtil.getAppLocale(itemView.context),
            )
            val dateString = f.format(articleModel.createdAt)
            createdAt.text = dateString

            articleModel.isUserLikeCache = articleModel.isUserLike
            setLikeIcon(articleModel.likeCount, articleModel.isUserLike)

            ivViewMore.visibility = View.GONE
            clFooterComment.visibility = View.GONE

            val userId: Int = articleModel.user?.id ?: 0
            photo.post {
                mGlideRequestManager
                    .load(articleModel.user?.imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(userId))
                    .placeholder(Util.noProfileImage(userId))
                    .fallback(Util.noProfileImage(userId))
                    .into(photo)
            }

            if (articleModel.isMostOnly == "Y") {
                // 최애공계 게시글인 경우에는  공유버튼을  gone처리
                iconSecret.visibility = View.VISIBLE
            } else {
                iconSecret.visibility = View.GONE
            }

            likeClickListener(footerLike, articleModel)
            likeClickListener(llLikeCount, articleModel)

            // 번역
            TranslateUiHelper.bindTranslateButton(
                context = itemView.context,
                view = viewTranslate,
                content = (articleModel.title ?: "") + articleModel.content, // 최애톡인 경우 자게가 아니면서 타이틀이 있을 수 있음
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
        }

    private fun likeClickListener(view: View, articleModel: ArticleModel) {
        view.setOnClickListener {
            if (!isClickAllowed) return@setOnClickListener

            isClickAllowed = false

            if (articleModel.isUserLikeCache) {
                articleModel.likeCount--
            } else {
                articleModel.likeCount++
            }
            articleModel.isUserLikeCache = !articleModel.isUserLikeCache
            setLikeIcon(articleModel.likeCount, articleModel.isUserLikeCache)
            onArticleItemClickListener?.onArticleLikeClicked(articleModel)

            view.postDelayed({
                isClickAllowed = true
            }, 1000)
        }
    }

    private fun setLikeIcon(articleLikeCount: Int, isUserLike: Boolean) {
        with(binding) {
            likeCount.text = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                .format(articleLikeCount)

            if (binding.articleAction.isVisible) {
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
                return
            }

            if(isUserLike) {
                ivLike.apply {
                    setImageResource(R.drawable.icon_board_like_active)
                    colorFilter = null
                }
                likeCountIcon.apply {
                    setImageResource(R.drawable.icon_board_like_active)
                    colorFilter = null
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
        }
    }
}