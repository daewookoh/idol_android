package net.ib.mn.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.RequestManager
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.FavoriteSettingActivity
import net.ib.mn.addon.IdolGson.instance
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.databinding.FavoriteHeaderBinding
import net.ib.mn.databinding.FavoriteRankingItemBinding
import net.ib.mn.databinding.NoBookmarkItemBinding
import net.ib.mn.fragment.FavoriteIdolBaseFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.model.MostPicksModel
import net.ib.mn.model.RankingModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil.getAppLocale
import net.ib.mn.utils.Logger
import net.ib.mn.utils.RankingBindingProxy
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.safeActivity
import net.ib.mn.utils.vote.VotePercentage
import java.lang.ref.WeakReference
import java.text.NumberFormat
import java.util.Locale
import java.util.Random
import kotlin.math.floor
import kotlin.math.max

class FavoriteRankingAdapter(
    private val context: Context,
    private val glideRequestManager: RequestManager,
    // 프사 눌러 펼치기
    private val fragment: FavoriteIdolBaseFragment,
    private val mListener: OnClickListener,
    private val lifecycleScope: CoroutineScope,
    private val idolsRepository: IdolsRepository,
) : NewRankingAdapter(context = context,
    glideRequestManager = glideRequestManager,
    mListener = mListener,
    fragment = fragment,
    animationMode = Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false),
    lifecycleScope = lifecycleScope,
    idolsRepository = idolsRepository,
    ) {
    private var mMaxVoteCount: Long = 0
    private var favorites : ArrayList<RankingModel> = ArrayList()
    private val mTopBannerUrls = arrayOfNulls<String>(3)
    private val fragRef = WeakReference(fragment)
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels // ✅ 전체 화면 너비 가져오기

    fun setItems(items: ArrayList<RankingModel>) {
        favorites = items

        // NewRankingAdapter에서 mItems를 사용하는데, 오류 방지를 위해 mItems를 초기화
        // favorites에서 idol만 추출하여 리스트로 만들고 setItems 호출
        mItems.clear()
        favorites.forEach {
            mItems.add(it.idol!!)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        mMaxVoteCount = if (favorites.size > 0) {
            favorites[0].idol?.heart ?: 0
        } else {
            0
        }

        when (viewType) {
            TYPE_TOP -> {
                mTopBannerUrls.fill(null) // 초기화
                val binding = FavoriteHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return TopViewHolder(binding)
            }
            TYPE_EMPTY_FAVORITES -> {
                val binding = NoBookmarkItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                    return EmptyFavoriteViewHolder(binding)
            }
            else -> {
                val binding = FavoriteRankingItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return FavoriteRankViewHolder(RankingBindingProxy(binding))
            }
        }
    }

    @OptIn(UnstableApi::class)
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            TYPE_TOP -> {
                val most = IdolAccount.getAccount(context)?.most
                (holder as TopViewHolder).bind(most)
            }
            TYPE_EMPTY_FAVORITES -> {
                (holder as EmptyFavoriteViewHolder).bind()
            }
            else -> {
                (holder as FavoriteRankViewHolder).bind(favorites[position - 1], position)
            }
        }
    }

    override fun getItemId(position: Int): Long {
        var id = 0L
        if(favorites.isNotEmpty()) {
            if( position == 0 ) {
                id = 100000L // 최애
            } else if(favorites.size >= position){
                id = (favorites[position - 1].idol?.getId()?.toLong() ?: 0)
            }
        }

        return id
    }

    override fun getItemCount(): Int {
        if(favorites.isEmpty())
            return 2 // 헤더/푸터만 있을 때
        return favorites.size + 1
    }

    override fun getItemViewType(position: Int): Int {
        if(position == 0) {
            return TYPE_TOP
        }
        if(favorites.isEmpty())
            return TYPE_EMPTY_FAVORITES

        return  TYPE_S_RANK
    }

    inner class TopViewHolder(val binding: FavoriteHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        @UnstableApi
        fun bind(idol: IdolModel?) = with(binding) {
            // 최애 없을때 부분
            btnFavoriteSetting.setOnClickListener {
                if (Util.mayShowLoginPopup(context.safeActivity)) {
                    return@setOnClickListener
                }
                fragRef.get()?.startActivityForResult(FavoriteSettingActivity.createIntent(context), 10)
            }

            val most = IdolAccount.getAccount(context)?.most
            emptyFavoriteHeader.visibility = if (most == null) View.VISIBLE else View.GONE
            nonemptyFavoriteHeader.visibility = if (most == null) View.GONE else View.VISIBLE

            var mostRank = -1
            var heart: Long? = -1L
            favorites.forEach { item ->
                if (item.idol?.getId() == idol?.getId()) {
                    mostRank = item.ranking
                    heart = item.idol?.heart
                    return@forEach
                }
            }

            if (heart == -1L) {
                heart = most?.heart ?: 0L
            }

            val rankCount = NumberFormat.getNumberInstance(getAppLocale(context))
                .format(mostRank.toLong())
            rank.text = if (mostRank > 0
            ) String.format(
                context.getString(R.string.rank_format),
                rankCount
            ) else "-"

            UtilK.setName(context, most, name, group)

            val voteCountComma = NumberFormat.getNumberInstance(Locale.getDefault()).format(
                heart ?: 0
            )
            idolVoteCount.text = voteCountComma

            btnHeart.setOnClickListener {
                most?.let {
                    fragRef.get()?.onVote(it)
                }
            }

            if (BuildConfig.CELEB) {
                rank.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.card_fix_text_black
                    )
                )
                name.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.card_fix_text_black
                    )
                )
                group.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.card_fix_text_black
                    )
                )
                idolVoteCount.setTextColor(
                    ContextCompat.getColor(
                        context,
                        R.color.card_fix_text_black
                    )
                )

                val headerbarView = headerBar
                val mostTypeList = UtilK.getTypeList(context, most?.type + most?.category)
                headerbarView.setBackgroundColor(
                    if (mostTypeList.type == null)
                        ContextCompat.getColor(context, R.color.main)
                    else Color.parseColor(
                        Util.getUiColor(
                            context, mostTypeList
                        )
                    )
                )
                btnHeart.setColorFilter(
                    if (mostTypeList.type == null)
                        ContextCompat.getColor(context, R.color.main)
                    else Color.parseColor(
                        Util.getUiColor(
                            context, mostTypeList
                        )
                    ), PorterDuff.Mode.SRC_IN
                )
            }

            val mostView = nonemptyFavoriteHeader
            mostView.setOnClickListener { arg0: View? ->
                most?.let {
                    fragRef.get()?.onItemClicked(it)
                }
            }

            idol?.let {
                updateTopIdolImages(binding, idol)
            }

            setMostPickBanner()
        }

        @UnstableApi
        private fun updateTopIdolImages(binding: FavoriteHeaderBinding, model: IdolModel) {
            // 펼치기 닫은 후 1등 움짤 재생이 안되서 막음
            //            if (oldTopId == model.getId()) return
            val imageUrls = UtilK.getTop3ImageUrl(context, model)

            var url: String? = mTopBannerUrls[0]
            var imgView = binding.photo1

            Util.log("===== updateTopIdolImages old=${oldTopId} current=${model.getId()}")

            // 펼쳐져있는 곳에서 재생되고 있던 것들 멈추고
            // 1위가 바뀌는 경우에만 하자. (자동갱신시 여기가 불려 펼쳐놓은 움짤 멈춤 방지)
            // top3가 바뀌는 경우에도 갱신
            if( (oldTopId != model.getId() || needUpdate || !oldTop3.equals(model.top3)
                    || oldTop3ImageVer != model.top3ImageVer
                    || leagueViewType != getItemViewType(1))) {
                Logger.v("===== asdlfjalskdf :: ---------- call stop exoplyer")
                fragRef.get()?.stopExoPlayer(fragRef.get()?.playerView1)
                fragRef.get()?.stopExoPlayer(fragRef.get()?.playerView2)
                fragRef.get()?.stopExoPlayer(fragRef.get()?.playerView3)

                if (url == null || url != imageUrls[0]) {
                    mTopBannerUrls[0] = imageUrls[0]
                    Util.loadGif(glideRequestManager, mTopBannerUrls[0], imgView)
                }

                imgView = binding.photo2
                url = mTopBannerUrls[1]
                if (url == null || url != imageUrls[1]) {
                    mTopBannerUrls[1] = imageUrls[1]
                    Util.loadGif(glideRequestManager, mTopBannerUrls[1], imgView)
                }

                imgView = binding.photo3
                url = mTopBannerUrls[2]
                if (url == null || url != imageUrls[2]) {
                    mTopBannerUrls[2] = imageUrls[2]
                    Util.loadGif(glideRequestManager, mTopBannerUrls[2], imgView)
                }

                // 화면에 보일 때에만. 그리고 펼쳐진 프사가 없을 때에만 재생 시도.
                if (fragRef.get()?.fragIsVisible == true && !hasExpanded) {
                    imgView.postDelayed({
                        if(fragRef.get()?.fragIsVisible == true) {
                            fragRef.get()?.playExoPlayer(0,
                                binding.headerPlayerview1,
                                binding.photo1,
                                mTopBannerUrls[0])
                            fragRef.get()?.playExoPlayer(1,
                                binding.headerPlayerview2,
                                binding.photo2,
                                mTopBannerUrls[1])
                            fragRef.get()?.playExoPlayer(2,
                                binding.headerPlayerview3,
                                binding.photo3,
                                mTopBannerUrls[2])
                        }
                    }, 200) // 삼성폰에서 바로 재생하면 코덱쪽 에러가 나서...
                }

                needUpdate = false
            }

            oldTopId = model.getId()
            oldTop3 = model.top3
            oldTop3ImageVer = model.top3ImageVer
        }

        private fun mostPickId(list: List<Int>): Int {
            val mutableList = list.toMutableList()
            mutableList.shuffle()
            return mutableList[0]
        }

        fun setMostPickBanner() = with(binding) {
            val listType = object : TypeToken<MostPicksModel?>() {}.type
            val mostPicksModel = instance.fromJson<MostPicksModel>(
                Util.getPreference(
                    context, Const.PREF_MOST_PICKS
                ), listType
            )
            var bannerTitle = ""
            var id = 0
            var kind = ""

            if (mostPicksModel == null) {
                clMostPickBanner.visibility = View.GONE
                return
            }

            val theme: List<String> = ArrayList(mutableListOf(themepick, heartpick, miracle))

            if (mostPicksModel.heartpick!!.isNotEmpty() && mostPicksModel.themepick!!.isNotEmpty() && mostPicksModel.miracle!!) {
                val random = Random()
                val randomIndex = random.nextInt(theme.size)

                val randomItem = theme[randomIndex]

                when (randomItem) {
                    themepick -> {
                        bannerTitle = context.getString(R.string.themepick)
                        id = mostPickId(mostPicksModel.themepick)
                        kind = themepick
                    }

                    heartpick -> {
                        bannerTitle = context.getString(R.string.heartpick)
                        id = mostPickId(mostPicksModel.heartpick)
                        kind = heartpick
                    }

                    miracle -> {
                        bannerTitle = context.getString(R.string.miracle_month)
                        id = 0
                        kind = miracle
                    }
                }
            } else if (mostPicksModel.heartpick.isNotEmpty() && mostPicksModel.themepick!!.isNotEmpty()) {
                val isHeartPick = Math.random() < 0.5
                if (isHeartPick) {
                    bannerTitle = context.getString(R.string.heartpick)
                    id = mostPickId(mostPicksModel.heartpick)
                    kind = heartpick
                } else {
                    bannerTitle = context.getString(R.string.themepick)
                    id = mostPickId(mostPicksModel.themepick)
                    kind = themepick
                }
            } else if (mostPicksModel.heartpick.isNotEmpty() && mostPicksModel.miracle!!) {
                val isHeartPick = Math.random() < 0.5
                if (isHeartPick) {
                    bannerTitle = context.getString(R.string.heartpick)
                    id = mostPickId(mostPicksModel.heartpick)
                    kind = heartpick
                } else {
                    bannerTitle = context.getString(R.string.miracle_month)
                    id = 0
                    kind = miracle
                }
            } else if (mostPicksModel.themepick!!.isNotEmpty() && mostPicksModel.miracle!!) {
                val isHeartPick = Math.random() < 0.5
                if (isHeartPick) {
                    bannerTitle = context.getString(R.string.themepick)
                    id = mostPickId(mostPicksModel.themepick)
                    kind = themepick
                } else {
                    bannerTitle = context.getString(R.string.miracle_month)
                    id = 0
                    kind = miracle
                }
            } else if (mostPicksModel.heartpick.isNotEmpty()) {
                bannerTitle = context.getString(R.string.heartpick)
                id = mostPickId(mostPicksModel.heartpick)
                kind = heartpick
            } else if (mostPicksModel.themepick.isNotEmpty()) {
                bannerTitle = context.getString(R.string.themepick)
                id = mostPickId(mostPicksModel.themepick)
                kind = themepick
            } else if (mostPicksModel.onepick!!.isNotEmpty()) {
                bannerTitle = context.getString(R.string.imagepick)
                id = mostPickId(mostPicksModel.onepick)
                kind = onepick
            } else if (mostPicksModel.miracle!!) {
                bannerTitle = context.getString(R.string.miracle_month)
                id = 0
                kind = miracle
            } else {
                clMostPickBanner.visibility = View.GONE
                return
            }
            clMostPickBanner.visibility = View.VISIBLE


            val message = String.format(context.getString(R.string.most_in_picks_banner_msg), bannerTitle)

            val start = message.indexOf(bannerTitle)
            val end = start + bannerTitle.length

            val spannableString = SpannableString(message)

            spannableString.setSpan(
                StyleSpan(Typeface.BOLD),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            tvMostPickBanner.text = spannableString

            val finalId = id
            val finalKind = kind
            clMostPickBanner.setOnClickListener {
                fragRef.get()?.clickMyMostBanner(finalId, finalKind)
            }
        }
    }

    inner class FavoriteRankViewHolder(val binding: RankingBindingProxy) : RecyclerView.ViewHolder(binding.root) {
        @UnstableApi
        fun bind(rankingItem: RankingModel, position: Int) = with(binding) {
            val idol = rankingItem.idol ?: return@with

            // 섹션 부분 클릭되지 않게
            section?.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        v.parent.requestDisallowInterceptTouchEvent(false)
                        true // 클릭 이벤트 차단
                    }
                    else -> {
                        v.performClick()
                        false // 스크롤 이벤트는 그대로 전달
                    }
                }
            }

            // 섹션별 투표수 바 길이 조절
            // sectionName이 같은 것들을 모은다
            val sectionIdols = favorites.filter { it.sectionName == rankingItem.sectionName }
            val maxIdolHeart = sectionIdols.maxOfOrNull { it.idol?.heart ?: 0 } ?: 0
            val minIdolHeart = sectionIdols.minOfOrNull { it.idol?.heart ?: 0 } ?: 0

            val viewHolder = RankViewHolder(binding)
            viewHolder.bind(idol, position, TYPE_S_RANK, maxIdolHeart, minIdolHeart)

            // 투표수 글자색
            if(BuildConfig.CELEB) {
                voteCountView.setTextColor(ContextCompat.getColor(context, R.color.fix_gray900))
            }

            when (idol.anniversary) {
                Const.ANNIVERSARY_BIRTH -> {
                    if (BuildConfig.CELEB || idol.type == "S") {
                        // CELEB 빌드이거나 type == S → Birth 뱃지
                        badgeBirth.visibility = View.VISIBLE
                        badgeDebut.visibility = View.GONE
                    } else {
                        // type != S → Debut 뱃지
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

                    val memorialDayText =
                        "${idol.anniversaryDays}${context.getString(R.string.lable_day)}"
                    badgeMemorialDay.text = memorialDayText
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

            UtilK.profileRoundBorder(idol.miracleCount, idol.fairyCount, idol.angelCount, photoBorder);
            
            // 차트별 순위 및 섹션 표시
            val rankNumber = NumberFormat.getNumberInstance(getAppLocale(context))
                .format(rankingItem.ranking.toLong())
            rankView.text = String.format(
                context.getString(R.string.rank_count_format),
                rankNumber)

            if( rankingItem.isSection ) {
                tvSection?.text = rankingItem.sectionName
                section?.visibility = View.VISIBLE
            } else {
                section?.visibility = View.GONE
            }

            // 제외된 경우 처리
            if (rankingItem.ranking < 0) {
                rankView.text = "-"
                progressBar.setBackgroundResource(R.drawable.progressbar_ranking_gray)
            } else {
                if (BuildConfig.CELEB) {
                    var typeList: TypeListModel? = null // 셀럽
                    if (BuildConfig.CELEB) {
                        typeList = UtilK.getTypeList(context, idol.type + idol.category)
                        if (rankingItem.category != null && rankingItem.isSection)
                            typeList = UtilK.getTypeList(
                                context, idol.type + rankingItem.category)
                    }

                    progressBar.setBackgroundResource(R.drawable.progressbar_ranking)
                    val progress = ResourcesCompat.getDrawable(context.resources, R.drawable.progressbar_ranking, null)
//                    progress?.setColorFilter(
//                        if (typeList?.type == null) context.resources.getColor(R.color.main) else Color.parseColor(
//                            Util.getUiColor(
//                                context, typeList
//                            )
//                        ), PorterDuff.Mode.MULTIPLY
//                    )
                    progress?.setTint(
                        if (typeList?.type == null)
                            context.resources.getColor(R.color.main)
                        else
                            Color.parseColor(Util.getUiColor(context, typeList))
                    )
                    progressBar.background = progress
                } else {
                    progressBar.setBackgroundResource(R.drawable.progressbar_ranking_s_league)
                }
            }

            val progressPercent: Int
            if(idol.heart > 0) {
                // 즐겨찾기는 섹션의 꼴찌도 기부횟수/투표수가 많아서 투표수가 가릴 수 있어서 추가 처리
                val maxCharityTypes = sectionIdols.maxOfOrNull { item ->
                    listOf(
                        item.idol?.angelCount,
                        item.idol?.fairyCount,
                        item.idol?.miracleCount,
                        item.idol?.rookieCount,
                        if((item.idol?.rookieCount ?: 0) > 2) 1 else 0 // 슈퍼루키
                    ).count { (it ?: 0) > 0 }
                } ?: 0

                // 투표수 실제 길이 계산
                val paint = Paint().apply {
                    this.textSize = voteCountView.textSize
                }
                val heartWidth = paint.measureText((numberFormatter.format(idol.heart)))

                // 1등 바 길이
                // 레이아웃이 그려지기 전이므로 화면 폭을 기준으로 계산한다
                val progressWidth = screenWidth -
                    Util.convertDpToPixel(context, 85f + 55f) - // 프사영역 85, 투표버튼 영역 55
                    Util.convertDpToPixel(context, 20f) // 좌우 패팅 합 20dp
                // 뱃지 길이
                val badgeWidth = (maxCharityTypes * Util.convertDpToPixel(context, 19f)) + Util.convertDpToPixel(context, 20f) // 뱃지 좌우 여백 10dp
                val minWidth = badgeWidth + heartWidth
                val minPercentage = max(floor(minWidth / progressWidth * 100f), VotePercentage.MAIN_RANKING_MIN_PERCENTAGE.toFloat())

                progressPercent = VotePercentage.getVotePercentage(
                    minPercentage = minPercentage.toInt(),
                    currentPlaceVote = idol.heart,
                    firstPlaceVote = maxIdolHeart,
                    lastPlaceVote = minIdolHeart,
                )
            } else {
                progressPercent = VotePercentage.getVotePercentage(
                    minPercentage = VotePercentage.MAIN_RANKING_MIN_PERCENTAGE,
                    currentPlaceVote = idol.heart,
                    firstPlaceVote = maxIdolHeart,
                    lastPlaceVote = minIdolHeart,
                )
            }
            progressBar.setWidthRatio(progressPercent)
        }
    }

    inner class EmptyFavoriteViewHolder(val binding: NoBookmarkItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() = with(binding) {
            // 최애 없을때 부분
            if (BuildConfig.CELEB) {
                tvBookmark.setText(R.string.actor_desc_bookmark)
                tvBookmark2.setText(R.string.actor_desc_bookmark2)
            }

            btnBookmarkSetting.setOnClickListener(fragRef.get())
            ivAggDescription.setOnClickListener(fragRef.get())
        }
    }

    companion object {
        val themepick = "themepick"
        val heartpick = "heartpick"
        val miracle = "miracle"
        val onepick = "onepick"
    }
}
