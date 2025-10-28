package net.ib.mn.awards

import android.animation.Animator
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.core.model.AwardModel
import net.ib.mn.databinding.CommonAwardsHeaderBinding
import net.ib.mn.databinding.ItemEmptyViewBinding
import net.ib.mn.databinding.RankingItemBinding
import net.ib.mn.databinding.RankingItemAwardsBinding
import net.ib.mn.databinding.TextureRankingItemBinding
import net.ib.mn.core.model.AwardChartsModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.smalltalk.viewholder.EmptyVH
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.setOnSingleClickListener
import net.ib.mn.utils.setConstraintVerticalBias
import net.ib.mn.view.ExodusImageView
import net.ib.mn.view.ProgressBarLayout
import java.text.DateFormat
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.collections.HashMap
import kotlin.math.sqrt

// 어워즈 실시간 순위 어댑터
class AwardsRankingAdapter(
    private val activity: Activity,
    private val context: Context,
    private val mListener: OnClickListener,
    private val glideRequestManager: RequestManager,
    private val onClickListener: OnClickListener,
    private val awardData: AwardModel?,
    private val votable: String,
    var requestChartModel: AwardChartsModel
) : ListAdapter<IdolModel, ViewHolder>(diffUtil) {

    private val voteMap = HashMap<Int, Long>()
    private var errorMsg: String? = null

    // 투표수 애니메이션 pool
    private val animatorPool = HashMap<Int, ValueAnimator?>()

    interface OnClickListener {
        fun onItemClicked(item: IdolModel?)
        fun onVote(item: IdolModel)

        fun onIntoAppBtnClicked(view: View)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            TYPE_TOP_OF_REAL_TIME -> {
                val headerRealTime: CommonAwardsHeaderBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.common_awards_header,
                        parent,
                        false,
                    )
                RealTimeTopViewHolder(headerRealTime)
            }
            TYPE_TOP_OF_GUIDE -> {
                val headerGuide: CommonAwardsHeaderBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.common_awards_header,
                        parent,
                        false,
                    )
                GuideTopViewHolder(headerGuide)
            }
            TYPE_RANK_TEXTURE -> {
                val rankTextureItem: TextureRankingItemBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.texture_ranking_item,
                        parent,
                        false,
                    )
                val binding = RankingItemBindingProxy(rankTextureItem)
                TextureRankViewHolder(binding)
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
                val rankItem: RankingItemBinding =
                    DataBindingUtil.inflate(
                        LayoutInflater.from(parent.context),
                        R.layout.ranking_item,
                        parent,
                        false,
                    )
                val binding = RankingItemBindingProxy(rankItem)
                RankViewHolder(binding)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return currentList[position].hashCode().toLong()
    }

    override fun getItemCount(): Int = currentList.size

    override fun getItemViewType(position: Int): Int {

        if (currentList[position].getId() == EMPTY_ITEM) {
            return EMPTY_ITEM
        }

        if (position == 0) {
            return if ("Y".equals(votable, ignoreCase = true)) {
                TYPE_TOP_OF_REAL_TIME
            } else {
                TYPE_TOP_OF_GUIDE
            }
        }

        if (!Util.isOSNougat()) {
            return TYPE_RANK_TEXTURE
        }

        return TYPE_RANK
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_TOP_OF_GUIDE -> {
                // 여기다 두면 중간즘 스크롤 한 뒤 커뮤에서 다시 돌아오면 위쪽이 안불려서 mMaxVoteCount가 0이 됨. onCreateViewHolder로 이동. => 남여 전환시 반영이 안되서 여기도 살려둠.
                (holder as GuideTopViewHolder).apply {
                    bind()
                }
            }
            TYPE_TOP_OF_REAL_TIME -> {
                (holder as RealTimeTopViewHolder).apply {
                    bind(currentList[position])
                }
            }
            TYPE_RANK_TEXTURE -> {
                (holder as TextureRankViewHolder).apply {
                    bind(currentList[position])
                    binding.viewBinding.executePendingBindings()
                }
            }
            EMPTY_ITEM -> {
                (holder as EmptyVH)
            }
            else -> {
                (holder as RankViewHolder).apply {
                    bind(currentList[position])
                    binding.viewBinding.executePendingBindings()
                }
            }
        }
    }

    fun setEmptyVHErrorMessage(msg: String?) {
        this.errorMsg = msg
    }

    inner class RealTimeTopViewHolder(val binding: CommonAwardsHeaderBinding) : ViewHolder(binding.root) {
        fun bind(idolModel: IdolModel) {

            // 이미지 안넣기로 함
//            setHeaderBackgroundImage(binding)

            with(binding) {

                tvAwardTitle.text = awardData?.realtimeTitle
                tvAwardDetail.text = requestChartModel.desc // 실시간 순위 설명
                getAwardPeriod { awardPeriod, awardToday ->
                    tvAwardPeriod.text = awardPeriod
                    tvAwardToday.text = awardToday
                }

                // 배너 이미지 사용 안하고 최상단에 표시하는 것으로 바뀜
                intoAwardsApp.visibility = View.GONE
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

    // 투표 예시/라인업
    inner class GuideTopViewHolder(val binding: CommonAwardsHeaderBinding) : ViewHolder(binding.root) {

        fun bind() {
            setHeaderBackgroundImage(binding)

            with(binding) {

                tvAwardToday.visibility = View.GONE

                //가이드 화면은 타이틀 쪽에 기간이 들어감.
//                getAwardPeriod { awardPeriod, _ ->
//                    tvAwardTitle.text = awardPeriod
//                }

                tvAwardTitle.text = requestChartModel.exampleTitle
                tvAwardPeriod.text = itemView.context.getString(R.string.award_before_title)
                tvAwardDetail.text = requestChartModel.exampleDesc

            }
        }
    }

    private fun setHeaderBackgroundImage(binding: CommonAwardsHeaderBinding) {

        with(binding) {

            if ("Y".equals(votable, ignoreCase = true)) {
                glideRequestManager.load(
                    awardData?.realtimeImgUrl
                    )
                    .override(Util.getDeviceWidth(context), 156)
                    .into(object : CustomTarget<Drawable?>() {

                        override fun onResourceReady(
                            resource: Drawable,
                            transition: Transition<in Drawable?>?,
                        ) {
                            clAwardHeader.background = resource
                        }

                        override fun onLoadCleared(placeholder: Drawable?) {}
                    })
            }
        }
    }

    inner class RankViewHolder(val binding: RankingItemBindingProxy) :
        ViewHolder(binding.root) {
        fun bind(idol: IdolModel) {
            configureViewHolder(this, binding, idol)
        }
    }

    inner class TextureRankViewHolder(val binding: RankingItemBindingProxy) :
        ViewHolder(binding.root) {
        fun bind(idol: IdolModel) {
            configureViewHolder(this, binding, idol)
        }
    }

    private fun configureViewHolder(viewHolder: ViewHolder, binding: RankingItemBindingProxy, idol: IdolModel) {
        val rank = idol.rank
        val itemView = viewHolder.itemView

        with(binding) {
            if(BuildConfig.CELEB) {
                val drawable =
                    ContextCompat.getDrawable(context, R.drawable.progressbar_ranking)
                drawable?.setColorFilter(
                    context.resources.getColor(R.color.main),
                    android.graphics.PorterDuff.Mode.SRC_IN
                )

                progress.background = drawable
                count.setTextColor(context.resources.getColor(R.color.text_white_black))
            }
            // 애돌이도 이동하게 한다
//            if(BuildConfig.CELEB) {
                btnHeart.setColorFilter(context.resources.getColor(R.color.main), android.graphics.PorterDuff.Mode.SRC_IN)
                itemView.setOnClickListener {
                    mListener.onItemClicked(idol)
                }
//            }
            // 뱃지들은 어워드에서 필요없으므로 gone 처리
            imageAngel?.visibility = View.GONE
            imageFairy?.visibility = View.GONE
            imageMiracle?.visibility = View.GONE

            containerPhotos?.visibility = View.GONE

            UtilK.setName(context, idol, name, group)

            val rankCount = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(rank + 1)
            rankIndex.text = String.format(
                context.getString(R.string.rank_count_format),
                rankCount,
            )

            val voteCount = idol.heart

            val oldVote: Long = voteMap.get(idol.getId()) ?: 0L

            var animator = animatorPool[itemView.hashCode()]
            animator?.removeAllUpdateListeners() // 기존 애니메이션 돌던거를 취소하고
            animator?.cancel()

            if (oldVote != voteCount && Util.getPreferenceBool(
                    context,
                    Const.PREF_ANIMATION_MODE,
                    false,
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
                        NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(value)
                    count.text = voteCountComma
                }
                animator?.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationRepeat(animation: Animator) {
                    }

                    override fun onAnimationCancel(animation: Animator) {
                    }

                    override fun onAnimationStart(animation: Animator) {
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        val voteCountComma = NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context))
                            .format(voteCount)
                        count.text = voteCountComma
                    }
                })
                animator.duration = 1000
                animator.start()
            } else {
                val voteCountComma =
                    NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(itemView.context)).format(voteCount)
                count.text = voteCountComma
            }
            voteMap.set(idol.getId(), voteCount)

            val idolId = idol.getId()
            glideRequestManager.load(UtilK.top1ImageUrl(context, idol, Const.IMAGE_SIZE_LOWEST, idol.sourceApp))
                .apply(RequestOptions.circleCropTransform())
                .error(Util.noProfileImage(idolId))
                .fallback(Util.noProfileImage(idolId))
                .placeholder(Util.noProfileImage(idolId))
                .dontAnimate()
                .into(photo)

            activity.windowManager
                .defaultDisplay
                .getMetrics(DisplayMetrics())

            if (currentList[1].heart == 0L) {
                progress.setWidthRatio(28)
            } else {
                // int 오버플로 방지
                if (voteCount == 0L) {
                    progress.setWidthRatio(28)
                } else {
                    progress.setWidthRatio(
                        28 + (
                            sqrt(sqrt(voteCount.toDouble())) * 72 / sqrt(
                                sqrt(currentList[1].heart.toDouble()),
                            )
                            ).toInt(),
                    )
                }
            }

            // 애돌/셀럽 동일하게 투표 가능
            btnHeart.setOnSingleClickListener {
                mListener.onVote(idol)
            }
//            if(BuildConfig.CELEB) {
//                btnHeart.setOnSingleClickListener {
//                    mListener.onVote(idol)
//                }
//            } else {
//                btnHeart.visibility = View.INVISIBLE
//            }
        }
    }

    private fun getAwardPeriod(periodCallBack: (String, String) -> Unit) {

        val now = Date()

        val awardBegin = ConfigModel.getInstance(context).awardBegin
        val awardEnd = ConfigModel.getInstance(context).awardEnd

        // 한국어일때 DATE_FIELD값이 이상하게 나와서 MEDIUM으로 바꿈.
        val formatter = if (LocaleUtil.getAppLocale(context) == Locale.KOREA) {
            DateFormat.getDateInstance(DateFormat.MEDIUM, LocaleUtil.getAppLocale(context))
        } else {
            DateFormat.getDateInstance(DateFormat.DATE_FIELD, LocaleUtil.getAppLocale(context))
        }
        val localPattern = (formatter as SimpleDateFormat).toLocalizedPattern()
        val startFormat = SimpleDateFormat(localPattern, Locale.ENGLISH)
        val endFormat = SimpleDateFormat(localPattern, Locale.ENGLISH)
        startFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")
        endFormat.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val awardPeriod = String.format(
            context.getString(R.string.gaon_voting_period),
            startFormat.format(awardBegin),
            endFormat.format(awardEnd),
        )

        val awardToday = String.format(
            context.getString(R.string.gaon_current_title),
            startFormat.format(now),
        )

        periodCallBack(awardPeriod, awardToday)
    }

    inner class RankingItemBindingProxy {
        val viewBinding: ViewDataBinding
        val root: View
        val name: AppCompatTextView
        val group: AppCompatTextView
        val imageAngel: AppCompatTextView?
        val imageFairy: AppCompatTextView?
        val imageMiracle: AppCompatTextView?
        val containerPhotos: ConstraintLayout?
        val rankIndex: AppCompatTextView
        val count: AppCompatTextView
        val photo: ExodusImageView
        val progress: ProgressBarLayout
        val btnHeart: AppCompatImageView

        constructor(binding: RankingItemBinding) {
            viewBinding = binding
            root = binding.root
            name = binding.name
            group = binding.group
            imageAngel = binding.imageAngel
            imageFairy = binding.imageFairy
            imageMiracle = binding.imageMiracle
            containerPhotos = binding.containerPhotos
            rankIndex = binding.rankIndex
            count = binding.count
            photo = binding.photo
            progress = binding.progress
            btnHeart = binding.btnHeart
        }

        constructor(binding: TextureRankingItemBinding) {
            viewBinding = binding
            root = binding.root
            name = binding.name
            group = binding.group
            imageAngel = binding.imageAngel
            imageFairy = binding.imageFairy
            imageMiracle = binding.imageMiracle
            containerPhotos = binding.containerPhotos
            rankIndex = binding.rankIndex
            count = binding.count
            photo = binding.photo
            progress = binding.progress
            btnHeart = binding.btnHeart
        }

        // CELEB
        constructor(binding: RankingItemAwardsBinding) {
            viewBinding = binding
            root = binding.root
            name = binding.name
            group = binding.group
            imageAngel = null
            imageFairy = null
            imageMiracle = null
            containerPhotos = null
            rankIndex = binding.rankIndex
            count = binding.count
            photo = binding.photo
            progress = binding.progress
            btnHeart = binding.btnHeart
        }
    }

    companion object {
        const val TYPE_TOP_OF_REAL_TIME = 0
        const val TYPE_TOP_OF_GUIDE = 2
        const val TYPE_RANK = 1
        const val TYPE_RANK_TEXTURE = 3
        const val EMPTY_ITEM = -2

        val diffUtil = object : DiffUtil.ItemCallback<IdolModel>() {
            override fun areItemsTheSame(oldItem: IdolModel, newItem: IdolModel): Boolean {
                return (oldItem.getId() == newItem.getId()) && (oldItem.rank == newItem.rank)
            }

            override fun areContentsTheSame(oldItem: IdolModel, newItem: IdolModel): Boolean {
                return oldItem == newItem
            }
        }
    }
}