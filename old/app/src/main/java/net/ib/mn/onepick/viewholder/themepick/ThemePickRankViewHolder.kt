/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.onepick.viewholder.themepick

import android.annotation.SuppressLint
import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import com.bumptech.glide.RequestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ItemThemePickRankBinding
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.model.ThemepickModel
import net.ib.mn.model.ThemepickRankModel
import net.ib.mn.model.toPresentation
import net.ib.mn.onepick.viewholder.base.BaseThemePickRankViewHolder
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.vote.VotePercentage
import java.text.NumberFormat
import kotlin.math.roundToInt

/**
 * @see
 * */

class ThemePickRankViewHolder(
    private val binding: ItemThemePickRankBinding,
    private val mTheme: ThemepickModel,
    private val glideRequestManager: RequestManager,
    private val themepickRankModel: ArrayList<ThemepickRankModel>,
    private val coroutineScope: CoroutineScope,
    private val getIdolByIdUseCase: GetIdolByIdUseCase,
) : BaseThemePickRankViewHolder<ItemThemePickRankBinding>(
    binding,
    glideRequestManager,
    binding.btnThemePickRankVote,
    binding.ivBlur,
    binding.eivThemePickRank,
) {

    @SuppressLint("SetTextI18n")
    override fun bind(item: ThemepickRankModel, theme: ThemepickModel, position: Int) = with(binding) {
        super.bind(item, theme, position)
        binding.inRankingPickNameAndGroup.setRecompose(mTheme.type != "I")
        binding.inRankingPickNameAndGroup.setNameAndGroupForNoIdol(
            idolName = item.title,
            idolGroup = item.subtitle,
            nameMaxLine = 1,
            groupMaxLine = 1,
        )
        setVoteProgress(item)

        item.idolId?.let { idolId ->
            binding.clRoot.setOnClickListener {
                // 커뮤 이동
                coroutineScope.launch(Dispatchers.IO) {
                    var idol = getIdolByIdUseCase(idolId)
                        .mapDataResource { it?.toPresentation() }
                        .awaitOrThrow()
                    itemView.context.startActivity(CommunityActivity.createIntent(itemView.context, idol))
                }
            }
        }

        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
        tvThemePickRank.text = numberFormat.format(item.rank)

        view.visibility = if (position == 2) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    private fun setVoteProgress(item: ThemepickRankModel) = with(binding.inGradientProgressBar) {
        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))

        tvVote.text = numberFormat.format(item.vote)

        val progressBarPercent = VotePercentage.getVotePercentage(
            minPercentage = item.minPercent.toInt(),
            firstPlaceVote = item.firstPlaceVote,
            currentPlaceVote = item.vote,
            lastPlaceVote = item.lastPlaceVote
        )
        progressBar.setWidthRatio(progressBarPercent, isApply = true)

        val voteParams = (tvVote.layoutParams as ConstraintLayout.LayoutParams).apply {
            setMargins(0, 0, Util.convertDpToPixel(itemView.context, 10f).toInt(), 0)
            horizontalBias = 1.0f
        }
        tvVote.layoutParams = voteParams

        val percent = (item.vote.toDouble() / mTheme.count.toDouble()) * 100
        tvVotePercent.text = numberFormat.format(percent.roundToInt()).plus("%")
    }

    companion object {
        const val MIN_PERCENT_PROGRESS = 5
    }
}