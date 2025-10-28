package net.ib.mn.fragment

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.lifecycleScope
import com.airbnb.lottie.LottieDrawable
import com.android.volley.VolleyError
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity.Companion.MEZZO_PLAYER_REQ_CODE
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.FeedActivity
import net.ib.mn.activity.HeartPlusFreeActivity
import net.ib.mn.activity.MezzoPlayerActivity
import net.ib.mn.activity.MyCouponActivity
import net.ib.mn.activity.NewHeartPlusActivity
import net.ib.mn.activity.VotingGuideActivity
import net.ib.mn.activity.WebViewActivity
import net.ib.mn.core.data.repository.CouponRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.domain.usecase.GetCouponMessage
import net.ib.mn.core.domain.usecase.PostVideoAdNotificationUseCase
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.FragmentMyHeartInfoBinding
import net.ib.mn.dialog.VideoAdNotifyToastFragment
import net.ib.mn.domain.usecase.datastore.GetAdCountUseCase
import net.ib.mn.domain.usecase.datastore.GetIsEnableVideoAdPrefsUseCase
import net.ib.mn.domain.usecase.datastore.IsSetAdNotificationPrefsUseCase
import net.ib.mn.domain.usecase.datastore.SetAdNotificationPrefsUseCase
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.MessageManager
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.ResultCode
import net.ib.mn.utils.UploadSingleton
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdManager
import net.ib.mn.utils.VideoAdManager.OnAdManagerListener
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.ext.setOnSingleClickListener
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.MainViewModel
import net.ib.mn.viewmodel.MyHeartInfoViewModel
import net.ib.mn.viewmodel.MyHeartInfoViewModelFactory
import java.text.NumberFormat
import java.util.Timer
import java.util.TimerTask
import javax.inject.Inject

@AndroidEntryPoint
class MyheartInfoFragment : BaseFragment(), OnScrollToTopListener {
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    @Inject
    lateinit var getIsEnableVideoAdPrefsUseCase: GetIsEnableVideoAdPrefsUseCase

    private lateinit var binding: FragmentMyHeartInfoBinding

    private val mainViewModel: MainViewModel by activityViewModels()
    private val myHeartInfoViewModel: MyHeartInfoViewModel by activityViewModels {
        MyHeartInfoViewModelFactory(requireContext(), SavedStateHandle(), usersRepository, accountManager, getIsEnableVideoAdPrefsUseCase)
    }

    private var missionHeart = 0L
    var idolAccount: IdolAccount? = null
    var handler: Handler? = Handler(Looper.getMainLooper())

    lateinit var videoAdManager: VideoAdManager
    private var loadingCount = 0
    private var loadingString: String = ""

    private var loadingTimer: Timer? = null

    private var isClickedVideoAd = false

    // 광고 소진 후 반복호출 방지용
    private val videoAdFailed = false

    @Inject
    lateinit var videoAdUtil: VideoAdUtil

    @Inject
    lateinit var getCouponMessageUseCase: GetCouponMessage

    @Inject
    lateinit var couponRepository: CouponRepositoryImpl

    @Inject
    lateinit var getAdCountUseCase: GetAdCountUseCase

    @Inject
    lateinit var postVideoAdNotificationUseCase: PostVideoAdNotificationUseCase

    @Inject
    lateinit var setAdNotificationPrefsUseCase: SetAdNotificationPrefsUseCase

    @Inject
    lateinit var isSetAdNotificationPrefsUseCase: IsSetAdNotificationPrefsUseCase

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_my_heart_info, container, false)
        binding.lifecycleOwner = this
        return binding.root
    }

        override fun onResume() {
        if (BuildConfig.CELEB) {
            (activity as? AppCompatActivity)?.supportActionBar?.show()
        }
        refreshData()

        super.onResume()

        context?.let {
            UtilK.videoDisableTimer(it, null, null, null)
            Util.checkVideoAdTimer(requireContext())
        }

        if (videoAdFailed) {
            return
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onScrollToTop() {
        // fragment가 재생성되는 경우 문제 발생할 수 있어 try catch 처리
        // 재생성이 안되도록 fragment manager 처리는 되어 있으나 예외 발생시 대비
        try {
            binding.scrollView.scrollTo(0, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initSet()
        observeVM()
        setClickEvent()
        setVisibleEventIcon()
        if (BuildConfig.CELEB) setCelebTutorial() else setTutorial()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (context == null || activity == null) {
            return
        }

        if (requestCode == REQUEST_CODE_CHARGE) {//이거 쓰는지 파악 안됨
            val account = IdolAccount.getAccount(context)
            accountManager.fetchUserInfo(context, {
                context?.let { c -> myHeartInfoViewModel.getHeartData(c) }
            })
            super.onActivityResult(requestCode, resultCode, data)


        } else if (requestCode == MEZZO_PLAYER_REQ_CODE) {//비광 보고나서
            Util.handleVideoAdResult(
                baseActivity, false, true, requestCode, resultCode, data, "myheart_videoad"
            ) { adType: String? ->
                //비광 시청 후 처리
                videoAdUtil.onVideoSawCommon(baseActivity, true, adType) {
                    //비광 보고 복귀시에 빨간점 업데이트를 위해  timer를 실행한다.
                    myHeartInfoViewModel.updateVideoRedDot()
                    myHeartInfoViewModel.getHeartData(requireContext())
                }
            }


        } else if (requestCode == RequestCode.COUPON_USE.value) {// 쿠폰 갔다와서  update
            if (resultCode == ResultCode.COUPON_USED.value) {
                myHeartInfoViewModel.getHeartData(requireContext())
            }
        } else if (requestCode == Const.REQUEST_GO_SHOP) {//상정 갔다와서
            if (resultCode == Const.RESULT_CODE_FROM_SHOP) {
                myHeartInfoViewModel.getHeartData(requireContext())
            }
        } else if (requestCode == Const.REQUEST_GO_FREE_CHARGE) {//무료 충전소 갔다와서
            myHeartInfoViewModel.getHeartData(requireContext())
        } else if (requestCode == REQUEST_CODE_MY_FEED) {
            myHeartInfoViewModel.getHeartData(requireContext())
        }
    }

    //초기  세팅
    @SuppressLint("SetTextI18n")
    private fun initSet() {
        idolAccount = IdolAccount.getAccount(requireActivity())
        mGlideRequestManager = Glide.with(this)

        // 비디오광고 프로세스 개선
        if (Const.FEATURE_VIDEO_AD_PRELOAD) {
            videoAdManager = VideoAdManager.getInstance(requireContext(), videoAdListener)
        } else {
            showVideoAdLoading(false)
        }

        binding.tvVideoHeart.text = "♥\uFE0E" + ConfigModel.getInstance(context).video_heart

        //나의 정보 관련  데이터를 뷰모델로 부터  observe해서  받아옴.
        //viewLifecycleOwner =>  데이터  생명주기 영향 안받게
        myHeartInfoViewModel.resultModelForMyHeartInfoFragment.observe(
            viewLifecycleOwner,
            SingleEventObserver {
                val missionHeartCount = it["missionHeart"] as Long
                val todayEarnEverHeart = it["todayEarnEverHeart"] as Long
                val account = it["account"] as IdolAccount
                updateMyHeartData(account, missionHeartCount, todayEarnEverHeart)
                showLevelBar(account)
                setSubscriptionBadge(account)
            })
    }

    private fun observeVM() = with(myHeartInfoViewModel) {
        myHeartInfoViewModel.currentAccount.observe(viewLifecycleOwner) { account ->
            // 데이터가 준비된 시점에만 UI 그리기
            this@MyheartInfoFragment.idolAccount = account
            updateMyHeartData(account, getMissionHeart(), 0)
            showLevelBar(account)
            setSubscriptionBadge(account)
        }

        myHeartInfoViewModel.errorPopup.observe(viewLifecycleOwner, SingleEventObserver { message ->
            Util.showDefaultIdolDialogWithBtn1(
                context,
                null,
                message,
                { Util.closeIdolDialog() }, true
            )
        })

        updateVideoRedDot.observe(viewLifecycleOwner, SingleEventObserver {
            UtilK.videoDisableTimer(
                requireContext(),
                null,
                null,
                null
            )
        })

        updateVideoBtnEnabled.observe(viewLifecycleOwner, SingleEventObserver {
            binding.layoutVideoAd.isEnabled = it
        })

        moveScreenToFreeCharge.observe(viewLifecycleOwner, SingleEventObserver {
            setUiActionFirebaseGoogleAnalyticsFragment(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "myheart_freestore"
            )
            startActivityForResult(
                HeartPlusFreeActivity.createIntent(context),
                Const.REQUEST_GO_FREE_CHARGE
            )
        })

        moveScreenToStore.observe(viewLifecycleOwner, SingleEventObserver {
            setUiActionFirebaseGoogleAnalyticsFragment(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "menu_shop_main"
            )
            startActivityForResult(
                NewHeartPlusActivity.createIntent(context),
                Const.REQUEST_GO_SHOP
            )
        })

        moveScreenToVideo.observe(viewLifecycleOwner, SingleEventObserver { isEnable ->
            if (isEnable) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "myheart_videoad"
                )
                startActivityForResult(
                    MezzoPlayerActivity.createIntent(requireContext(), Const.ADMOB_REWARDED_VIDEO_PROFILE_UNIT_ID),
                    MEZZO_PLAYER_REQ_CODE
                )
            } else {
                val dialogFragment = AdExceedDialogFragment()
                dialogFragment.show(parentFragmentManager, "AdExceedDialogFragment")
            }
        })

        mainViewModel.refreshMyHearData.observe(viewLifecycleOwner, Observer {
            refreshData()
        })
    }

    private fun refreshData() {
        myHeartInfoViewModel.getHeartData(context ?: return)
    }

    //나의 하트 정보가 업데이트가 필요할떄 업데이트를 진행한다.
    private fun updateMyHeartData(
        account: IdolAccount,
        missonHeartCount: Long?,
        todayEarnEverHeart: Long?
    ) {
        val userModel = account?.userModel ?: return
        val locale = LocaleUtil.getAppLocale(context ?: return)

        val heartCountComma =
            NumberFormat.getNumberInstance(locale).format(account.heartCount.toLong())
        val everHeartCountComma = NumberFormat.getNumberInstance(locale).format(
            userModel.strongHeart.toLong()
        )
        val weakHeartCountComma = NumberFormat.getNumberInstance(locale).format(
            userModel.weakHeart.toLong()
        )
        val diaCountComma =
            NumberFormat.getNumberInstance(locale).format(userModel.diamond.toLong())
        val myAggVoteCount = NumberFormat.getNumberInstance(locale).format(account.levelHeart)

        val remainNextLevel =
            NumberFormat.getNumberInstance(locale).format(getNextLevelUpHeart(account.levelHeart))

        val myCouponCount = Util.messageParse(requireActivity(), userModel.messageInfo, "C")

        if (myCouponCount > Util.getPreferenceInt(requireActivity(), "message_coupon_count", -1)) {
            Util.setPreference(requireActivity(), "message_new", true)
        }
        if (Util.getPreferenceBool(requireActivity(), "message_new", false)) {
            binding.layoutCoupon.visibility = View.VISIBLE
        }

        if (myCouponCount > 0) {
            binding.tvCouponCount.text =
                NumberFormat.getNumberInstance(locale).format(myCouponCount)
            binding.layoutCoupon.visibility = View.VISIBLE
        } else {
            binding.layoutCoupon.visibility = View.GONE
            lifecycleScope.launch {
                MessageManager.shared().getCoupons(context, getCouponMessageUseCase) {
                    Util.closeProgress()
                }
            }
        }

        if (missonHeartCount != null) {
            missionHeart = missonHeartCount
        }


        binding.name.text = account.userModel?.nickname
        binding.level.setImageDrawable(
            Util.getLevelImageDrawable(
                requireActivity(),
                account.userModel?.level ?: 0
            )
        )


        // 프로필 이미지가 안나온다는 경우가 있어서 false로 변경
        mGlideRequestManager
            .load(account.profileUrl)
            .apply(RequestOptions.circleCropTransform())
            .error(Util.noProfileImage(account.userId))
            .fallback(Util.noProfileImage(account.userId))
            .placeholder(Util.noProfileImage(account.userId))
            .into(binding.photo)


        setFavoritesName(account.most)

        binding.tvMyHeartCount.text = heartCountComma
        binding.tvEverHeart.text = everHeartCountComma
        binding.tvDailyHeart.text = weakHeartCountComma
        binding.tvMyDiaCount.text = diaCountComma
        binding.tvAllExp.text = String.format(getString(R.string.total_amount), myAggVoteCount)
        binding.tvNextLevel.text = String.format(getString(R.string.next_level_progress), remainNextLevel)
    }

    private fun showLevelBar(account: IdolAccount) {

        try {
            val level = account.level
            val levelHeart = account.levelHeart
            val currentLevelHeart = Const.LEVEL_HEARTS[level]
            val nextLevelHeart =
                if (level < Const.LEVEL_HEARTS.size - 1) Const.LEVEL_HEARTS[level + 1] else currentLevelHeart

            val total = nextLevelHeart - currentLevelHeart
            val curr = levelHeart - currentLevelHeart

            val textLevelCount =
                NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(requireContext()))
                    .format(level)
            binding.tvLevel.text = "Lv. $textLevelCount"

            val isMaxLevel = level == Const.MAX_LEVEL
            val progressPercentage = if (isMaxLevel) 100 else (curr * 100 / total).toInt()

            val levelProgress = binding.progressLevel

            levelProgress.apply {
                if (progress > 0) {
                    animateProgressBar(this, progressPercentage)
                } else {
                    progress = 0
                    postDelayed({
                        animateProgressBar(this, progressPercentage)
                    }, 10)
                }
            }

            binding.tvNextLevel.visibility = if (isMaxLevel) View.GONE else View.VISIBLE

            val updateLevelProgress = {
                updateProgress(level, curr, total, isMaxLevel)
                animateProgressBar(levelProgress, progressPercentage)
            }

            if (levelProgress.progress > 0) {
                updateLevelProgress()
            } else {
                levelProgress.apply {
                    progress = 0
                    postDelayed({
                        updateLevelProgress()
                    }, 10)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun animateProgressBar(
        progressBar: ProgressBar,
        targetProgress: Int,
        duration: Long = 300
    ) {
        ValueAnimator.ofInt(progressBar.progress, targetProgress).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                progressBar.progress = animation.animatedValue as Int
            }
            start()
        }
    }

    private fun updateProgress(level: Int, curr: Long, total: Int, isMaxLevel: Boolean) {
        val progress = if (isMaxLevel || level >= Const.LEVEL_HEARTS.size - 1) {
            100
        } else {
            (curr.toFloat() / total.toFloat() * 100.0f).toInt()
        }

        binding.progressLevel.apply {
            setProgress(progress)
        }
    }

    private fun getNextLevelUpHeart(heart: Long): Long {
        var next = heart
        for (i in 1 until Const.LEVEL_HEARTS.size) {
            if (next < Const.LEVEL_HEARTS[i]) {
                next = Const.LEVEL_HEARTS[i].toLong()
                break
            }
        }
        return next - heart
    }

    //클릭이벤트 모음
    private fun setClickEvent() {

        //프로필 눌렀을때 이동하게 수정
        binding.photo.setOnClickListener {
            setUiActionFirebaseGoogleAnalyticsFragment(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "menu_feed"
            )

            val intent = Intent(
                FeedActivity.createIntent(
                    requireActivity(),
                    IdolAccount.getAccount(requireActivity())?.userModel
                )
            )

            //나의 정보피드 화면 으로 갔을때 -> 나의 정보 화면 가는 버튼 안보이게 하는 값.
            intent.putExtra(Const.MY_INFO_TO_FEED_PREFERENCE_KEY, true)
            requireActivity().startActivityForResult(intent, REQUEST_CODE_MY_FEED)

        }


        //누적 투표 설명 화면 가기
        binding.btnLevelInfo.setOnClickListener {
            setUiActionFirebaseGoogleAnalyticsFragment(
                GaAction.MYHEART_LEVEL.actionValue,
                GaAction.MYHEART_LEVEL.label
            )
            startActivity(Intent(requireActivity(), VotingGuideActivity::class.java))
        }

        //나의 쿠폰 화면  가기
        binding.layoutCoupon.setOnClickListener {
            requireActivity().startActivityForResult(
                MyCouponActivity.createIntent(requireActivity()),
                RequestCode.COUPON_USE.value
            )
        }

        //하트모으는 방법 화면 가기
        binding.btnCurrencyInfo.setOnClickListener {
            setUiActionFirebaseGoogleAnalyticsFragment(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "myheart_tip"
            )
            startActivity(
                WebViewActivity.createIntent(
                    requireActivity(), Const.TYPE_NOTICE,
                    ConfigModel.getInstance(requireActivity()).earnWayNoticeId,
                    getString(R.string.title_notice)
                )
            )
        }

        binding.layoutFreeCharge.setOnClickListener {
            if (binding.lottieTutorialFreeCharge.isVisible) return@setOnClickListener
            myHeartInfoViewModel.moveScreenToFreeCharge()
        }

        binding.layoutShop.setOnClickListener {
            if (binding.lottieTutorialShop.isVisible) return@setOnClickListener
            myHeartInfoViewModel.moveScreenToStore()
        }

        binding.layoutVideoAd.setOnSingleClickListener {
            if (binding.lottieTutorialVideoAd.isVisible) return@setOnSingleClickListener
            showVideoAd()
        }

        binding.tvHistory.setOnClickListener {
            if (binding.lottieTutorialHistory.isVisible) return@setOnClickListener
            onClickHistory()
        }
    }

    private fun setVisibleEventIcon() {
        val config = ConfigModel.getInstance(context)

        if (config.showStoreEventMarker == "Y") {
            binding.ivShopEvent.visibility = View.VISIBLE
        }

        if (config.showFreeChargeMarker == "Y") {
            binding.ivFreeChargeEvent.visibility = View.VISIBLE
        }
    }

    private fun setSubscriptionBadge(account: IdolAccount) {
        try {

            val userModel = account.userModel
            val subscriptions = userModel?.subscriptions

            if (userModel != null && !subscriptions.isNullOrEmpty()) {
                for (mySubscription in subscriptions) {
                    if (mySubscription.familyappId == 1 || mySubscription.familyappId == 2) {
                        binding.tvSubscriptionName.apply {
                            visibility = View.VISIBLE
                            if (BuildConfig.CELEB) {
                                background = ResourcesCompat.getDrawable(context.resources, R.drawable.bg_daily_badge_celeb, null)
                            }
                            text = mySubscription.name
                        }
                        break
                    } else {
                        binding.tvSubscriptionName.visibility = View.GONE
                    }
                }
            } else {
                binding.tvSubscriptionName.visibility = View.GONE
            }
        } catch (e: NullPointerException) {
            binding.tvSubscriptionName.visibility = View.GONE
        }
    }

    //최애 이름 가져오기
    private fun setFavoritesName(most: IdolModel?) {

        if (most?.type != null) {
//            most.setLocalizedName(this)

            //최애이름  적용 전에  -> 로케일 한번더  set해줌.
            //무충에서 오퍼워나 탭조이 갔다오면 현재 언어 설정 풀리는 경우가 있어서...
            val text: SpannableString
            if (most.type.contains("G")) {
                text = SpannableString(most.getName(requireActivity()))
            } else {
                if (most.getName(requireActivity()).contains("_")) {
                    val mostSoloName = Util.nameSplit(requireActivity(), most)[0]
                    val mostGroupName = Util.nameSplit(requireActivity(), most)[1]
                    if (Util.isRTL(requireActivity())) {
                        text = SpannableString(
                            mostGroupName
                                + " "
                                + mostSoloName
                        )
                        text.setSpan(
                            AbsoluteSizeSpan(Util.convertDpToPixel(requireActivity(), 10f).toInt()),
                            0,
                            mostGroupName.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        text.setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    requireActivity(),
                                    R.color.gray300
                                )
                            ),
                            0,
                            mostGroupName.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                    } else {
                        text = SpannableString(
                            mostSoloName
                                + " "
                                + mostGroupName
                        )
                        text.setSpan(
                            AbsoluteSizeSpan(Util.convertDpToPixel(requireActivity(), 10f).toInt()),
                            mostSoloName.length + 1,
                            text.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        text.setSpan(
                            ForegroundColorSpan(
                                ContextCompat.getColor(
                                    requireActivity(),
                                    R.color.gray300
                                )
                            ),
                            mostSoloName.length + 1,
                            text.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                    }
                } else {
                    text = SpannableString(most.getName(requireActivity()))
                }
            }

            binding.tvFavoriteName.text = text
        } else {
            binding.tvFavoriteName.text = requireActivity().getString(R.string.empty_most)
        }
    }

    private val videoAdListener: OnAdManagerListener = object : OnAdManagerListener {
        override fun onAdPreparing() {
            showVideoAdLoading(true)
            myHeartInfoViewModel.updateVideoBtnEnabled(false)
        }

        override fun onAdReady() {
            showVideoAdLoading(false)
        }

        override fun onAdRewared() {
        }

        override fun onAdFailedToLoad() {
            // 해외면 1분 후 다시 로드 시도
            showVideoAdLoading(false)
        }

        override fun onAdClosed() {}
    }

    private fun showVideoAdLoading(show: Boolean) {
        if (loadingTimer != null) {
            loadingTimer!!.cancel()
            loadingTimer!!.purge()
            loadingTimer = null
        }
        if (show) {
            loadingCount = 0
            val dots = "..."
            val spaces = "   "
            loadingString = getString(R.string.loading).replace("...", "").replace("…", "")
            loadingTimer = Timer()
            loadingTimer!!.schedule(object : TimerTask() {
                override fun run() {
                    Handler(Looper.getMainLooper()).post {
                        loadingCount = (loadingCount + 1) % 3
                        myHeartInfoViewModel.updateVideoText(
                            loadingString + dots.substring(2 - loadingCount) + spaces.substring(
                                loadingCount
                            )
                        )
                    }
                }
            }, 100, 500)
        } else {
            myHeartInfoViewModel.updateVideoText(
                String.format(
                    getString(R.string.desc_reward_video),
                    ConfigModel.getInstance(context).video_heart.toString() + ""
                )
            )

            myHeartInfoViewModel.updateVideoBtnEnabled(true)
        }
    }

    private fun showVideoAd() {
        if (isClickedVideoAd) return
        isClickedVideoAd = true

        val endTime = Util.getPreferenceLong(
            context,
            Const.VIDEO_TIMER_END_TIME_PREFERENCE_KEY,
            Const.DEFAULT_VIDEO_DISABLE_TIME
        )

        val currentTime = System.currentTimeMillis()
        val isTimerRunning = endTime != Const.DEFAULT_VIDEO_DISABLE_TIME && endTime > currentTime

        if (isTimerRunning) {
            lifecycleScope.launch {
                val isSet: Boolean = isSetAdNotificationPrefsUseCase()
                    .mapDataResource { it }
                    .awaitOrThrow() ?: false

                withContext(Dispatchers.Main) {
                    Util.showVideoAdDisableTimerDialog(
                        context,
                        requireActivity().supportFragmentManager,
                        outsideCancel = false,
                        isAlreadySetNotification = isSet,
                        listener1 = { _: View? ->
                            context?.let {
                                UtilK.videoDisableTimer(it, null, null, null)
                            }
                            Util.closeIdolDialog()
                        },
                    ) {
                        lifecycleScope.launch {
                            postVideoAdNotificationUseCase().collectLatest { result ->
                                if (result.success) {
                                    VideoAdNotifyToastFragment().show(
                                        requireActivity().supportFragmentManager,
                                        "VideoAdNotifyToast"
                                    )
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(requireContext(), result.error?.message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            setAdNotificationPrefsUseCase(true)
                                .mapDataResource { it }
                                .awaitOrThrow()
                        }
                    }
                }
            }
        } else {
            lifecycleScope.launch {
                setAdNotificationPrefsUseCase(false)
                    .mapDataResource { it }
                    .awaitOrThrow()
            }

            // 타이머 없음 또는 만료됨 → 광고 실행
            binding.layoutVideoAd.isEnabled = false
            Handler().postDelayed({ binding.layoutVideoAd.isEnabled = true }, 1500)
            myHeartInfoViewModel.moveScreenToVideo()
        }
        isClickedVideoAd = false
    }

    private fun onClickHistory() {
        val fragment = MyheartHistoryFragment()
        val transaction = requireActivity().supportFragmentManager.beginTransaction()

        if (BuildConfig.CELEB) {
            (requireView().parent as? View)?.id?.let {
                transaction
                    .setCustomAnimations(0, 0, 0, 0)
                    .replace(it, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        } else {
            transaction
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .replace(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun setTutorial() {
        when(val currentTutorialIndex = TutorialManager.getTutorialIndex()) {
            TutorialBits.MY_HEAET_VIDEO_AD -> {
                setupLottieTutorial(binding.lottieTutorialVideoAd) {
                    mainViewModel.updateTutorial(currentTutorialIndex)
                    showVideoAd()
                }
            }
            TutorialBits.MY_HEART_HEART_SHOP -> {
                setupLottieTutorial(binding.lottieTutorialShop) {
                    mainViewModel.updateTutorial(currentTutorialIndex)
                    myHeartInfoViewModel.moveScreenToStore()
                }
            }
            TutorialBits.MY_HEART_FREE_HEART -> {
                setupLottieTutorial(binding.lottieTutorialFreeCharge) {
                    mainViewModel.updateTutorial(currentTutorialIndex)
                    myHeartInfoViewModel.moveScreenToFreeCharge()
                }
            }
            TutorialBits.MY_HEART_EARN -> {
                setupLottieTutorial(binding.lottieTutorialHistory) {
                    mainViewModel.updateTutorial(currentTutorialIndex)
                    onClickHistory()
                }
            }
        }
    }

    private fun setCelebTutorial() {
        when(val currentTutorialIndex = TutorialManager.getTutorialIndex()) {
            CelebTutorialBits.MY_HEART_VIDEO_AD -> {
                setupLottieTutorial(binding.lottieTutorialVideoAd) {
                    mainViewModel.updateTutorial(currentTutorialIndex)
                    showVideoAd()
                }
            }
            CelebTutorialBits.MY_HEART_HEART_SHOP -> {
                setupLottieTutorial(binding.lottieTutorialShop) {
                    mainViewModel.updateTutorial(currentTutorialIndex)
                    myHeartInfoViewModel.moveScreenToStore()
                }
            }
            CelebTutorialBits.MY_HEART_FREE_HEART -> {
                setupLottieTutorial(binding.lottieTutorialFreeCharge) {
                    mainViewModel.updateTutorial(currentTutorialIndex)
                    myHeartInfoViewModel.moveScreenToFreeCharge()
                }
            }
            CelebTutorialBits.MY_HEART_EARN -> {
                setupLottieTutorial(binding.lottieTutorialHistory) {
                    mainViewModel.updateTutorial(currentTutorialIndex)
                    onClickHistory()
                }
            }
        }
    }

    companion object {
        const val REQUEST_CODE_CHARGE = 10
        const val REQUEST_CODE_MY_FEED = 1022
    }
}