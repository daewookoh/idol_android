package net.ib.mn.feature.halloffame

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.HallTopItemBinding
import net.ib.mn.model.HallTopModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.Locale

class HallTopViewHolder(
    val binding: HallTopItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    val context = binding.root.context!!

    fun bind(
        item: HallTopModel
    ) = with(binding) {

        val rankCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.rank)
        val rank = String.format(context.getString(R.string.rank_count_format), rankCount)
        textRanking.text = rank

        if (item.status.equals(RANK_NEW)) {
            ranking.visibility = View.GONE
            newRanking.visibility = View.VISIBLE
        } else {
            ranking.visibility = View.VISIBLE
            newRanking.visibility = View.GONE

            val rankImageResource = if (item.status.equals(RANKING_INCREASE)) {
                R.drawable.icon_change_ranking_up
            } else if (item.status.equals(RANKING_DECREASE)) {
                R.drawable.icon_change_ranking_no_change
            } else {
                R.drawable.icon_change_ranking_no_change
            }
            iconRanking.setImageResource(rankImageResource)

            val changeCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(item.difference)
            changeRanking.text = changeCount
        }

        val idolId = item.idol?.getId() ?: 0
        val imageUrl = UtilK.trendImageUrl(context, item.id)

        if (imageUrl.isNotEmpty()) {
            Glide.with(binding.root)
                .load(imageUrl)
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(photo)
        } else {
            photo.setImageResource(Util.noProfileImage(idolId))
        }

        UtilK.setName(context, item.idol, name, group)

        val voteCount = item.heart
        val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(voteCount)
        val voteCountText = String.format(context.getString(R.string.vote_count_format), voteCountComma)
        count.text = voteCountText
    }

    companion object {
        private const val RANK_NEW = "new"
        private const val RANKING_INCREASE = "increase"
        private const val RANKING_DECREASE = "decrease"
    }
}