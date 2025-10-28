/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.smalltalk.viewholder

import android.annotation.SuppressLint
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import net.ib.mn.BuildConfig
import net.ib.mn.databinding.ItemSmallTalkBinding
import net.ib.mn.model.ArticleModel
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.DateUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.util.Calendar
import java.util.Locale

class SmallTalkVH(
    val binding: ItemSmallTalkBinding,
    val now: Calendar,
    val locale: Locale,
    val isFeedArticle: Boolean = false,
) : RecyclerView.ViewHolder(binding.root) {

    @SuppressLint("SetTextI18n")
    fun bind(
        articleModel: ArticleModel,
        isSmallTalkViewMore: Boolean = false,
        isSmallTalkListLastPosition: Boolean = false,
        isFirstItem: Boolean = false,
        clickListener: (isTutorial: Boolean) -> Unit = {},
    ) {
        with(binding) {
            clRoot.setOnClickListener {
                clickListener(false)
            }

            if (isFirstItem) {
                val isCurrentTutorial = if (BuildConfig.CELEB) {
                    TutorialManager.getTutorialIndex() == CelebTutorialBits.FAN_TALK_DETAIL
                } else {
                    TutorialManager.getTutorialIndex() == TutorialBits.COMMUNITY_FAN_TALK_DETAIL
                }
                if (isCurrentTutorial) {
                    setupLottieTutorial(lottieTutorialSmallTalk) {
                        clickListener(true)
                    }
                }
            } else {
                lottieTutorialSmallTalk.visibility = View.GONE
            }

            tvTitle.text = articleModel.title

            if (articleModel.content.isNullOrEmpty()) {
                tvContent.visibility = View.GONE
            } else {
                tvContent.apply {
                    visibility = View.VISIBLE
                    text = articleModel.content
                }
            }

            ivSmallTalkMostOnly.visibility = if (articleModel.isMostOnly == "Y") {
                View.VISIBLE
            } else {
                View.GONE
            }

            ivPopular.visibility = if (articleModel.isPopular) {
                View.VISIBLE
            } else {
                View.GONE
            }

            tvName.text = articleModel.user?.nickname

            val context = itemView.context

            tvDate.text = DateUtil.formatCreatedAtRelativeToNow(
                now = now,
                created = Calendar.getInstance(),
                articleCreatedAt = articleModel.createdAt,
                locale = locale,
            )

            tvLike.text = UtilK.countWithLocale(context, articleModel.likeCount)
            tvComment.text = UtilK.countWithLocale(context, articleModel.commentCount)
            tvViewer.text = UtilK.countWithLocale(context, articleModel.viewCount)

            if (!articleModel.thumbnailUrl.isNullOrEmpty()) {
                val userId = articleModel.user?.id ?: 0
                ivThumb.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(articleModel.thumbnailUrl)
                    .transform(
                        CenterCrop(),
                        RoundedCorners(Util.convertDpToPixel(itemView.context, 12f).toInt()),
                    )
                    .error(Util.noProfileImage(userId))
                    .fallback(Util.noProfileImage(userId))
                    .placeholder(Util.noProfileImage(userId))
                    .dontAnimate()
                    .into(ivThumb)

                tvImageCount.apply {
                    val count = articleModel.files?.size ?: 0
                    if (count > 1) {
                        text = "+${count - 1}"
                        visibility = View.VISIBLE
                    } else {
                        visibility = View.GONE
                    }
                }
            } else {
                ivThumb.visibility = View.GONE
                tvImageCount.visibility = View.GONE
            }
        }

        if (isSmallTalkViewMore) {
            binding.viewMoreSmallTalkList.visibility =
                if (isSmallTalkListLastPosition) View.VISIBLE else View.GONE
        } else {
            binding.viewMoreSmallTalkList.visibility = View.GONE
        }

        if (isFeedArticle) {
            binding.viewMoreSmallTalkList.visibility = View.VISIBLE
            binding.tvMore.visibility = View.GONE
        }
    }
}