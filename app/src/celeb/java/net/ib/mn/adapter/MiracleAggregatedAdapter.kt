package net.ib.mn.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.R
import net.ib.mn.core.model.ChartModel
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.databinding.MiracleHeaderBinding
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.Locale


/**
 * 이달의 기적 누적 순위 adapter
 */
class MiracleAggregatedAdapter(
    private var items: ArrayList<AggregateRankModel>,
    private var chartModel: ChartModel,
    private val onClickListener: OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnClickListener {
        fun onInfoClicked()
    }

    private var topViewCount = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            TYPE_TOP -> {
                val binding = DataBindingUtil.inflate<MiracleHeaderBinding>(
                    LayoutInflater.from(parent.context),
                    R.layout.miracle_header,
                    parent,
                    false
                )
                TopViewHolder(binding)
            }

            else -> {
                val binding = AggregatedHofItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                RankViewHolder(binding)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        var id = 0L
        if (items.isNotEmpty()) {
            if (position == 0) {
                id = 0
            } else if (items.size >= position) {
                id = items[position - topViewCount].idolId.toLong()
            }
        }
        return id
    }

    override fun getItemCount(): Int {
        if (items.size == 0)
            return 0
        return items.size + topViewCount
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_TOP
        }
        return TYPE_RANK
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (items.isEmpty()) return

        when (holder) {
            is TopViewHolder -> {
                holder.bind(chartModel)
            }

            is RankViewHolder -> {
                holder.bind(items[position - topViewCount])
            }
        }
    }

    fun clear() {
        items.clear()
    }

    fun setItems(newItems: ArrayList<AggregateRankModel>) {
        items.apply {
            clear()
            addAll(newItems)
        }
    }

    inner class TopViewHolder(
        private val binding: MiracleHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val context: Context = binding.root.context

        @SuppressLint("SetTextI18n")
        fun bind(item: ChartModel) {

            Glide.with(binding.root)
                .load(item.imageRankUrl)
                .into(binding.ivMiraclePhoto)

            if (!UtilK.dateToKST(item.beginDate)
                    .isNullOrEmpty() && !UtilK.dateToKST(item.endDate).isNullOrEmpty()
            ) {
                binding.tvMiraclePeriod.text =
                    "${context.getString(R.string.miracle_aggregating_period) + " : "} ${
                        UtilK.dateToKST(item.beginDate)
                    } ~ ${UtilK.dateToKST(item.endDate)}"
            }

            if (ConfigModel.getInstance(context).showMiracleInfo != 0) {
                binding.btnMiracleInfo.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { onClickListener.onInfoClicked() }
                }
            }
        }
    }

    inner class RankViewHolder(
        val binding: AggregatedHofItemBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        val context: Context = binding.root.context

        fun bind(item: AggregateRankModel) = with(binding) {

            ivArrowGo.visibility = View.GONE

            val scoreRank = item.scoreRank

            if (scoreRank < 3) {
                iconRanking.visibility = View.VISIBLE
                when (scoreRank) {
                    0 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                    1 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                    2 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                }
                rank.setTextColor(ContextCompat.getColor(context, R.color.main))
            } else {
                iconRanking.visibility = View.GONE
                rank.setTextColor(ContextCompat.getColor(context, R.color.gray580))
            }

            val nameSplit = item.name.split("_")
            if (nameSplit.size > 1) {
                name.text = nameSplit.first()
                group.apply {
                    text = nameSplit.last()
                    visibility = View.VISIBLE
                }
            } else {
                name.text = nameSplit.first()
                group.visibility = View.GONE
            }

            val scoreCount =
                NumberFormat.getNumberInstance(Locale.getDefault()).format(item.score.toLong())
                    .replace(",", "")
            val scoreText: String =
                String.format(context.getString(R.string.score_format), scoreCount)
            score.text = scoreText

            rank.text = String.format(context.getString(R.string.rank_format), scoreRank.toString())

            val idolId = item.idolId
            val glide = Glide.with(binding.root)

            if (item.trendId != 0) {
                val imageUrl = UtilK.trendImageUrl(context, item.trendId)
                glide
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(idolId))
                    .fallback(Util.noProfileImage(idolId))
                    .placeholder(Util.noProfileImage(idolId))
                    .dontAnimate()
                    .into(photo)
            } else {
                glide.clear(photo)
                photo.setImageResource(Util.noProfileImage(idolId))
            }
        }
    }

    companion object {
        const val TYPE_TOP = 0
        const val TYPE_RANK = 1
    }
}
