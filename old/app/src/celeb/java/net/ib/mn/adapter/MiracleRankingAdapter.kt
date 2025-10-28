package net.ib.mn.adapter

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.databinding.MiracleHeaderBinding
import net.ib.mn.databinding.RankingItemBinding
import net.ib.mn.databinding.TextureRankingItemBinding
import net.ib.mn.core.model.ChartModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.RankingBindingProxy
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.setOnSingleClickListener
import net.ib.mn.utils.setIdolBadgeIcon
import java.text.NumberFormat
import java.util.*
import kotlin.math.sqrt

/**
 * 이달의 기적 실시간 순위 adapter
 */
// TODO: 2022/10/24
class MiracleRankingAdapter(
    private val context: Context,
    private var mItems: ArrayList<IdolModel>,
    private var chartModel: ChartModel,
    private val onClickListener: OnClickListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    interface OnClickListener {
        fun onItemClicked(item: IdolModel?)
        fun onVote(item: IdolModel)
        fun onInfoClicked()
    }

    private var mMaxVoteCount: Long = 0
    private val voteMap = HashMap<Int, Long>()
    private var topViewCount = 1

    // 투표수 애니메이션 pool
    private val animatorPool = HashMap<Int, ValueAnimator?>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mMaxVoteCount = if (mItems.size > 0) {
            mItems[0].heart
        } else {
            0
        }

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
                val viewHolder = if (Util.isOSNougat()) {
                    val binding = RankingItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    RankViewHolder(RankingBindingProxy(binding))
                } else {
                    val binding = TextureRankingItemBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    RankViewHolder(RankingBindingProxy(binding))
                }

                return viewHolder
            }
        }
    }

    override fun getItemId(position: Int): Long {
        var id = 0L
        if (mItems.isNotEmpty()) {
            if (position == 0) {
                // 100000번대 아이돌들이 많이 있어서 안전하게 10000000으로
                id = mItems[position].getId().toLong() + 10000000L
            } else if (mItems.size >= position) {
                id = mItems[position - topViewCount].getId().toLong()
            }
        }
        return id
    }

    override fun getItemCount(): Int {
        if (mItems.size == 0)
            return 0
        return mItems.size + topViewCount
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_TOP
        }
        return TYPE_RANK
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mItems.isEmpty()) return

        when (holder) {
            is TopViewHolder -> {
                mMaxVoteCount = mItems[position].heart
                holder.bind(chartModel, onClickListener)
            }

            is RankViewHolder -> {
                val item = mItems[position - topViewCount]
                holder.bind(item, voteMap, animatorPool, onClickListener)
            }
        }
    }

    fun clear() {
        mItems.clear()
    }

    fun setItems(items: ArrayList<IdolModel>, chartModel: ChartModel?) {
        mItems.clear()
        mItems.addAll(items)

        if (chartModel != null) {
            this.chartModel = chartModel
        }

        mMaxVoteCount = if (mItems.size > 0) {
            mItems[0].heart
        } else {
            0
        }

        notifyDataSetChanged()
    }

    inner class TopViewHolder(
        private val binding: MiracleHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(miracleInfoModel: ChartModel, clickListener: OnClickListener) {

            val context = binding.root.context

            Glide
                .with(binding.root)
                .load(chartModel.imageUrl)
                .into(binding.ivMiraclePhoto)

            if (!UtilK.dateToKST(miracleInfoModel.beginDate).isNullOrEmpty() && !UtilK.dateToKST(
                    miracleInfoModel.endDate
                ).isNullOrEmpty()
            ) {
                binding.tvMiraclePeriod.text =
                    "${context.getString(R.string.award_guide_sub2) + " : "} ${
                        UtilK.dateToKST(miracleInfoModel.beginDate)
                    } ~ ${UtilK.dateToKST(miracleInfoModel.endDate)}"
            }

            if (ConfigModel.getInstance(context).showMiracleInfo != 0) {
                binding.btnMiracleInfo.apply {
                    visibility = View.VISIBLE
                    setOnClickListener { clickListener.onInfoClicked() }
                }
            }
        }
    }

    inner class RankViewHolder(
        val binding: RankingBindingProxy
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            idol: IdolModel,
            voteMap: HashMap<Int, Long>,
            animatorPool: HashMap<Int, ValueAnimator?>,
            clickListener: OnClickListener
        ) {
            with(binding) {
                containerPhotos.visibility = View.GONE

                setIdolBadgeIcon(
                    iconAngel,
                    iconFairy,
                    iconMiracle,
                    iconRookie,
                    iconSuperRookie,
                    idol
                )

                itemView.setOnClickListener {
                    clickListener.onItemClicked(idol)
                }

                val rank = idol.rank

                UtilK.setName(context, idol, nameView, groupView)

                val rankCount = NumberFormat.getNumberInstance(Locale.getDefault()).format(rank + 1)
                rankView.text = String.format(
                    context.getString(R.string.rank_count_format),
                    rankCount
                )

                when (idol.anniversary) {
                    Const.ANNIVERSARY_BIRTH -> {
                        if (BuildConfig.CELEB || idol.type == "S") {
                            badgeBirth.visibility = View.VISIBLE
                            badgeDebut.visibility = View.GONE
                        } else {
                            // 그룹은 데뷔뱃지로 보여주기
                            badgeBirth.visibility = View.GONE
                            badgeDebut.visibility = View.VISIBLE
                        }
                        badgeComeback.visibility = View.GONE
                        badgeMemorialDay.visibility = View.GONE
                        badgeAllInDay.visibility = View.GONE
                    }

                    Const.ANNIVERSARY_DEBUT -> {
                        badgeBirth.visibility = View.GONE
                        badgeDebut.visibility = View.VISIBLE
                        badgeComeback.visibility = View.GONE
                        badgeMemorialDay.visibility = View.GONE
                        badgeAllInDay.visibility = View.GONE
                    }

                    Const.ANNIVERSARY_COMEBACK -> {
                        badgeBirth.visibility = View.GONE
                        badgeDebut.visibility = View.GONE
                        badgeComeback.visibility = View.VISIBLE
                        badgeMemorialDay.visibility = View.GONE
                        badgeAllInDay.visibility = View.GONE
                    }

                    Const.ANNIVERSARY_MEMORIAL_DAY -> {
                        badgeBirth.visibility = View.GONE
                        badgeDebut.visibility = View.GONE
                        badgeComeback.visibility = View.GONE
                        badgeMemorialDay.visibility = View.VISIBLE
                        badgeAllInDay.visibility = View.GONE
                        val memorialDayCount: String
                        if (Util.isRTL(context)) {
                            memorialDayCount = NumberFormat.getNumberInstance(Locale.getDefault())
                                .format(idol.anniversaryDays)
                        } else {
                            memorialDayCount = idol.anniversaryDays.toString()
                        }
                        badgeMemorialDay.text = memorialDayCount.replace(("[^\\d.]").toRegex(), "")
                            .plus(context.getString(R.string.lable_day))
                    }

                    Const.ANNIVERSARY_ALL_IN_DAY -> {
                        badgeBirth.visibility = View.GONE
                        badgeDebut.visibility = View.GONE
                        badgeComeback.visibility = View.GONE
                        badgeMemorialDay.visibility = View.GONE
                        badgeAllInDay.visibility = View.VISIBLE
                    }

                    else -> {
                        badgeBirth.visibility = View.GONE
                        badgeDebut.visibility = View.GONE
                        badgeComeback.visibility = View.GONE
                        badgeMemorialDay.visibility = View.GONE
                        badgeAllInDay.visibility = View.GONE
                    }
                }

                UtilK.profileRoundBorder(
                    idol.miracleCount,
                    idol.fairyCount,
                    idol.angelCount,
                    photoBorder
                )

                if (BuildConfig.CELEB) {
                    voteCountView.setTextColor(
                        ContextCompat.getColor(
                            context,
                            R.color.text_white_black
                        )
                    )

                    val progressBar =
                        ContextCompat.getDrawable(context, R.drawable.progressbar_ranking)
                    progressBar?.setColorFilter(
                        context.resources.getColor(R.color.main),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )

                    binding.progressBar.background = progressBar
                    binding.voteBtn.setColorFilter(
                        context.resources.getColor(R.color.main),
                        android.graphics.PorterDuff.Mode.SRC_IN
                    )
                }

                val voteCount = idol.heart

                val oldVote: Long = voteMap[idol.getId()] ?: 0L

                // ViewHolder 멤버로 넣었더니 이전 animation이 안가져 와져서 pool에서 꺼내옴
                var animator = animatorPool[itemView.hashCode()]
                animator?.removeAllUpdateListeners()    // 기존 애니메이션 돌던거를 취소하고
                animator?.cancel()
                if (oldVote != voteCount && Util.getPreferenceBool(
                        context,
                        Const.PREF_ANIMATION_MODE,
                        false
                    )
                ) {
                    animator = ValueAnimator.ofFloat(0f, 1f)
                    // 애니메이터 생성 후 풀에 넣기
                    animatorPool.set(itemView.hashCode(), animator)
                    animator.addUpdateListener {
                        var value =
                            (oldVote + (voteCount - oldVote) * (it.animatedValue as Float)).toLong()
                        value = if (value > voteCount) voteCount else value
                        val voteCountComma =
                            NumberFormat.getNumberInstance(Locale.getDefault()).format(value)
                        voteCountView.text = voteCountComma
                    }
                    animator?.addListener(object : Animator.AnimatorListener {
                        override fun onAnimationRepeat(animation: Animator) {
                        }

                        override fun onAnimationCancel(animation: Animator) {
                        }

                        override fun onAnimationStart(animation: Animator) {
                        }

                        override fun onAnimationEnd(animation: Animator) {
                            val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault())
                                .format(voteCount)
                            voteCountView.text = voteCountComma
                        }
                    })
                    animator?.duration = 1000
                    animator?.start()

                } else {
                    val voteCountComma =
                        NumberFormat.getNumberInstance(Locale.getDefault()).format(voteCount)
                    voteCountView.text = voteCountComma
                }
                voteMap.set(idol.getId(), voteCount)

                val idolId = idol.getId()
                Glide.with(binding.root)
                    .load(UtilK.top1ImageUrl(context, idol, Const.IMAGE_SIZE_LOWEST))
                    .apply(RequestOptions.circleCropTransform())
                    .error(Util.noProfileImage(idolId))
                    .fallback(Util.noProfileImage(idolId))
                    .placeholder(Util.noProfileImage(idolId))
                    .dontAnimate()
                    .into(imageView)

                (context as Activity).windowManager
                    .defaultDisplay
                    .getMetrics(DisplayMetrics())

                if (mMaxVoteCount == 0L) {
                    progressBar.setWidthRatio(38)
                } else {
                    // int 오버플로 방지
                    if (voteCount == 0L) {
                        progressBar.setWidthRatio(38)
                    } else {
                        progressBar.setWidthRatio(
                            38 + (sqrt(sqrt(voteCount.toDouble())) * 62 / sqrt(
                                sqrt(mMaxVoteCount.toDouble())
                            )).toInt()
                        )
                    }
                }

                voteBtn.setOnSingleClickListener {
                    clickListener.onVote(idol)
                }
            }
        }
    }

    companion object {
        const val TYPE_TOP = 0
        const val TYPE_RANK = 1
    }
}
