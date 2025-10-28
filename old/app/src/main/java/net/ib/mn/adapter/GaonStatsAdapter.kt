/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 기록실 gaon2016 Adapter
 *
 * */

package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.databinding.GaonResultHeaderBinding
import net.ib.mn.model.HallModel
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.Locale

class GaonStatsAdapter(
    private val context: Context,
    private var mItems: ArrayList<HallModel>,
    private val awardStatsCode: String?,
    private val title: String?, // 조합된 타이틀 (ex. 가온 어워드 팬투표 <CHART_NAME> 인기상 최종 결과)
    private val glideRequestManager: RequestManager,
    private val mListener: OnClickListener,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private lateinit var date: String

    interface OnClickListener {
        fun onItemClickListener(item: HallModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_TOP -> {
                val binding: GaonResultHeaderBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.gaon_result_header, parent, false)
                TopViewHolder(binding)
            } else -> {
                val binding: AggregatedHofItemBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context), R.layout.aggregated_hof_item, parent, false)
                RankingViewHolder(binding)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_TOP
        }
        return TYPE_RANK
    }

    override fun getItemId(position: Int): Long {
        return mItems[position].hashCode().toLong()
    }

    override fun getItemCount(): Int {
        if (mItems.size == 0) {
            return 0
        }
        return mItems.size
    }

    fun setItems(mItems: ArrayList<HallModel>) {
        this.mItems = mItems
        notifyDataSetChanged()
    }

    fun setDate(date: String) {
        this.date = date
        notifyItemChanged(0)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == TYPE_TOP) {
            (holder as TopViewHolder).apply {
                bind()
            }
        } else {
            (holder as RankingViewHolder).apply {
                bind(mItems[position])
            }
        }
    }

    inner class TopViewHolder(val binding: GaonResultHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            with(binding) {
                labelGaon1.text = title
                period.text = date
            }
        }
    }

    inner class RankingViewHolder(val binding: AggregatedHofItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(hallModel: HallModel) {
            with(binding) {
                clAggContainer.setOnClickListener {
                    mListener.onItemClickListener(hallModel)
                }
                val ranking = hallModel.rank
                if (ranking < 3) {
                    iconRanking.visibility = View.VISIBLE
                    when (ranking) {
                        0 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                        1 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                        2 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                    }
                } else {
                    iconRanking.visibility = View.GONE
                }

                UtilK.setName(context, hallModel, name, group)

                val scoreCount: String =
                    NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(hallModel.score.toLong())
                        .replace(",", "")
                val scoreText: String = String.format(context.getString(R.string.score_format), scoreCount)
                score.text = scoreText
                val rankCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format((ranking + 1).toLong())
                rank.text = String.format(
                    context.getString(R.string.rank_format),
                    rankCount,
                )

                val idolId: Int = hallModel.idol?.getId() ?: 0
                if (hallModel.idol?.imageUrl != null) {
                    glideRequestManager
                        .load(hallModel.idol?.imageUrl)
                        .apply(RequestOptions.circleCropTransform())
                        .error(Util.noProfileImage(idolId))
                        .fallback(Util.noProfileImage(idolId))
                        .placeholder(Util.noProfileImage(idolId))
                        .into(photo)
                } else {
                    glideRequestManager.clear(photo)
                    photo.setImageResource(Util.noProfileImage(idolId))
                }
            }
        }
    }

    companion object {
        const val TYPE_TOP = 0
        const val TYPE_RANK = 1
    }
}