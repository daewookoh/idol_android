package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.HallOfFameAggHistoryActivity
import net.ib.mn.activity.HallOfFameAggHistoryLeagueActivity
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.HallAggTopItemBinding
import net.ib.mn.model.ConfigModel.Companion.getInstance
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.trendImageUrl
import java.text.NumberFormat

class HallAggHistoryAdapter : ArrayAdapter<HallAggHistoryModel?> {
    private val mContext: Context
    private val mGlideRequestManager: RequestManager

    private var sourceApp: String? = null

    constructor(context: Context, glideRequestManager: RequestManager) : super(
        context,
        R.layout.hall_agg_top_item
    ) {
        mContext = context
        mGlideRequestManager = glideRequestManager
    }

    constructor(context: Context, glideRequestManager: RequestManager, sourceApp: String?) : super(
        context,
        R.layout.hall_agg_top_item
    ) {
        mContext = context
        mGlideRequestManager = glideRequestManager
        this.sourceApp = sourceApp
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: HallAggTopItemBinding

        if(convertView == null) {
            binding = HallAggTopItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as HallAggTopItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: HallAggHistoryModel?, position: Int) = with(view?.tag as HallAggTopItemBinding) {
        val view = view ?: return
        val item = item ?: return

        val idolRank =
            NumberFormat.getNumberInstance(getAppLocale(mContext)).format(item.rank.toLong())
        val rank = if (item.rank == 999)
            "-"
        else String.format(mContext.getString(R.string.rank_count_format), idolRank)
        textRanking.setText(rank)

        if (mContext.javaClass.getSimpleName().contains(
                if (BuildConfig.CELEB) HallOfFameAggHistoryActivity::class.java.getSimpleName() else HallOfFameAggHistoryLeagueActivity::class.java.getSimpleName()
            )
        ) {    //명예전당에서 들어왔을 때만 화살표 보이게
            ivArrowGo.setVisibility(View.VISIBLE)
        }

        if (item.status.equals(RANKING_NEW, ignoreCase = true)) {
            ranking.setVisibility(View.GONE)
            newRanking.setVisibility(View.VISIBLE)
        } else {
            ranking.setVisibility(View.VISIBLE)
            newRanking.setVisibility(View.GONE)
            if (item.status.equals(RANKING_INCREASE, ignoreCase = true)) {
                iconRanking.setImageResource(R.drawable.icon_change_ranking_up)
            } else if (item.status.equals(RANKING_DECREASE, ignoreCase = true)) {
                iconRanking.setImageResource(R.drawable.icon_change_ranking_down)
            } else {
                iconRanking.setImageResource(R.drawable.icon_change_ranking_no_change)
            }
            val changeRankingCount = NumberFormat.getNumberInstance(getAppLocale(mContext))
                .format(item.difference.toLong())
            changeRanking.setText(changeRankingCount)
        }

        val idolId = item.idol?.getId() ?: 0
        if (item.resource_uri != null) {
            val imageUrl = trendImageUrl(context, item.getResourceId(), sourceApp)
            Util.log("HallAggHistory:: " + imageUrl)
            mGlideRequestManager
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(photo)
        } else {
            mGlideRequestManager.clear(photo)
            photo.setImageResource(Util.noProfileImage(idolId))
        }
        val scoreCount = NumberFormat.getNumberInstance(getAppLocale(mContext))
            .format((if (item.rank < (getInstance(mContext).cutLine + 1)) (getInstance(mContext).cutLine + 1) - item.rank else 0).toLong())
        var scoreText = String.format(
            mContext.getString(R.string.score_format), scoreCount
        )
        if (item.rank < (getInstance(mContext).cutLine + 1)) {
            scoreText = "+" + scoreText
        }
        score.setText(scoreText)
        val dateValue = item.getRefdate(mContext)
        date.setText(dateValue)
        val voteCount = item.heart
        val voteCountComma = NumberFormat.getNumberInstance(getAppLocale(mContext))
            .format(voteCount)
        val voteCountText = String.format(
            mContext.getString(R.string.vote_count_format),
            voteCountComma
        )
        count.setText(voteCountText)
    }

    companion object {
        private const val RANKING_INCREASE = "increase"
        private const val RANKING_DECREASE = "decrease"
        private const val RANKING_SAME = "same"
        private const val RANKING_NEW = "new"
    }
}
