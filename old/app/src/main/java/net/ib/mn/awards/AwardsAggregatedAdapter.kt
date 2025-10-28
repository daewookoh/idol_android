package net.ib.mn.awards

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import net.ib.mn.R
import net.ib.mn.awards.viewHolder.AwardFinalTopViewHolder
import net.ib.mn.awards.viewHolder.RankViewHolder
import net.ib.mn.core.model.AwardModel
import net.ib.mn.databinding.AggregatedHofItemBinding
import net.ib.mn.databinding.AwardsResultHeaderBinding
import net.ib.mn.databinding.CommonAwardsHeaderBinding
import net.ib.mn.databinding.ItemEmptyViewBinding
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.smalltalk.viewholder.EmptyVH
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.setConstraintVerticalBias
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

// 어워즈 최종결과 어댑터
class AwardsAggregatedAdapter(
    private val activity: Activity,
    private val context: Context,
    private val glideRequestManager: RequestManager,
    private val onItemClicked: (IdolModel?) -> Unit,
    private val onClickListener: OnClickListener,
    private val awardData: AwardModel?,
) : ListAdapter<HallModel, RecyclerView.ViewHolder>(
    diffUtil,
) {

    private var isOffset = 0

    val votable = ConfigModel.getInstance(context).votable

    private var errorMsg: String? = null
    private var visibleAwardToday = true

    interface OnClickListener {
        fun onItemClicked(item: IdolModel?)

        fun onIntoAppBtnClicked(view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            AWARD_AGGREGATED_TOP -> {
                val awardsAggregatedHeader: CommonAwardsHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.common_awards_header,
                    parent,
                    false,
                )
                AggregatedTopViewHolder(awardsAggregatedHeader)
            }
            AWARD_FINAL_TOP -> {
                val awardsResultHeader: AwardsResultHeaderBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.awards_result_header,
                    parent,
                    false,
                )
                AwardFinalTopViewHolder(
                    binding = awardsResultHeader,
                    visibleBackground = true,
                    glideRequestManager = glideRequestManager,
                    awardData = awardData,
                    { idol -> onItemClicked(idol) }
                ) {
//                    onClickListener.onFinalResultBottomSheetClicked()
                }
            }
            EMPTY_ITEM -> {
                val itemEmpty: ItemEmptyViewBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.item_empty_view,
                    parent,
                    false
                )
                EmptyVH(itemEmpty).apply {

                    binding.clEmpty.setConstraintVerticalBias(0.3f, binding.tvSmallTalkEmptyView.id)

                    if (errorMsg == null) {
                        binding.tvSmallTalkEmptyView.text =
                            itemView.context.getString(R.string.no_data)
                        return@apply
                    }

                    binding.tvSmallTalkEmptyView.text = errorMsg
                }
            }
            else -> {
                val aggregatedHofItem: AggregatedHofItemBinding = DataBindingUtil.inflate(
                    LayoutInflater.from(parent.context),
                    R.layout.aggregated_hof_item,
                    parent,
                    false,
                )
                RankViewHolder(
                    aggregatedHofItem,
                    glideRequestManager = glideRequestManager,
                    votable = votable,
                    isOffset = isOffset
                ) { idol ->
                    onItemClicked(idol)
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].hashCode().toLong()
    }

    override fun getItemCount(): Int = currentList.size


    override fun getItemViewType(position: Int): Int {

        if (currentList[position].id == EMPTY_ITEM) {
            return EMPTY_ITEM
        }

        if (position == 0) {
            return when (ConfigModel.getInstance(context).votable) {
                "Y" -> {
                    isOffset = 1
                    AWARD_AGGREGATED_TOP
                }
                else -> {
                    isOffset = 0
                    AWARD_FINAL_TOP
                }
            }
        }
        return TYPE_RANK
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            AWARD_AGGREGATED_TOP -> {
                (holder as AggregatedTopViewHolder).apply {
                    bind()
                }
            }
            AWARD_FINAL_TOP -> {
                (holder as AwardFinalTopViewHolder).apply {
                    bind(currentList[position])
                    binding.executePendingBindings()
                }
            }
            EMPTY_ITEM -> {
                (holder as EmptyVH)
            }
            else -> {
                (holder as RankViewHolder).apply {
                    bind(currentList[position])
                    binding.executePendingBindings()
                }
            }
        }
    }

    fun setEmptyVHErrorMessage(msg: String?) {
        this.errorMsg = msg
    }

    inner class AggregatedTopViewHolder(val binding: CommonAwardsHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            with(binding) {

//                if (awardData != null) {
//                    glideRequestManager
//                        .load(
//                            if (Util.isDarkTheme(activity)) {
//                                awardData.aggDarkImgUrl
//                            } else {
//                                awardData.aggLightImgUrl
//                            },
//                        )
//                        .override(Util.getDeviceWidth(context), 156)
//                        .into(object : CustomTarget<Drawable?>() {
//
//                            override fun onResourceReady(
//                                resource: Drawable,
//                                transition: Transition<in Drawable?>?,
//                            ) {
//                                binding.clAwardHeader.background = resource
//                            }
//
//                            override fun onLoadCleared(placeholder: Drawable?) {}
//                        })
//                }

                binding.tvAwardTitle.text = awardData?.aggTitle // awards/current의 aggregate_title
                binding.tvAwardDetail.text = awardData?.aggDesc // awards/current의 aggregate_desc

                val awardBegin = ConfigModel.getInstance(context).awardBegin
                val awardEnd = ConfigModel.getInstance(context).awardEnd

                // 한국어일때 DATE_FIELD값이 이상하게 나와서 MEDIUM으로 바꿈.
                val formatter = if (LocaleUtil.getAppLocale(itemView.context) == Locale.KOREA) {
                    DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(itemView.context))
                } else {
                    DateFormat.getDateInstance(DateFormat.DATE_FIELD, LocaleUtil.getAppLocale(itemView.context))
                }
                val localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()
                val startFormat = SimpleDateFormat(localPattern, LocaleUtil.getAppLocale(itemView.context))
                val endFormat = SimpleDateFormat(localPattern, LocaleUtil.getAppLocale(itemView.context))
                startFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
                endFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

                // 투표 기간동안은 어제 날짜. 투표 종료시 awardEnd
                var end = awardEnd
                if (ConfigModel.getInstance(context).votable == "Y") {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DATE, -1)
                    end = cal.time
                }
                binding.tvAwardPeriod.text = String.format(
                    context.getString(R.string.gaon_voting_period),
                    startFormat.format(
                        awardBegin,
                    ),
                    endFormat.format(awardEnd),
                )

                if (visibleAwardToday) {
                    tvAwardToday.visibility = View.VISIBLE
                } else {
                    tvAwardToday.visibility = View.GONE
                }

                tvAwardToday.text = String.format(
                    "%1s ~ %2s %3s",
                    startFormat.format(awardBegin),
                    endFormat.format(end),
                    context.getString(
                        R.string.label_header_result
                    )
                )

                // 배너 안보여주기로 함
//                if (awardData?.showBanner == "Y" && Util.getSystemLanguage(
//                        context,
//                    ).startsWith("ko")
//                ) {
//                    glideRequestManager
//                        .load(
//                            if (Util.isDarkTheme(activity)) {
//                                awardData.bannerDarkImgUrl
//                            } else awardData.bannerLightImgUrl,
//                        )
//                        .override(Util.getDeviceWidth(context), 35)
//                        .into(object : CustomTarget<Drawable?>() {
//                            override fun onResourceReady(
//                                resource: Drawable,
//                                transition: Transition<in Drawable?>?,
//                            ) {
//                                intoAwardsApp.visibility = View.VISIBLE
//                                intoAwardsApp.background = resource
//                            }
//
//                            override fun onLoadCleared(placeholder: Drawable?) {}
//                        })
//
//                    intoAwardsApp.setOnClickListener {
//                        onClickListener.onIntoAppBtnClicked(intoAwardsApp)
//                    }
//                } else {
//                    intoAwardsApp.visibility =
//                        View.GONE
//                }
            }
        }
    }

    fun setVisibleAwardToday(isVisible: Boolean) {
        this.visibleAwardToday = isVisible
    }

    companion object {
        const val AWARD_FINAL_TOP = 0
        const val AWARD_AGGREGATED_TOP = 1
        const val TYPE_RANK = 2
        const val EMPTY_ITEM = -2

        val diffUtil = object : DiffUtil.ItemCallback<HallModel>() {
            override fun areItemsTheSame(oldItem: HallModel, newItem: HallModel): Boolean {
                return oldItem == newItem
            }

            @SuppressLint("DiffUtilEquals")
            override fun areContentsTheSame(oldItem: HallModel, newItem: HallModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}