package net.ib.mn.adapter

import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.AggregatedHofHeaderBinding
import net.ib.mn.databinding.AggregatedHofItemBinding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.fragment.HallOfFameAggFragment
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.setMargins
import java.text.DateFormat
import java.text.NumberFormat
import java.util.*

/**
 * Copyright 2023-01-12,목,15:26. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 개인/그룹 누적 Adapter
 *
 **/
class HallOfFameAggAdapter(
    private val context: Context,
    private val fragment: HallOfFameAggFragment,
    private val mListener: OnClickListener,
    private var mItems: MutableList<AggregateRankModel>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnClickListener {
        fun onItemClicked(item: AggregateRankModel?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TOP -> {
                val aggregatedHofHeader : AggregatedHofHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.aggregated_hof_header,
                    parent,
                    false
                )
                TopViewHolder(aggregatedHofHeader)
            }

            else -> {
                val aggregatedHofItem : AggregatedHofItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.aggregated_hof_item,
                    parent,
                    false
                )
                RankViewHolder(aggregatedHofItem)
            }
        }
    }

    override fun getItemCount(): Int = mItems.size

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            TYPE_TOP
        } else {
            TYPE_RANK
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mItems.isNullOrEmpty()) return

        if (holder.itemViewType == TYPE_TOP) {
            (holder as TopViewHolder).apply {
                bind(mItems[position])
            }
        } else {
            (holder as RankViewHolder).apply {
                bind(mItems[position], position)

                val margin = if (position == mItems.size - 1) {
                    9f
                } else {
                    0f
                }
                holder.itemView.setMargins(bottom = margin)
            }
        }
    }

    fun setItems(items: MutableList<AggregateRankModel>) {
        mItems.clear()
        mItems = items
        notifyDataSetChanged()
    }

    inner class TopViewHolder(val binding: AggregatedHofHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(rankModel: AggregateRankModel) {
            val idolCount: String = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                .format(rankModel.difference.toLong())
            binding.view = fragment // info 버튼 클릭용

            //누적순위 변화 화면으로 이동
            binding.btnIdol.setOnClickListener {
                mListener.onItemClicked(rankModel)
            }

            //순위 변동 값
            binding.tvChangeRanking.text = idolCount

            //status에 따라  icon  다르게 넣어줌.
            if (rankModel.status.equals(RANKING_INCREASE, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.GONE
                binding.llChangeRanking.visibility = View.VISIBLE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_up)
            } else if (rankModel.status.equals(
                    RANKING_DECREASE,
                    ignoreCase = true
                )
            ) {
                binding.iconNewRanking.visibility = View.GONE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_down)
                binding.llChangeRanking.visibility = View.VISIBLE
            } else if (rankModel.status.equals(RANKING_SAME, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.GONE
                binding.llChangeRanking.visibility = View.GONE
            } else if (rankModel.status.equals(RANKING_NEW, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.VISIBLE
                binding.llChangeRanking.visibility = View.GONE
                binding.iconNewRanking.setImageResource(R.drawable.icon_change_ranking_new)
            }

            UtilK.setName(rankModel.name, binding.name, binding.group)

            val scoreCount: String =
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(rankModel.score.toLong())
                    .replace(",", "")
            val scoreText: String =
                String.format(context.getString(R.string.score_format), scoreCount)
            binding.score.text = "/ $scoreText"
            binding.titleRank.text = String.format(
                context.getString(R.string.rank_format),
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format((1).toLong())
            )

            val glide = Glide.with(binding.root)

            if (rankModel.trendId != 0) {
                val imageUrl = UtilK.trendImageUrl(context, rankModel.trendId)
                glide
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(rankModel.idolId))
                    .fallback(Util.noProfileImage(rankModel.idolId))
                    .placeholder(Util.noProfileImage(rankModel.idolId))
                    .dontAnimate()
                    .into(binding.photo)
            } else {
                glide.clear(binding.photo)
                binding.photo.setImageResource(Util.noProfileImage(rankModel.idolId))
            }

            if (rankModel.suddenIncrease) {
                binding.tvIncreaseStep.visibility = View.VISIBLE
                binding.ivIconUp.visibility = View.VISIBLE
                binding.tvIncreaseStep.text = String.format(
                    LocaleUtil.getAppLocale(itemView.context),
                    context.resources.getString(R.string.label_rising),
                    rankModel.difference
                )

            } else { //급상승 1위 아이돌이 아닐경우
                binding.tvIncreaseStep.visibility = View.INVISIBLE
                binding.ivIconUp.visibility = View.INVISIBLE
            }

            val today = Date()
            var cal: Calendar = GregorianCalendar()
            cal.time = today
            cal.add(Calendar.DATE, -30)
            cal[Calendar.HOUR_OF_DAY] = 11
            val fromDate = cal.time

            cal = GregorianCalendar()
            cal.setTime(today)
            cal[Calendar.HOUR_OF_DAY] = 23
            var toDate = cal.getTime()
            val f = DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))
            val fromText = f.format(fromDate)
            if (today.time < toDate.time) {
                // time less than agg time
                cal = GregorianCalendar()
                cal.setTime(today)
                cal.add(Calendar.DATE, -1)
                cal[Calendar.HOUR_OF_DAY] = 23
                toDate = cal.getTime()
            }
            val toText = f.format(toDate)
            binding.period.text = "$fromText ~ $toText"
        }
    }

    inner class RankViewHolder(val binding: AggregatedHofItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(rankModel: AggregateRankModel, position: Int) {
            val idolCount: String = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                .format(rankModel.difference.toLong())

            //누적순위 변화 화면으로 이동
            binding.clAggContainer.setOnClickListener {
                mListener.onItemClicked(rankModel)
            }

            //순위 변동 값
            binding.tvChangeRanking.text = idolCount

            //status에 따라  icon  다르게 넣어줌.
            if (rankModel.status.equals(RANKING_INCREASE, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.GONE
                binding.llChangeRanking.visibility = View.VISIBLE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_up)
            } else if (rankModel.status.equals(
                    RANKING_DECREASE,
                    ignoreCase = true
                )
            ) {
                binding.iconNewRanking.visibility = View.GONE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_down)
                binding.llChangeRanking.visibility = View.VISIBLE
            } else if (rankModel.status.equals(RANKING_SAME, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.GONE
                binding.llChangeRanking.visibility = View.VISIBLE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_no_change)
            } else if (rankModel.status.equals(RANKING_NEW, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.VISIBLE
                binding.llChangeRanking.visibility = View.GONE
                binding.iconNewRanking.setImageResource(R.drawable.icon_change_ranking_new)
            }

            // 동점자 처리
            val rank: Int = rankModel.scoreRank // rank는 0부터
            if (rank < 3) {
                binding.iconRanking.visibility = View.VISIBLE
                when (rank) {
                    0 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                    1 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                    2 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                }
                binding.rank.setTextColor(ContextCompat.getColor(context, R.color.main))
            } else {
                binding.iconRanking.visibility = View.GONE
                binding.rank.setTextColor(ContextCompat.getColor(context, R.color.gray580))
            }

            UtilK.setName(rankModel.name, binding.name, binding.group)

            val scoreCount: String = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                .format(rankModel.score.toLong()).replace(",", "")
            val scoreText: String =
                String.format(context.getString(R.string.score_format), scoreCount)
            binding.score.text = scoreText
            val idolCount2 =
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format((rank + 1).toLong())
            binding.rank.text = String.format(context.getString(R.string.rank_format), idolCount2)

            if (rankModel.suddenIncrease) {
                binding.tvIncreaseStep.visibility = View.VISIBLE
                binding.ivIconUp.visibility = View.VISIBLE
                binding.tvIncreaseStep.text = String.format(
                    LocaleUtil.getAppLocale(itemView.context),
                    context.resources.getString(R.string.label_rising),
                    rankModel.difference
                )
                binding.clAggContainer.background = ContextCompat.getDrawable(context, R.drawable.bg_cumulative_best)

                if (position == mItems.size - 1) {
                    binding.clAggContainer.setPadding(
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 50f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt()
                    )
                } else { //나머지 포지션의 경우 아래처럼
                    binding.clAggContainer.setPadding(
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt()
                    )
                }
            } else { //급상승 1위 아이돌이 아닐경우
                binding.clAggContainer.setPadding(
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 10f).toInt()
                )
                binding.clAggContainer.background = ContextCompat.getDrawable(context, R.color.background_100)
                binding.tvIncreaseStep.visibility = View.INVISIBLE
                binding.ivIconUp.visibility = View.INVISIBLE
            }

            val glide = Glide.with(binding.root)

            if (rankModel.trendId != 0) {
                val cacheKey = "hof_thumb_${rankModel.idolId}_${rankModel.trendId}"
                val imageUrl = UtilK.trendImageUrl(context, rankModel.trendId)
                glide
                    .asBitmap()
                    .load(imageUrl)
                    .apply(
                        RequestOptions()
                            .circleCrop()
                            .placeholder(Util.noProfileImage(rankModel.idolId)) // 로딩 중 기본 이미지
                            .error(Util.noProfileImage(rankModel.idolId))
                            .signature(ObjectKey(cacheKey))
                    )
                    .dontAnimate()
                    .into(binding.photo)
            } else {
                glide.clear(binding.photo)
                binding.photo.setImageResource(Util.noProfileImage(rankModel.idolId))
            }
        }
    }

    companion object {
        const val TYPE_TOP = 0
        const val TYPE_RANK = 1
        const val RANKING_INCREASE = "increase"
        const val RANKING_DECREASE = "decrease"
        const val RANKING_SAME = "same"
        const val RANKING_NEW = "new"
    }
}