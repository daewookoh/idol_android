package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.addon.ArrayAdapter
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.model.HallModel
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK.Companion.setName
import net.ib.mn.utils.UtilK.Companion.setRelativePadding
import net.ib.mn.utils.UtilK.Companion.trendImageUrl
import java.text.NumberFormat

/**
 * 개인누적순위
 */

class HallAggregatedAdapter(
    context: Context,
    private val mGlideRequestManager: RequestManager
) : ArrayAdapter<HallModel?>(
    context, R.layout.aggregated_hof_item
) {
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val binding: AggregatedHofItemBinding

        if(convertView == null) {
            binding = AggregatedHofItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
            binding.root.tag = binding
        } else {
            binding = convertView.tag as AggregatedHofItemBinding
        }

        update(binding.root, getItem(position), position)
        return binding.root
    }

    override fun update(view: View?, item: HallModel?, position: Int): Unit = with(view?.tag as AggregatedHofItemBinding) {
        val view = view ?: return
        val item = item ?: return

        val idolCount = NumberFormat.getNumberInstance(getAppLocale(context))
            .format(item.difference.toLong())
        //급상승 1위 아이디 설정값과  해당 item 의  id 값이 같으면,  급상승 1위 아이돌이다.
        if (item.id == item.topOneDifferenceId) {
            tvIncreaseStep.setVisibility(View.VISIBLE)
            ivIconUp.setVisibility(View.VISIBLE)
            tvIncreaseStep.setText(
                String.format(
                    getAppLocale(context), view.getContext().getResources().getString(
                        R.string.label_rising
                    ), item.difference
                )
            )
            view.setBackground(
                ContextCompat.getDrawable(
                    view.getContext(),
                    R.drawable.bg_cumulative_best
                )
            )

            //가장 마지막  포지션이  급상승일때는  ->  전체 컨테이ㅓ end 부분에 급상승 추가해준다.
            if (isLastPosition(position)) {
                clAggContainer.setRelativePadding(10f, 10f, 50f, 10f)
            } else { //나머지 포지션의 경우 아래처럼
                clAggContainer.setRelativePadding(10f, 10f, 10f, 10f)
            }
        } else { //급상승 1위 아이돌이 아닐경우
            clAggContainer.setRelativePadding(10f, 10f, 10f, 10f)
            tvIncreaseStep.setVisibility(View.INVISIBLE)
            ivIconUp.setVisibility(View.INVISIBLE)
            view.setBackground(null)
        }

        //순위 변동 값
        tvChangeRanking.setText(idolCount)

        //status에 따라  icon  다르게 넣어줌.
        if (item.status.equals(RANKING_INCREASE, ignoreCase = true)) {
            iconNewRanking.setVisibility(View.GONE)
            llChangeRanking.setVisibility(View.VISIBLE)
            iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_up)
        } else if (item.status.equals(RANKING_DECREASE, ignoreCase = true)) {
            iconNewRanking.setVisibility(View.GONE)
            iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_down)
            llChangeRanking.setVisibility(View.VISIBLE)
        } else if (item.status.equals(RANKING_SAME, ignoreCase = true)) {
            iconNewRanking.setVisibility(View.GONE)
            llChangeRanking.setVisibility(View.VISIBLE)
            iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_no_change)
        } else if (item.status.equals(RANKING_NEW, ignoreCase = true)) {
            iconNewRanking.setVisibility(View.VISIBLE)
            llChangeRanking.setVisibility(View.GONE)
            iconNewRanking.setImageResource(R.drawable.icon_change_ranking_new)
        } else {
            llChangeRanking.setVisibility(View.GONE)
            iconNewRanking.setVisibility(View.GONE)
        }


        // 동점자 처리
        val rankValue = item.rank // rank는 0부터

        if (rankValue < 3) {
            iconRanking.setVisibility(View.VISIBLE)
            when (rankValue) {
                0 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                1 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                2 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
            }
            rank.setTextColor(ContextCompat.getColor(context, R.color.main))
        } else {
            iconRanking.setVisibility(View.GONE)
            rank.setTextColor(ContextCompat.getColor(context, R.color.text_default))
        }

        setName(context, item, name, group)

        val scoreCount =
            NumberFormat.getNumberInstance(getAppLocale(context)).format(item.score.toLong())
                .replace(",", "")
        val scoreText = String.format(
            context.getString(R.string.score_format), scoreCount
        )
        score.setText(scoreText)
        val idolCount2 =
            NumberFormat.getNumberInstance(getAppLocale(context)).format((rankValue + 1).toLong())
        rank.setText(
            String.format(
                context.getString(R.string.rank_format),
                idolCount2
            )
        )
        val idolId = item.idol?.getId() ?: 0
        if (item.idol?.imageUrl != null) {
            val imageUrl = trendImageUrl(context, item.id)

            Util.log("HallAgg::" + imageUrl)
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

        // 기부천사/기부요정 -> 명전에서는 숨김
//        Util.setAngelFairyIcon(iconAngel, iconFairy, item.getIdol());
    }

    companion object {
        private const val RANKING_INCREASE = "increase"
        private const val RANKING_DECREASE = "decrease"
        private const val RANKING_SAME = "same"
        private const val RANKING_NEW = "new"
    }
}
