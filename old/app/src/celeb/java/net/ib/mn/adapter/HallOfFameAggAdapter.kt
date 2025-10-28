/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 개인/그룹 누적 Adapter
 *
 * */

package net.ib.mn.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.AggregatedHofHeaderBinding
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Const
import com.bumptech.glide.Glide
import net.ib.mn.utils.DateUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getFontColor
import java.text.NumberFormat
import java.util.*

class HallOfFameAggAdapter(
    private val context: Context,
    private val mListener: OnClickListener,
    private var mItems: ArrayList<HallModel>,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mGlideRequestManager: RequestManager = Glide.with(context)

    private var typeName: String = context.getString(R.string.overall)
    private var type: String? = null

    interface OnClickListener {
        fun filterClicked()
        fun onItemClicked(item: HallModel?)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TOP -> {
                val aggregatedHofHeader: AggregatedHofHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.aggregated_hof_header,
                    parent,
                    false,
                )
                TopViewHolder(aggregatedHofHeader)
            }

            else -> {
                val aggregatedHofItem: AggregatedHofItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.aggregated_hof_item,
                    parent,
                    false,
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
            } }
    }

    fun setItems(items: ArrayList<HallModel>, typeName: String?, type: String?) {
        mItems.clear()
        mItems = items
        if (!typeName.isNullOrEmpty()) {
            this.typeName = typeName
        }
        this.type = type
        notifyDataSetChanged()
    }

    inner class TopViewHolder(val binding: AggregatedHofHeaderBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hallModel: HallModel) {
            val idolCount: String = NumberFormat.getNumberInstance(Locale.getDefault())
                .format(hallModel.difference.toLong())

            binding.tvAggTypeFilter.text = typeName
            // 셀럽 타입 필터 선택
            binding.aggTypeFilter.setOnClickListener {
                mListener.filterClicked()
            }

            // 누적순위 변화 화면으로 이동
            binding.btnIdol.setOnClickListener {
                mListener.onItemClicked(hallModel)
            }

            // 급상승 1위 아이디 설정값과  해당 item 의  id 값이 같으면,  급상승 1위 아이돌이다.
            if (hallModel.id == hallModel.topOneDifferenceId && type == null) {
                binding.tvIncreaseStep.visibility = View.VISIBLE
                binding.ivIconUp.visibility = View.VISIBLE
                binding.tvIncreaseStep.text = String.format(
                    Locale.getDefault(),
                    context.resources.getString(R.string.label_rising),
                    hallModel.difference,
                )
            } else { // 급상승 1위 아이돌이 아닐경우
                binding.tvIncreaseStep.visibility = View.INVISIBLE
                binding.ivIconUp.visibility = View.INVISIBLE
            }

            // 순위 변동 값
            binding.tvChangeRanking.text = idolCount

            // status에 따라  icon  다르게 넣어줌.
            if (hallModel.status.equals(RANKING_INCREASE, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.GONE
                binding.llChangeRanking.visibility = View.VISIBLE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_up)
            } else if (hallModel.status.equals(
                    RANKING_DECREASE,
                    ignoreCase = true,
                )
            ) {
                binding.iconNewRanking.visibility = View.GONE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_down)
                binding.llChangeRanking.visibility = View.VISIBLE
            } else if (hallModel.status.equals(RANKING_SAME, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.GONE
                binding.llChangeRanking.visibility = View.GONE
            } else if (hallModel.status.equals(RANKING_NEW, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.VISIBLE
                binding.llChangeRanking.visibility = View.GONE
                binding.iconNewRanking.setImageResource(R.drawable.icon_change_ranking_new)
            }

            UtilK.setName(context, hallModel.idol, binding.name, binding.group)
            val rankViewList = listOf<AppCompatTextView>(binding.titleRank, binding.name, binding.group, binding.score)
            val hallColor = if(type == null) ContextCompat.getColor(context, R.color.main) else Color.parseColor(UtilK.getTypeList(context, type).getFontColor(context))
            rankViewList.forEach { it.setTextColor(hallColor) }


            val scoreCount: String =
                NumberFormat.getNumberInstance(Locale.getDefault()).format(hallModel.score.toLong())
                    .replace(",", "")
            val scoreText: String =
                String.format(context.getString(R.string.score_format), scoreCount)
            binding.score.text = "/ $scoreText"
            binding.titleRank.text = String.format(
                context.getString(R.string.rank_format),
                NumberFormat.getNumberInstance(Locale.getDefault()).format((1).toLong()),
            )

            val idolId: Int = hallModel.idol?.getId() ?: 0
            if (hallModel.idol?.imageUrl != null) {
                val imageUrl =
                    ConfigModel.getInstance(context).cdnUrl + "/t/" + hallModel.trendId + ".1_" + Const.IMAGE_SIZE_LOWEST + ".webp"
                Util.log("HallAgg::$imageUrl")
                mGlideRequestManager
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(idolId))
                    .fallback(Util.noProfileImage(idolId))
                    .placeholder(Util.noProfileImage(idolId))
                    .dontAnimate()
                    .into(binding.photo)
            } else {
                mGlideRequestManager.clear(binding.photo)
                binding.photo.setImageResource(Util.noProfileImage(idolId))
            }

            binding.period.text = DateUtil.getHallOfFameDateString()

            //종합 이외 일떄 급상승, 순위변화 아이콘 안보이게 함.
            if (type != null) {
                binding.llChangeRanking.visibility = View.GONE
                binding.tvIncreaseStep.visibility = View.INVISIBLE
                binding.ivIconUp.visibility = View.INVISIBLE
                binding.iconNewRanking.visibility = View.GONE
            }
        }
    }

    inner class RankViewHolder(val binding: AggregatedHofItemBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(hallModel: HallModel, position: Int) {
            val idolCount: String = NumberFormat.getNumberInstance(Locale.getDefault())
                .format(hallModel.difference.toLong())

            // 누적순위 변화 화면으로 이동
            binding.clAggContainer.setOnClickListener {
                mListener.onItemClicked(hallModel)
            }
            // 급상승 1위 아이디 설정값과  해당 item 의  id 값이 같으면,  급상승 1위 아이돌이다.
            if (hallModel.id == hallModel.topOneDifferenceId && type == null) {
                binding.tvIncreaseStep.visibility = View.VISIBLE
                binding.ivIconUp.visibility = View.VISIBLE
                binding.tvIncreaseStep.text = String.format(
                    Locale.getDefault(),
                    context.resources.getString(R.string.label_rising),
                    hallModel.difference,
                )
                binding.clAggContainer.background = ContextCompat.getDrawable(context, R.drawable.bg_cumulative_best)

//                context.setBackground(ContextCompat.getDrawable(context,R.drawable.bg_cumulative_best))
                // 가장 마지막  포지션이  급상승일때는  ->  전체 컨테이ㅓ end 부분에 급상승 추가해준다.
                if (position == mItems.size - 1) {
                    binding.clAggContainer.setPadding(
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 50f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt(),
                    )
                } else { // 나머지 포지션의 경우 아래처럼
                    binding.clAggContainer.setPadding(
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt(),
                        Util.convertDpToPixel(context, 10f).toInt(),
                    )
                }
            } else { // 급상승 1위 아이돌이 아닐경우
                binding.clAggContainer.setPadding(
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 10f).toInt(),
                    Util.convertDpToPixel(context, 10f).toInt(),
                )
                binding.tvIncreaseStep.visibility = View.INVISIBLE
                binding.ivIconUp.visibility = View.INVISIBLE
//                view.setBackground(null)
                binding.clAggContainer.background = ContextCompat.getDrawable(context, R.color.background)
            }

            // 순위 변동 값
            binding.tvChangeRanking.text = idolCount

            // status에 따라  icon  다르게 넣어줌.
            if (hallModel.status.equals(RANKING_INCREASE, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.GONE
                binding.llChangeRanking.visibility = View.VISIBLE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_up)
            } else if (hallModel.status.equals(
                    RANKING_DECREASE,
                    ignoreCase = true,
                )
            ) {
                binding.iconNewRanking.visibility = View.GONE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_down)
                binding.llChangeRanking.visibility = View.VISIBLE
            } else if (hallModel.status.equals(RANKING_SAME, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.GONE
                binding.llChangeRanking.visibility = View.VISIBLE
                binding.iconChangeRanking.setImageResource(R.drawable.icon_change_ranking_no_change)
            } else if (hallModel.status.equals(RANKING_NEW, ignoreCase = true)) {
                binding.iconNewRanking.visibility = View.VISIBLE
                binding.llChangeRanking.visibility = View.GONE
                binding.iconNewRanking.setImageResource(R.drawable.icon_change_ranking_new)
            }

            // 동점자 처리
            val rank: Int = hallModel.rank // rank는 0부터
            if (rank < 3) {
                binding.iconRanking.visibility = View.VISIBLE
                when (rank) {
                    1 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                    2 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                }
                binding.rank.setTextColor(ContextCompat.getColor(context, R.color.main))
            } else {
                binding.iconRanking.visibility = View.GONE
                binding.rank.setTextColor(ContextCompat.getColor(context, R.color.gray580))
            }

            UtilK.setName(context, hallModel.idol, binding.name, binding.group)

            val scoreCount: String = NumberFormat.getNumberInstance(Locale.getDefault())
                .format(hallModel.score.toLong()).replace(",", "")
            val scoreText: String =
                String.format(context.getString(R.string.score_format), scoreCount)
            binding.score.text = scoreText
            val idolCount2 =
                NumberFormat.getNumberInstance(Locale.getDefault()).format((rank + 1).toLong())
            binding.rank.text = String.format(context.getString(R.string.rank_format), idolCount2)
            val idolId: Int = hallModel.idol?.getId() ?: 0
            if (hallModel.idol?.imageUrl != null) {
                val imageUrl =
                    ConfigModel.getInstance(context).cdnUrl + "/t/" + hallModel.trendId + ".1_" + Const.IMAGE_SIZE_LOWEST + ".webp"
                mGlideRequestManager
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(idolId))
                    .fallback(Util.noProfileImage(idolId))
                    .placeholder(Util.noProfileImage(idolId))
                    .dontAnimate()
                    .into(binding.photo)
            } else {
                mGlideRequestManager.clear(binding.photo)
                binding.photo.setImageResource(Util.noProfileImage(idolId))
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