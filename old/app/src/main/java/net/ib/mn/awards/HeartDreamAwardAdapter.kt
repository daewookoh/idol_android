package net.ib.mn.awards

import android.content.Context
import android.content.res.Configuration
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.NonNull
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
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import java.text.NumberFormat
import java.util.*

class HeartDreamAwardAdapter(
    private val context: Context,
    private val glideRequestManager: RequestManager,
    private val onClickListener: OnClickListener,
    private var mItems: ArrayList<HallModel>,
    private var awardPeriod: String?,
    private val awardStatsCode: String?,
    private var awardTitle: String,
    private val awardKind: String?,
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
        if (mItems.size == 0) {
            return 0
        }
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
            (holder as TopViewHolder).apply {
                bind(mItems[position])
            }
        } else {
            (holder as RankViewHolder).apply {
                bind(mItems[position - isOffset])
            }
        }
    }

    fun clear() {
        mItems.clear()
    }

    fun setItems(@NonNull items: ArrayList<HallModel>) {
        mItems = items
    }

    fun setAwardPeriod(awardPeriod: String) {
        this.awardPeriod = awardPeriod
        notifyItemChanged(0)
    }
    inner class TopViewHolder(binding: StatsAwardsResultHeaderBinding) : BaseAwardsTopViewHolder(binding) {

        override fun bind(item: HallModel) {
            with(binding) {
                super.bind(item)
                llAwardBackground.background = if (awardKind == Const.AWARD_2022) {
                    ContextCompat.getDrawable(context, R.drawable.img_awards_1st_heart_dream)
                } else {
                    ContextCompat.getDrawable(context, R.drawable.img_awards_1st)
                }
                // 하트드림 최종결과 색 변경
                name.setTextColor(ContextCompat.getColor(context, R.color.fix_white))
                group.setTextColor(ContextCompat.getColor(context, R.color.fix_white))
                score.setTextColor(ContextCompat.getColor(context, R.color.fix_white))
                rank.setTextColor(ContextCompat.getColor(context, R.color.fix_white))

                tvAwardTitle.text = title

                tvAwardPeriod.visibility = View.VISIBLE
                tvAwardPeriod.text = awardPeriod

                val idolId = item.idol?.getId() ?: 0

                val imageUrl = UtilK.trendImageUrl(context, item.id, item.idol?.sourceApp)
                glideRequestManager
                    .load(imageUrl)
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(idolId))
                    .fallback(Util.noProfileImage(idolId))
                    .placeholder(Util.noProfileImage(idolId))
                    .dontAnimate()
                    .into(photo)
                setMargins(photo, 0, 0, 0, Util.convertDpToPixel(context, 23f).toInt())	// layout 새로 안만들고 기존에서 marginBottom 추가
                if (context.resources.configuration.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK == Configuration.SCREENLAYOUT_SIZE_XLARGE) { // 매우 큰 화면 사이즈(10인치 이상 테블릿). 테블릿에서 유저 이미지가 배경 이미지를 덮는 현상 때문에 추가
                    photo.setPadding(Util.convertDpToPixel(context, 50f).toInt(), Util.convertDpToPixel(context, 50f).toInt(), Util.convertDpToPixel(context, 50f).toInt(), Util.convertDpToPixel(context, 50f).toInt())
                    setMargins(photo, 0, 0, 0, Util.convertDpToPixel(context, 40f).toInt())
                }
                itemView.setOnClickListener { onClickListener.onItemClickListener(item) }
            }
        }
    }

    inner class RankViewHolder(val binding: AggregatedHofItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: HallModel) {
            with(binding) {
                // 동점자 처리
                val rank: Int = item.rank // rank는 0부터

                if (rank < 3) {
                    iconRanking.visibility = View.VISIBLE
                    when (rank) {
                        0 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_1st)
                        1 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_2nd)
                        2 -> iconRanking.setImageResource(R.drawable.icon_rating_heart_voting_3rd)
                    }
                } else {
                    iconRanking.visibility = View.GONE
                }

                UtilK.setName(context, item.idol, name, group)

                val scoreCount: String = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(item.score).replace(("[^\\d.]").toRegex(), "")
                val scoreText: String = String.format(
                    context.getString(R.string.score_format),
                    scoreCount,
                )
                score.text = scoreText
                val rankCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(rank + 1)
                binding.rank.text = String.format(
                    context.getString(R.string.rank_format),
                    rankCount,
                )
                val idolId = item.idol?.getId() ?: 0
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
                itemView.setOnClickListener { onClickListener.onItemClickListener(item) }
            }
        }
    }

    // 뷰의 마진을 코드로 설정하기 위한  기능
    private fun setMargins(
        view: View,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
    ) {
        if (view.layoutParams is ViewGroup.MarginLayoutParams) {
            val p = view.layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            view.requestLayout()
        }
    }

    companion object {
        const val TYPE_TOP = 0
        const val TYPE_RANK = 1
    }
}