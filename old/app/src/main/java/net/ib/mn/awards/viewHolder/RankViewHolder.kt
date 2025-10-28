package net.ib.mn.awards.viewHolder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.Locale

class RankViewHolder(
    val binding: AggregatedHofItemBinding,
    val glideRequestManager: RequestManager,
    val votable: String,
    val isOffset: Int,
    private val onItemClicked: (IdolModel?) -> Unit,
) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(item: HallModel) {
        with(binding) {
            val context = itemView.context
            // 동점자 처리
            val rank: Int = item.rank

            val rankRange =
                if (votable == "Y") rank else rank + 1 // 누적순위의 경우 TopViewHolder에 1등이 없고, 최종결과의 경우 TopViewHolder에 1등이 존재하여 왕관 범위를 다르게 설정해야한다.
            iconRanking.visibility = View.VISIBLE
            when (rankRange) {
                1 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                2 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                3 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                else -> iconRanking.visibility = View.GONE
            }

            UtilK.setName(context, item.idol, name, group)

            val scoreCount: String =
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.score)
                    .replace(("[^\\d.]").toRegex(), "")
            val scoreText: String = String.format(
                context.getString(R.string.score_format),
                scoreCount,
            )
            score.text = scoreText
            val rankCount =
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(rank + 1 - isOffset)
            binding.rank.text = String.format(
                context.getString(R.string.rank_format),
                rankCount,
            )
            val idolId = item.idol?.getId() ?: 0
            if (item.id != null) {
                val imageUrl = UtilK.trendImageUrl(context, item.id, item.idol?.sourceApp)
                Util.log("AwardsAggregated:: $imageUrl")
                glideRequestManager
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(idolId))
                    .fallback(Util.noProfileImage(idolId))
                    .placeholder(Util.noProfileImage(idolId))
                    .dontAnimate()
                    .into(photo)
            } else {
                if (item.imageUrl != null) {
                    glideRequestManager
                        .load(item.imageUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .error(Util.noProfileImage(idolId))
                        .fallback(Util.noProfileImage(idolId))
                        .placeholder(Util.noProfileImage(idolId))
                        .dontAnimate()
                        .into(photo)
                } else {
                    glideRequestManager.clear(photo)
                    photo.setImageResource(Util.noProfileImage(idolId))
                }
            }

            // 기부천사/기부요정 -> 명전에서는 숨김
            //        Util.setAngelFairyIcon(iconAngel, iconFairy, item.getIdol())
            itemView.setOnClickListener { onItemClicked(item.idol) }
        }
    }
}