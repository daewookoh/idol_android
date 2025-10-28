package net.ib.mn.adapter

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.OptIn
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.RequestManager
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.signature.ObjectKey
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.RankingHeaderBinding
import net.ib.mn.databinding.RankingItemBinding
import net.ib.mn.databinding.SRankingItemBinding
import net.ib.mn.databinding.TextureRankingHeaderBinding
import net.ib.mn.databinding.TextureRankingItemBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.Const
import net.ib.mn.utils.Logger
import net.ib.mn.utils.RankingBindingProxy
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.safeActivity
import net.ib.mn.utils.ext.setOnSingleClickListener
import net.ib.mn.utils.ext.setVisibilityIfNeeded
import net.ib.mn.utils.setIdolBadgeIcon
import net.ib.mn.utils.setMargins
import net.ib.mn.utils.vote.VotePercentage
import org.json.JSONException
import java.text.NumberFormat
import java.util.Locale

// ⚠️⚠️⚠️ 셀럽은 별도 파일로 존재하니 꼭 같이 수정!️ (NewRankingAdapter2.kt)

open class NewRankingAdapter(
    private val context: Context,
    private val fragment: BaseFragment,
    private val glideRequestManager: RequestManager,
    private val mListener: OnClickListener,
    private var league: String = Const.LEAGUE_S,
    private var animationMode: Boolean,
    private val lottieListener: LottieListener? = null,
    private val lifecycleScope: CoroutineScope,
    private val idolsRepository: IdolsRepository,
    private val isTutorialAdapter: Boolean = false,
    private val lottieClick: (Int) -> Unit = {}
) : BaseRankingAdapter() {

    private val gson = IdolGson.getInstance()
    protected var most: IdolModel? = null
    protected var mItems: ArrayList<IdolModel> = ArrayList()
    protected val numberFormatter = NumberFormat.getNumberInstance(Locale.getDefault())

    interface OnClickListener {
        fun onItemClicked(item: IdolModel)
        fun onVote(item: IdolModel)
        fun onPhotoClicked(item: IdolModel, position: Int)
    }

    interface LottieListener {
        fun lottieListener(view: LottieAnimationView)
    }

    private var chartCode: String = ""
    private var mMaxVoteCount: Long = 0
    private val mTopBannerUrls = arrayOfNulls<String>(3)
    private val voteMap = HashMap<Int, Long>()
    var hasExpanded: Boolean = false // 움짤프사 한번이라도 펼치면 1위프사 움짤은 화면 재진입 이전까지 재생 안함. 10초 자동갱신때문에 추가.
    var oldTopId: Int = 0 // 1위가 바뀌는지 알기 위함, 10초 자동갱신때문에 추가.
    var needUpdate: Boolean = false // 남여 전환시나 화면 전환시 강제로 다시 갱신하기 위한 용도
    var oldTop3: String? = "" // top3가 변경되는 경우 1위 프사 이붙을 갱신
    var oldTop3ImageVer: String? = "" // top3 이미지 버전이 변경되는 경우 1위 프사 이붙을 갱신
    var leagueViewType: Int = 0
    var found = false

    // 투표수 바 길이
    var firstIdolHeart = 0L
    var lastIdolHeart = 0L

    // 가온
    var voteEnabled = true
    var isGaon = false  // 가온 순위 탭에서 기념일/몰빵일 등 숨기기

    // 투표수 애니메이션 pool
    private val animatorPool = HashMap<Int, ValueAnimator?>()

    private var startLottieAnimation: Boolean = true   //로티 애니메이션 보여줘야하는지 여부

    fun updateChartCode(chartCode: String) {
        // 유저가 중간에 최애 바꿀 수 있는거 대비
        updateMostIdol()
        this.chartCode = chartCode
    }

    fun updateMostIdol() {
        most = IdolAccount.getAccount(context)?.most
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mMaxVoteCount = if (mItems.size > 0) {
            mItems[0].heart
        } else {
            0
        }

        return when (viewType) {
            TYPE_TOP -> {
                val viewHolder = if (Util.isOSNougat()) {
                    val binding = RankingHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    TopViewHolder(HeaderBindingProxy(binding), chartCode)
                } else {
                    val binding = TextureRankingHeaderBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                    TopViewHolder(HeaderBindingProxy(binding), chartCode)
                }
                return viewHolder
            }

            TYPE_S_RANK, TYPE_S_RANK_LOTTIE -> {
                val binding = SRankingItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                RankViewHolder(RankingBindingProxy(binding))
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
                id = mItems[position].getId().toLong() + 100000L
            } else if (mItems.size >= position) {
                id = mItems[position - 1].getId().toLong()
            }
        }

        return id
    }

    override fun getItemCount(): Int {
        if (mItems.isEmpty())
            return 0
        return mItems.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if (position == 0) {
            return TYPE_TOP
        }
        return if (league == "S") {
            TYPE_S_RANK
        } else {
            TYPE_A_RANK
        }
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (mItems.isEmpty()) return

        when (holder.itemViewType) {
            TYPE_TOP -> {
                // 여기다 두면 중간즘 스크롤 한 뒤 커뮤에서 다시 돌아오면 위쪽이 안불려서 mMaxVoteCount가 0이 됨. onCreateViewHolder로 이동. => 남여 전환시 반영이 안되서 여기도 살려둠.
                mMaxVoteCount = mItems[position].heart
                (holder as TopViewHolder).bind(mItems[position])
            }

            TYPE_S_RANK -> {
                (holder as RankViewHolder).apply {
                    val index = if (isTutorialAdapter) TutorialManager.getTutorialIndex() else TutorialBits.NO_TUTORIAL
                    bind(mItems[position - 1], position, TYPE_S_RANK, tutorialIndex = index)

                    val margin = if (position == mItems.size) {
                        9f
                    } else {
                        0f
                    }

                    holder.itemView.setMargins(bottom = margin)
                }

            }

            TYPE_S_RANK_LOTTIE -> {
                (holder as RankViewHolder).bind(mItems[position - 1], position, TYPE_S_RANK_LOTTIE)
            }

            else -> {
                (holder as RankViewHolder).bind(mItems[position - 1], position, TYPE_A_RANK)
            }
        }
    }

    fun clearAnimation() {
        for ((k, v) in animatorPool) {
            v?.removeAllUpdateListeners()
        }
        animatorPool.clear()
    }

    fun setItems(newItems: ArrayList<IdolModel>, league: String) {
        this.league = league
        mItems.clear()
        mItems.addAll(newItems)

        notifyItemRangeChanged(0, mItems.size + 1)

        mMaxVoteCount = if (mItems.isNotEmpty()) mItems[0].heart else 0
    }

    fun startLottieAnimation() {
        startLottieAnimation = true
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)

        val animator = animatorPool[holder.itemView.hashCode()]
        animator?.apply {
            cancel()
            removeAllUpdateListeners()
        }
    }

    inner class TopViewHolder(val binding: HeaderBindingProxy, chartCode: String) :
        RecyclerView.ViewHolder(binding.root) {
        @UnstableApi
        fun bind(idol: IdolModel) {
            with(binding) {
                topRankBadge.setImageBitmap(null)

                try {
                    var model = idol

                    // 미션 달성하면 내 최애가 1등 프사로
                    val most = IdolAccount.getAccount(context)?.most
                    var missionCompleted =
                        Util.getPreferenceBool(context, Const.PREF_MISSION_COMPLETED, false)

                    val preferenceJson =
                        Util.getPreference(context, Const.PREF_MOST_CHART_CODE).toString()

                    val mostChartCodes: ArrayList<String> = if (preferenceJson.isNullOrEmpty()) {
                        arrayListOf()
                    } else {
                        val listType = object : TypeToken<ArrayList<String>>() {}.type
                        gson.fromJson(preferenceJson, listType)
                    }

                    val isSame = mostChartCodes.firstOrNull { it == chartCode }

                    missionCompleted = missionCompleted && isSame != null

                    if (most != null && missionCompleted) {
//                model = most   // 제외된 아이돌을 최애로 해놓는경우 대비
                        // 1등 뱃지 제거
                        topRankBadge.setImageResource(R.drawable.img_top_badge_02)

                        model = most

                        if (model.type.equals(most.type, true)) {
                            // 캐시 문제로 items에서 최애를 찾기
                            for (item in mItems) {
                                if (item.resourceUri != null && item.getId() == most.getId()) {
                                    model = item
                                    found = true
                                    break
                                }
                            }

                            // 없다면 idol 정보를 직접 가져온다.
                            if (!found) {
                                lifecycleScope.launch {
                                    idolsRepository.getIdolsForSearch(
                                        id = most.getId(),
                                        listener = { response ->
                                            try {
                                                val idol = IdolGson.getInstance()
                                                    .fromJson(
                                                        response.getJSONArray("objects")
                                                            .getJSONObject(0)
                                                            .toString(), IdolModel::class.java
                                                    )
                                                Util.log(idol.toString())
                                                itemView.setOnClickListener {
                                                    mListener.onItemClicked(
                                                        idol
                                                    )
                                                }
                                                updateTopIdolImages(idol)
                                            } catch (e: JSONException) {
                                                e.printStackTrace()
                                            }

                                        }, errorListener = {
                                            // 제외된 아이돌이면 404가 나와서 움짤프사때문에 검은화면이 나옴. 그래서 다시 살림.
                                            itemView.setOnClickListener {
                                                mListener.onItemClicked(
                                                    most
                                                )
                                            }
                                            updateTopIdolImages(most)
                                        }
                                    )
                                }
                            }
                            found =
                                true    //api호출 후 getIdolsForSearch 리그 변경될 때까지 호출 안하게 하기 위해 true로 변경
                        }
                    }

                    itemView.setOnClickListener { mListener.onItemClicked(model) }
                    updateTopIdolImages(model)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        @UnstableApi
        private fun updateTopIdolImages(model: IdolModel) = with(binding) {
            // 펼치기 닫은 후 1등 움짤 재생이 안되서 막음
//            if (oldTopId == model.getId()) return

            val imageUrls = UtilK.getTop3ImageUrl(context, model)

            var url: String? = mTopBannerUrls[0]

            Util.log("===== updateTopIdolImages old=${oldTopId} current=${model.getId()}")

            // 펼쳐져있는 곳에서 재생되고 있던 것들 멈추고
            // 1위가 바뀌는 경우에만 하자. (자동갱신시 여기가 불려 펼쳐놓은 움짤 멈춤 방지)
            // top3가 바뀌는 경우에도 갱신
            if ((oldTopId != model.getId() || needUpdate || !oldTop3.equals(model.top3)
                    || oldTop3ImageVer != model.top3ImageVer
                    || leagueViewType != getItemViewType(1))
            ) {
                Logger.v("===== TOP3 갱신 ---------- mTopBannerUrls= ${mTopBannerUrls.joinToString()}\n" +
                        "imageUrls= ${imageUrls.joinToString()}")
                fragment.stopExoPlayer(fragment.playerView1)
                fragment.stopExoPlayer(fragment.playerView2)
                fragment.stopExoPlayer(fragment.playerView3)

                if (url == null || url != imageUrls[0]) {
                    Logger.v("===== TOP3 갱신 imageUrls[0]= ${imageUrls[0]}")
                    mTopBannerUrls[0] = imageUrls[0]
                    glideRequestManager
                        .load(imageUrls[0])
                        .into(photo1)
                }

                url = mTopBannerUrls[1]
                if (url == null || url != imageUrls[1]) {
                    mTopBannerUrls[1] = imageUrls[1]
                    glideRequestManager
                        .load(imageUrls[1])
                        .into(photo2)
                }

                url = mTopBannerUrls[2]
                if (url == null || url != imageUrls[2]) {
                    mTopBannerUrls[2] = imageUrls[2]
                    glideRequestManager
                        .load(imageUrls[2])
                        .into(photo3)
                }

                // 화면에 보일 때에만. 그리고 펼쳐진 프사가 없을 때에만 재생 시도.
                if (fragment.fragIsVisible && !hasExpanded) {
                    photo1.postDelayed({
                        if (fragment.fragIsVisible) {
                            fragment.playExoPlayer(
                                0,
                                headerPlayerview1,
                                photo1,
                                mTopBannerUrls[0],
                                imageUrls[0]
                            )
                            fragment.playExoPlayer(
                                1,
                                headerPlayerview2,
                                photo2,
                                mTopBannerUrls[1],
                                imageUrls[1]
                            )
                            fragment.playExoPlayer(
                                2,
                                headerPlayerview3,
                                photo3,
                                mTopBannerUrls[2],
                                imageUrls[2]
                            )
                        }
                    }, 200) // 삼성폰에서 바로 재생하면 코덱쪽 에러가 나서...
                }

                needUpdate = false
            }


            // 움짤
            // 개인/그룹 전환시 이전 화면에 까만 화면이 나옴 방지.
//        playerView1 = (PlayerView)headerView.findViewById(R.id.header_playerview1);
//        playerView2 = (PlayerView)headerView.findViewById(R.id.header_playerview2);
//        playerView3 = (PlayerView)headerView.findViewById(R.id.header_playerview3);


            oldTopId = model.getId()
            leagueViewType = getItemViewType(1)
            oldTop3 = model.top3
            oldTop3ImageVer = model.top3ImageVer
        }
    }

    open inner class RankViewHolder(val binding: RankingBindingProxy) :
        RecyclerView.ViewHolder(binding.root) {
        @OptIn(UnstableApi::class)
        fun bind(
            idol: IdolModel,
            position: Int,
            viewType: Int,
            maxHeart: Long? = null,
            minHeart: Long? = null,
            tutorialIndex: Int = TutorialBits.NO_TUTORIAL
        ) {
            with(binding) {
                firstIdolHeart = maxHeart ?: mItems.firstOrNull()?.heart ?: 0
                lastIdolHeart = minHeart ?: mItems.lastOrNull()?.heart ?: 0

                itemView.setOnClickListener {
                    mListener.onItemClicked(idol)
                }

                nameView.setOnClickListener {
                    if (binding.nameLottie?.isVisible == true) return@setOnClickListener
                    mListener.onItemClicked(idol)
                    return@setOnClickListener
                }

                val rank = idol.rank

                if (isGaon) idol.anniversary = "N"

                UtilK.setName(context, idol, nameView, groupView)

                if (BuildConfig.CELEB) {
                    badgeBirth.setVisibilityIfNeeded(if (idol.anniversary == Const.ANNIVERSARY_BIRTH) View.VISIBLE else View.GONE)
                } else {
                    badgeBirth.setVisibilityIfNeeded(if (idol.anniversary == Const.ANNIVERSARY_BIRTH && idol.type == "S") View.VISIBLE else View.GONE)
                    badgeDebut.setVisibilityIfNeeded(if ((idol.anniversary == Const.ANNIVERSARY_BIRTH && idol.type != "S") || idol.anniversary == Const.ANNIVERSARY_DEBUT) View.VISIBLE else View.GONE)
                }
                badgeComeback.setVisibilityIfNeeded(if (idol.anniversary == Const.ANNIVERSARY_COMEBACK) View.VISIBLE else View.GONE)
                badgeMemorialDay.setVisibilityIfNeeded(if (idol.anniversary == Const.ANNIVERSARY_MEMORIAL_DAY) View.VISIBLE else View.GONE)
                badgeAllInDay.setVisibilityIfNeeded(if (idol.anniversary == Const.ANNIVERSARY_ALL_IN_DAY) View.VISIBLE else View.GONE)
                if (idol.anniversary == Const.ANNIVERSARY_MEMORIAL_DAY) {
                    val memorialDayCount: String = if (Util.isRTL(context)) {
                        numberFormatter.format(idol.anniversaryDays)
                    } else {
                        idol.anniversaryDays.toString()
                    }
                    badgeMemorialDay.text = memorialDayCount.replace(("[^\\d.]").toRegex(), "")
                        .plus(context.getString(R.string.lable_day))
                }

                UtilK.profileRoundBorder(
                    idol.miracleCount,
                    idol.fairyCount,
                    idol.angelCount,
                    photoBorder
                )

                if (position == 1) {
                    when (tutorialIndex) {
                        TutorialBits.MAIN_BANNER_GRAM -> {
                            setupLottieTutorial(binding.rankLottie ?: return) {
                                lottieClick(tutorialIndex)
                                onClickPhoto(idol, position)
                            }
                        }

                        TutorialBits.MAIN_COMMUNITY -> {
                            setupLottieTutorial(binding.nameLottie ?: return) {
                                lottieClick(tutorialIndex)
                                mListener.onItemClicked(idol)
                            }
                        }

                        TutorialBits.MAIN_VOTE -> {
                            setupLottieTutorial(binding.voteLottie ?: return) {
                                lottieClick(tutorialIndex)
                                mListener.onVote(idol)
                            }
                        }
                    }
                } else {
                    binding.rankLottie?.apply {
                        visibility = View.GONE
                        removeAllAnimatorListeners()
                        cancelAnimation()
                    }
                    binding.nameLottie?.apply {
                        visibility = View.GONE
                        removeAllAnimatorListeners()
                        cancelAnimation()
                    }
                    binding.voteLottie?.apply {
                        visibility = View.GONE
                        removeAllAnimatorListeners()
                        cancelAnimation()
                    }
                }

                val rankCountComma = numberFormatter.format(rank + 1)
                rankView.text = String.format(
                    context.getString(R.string.rank_count_format),
                    rankCountComma
                )

                val voteCount = idol.heart

                val oldVote: Long = voteMap.get(idol.getId()) ?: 0L

                voteCountView.text = numberFormatter.format(oldVote)

                var animator = animatorPool[itemView.hashCode()]
                animator?.removeAllUpdateListeners()    // 기존 애니메이션 돌던거를 취소하고
                animator?.cancel()

                if (oldVote != voteCount && animationMode) {
                    animator = ValueAnimator.ofFloat(0f, 1f).apply {
                        addUpdateListener {
                            val value =
                                (oldVote + (voteCount - oldVote) * (it.animatedValue as Float)).toLong()
                            voteCountView.text = numberFormatter.format(value)
                        }
                        addListener(object : Animator.AnimatorListener {
                            override fun onAnimationRepeat(animation: Animator) {
                            }

                            override fun onAnimationCancel(animation: Animator) {
                            }

                            override fun onAnimationStart(animation: Animator) {
                            }

                            override fun onAnimationEnd(animation: Animator) {
                                val voteCountComma = numberFormatter.format(voteCount)
                                voteCountView.text = voteCountComma
                            }
                        })
                        duration = 1000
                        start()
                    }
                    animatorPool.set(itemView.hashCode(), animator)
                } else {
                    val voteCountComma = numberFormatter.format(voteCount)
                    voteCountView.text = voteCountComma
                }
                voteMap[idol.getId()] = voteCount

                val profileThumb: String? =
                    UtilK.top1ImageUrl(context, idol, Const.IMAGE_SIZE_LOWEST)
                Logger.i("NewRankingAdapter", "idol=${idol.getName()} profileThumb: $profileThumb")

                val idolId = idol.getId()
                if (profileThumb == null) {
                    glideRequestManager
                        .load(Util.noProfileThemePickImage(idolId))
                        .apply(RequestOptions.circleCropTransform())
                        .diskCacheStrategy(DiskCacheStrategy.RESOURCE)
                        .into(imageView)
                } else {
                    glideRequestManager.clear(imageView)
                    val cacheKey = "profile_thumb_${idolId}_${profileThumb}"
                    glideRequestManager
                        .load(profileThumb)
                        .apply(
                            RequestOptions()
                                .circleCrop()
                                .placeholder(Util.noProfileImage(idolId)) // 로딩 중 기본 이미지
                                .signature(ObjectKey(cacheKey))
                        )
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .dontAnimate()
                        .into(imageView)
                }

                context.safeActivity?.windowManager
                    ?.defaultDisplay
                    ?.getMetrics(DisplayMetrics())


                val progressPercent = VotePercentage.getVotePercentage(
                    minPercentage = VotePercentage.MAIN_RANKING_MIN_PERCENTAGE,
                    currentPlaceVote = voteCount,
                    firstPlaceVote = firstIdolHeart,
                    lastPlaceVote = lastIdolHeart,
                )
                progressBar.setWidthRatio(progressPercent)

                voteBtn.setOnSingleClickListener {
                    if (binding.voteLottie?.isVisible == true) return@setOnSingleClickListener
                    mListener.onVote(idol)
                }

                // 프사 눌러 펼치기.
                if (!isGaon) {
                    val key = position

                    if (fragment.mapExpanded[key] == true) {
                        containerPhotos.visibility = View.VISIBLE
                    } else {
                        containerPhotos.visibility = View.GONE
                    }

                    // 프사 펼쳐져있는 상태라면
                    if (containerPhotos.isVisible) {
                        val imageUrls = UtilK.getTop3ImageUrl(context, idol)
                        Logger.i("NewRankingAdapter", "bind imageUrls: $imageUrls")
                        glideRequestManager.load(imageUrls[0]).into(photo1)
                        glideRequestManager.load(imageUrls[1]).into(photo2)
                        glideRequestManager.load(imageUrls[2]).into(photo3)
                    }
                    // 프사 펼쳐져있는 상태일 때 탑3가 변경되어 움짤 다시 재생하려면 여기다 작성하면 되는데.. 펼쳐진거마다 이전 탑3 상태를 기억해야해서 일단 보류 (iOS 동일)

                    imageView.isClickable = true
                    imageView.setOnClickListener {
                        if (binding.rankLottie?.isVisible == true) return@setOnClickListener
                        onClickPhoto(idol, position)

                    }

                    setIdolBadgeIcon(
                        iconAngel,
                        iconFairy,
                        iconMiracle,
                        iconRookie,
                        iconSuperRookie,
                        idol
                    )
                }

                if (!voteEnabled) voteBtn.visibility = View.INVISIBLE

                val containerBackGroundColor = if (most?.getId() == idol.getId()) {
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.main100
                    )

                } else {
                    ContextCompat.getColor(
                        itemView.context,
                        R.color.background_100
                    )
                }

                binding.containerRanking.setBackgroundColor(containerBackGroundColor)
            }
        }

        @OptIn(UnstableApi::class)
        fun onClickPhoto(idol: IdolModel, position: Int) = with(binding) {
            mListener.onPhotoClicked(idol, position)

            if(containerPhotos.isVisible) {
                collapse(containerPhotos)
                fragment.mapExpanded[position] = false
                // 이전에 펼쳐진 것들 처리
                fragment.stopExoPlayer(fragment.playerView1)
                fragment.stopExoPlayer(fragment.playerView2)
                fragment.stopExoPlayer(fragment.playerView3)
            } else {
                val imageUrls = UtilK.getTop3ImageUrl(context, idol)
                Logger.i("NewRankingAdapter", "onClickPhoto imageUrls: $imageUrls")
                glideRequestManager.load(imageUrls[0])
                    .error(Util.noProfileImage(idol.getId()))
                    .fallback(Util.noProfileImage(idol.getId()))
                    .into(photo1)

                glideRequestManager.load(imageUrls[1])
                    .error(Util.noProfileImage(idol.getId()))
                    .fallback(Util.noProfileImage(idol.getId()))
                    .into(photo2)

                glideRequestManager.load(imageUrls[2])
                    .error(Util.noProfileImage(idol.getId()))
                    .fallback(Util.noProfileImage(idol.getId()))
                    .into(photo3)

                expand(containerPhotos) {
                    // 펼치기가 끝나면 움짤 재생
                    fragment.playExoPlayer(
                        0,
                        playerview1,
                        photo1,
                        idol.imageUrl
                    )
                    fragment.playExoPlayer(
                        1,
                        playerview2,
                        photo2,
                        idol.imageUrl2
                    )
                    fragment.playExoPlayer(
                        2,
                        playerview3,
                        photo3,
                        idol.imageUrl3
                    )
                }
                fragment.mapExpanded[position] = true
            }

            hasExpanded = true

            // 이전에 펼쳐진 것들 처리
            fragment.stopExoPlayer(fragment.playerView1)
            fragment.stopExoPlayer(fragment.playerView2)
            fragment.stopExoPlayer(fragment.playerView3)

            if(fragment.mapExpanded.values.all { it == false }) {
                needUpdate = true
                hasExpanded = false
            }
        }
    }

    inner class HeaderBindingProxy {
        val root: View
        val topRankBadge: AppCompatImageView
        val photo1: ImageView
        val photo2: ImageView
        val photo3: ImageView
        val headerPlayerview1: PlayerView
        val headerPlayerview2: PlayerView
        val headerPlayerview3: PlayerView

        constructor(binding: RankingHeaderBinding) {
            root = binding.root
            topRankBadge = binding.topRankBadge
            photo1 = binding.photo1
            photo2 = binding.photo2
            photo3 = binding.photo3
            headerPlayerview1 = binding.headerPlayerview1
            headerPlayerview2 = binding.headerPlayerview2
            headerPlayerview3 = binding.headerPlayerview3
        }

        constructor(binding: TextureRankingHeaderBinding) {
            root = binding.root
            topRankBadge = binding.topRankBadge
            photo1 = binding.photo1
            photo2 = binding.photo2
            photo3 = binding.photo3
            headerPlayerview1 = binding.headerPlayerview1
            headerPlayerview2 = binding.headerPlayerview2
            headerPlayerview3 = binding.headerPlayerview3
        }
    }

    companion object {
        const val TYPE_TOP = 0
        const val TYPE_A_RANK = 1
        const val TYPE_S_RANK = 2
        const val TYPE_S_RANK_LOTTIE = 3
        const val TYPE_EMPTY_FAVORITES = 4 // 즐찾 없음
    }
}
