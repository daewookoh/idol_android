/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.onepick.viewholder.themepick

import android.annotation.SuppressLint
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.RequestManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ItemThemePickRank1stBinding
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

class ThemePick1stViewHolder(
    private val binding: ItemThemePickRank1stBinding,
    private val glideRequestManager: RequestManager,
    private val coroutineScope: CoroutineScope,
    private val getIdolByIdUseCase: GetIdolByIdUseCase,
) : BaseThemePickRankViewHolder<ItemThemePickRank1stBinding>(
    binding,
    glideRequestManager,
    binding.btnThemePickRankVote,
    binding.ivBlur,
    binding.eivThemePickRank,
) {

    @SuppressLint("SetTextI18n")
    override fun bind(item: ThemepickRankModel, theme: ThemepickModel, position: Int) {
        super.bind(item, theme, position)
        binding.inRankingPickNameAndGroup.setRecompose(theme.type != "I")
        binding.inRankingPickNameAndGroup.setNameAndGroupForNoIdol(
            idolName = item.title,
            idolGroup = item.subtitle,
            nameMaxLine = 2,
            groupMaxLine = 1,
        )
        binding.inRankingPickNameAndGroup.setTitleTextSize(titleSize = 16f, subTitleSize = 12f)
        item.idolId?.let { idolId ->
            binding.clContainer.setOnClickListener {
                // 커뮤 이동
                coroutineScope.launch(Dispatchers.IO) {
                    var idol = getIdolByIdUseCase(idolId)
                        .mapDataResource { it?.toPresentation() }
                        .awaitOrThrow()
                    itemView.context.startActivity(CommunityActivity.createIntent(itemView.context, idol))
                }
            }
        }
        setVoteProgress(item, theme)
    }

    private fun setVoteProgress(item: ThemepickRankModel, theme: ThemepickModel) = with(binding.inGradientProgressBar) {
        val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
        val percent =
            (item.vote.toDouble() / theme.count.toDouble()) * 100
        tvVote.text = numberFormat.format(item.vote)
        tvVotePercent.text = numberFormat.format(percent.roundToInt()).plus("%")

        progressBar.setWidthRatio(
            VotePercentage.MAX_PERCENTAGE, isApply = true
        )

        val voteParams = (tvVote.layoutParams as ConstraintLayout.LayoutParams).apply {
            setMargins(0, 0, Util.convertDpToPixel(itemView.context, 10f).toInt(), 0)
            horizontalBias = 1.0f
        }
        tvVote.layoutParams = voteParams
    }
}