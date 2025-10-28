package net.ib.mn.viewholder

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.adapter.HeartPickAdapter
import net.ib.mn.databinding.ItemHeartPickRankBinding
import net.ib.mn.heartpick.VotingStatus
import net.ib.mn.model.HeartPickIdol
import net.ib.mn.model.HeartPickModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.setOnSingleClickListener
import net.ib.mn.utils.vote.VotePercentage
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

class HeartPickRankViewHolder (
    val binding: ItemHeartPickRankBinding,
    private val heartPickListner: HeartPickAdapter.HeartPickListener,
    private val heartPickId: Int,
    val lifecycleScope: LifecycleCoroutineScope
) : RecyclerView.ViewHolder(binding.root) {

    private lateinit var mGlideRequestManager: RequestManager
    private val numberFormat = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))

    fun bind(
        heartPickModel: HeartPickModel?,
        type: String?,
        position: Int
    ) = with(binding) {
        val heartPickIdol = heartPickModel?.heartPickIdols?.get(position + 1) ?: return@with

        mGlideRequestManager = Glide.with(itemView.context)
        mGlideRequestManager.load(heartPickIdol.image_url)
            .centerCrop()
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(heartPickIdol.idol_id))
            .fallback(Util.noProfileImage(heartPickIdol.idol_id))
            .placeholder(Util.noProfileImage(heartPickIdol.idol_id))
            .dontAnimate()
            .into(ivPhoto)

        inRankingPickNameAndGroup.setRecompose(type != "I")
        inRankingPickNameAndGroup.setNameAndGroupForNoIdol(
            idolName = heartPickIdol.title,
            idolGroup = heartPickIdol.subtitle,
            nameMaxLine = 1,
            groupMaxLine = 1
        )

        tvRank.text = heartPickIdol.rank.toString()

        setToolTip(position, heartPickIdol, heartPickModel.status, heartPickModel.hasGoneToolTip)
        setBtnHeart(heartPickModel.status)
        setVote(
            heartPickModel = heartPickModel,
            heartPickIdol = heartPickIdol,
            heartPickId = heartPickId,
        )

        itemView.setOnClickListener {
            heartPickListner.goCommunity(heartPickIdol)
        }
    }

    private fun setToolTip(
        position: Int,
        heartPickIdol: HeartPickIdol?,
        status: Int,
        hasGoneToolTip: Boolean
    ) = with(binding) {
        if (status == VotingStatus.VOTE_FINISHED.status || hasGoneToolTip) return@with
        if((position == VotingStatus.BEFORE_VOTE.status || position == VotingStatus.VOTING.status) && heartPickIdol?.rank!! > 1) {
            clToolTip.visibility = View.VISIBLE
            tvTooltipDown.text = String.format(itemView.context.getString(R.string.heartpick_tooltip_msg), numberFormat.format(heartPickIdol.rank.minus(1)), numberFormat.format(heartPickIdol.diffVote))
            heartPickListner.onTimer(clToolTip, position)
        } else {
            clToolTip.visibility = View.INVISIBLE
        }
    }

    private fun setBtnHeart(status: Int) = with(binding) {
        val params = clRankingHeart.layoutParams as ConstraintLayout.LayoutParams
        if(status == 2) {
            btnHeart.visibility = View.GONE
            params.marginEnd = Util.convertDpToPixel(itemView.context, 26f).toInt()
        } else {
            btnHeart.visibility = View.VISIBLE
            params.marginEnd = Util.convertDpToPixel(itemView.context, 10f).toInt()
        }
    }

    private fun setVote(
        heartPickModel: HeartPickModel?,
        heartPickIdol: HeartPickIdol?,
        heartPickId: Int
    ) = with(binding) {
        heartPickIdol?:return@with

        btnHeart.setOnSingleClickListener {
            heartPickListner.onVote(heartPickIdol, heartPickId)
        }

        inGradientProgressBar.tvVote.text = numberFormat.format(heartPickIdol.vote)

        val progressBarPercent = VotePercentage.getVotePercentage(
            minPercentage = heartPickModel?.minPercent?.toInt(),
            firstPlaceVote = heartPickModel?.firstPlaceVote?.toLong() ?: 1L,
            currentPlaceVote = heartPickIdol.vote.toLong(),
            lastPlaceVote = heartPickModel?.lastPlaceVote?.toLong() ?: 1L
        )
        inGradientProgressBar.progressBar.setWidthRatio(progressBarPercent, isApply = true)

        val voteParams = (inGradientProgressBar.tvVote.layoutParams as ConstraintLayout.LayoutParams).apply {
            setMargins(0, 0, Util.convertDpToPixel(itemView.context, 10f).toInt(), 0)
            horizontalBias = 1.0f
        }
        inGradientProgressBar.tvVote.layoutParams = voteParams

        if(heartPickModel?.vote == 0 || heartPickModel?.heartPick1stPercent == 0) {
            inGradientProgressBar.tvVotePercent.text = numberFormat.format(0).plus("%")
            return@with
        }
        val votePercent: Float = 100.0f * heartPickIdol.vote.toFloat() / (heartPickModel?.vote
            ?: return@with).toFloat()    // 분모가 0일 경우 앱이 죽기때문에 위에서 예외 처리
        inGradientProgressBar.tvVotePercent.text = numberFormat.format(votePercent.roundToInt()).plus("%")
    }
}