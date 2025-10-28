package net.ib.mn.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.awards.viewHolder.BaseAwardsTopViewHolder
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.databinding.StatsAwardsResultHeaderBinding
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.*


class AAA2020Adapter(
    private val context: Context,
    private val glideRequestManager: RequestManager,
    private val onClickListener: OnClickListener,
    private var mItems: ArrayList<HallModel>,
    private val awardStatsCode: String?,
    private val title: String?,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var isOffset = 0

    interface OnClickListener {
        fun onItemClickListener(item: HallModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        return when (viewType) {
            TYPE_TOP -> {
                val awardsResultHeader: StatsAwardsResultHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.stats_awards_result_header,
                    parent,
                    false,
                )
                TopViewHolder(awardsResultHeader)
            }

            else -> {
                val binding: AggregatedHofItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.aggregated_hof_item,
                    parent,
                    false,
                )
                RankViewHolder(binding)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        var id = 0L
        if (!mItems.isEmpty()) {
            if (position == 0) {
                id = (mItems[position].idol?.getId()?.toLong() ?: 0) + 10000000L
            } else if (mItems.size >= position) {
                id = mItems[position - isOffset].idol?.getId()?.toLong() ?: 0
            }
        }

        return id
    }

    override fun getItemCount(): Int {
        if (mItems.size == 0)
            return 0
        return mItems.size + isOffset
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_TOP
        }
        return TYPE_RANK
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mItems.isEmpty()) return

        if (holder.itemViewType == TYPE_TOP) {
            (holder as TopViewHolder).bind(mItems[position])
        } else {
            (holder as RankViewHolder).bind(mItems[position - isOffset], position)
        }
    }

    fun clear() {
        mItems.clear()
    }

    fun setItems(@NonNull items: ArrayList<HallModel>) {
        mItems = items
    }

    //1위
    inner class TopViewHolder(binding: StatsAwardsResultHeaderBinding) : BaseAwardsTopViewHolder(binding) {

        override fun bind(item: HallModel) = with(binding) {
            super.bind(item)
            name.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_default))
            group.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_default))
            score.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_default))

            tvAwardTitle.text = title

            val idolId = item.idol?.getId() ?: 0

            glideRequestManager
                .load(UtilK.trendImageUrl(context, item.id))
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(photo)
            itemView.setOnClickListener { onClickListener.onItemClickListener(item) }

        }
    }

    inner class RankViewHolder(val binding: AggregatedHofItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HallModel, position: Int) {
            // 동점자 처리
            val rank: Int = item.rank // rank는 0부터

            if (rank < 3) {
                binding.iconRanking.visibility = View.VISIBLE
                when (rank) {
                    0 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                    1 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                    2 -> binding.iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                }
            } else {
                binding.iconRanking.visibility = View.GONE
            }

            UtilK.setName(context, item.idol, binding.name, binding.group)

            val scoreCount: String =
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.score)
                    .replace(("[^\\d.]").toRegex(), "")
            val scoreText: String = String.format(
                context.getString(R.string.score_format), scoreCount
            )
            binding.score.text = scoreText
            val rankCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(rank + 1)
            binding.rank.text = String.format(context.getString(R.string.rank_format), rankCount)
            val idolId = item.idol?.getId() ?: 0

            glideRequestManager
                .load(UtilK.trendImageUrl(context, item.id))
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(binding.photo)

            itemView.setOnClickListener { onClickListener.onItemClickListener(item) }
        }
    }

    companion object {
        const val TYPE_TOP = 0
        const val TYPE_RANK = 1
    }
}
