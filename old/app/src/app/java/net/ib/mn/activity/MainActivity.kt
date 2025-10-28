/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description: kotlin MainActivity
 *
 * */

package net.ib.mn.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.SpannableString
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinSdk
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import jp.maio.sdk.android.MaioAds
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import net.ib.mn.BuildConfig
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.addon.IdolGson
import net.ib.mn.attendance.AttendanceActivity
import net.ib.mn.awards.IdolAwardsActivity
import net.ib.mn.core.data.repository.AuthRepository
import net.ib.mn.core.data.repository.MessagesRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.designsystem.navigation.MainBottomNavigation
import net.ib.mn.core.designsystem.toggle.SwitchToggleButton
import net.ib.mn.core.model.AwardModel
import net.ib.mn.core.model.EndPopupModel
import net.ib.mn.core.model.FamilyAppModel
import net.ib.mn.databinding.ActivityMainBinding
import net.ib.mn.databinding.DialogReviewBinding
import net.ib.mn.dialog.AdBannerDialog
import net.ib.mn.dialog.VoteDialogFragment.Companion.VOTE_DIALOG_TAG
import net.ib.mn.feature.friend.FriendsActivity
import net.ib.mn.feature.search.history.SearchHistoryActivity
import net.ib.mn.fragment.FavoritIdolFragment
import net.ib.mn.fragment.FreeboardFragment
import net.ib.mn.fragment.MyInfoFragment
import net.ib.mn.fragment.MyheartInfoFragment
import net.ib.mn.fragment.NewSoloRankingFragment
import net.ib.mn.fragment.SignupFragment
import net.ib.mn.fragment.WelcomeMissionFragment
import net.ib.mn.gcm.GcmUtils
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.liveStreaming.LiveStreamingListFragment
import net.ib.mn.liveStreaming.LiveTrailerSlideFragment
import net.ib.mn.model.AccessModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.pushy.PushyUtil
import net.ib.mn.ranking.MainRankingFragment
import net.ib.mn.remote.IdolBroadcastManager
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.AppConst
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Gcode
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.MidnightTaskScheduler
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.dpToPx
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.ext.getSerializableData
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.setFirebaseScreenViewEvent
import net.ib.mn.viewmodel.MainViewModel
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlin.math.min

/**
 * @see
 * */

@UnstableApi
@SuppressLint("NewApi")
@AndroidEntryPoint
class MainActivity :
    BaseActivity(), WelcomeMissionFragment.WelcomeMissionDialogListener, HasFreeboard {
    enum class MainTab(val index: Int) {
        RANKING(0),
        FAVORITE(1),
        MY_HEART(2),
        FREE_BOARD_OR_LIVE(3),
        MENU(4);

        companion object {
            fun fromIndex(index: Int): MainTab {
                return entries.find { it.index == index } ?: MainTab.RANKING
            }
        }
    }

    @Inject
    lateinit var sharedAppState: SharedAppState

    @Inject
    lateinit var videoAdUtil: VideoAdUtil

    @Inject
    lateinit var messagesRepository: MessagesRepositoryImpl

    @Inject
    lateinit var usersRepository: UsersRepository

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var idolApiManager: IdolApiManager

    @Inject
    lateinit var idolBroadcastManager: IdolBroadcastManager

    @Inject
    lateinit var accountManager: IdolAccountManager

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private var mainRankingFragment: MainRankingFragment? = null
    private var favoriteIdolFragment: FavoritIdolFragment? = null
    private var myHeartInfoFragment: MyheartInfoFragment? = null
    override var freeboardFragment: FreeboardFragment? = null
    private var liveFragment: LiveStreamingListFragment? = null
    private var menuFragment: MyInfoFragment? = null
    private var activeFragment: Fragment? = null
    private var begin: Date? = null
    private var end: Date? = null
    private var aggregatingTime: String? = null
    private var aggregatingTimeFormatOne: String? = null
    private var aggregatingTimeFormatFew: String? = null
    private var isMissionPage: Boolean = false
    private var isInitialize = false
    private var isShowMissionBtn = false

    private lateinit var consentInformation: ConsentInformation
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)
    private var currentIndex = 0
    private val triggerClick = mutableStateOf(false)
    private var awardAnimatorSet: AnimatorSet? = null
    private var disableTab = false // 하단 탭 연타 방지

    private val timeHandler = TimeHandler(this)

    private val timeThread =
        Thread {
            Timer().scheduleAtFixedRate(
                object : TimerTask() {
                    override fun run() {
                        timeHandler.sendEmptyMessage(0)
                    }
                },
                0,
                1000,
            )
        }

    private class TimeHandler(
        activity: MainActivity,
    ) : Handler() {
        private val mActivity = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = mActivity.get()
            activity?.handleMessage(msg)
        }
    }

    private fun handleMessage(msg: Message) {
        if (begin == null && end == null) {
            return
        }

        val now = (Date().time + 32400000) % 86400000
        val beginTime = (begin!!.time + 32400000) % 86400000
        val endTime = (end!!.time + 32400000) % 86400000
        val strTime: String
        if (now in beginTime..endTime) {
            strTime = aggregatingTime ?: ""
            Util.setPreference(this, Const.PREF_IS_AGGREGATING_TIME, true)
        } else {
            val time = if (endTime < now) beginTime + 86400000 else beginTime
            val deadline = time - now
            strTime =
                when {
                    deadline <= 60000 -> String.format(aggregatingTimeFormatOne ?: "", 1)
                    deadline <= 600000 ->
                        String.format(
                            aggregatingTimeFormatFew ?: "",
                            deadline / 60000 + 1,
                        )

                    else -> {
                        val sdf = SimpleDateFormat("HH:mm:ss", LocaleUtil.getAppLocale(this))
                        sdf.timeZone = TimeZone.getTimeZone("UTC")
                        sdf.format(Date(deadline))
                    }
                }
            Util.setPreference(this, Const.PREF_IS_AGGREGATING_TIME, false)
        }

        binding.mainToolbar.tvDeadline.text = strTime
    }

    private lateinit var glideRequestManager: RequestManager

    private var locale: String? = null

    // 구글 리뷰 팝업 이쪽으로 옮김
    private var reviewDialog: Dialog? = null
    private lateinit var mRequestHeartHandler: Handler

    val onGcmRegistered =
        SignupFragment.OnRegistered { id ->
            // preference에 있는 값과 비교하여 변경이 있을 때에만 발송

            Util.log("FCM KEY CHANGED.")
            Util.setPreference(this@MainActivity, Const.PREF_GCM_PUSH_KEY, id)

            val deviceId = Util.getDeviceUUID(this@MainActivity)
            lifecycleScope.launch {
                usersRepository.updatePushKey(
                    id,
                    deviceId,
                    {
                        Util.log("FCM KEY UPDATE SUCCESS.")
                    },
                    {
                        Util.log("FCM KEY UPDATE ERROR.")
                    }
                )
            }
        }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Logger.v("onsaveInstance 값" + savedInstanceState.getSerializable(BANNER_LIST_SAVE))

        // main으로 다시 돌아올 때 mainActivity가 재생성될 경우 deeplink를 타고 왔으면 다시 해당 로직이 실행되므로,
        // 해당 intent extra key EXTRA_NEXT_ACTIVITY를 지워준다.
        intent.removeExtra(EXTRA_NEXT_ACTIVITY)
        intent.removeExtra(PARAM_NEXT_INTENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        binding.mainScreen.applySystemBarInsets()
        glideRequestManager = Glide.with(this)

        checkVoteDialog()
        MidnightTaskScheduler(this).setDailyAlarm()
        initAds()
        initIdolApplication()
        getIntentData()
        initializeFragments(savedInstanceState)
        setToolBar()
        setToolbarClickListener()
        changeAttendanceIconStatus()
        setupBottomNavigation()
        observedVM()
        setDeepLinkIntent(savedInstanceState)
        setMainTutorial()
        setSwitchToggleButton()
        initPreferenceData()
        updatePushKey(savedInstanceState)
        setRequestEvent()
        setGoogleReview()
        setTimerConfiguration()
        appVersionCheck()
        setupUdpConnections()
        setEventListener()

        // Auth process token
        setAuthRequest()
        setBackStackConfiguration()

        checkAwards()

        // 이모티콘 압축해제 시작.
        mainViewModel.triggerEmoticonUnzipProcess(this)

        // 푸시 설정 유도 초기화
        sharedAppState.setIncitePushHidden(false)
    }

    private fun checkVoteDialog() {
        // BottomSheet 복원 방지. 투표 보상하트는 activity의 fragment manager를 사용함.
        supportFragmentManager.findFragmentByTag(VOTE_DIALOG_TAG)?.let { frag ->
            supportFragmentManager.beginTransaction()
                .remove(frag)
                .commitNowAllowingStateLoss()
        }
    }

    private fun initAds() {
        // maio
        if (Const.USE_MAIO) {
            MaioAds.init(this, Const.MAIO_MEDIA_ID, null)
        }

        // pushy
        if (BuildConfig.CHINA) {
            PushyUtil.listen(this)
        }

        UtilK.setTapJoy(this) { _ ->
            //nothing to do
        }

        if (!BuildConfig.CHINA) {
            // MobileAd 동의 요청.
            requestMobileAdConsent()

            // appLovin max
            AppLovinSdk.getInstance(this).mediationProvider = "max"
            AppLovinSdk.initializeSdk(this) {
                val max = MaxRewardedAd.getInstance(Const.APPLOVIN_MAX_UNIT_ID, this)
                max.setListener(null)
                max.loadAd()
            }
        }
    }

    private fun initIdolApplication() {
        IdolApplication.getInstance(this).apply {
            mainActivity = this@MainActivity
            mapHeartboxViewable.clear()
        }
    }

    private fun getIntentData() {
        if (intent.getBooleanExtra(OTHER_APP_SHARE, false)) {
            Util.showDefaultIdolDialogWithBtn2(
                this,
                getString(R.string.empty_most),
                getString(R.string.lockscreen_not_available),
                {
                    Util.showProgress(this)
                    Util.closeIdolDialog()
                    startActivity(Intent(this, FavoriteSettingActivity::class.java))
                    finish()
                },
                {
                    Util.closeIdolDialog()
                },
            )
        }
    }

    private fun initPreferenceData() {
        locale = Util.getPreference(this, Const.PREF_LANGUAGE)

        // 하트박스 안나온다는 사람이 많아서 옮겨봄...
        Util.setPreference(this, Const.PREF_HEART_BOX_VIEWABLE, true) // 앱을 실행할 때 true로 설정

        val firstCategory = Util.getPreference(this, Const.PREF_DEFAULT_CATEGORY) ?: Const.TYPE_MALE
        mainViewModel.setIsMaleGender(firstCategory == Const.TYPE_MALE)

        if (firstCategory == "M") {
            setFirebaseScreenViewEvent(
                GaAction.RANKING_BOY_INDV,
                NewSoloRankingFragment::class.simpleName
            )
        } else {
            setFirebaseScreenViewEvent(
                GaAction.RANKING_GIRL_INDV,
                NewSoloRankingFragment::class.simpleName
            )
        }
    }

    private fun updatePushKey(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            // 앱 시작시 push key 업데이트
            GcmUtils.registerDevice(this, onGcmRegistered)
        }
    }

    private fun setGoogleReview() {
        // 앱 설치 후 24시간 후에 구글리뷰 팝업
        if (!BuildConfig.ONESTORE) {
            mRequestHeartHandler =
                Handler(Looper.getMainLooper()) {
                    lifecycleScope.launch {
                        usersRepository.provideHeart(
                            type = "review",
                            listener = {},
                            errorListener = {}
                        )
                    }
                    true
                }

            val appInstall = Util.getPreferenceLong(this, Const.PREF_APP_INSTALL, 0)
            if (appInstall == 0L) {
                Util.setPreference(this, Const.PREF_APP_INSTALL, Date().time)
            } else {
                val timeDiff = Date().time - appInstall
                if (timeDiff > 24 * 60 * 60 * 1000) {
                    val reviewAlreadyRequest =
                        Util.getPreferenceBool(this, Const.PREF_REQUEST_REVIEW, false)
                    if (!reviewAlreadyRequest && !BuildConfig.CHINA) {
                        showGoogleReviewDialog(this@MainActivity)
                    }
                }
            }
        }
    }

    private fun setupUdpConnections() {
        // udp
        if (ConfigModel.getInstance(this).udp_stage > 1) {
            idolBroadcastManager.setupConnection(
                applicationContext,
                ConfigModel.getInstance(this).udp_broadcast_url,
            )
        }
    }

    @SuppressLint("CommitTransaction")
    private fun setEventListener() = with(binding) {
        ivMission.setOnClickListener {
            isMissionPage = true
            val fragment = WelcomeMissionFragment()
            val fragmentManager = supportFragmentManager
            val transaction = fragmentManager.beginTransaction()

            transaction.add(android.R.id.content, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun checkAwards() {
        // awards
        if (ConfigModel.getInstance(this).showAwardTab) {
            try {
                val json = Util.getPreference(this, Const.AWARD_MODEL)
                val awardData = Json {
                    ignoreUnknownKeys = true
                }.decodeFromString<AwardModel?>(json)
                awardData?.let {
                    val url = awardData.mainFloatingImgUrl
                    glideRequestManager.load(url).into(binding.ivAwards)
                }

                showAwardButton(true)
                binding.ivAwards.setOnClickListener {
                    val i = Intent(this, IdolAwardsActivity::class.java)
                    startActivity(i)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializeFragments(savedInstanceState: Bundle?) {
        if (savedInstanceState == null && supportFragmentManager.fragments.isEmpty()) {
            // 앱 최초 실행 시 → RANKING 탭으로 진입
            navigateToFragment(MainTab.RANKING)
        } else {
            // 복원된 Fragment에 맞춰 바텀탭 index 동기화
            val currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragment_container_view)
            val tab = when (currentFragment) {
                is MainRankingFragment -> MainTab.RANKING
                is FavoritIdolFragment -> MainTab.FAVORITE
                is MyheartInfoFragment -> MainTab.MY_HEART
                is MyInfoFragment -> MainTab.MENU
                is FreeboardFragment -> MainTab.FREE_BOARD_OR_LIVE
                is LiveStreamingListFragment -> MainTab.FREE_BOARD_OR_LIVE
                else -> MainTab.RANKING
            }

            mainViewModel.setSelectedIndex(tab.index)
            mainViewModel.updateBottomTabIndex(tab.index)
            currentIndex = tab.index
        }

        // 화면이 재생성 되었을때 화면이 중복 생성 되지 않게 기존 화면 사용.
        mainRankingFragment =
            supportFragmentManager.findFragmentByTag(MainRankingFragment::class.java.name) as? MainRankingFragment
        favoriteIdolFragment =
            supportFragmentManager.findFragmentByTag(FavoritIdolFragment::class.java.name) as? FavoritIdolFragment
        myHeartInfoFragment =
            supportFragmentManager.findFragmentByTag(MyheartInfoFragment::class.java.name) as? MyheartInfoFragment
        menuFragment =
            supportFragmentManager.findFragmentByTag(MyInfoFragment::class.java.name) as? MyInfoFragment

        if (ConfigModel.getInstance(this).showLiveStreamingTab) {
            liveFragment =
                supportFragmentManager.findFragmentByTag(LiveStreamingListFragment::class.java.name) as? LiveStreamingListFragment
        } else {
            freeboardFragment =
                supportFragmentManager.findFragmentByTag(FreeboardFragment::class.java.name) as? FreeboardFragment
        }

        // 메모리 부족으로 날라갔을경우 뷰모델에 넣어도 다시 초기화 되므로 preference에 넣어줌.
        currentIndex = Util.getPreferenceInt(
            this,
            Const.MAIN_BOTTOM_TAB_CURRENT_INDEX,
            mainViewModel.selectedBottomNavIndex.value ?: 0
        )
        mainViewModel.setSelectedIndex(currentIndex)

        // 이전에 활성화 되어있던 화면 다시 넣어줍니다.
        activeFragment = when (MainTab.fromIndex(currentIndex)) {
            MainTab.RANKING -> mainRankingFragment
            MainTab.FAVORITE -> favoriteIdolFragment
            MainTab.MY_HEART -> myHeartInfoFragment
            MainTab.FREE_BOARD_OR_LIVE -> {
                if (ConfigModel.getInstance(this).showLiveStreamingTab) {
                    liveFragment
                } else {
                    freeboardFragment
                }
            }

            MainTab.MENU -> menuFragment
        }
    }

    private fun setBackStackConfiguration() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            @OptIn(UnstableApi::class)
            override fun handleOnBackPressed() {
                val fragments = supportFragmentManager.fragments
                val topFragment = fragments.reversed().find { it.isVisible }
                when (topFragment) {
                    // FIXME liveFragment 만들 때 화면 스택이 좀 바뀌는거 같은데 awards/play 화면 둘 다 근 1년은 안 쓸 화면같아서 그냥 임시 처리함
                    is MainRankingFragment, is LiveTrailerSlideFragment -> {
                        showEndPopup()
                    }

                    else -> {
                        if (isMissionPage) {
                            isMissionPage = false
                            supportFragmentManager.popBackStack()
                        } else {
                            if (isShowMissionBtn) {
                                binding.ivMission.visibility = View.VISIBLE
                            }
                            navigateToFragment(MainTab.RANKING)
                        }

                        showAwardButton(ConfigModel.getInstance(this@MainActivity).showAwardTab)
                    }
                }
            }
        })
    }

    private fun setTimerConfiguration() {
        aggregatingTime = resources.getString(R.string.aggregating_time)
        aggregatingTimeFormatOne = resources.getString(R.string.deadline_format_one)
        aggregatingTimeFormatFew = resources.getString(R.string.deadline_format_few)
        begin = ConfigModel.getInstance(this).inactiveBegin
        end = ConfigModel.getInstance(this).inactiveEnd
    }

    private fun setAuthRequest() {
        intent.extras?.let {
            val isAuthRequest = it.getBoolean("is_auth_request", false)
            val uri = it.getString("uri")

            if (isAuthRequest) {
                processAccessToken(uri, null)
            }
        }
    }

    private fun setRequestEvent() {
        val bundle = intent.extras
        val push: Boolean

        if (bundle != null) {
            push = bundle.getBoolean("push", false)

            val account = IdolAccount.getAccount(this)
            if (push && account != null) {
                accountManager.fetchUserInfo(
                    this,
                    {
                        mainViewModel.requestEvent(this@MainActivity)
                    }
                )
            } else {
                mainViewModel.requestEvent(this@MainActivity)
            }

            if (bundle.containsKey(PARAM_IS_MALE)) {
                val isMale = bundle.getBoolean(PARAM_IS_MALE, true)

                if (isMale) {
                    Util.setPreference(this@MainActivity, Const.PREF_DEFAULT_CATEGORY, "M")
                } else {
                    Util.setPreference(this@MainActivity, Const.PREF_DEFAULT_CATEGORY, "F")
                }
            }
        } else {
            mainViewModel.requestEvent(this@MainActivity)
        }
    }

    private fun setDeepLinkIntent(savedInstanceState: Bundle?) {

        if (savedInstanceState != null) {
            intent.removeExtra(EXTRA_NEXT_ACTIVITY)
            intent.removeExtra(PARAM_NEXT_INTENT)
            return
        }

        val nextActivity = intent.getSerializableData<Class<*>>(EXTRA_NEXT_ACTIVITY)
        val nextIntent = mainViewModel.getNextIntent(intent) ?: return

        // 다음 화면에 관한 정보가 없을면 이동을 시키지 않습니다.
        if (nextActivity == null) {
            Logger.v("AppLink::----------- ${nextActivity.toString()}")
            return
        }

        // 메인화면일경우 탭을 이동시켜줍니다.
        if (nextActivity == MainActivity::class.java) {
            moveMainScreenTabFromLink(nextIntent)
            mainViewModel.setLinkIntent(nextIntent, isNotExistTab = true)
        }

        startActivityForResult(nextIntent, REQUEST_CODE_DEEP_LINK)
    }

    @SuppressLint("MissingSuperCall")
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        moveMainScreenTabFromLink(intent)
        // 랭킹 탭에서 상단탭 이동으로 intent를 뷰모델에 저장해줍니다.
        mainViewModel.setLinkIntent(intent)
    }

    @OptIn(UnstableApi::class)
    private fun moveMainScreenTabFromLink(intent: Intent) {

        val nextActivity = intent.getSerializableData<Class<*>>(EXTRA_NEXT_ACTIVITY)

        if (nextActivity != MainActivity::class.java) {
            return
        }

//        if (intent.getBooleanExtra(EXTRA_IS_FREE_BOARD_REFRESH, false)) {
//            navigateToFragment(MainTab.FREE_BOARD, refresh = true, intent)
//        } else

        if (intent.getBooleanExtra(EXTRA_IS_MENU, false)) {
            navigateToFragment(MainTab.MENU)
        } else if (intent.getBooleanExtra(EXTRA_IS_MY_HEART_INFO, false)) {
            navigateToFragment(MainTab.MY_HEART)
        } else if (intent.getBooleanExtra(EXTRA_IDOL_STATUS_CHANGE, true)) {
            navigateToFragment(MainTab.RANKING)
        }
    }

    @OptIn(UnstableApi::class)
    private fun setupBottomNavigation() {
        val configModel = ConfigModel.getInstance(this)
        val bottomNavigation = findViewById<ComposeView>(R.id.compose_bottom_navigation)

        val tabIndex = when (TutorialManager.getTutorialIndex()) {
            TutorialBits.RANKING -> 0
            TutorialBits.MY_IDOL -> 1
            TutorialBits.PROFILE -> 2
            TutorialBits.MENU -> 4
            else -> -1
        }

        bottomNavigation.setContent {
            val selectedIndex by mainViewModel.selectedBottomNavIndex.collectAsState(
                initial = 0
            )
            setToolbarMenuVisibility(
                isMyInfo = selectedIndex == MainTab.MENU.index,
                showToggleButton = selectedIndex == 0
            )
            MainBottomNavigation(
                menus = listOf(
                    stringResource(id = R.string.hometab_title_rank),
                    stringResource(id = R.string.hometab_title_myidol),
                    stringResource(id = R.string.hometab_title_profile),
                    if (configModel.showLiveStreamingTab) stringResource(id = R.string.menu_live) else stringResource(
                        id = R.string.hometab_title_freeboard
                    ),
                    stringResource(id = R.string.hometab_title_menu),
                ),
                iconsOfSelected = listOf(
                    painterResource(id = R.drawable.btn_bottom_nav_ranking_on),
                    painterResource(id = R.drawable.btn_bottom_nav_favorite_on),
                    painterResource(id = R.drawable.btn_bottom_nav_my_on),
                    if (configModel.showLiveStreamingTab) painterResource(id = R.drawable.icon_20_bottom_play_on) else painterResource(
                        id = R.drawable.btn_bottom_nav_board_on
                    ),
                    painterResource(id = R.drawable.btn_bottom_nav_menu_on),
                ),
                iconsOfUnSelected = listOf(
                    painterResource(id = R.drawable.btn_bottom_nav_ranking_off),
                    painterResource(id = R.drawable.btn_bottom_nav_favorite_off),
                    painterResource(id = R.drawable.btn_bottom_nav_my_off),
                    if (configModel.showLiveStreamingTab) painterResource(id = R.drawable.icon_20_bottom_play_off) else painterResource(
                        id = R.drawable.btn_bottom_nav_board_off
                    ),
                    painterResource(id = R.drawable.btn_bottom_nav_menu_off),
                ),
                initialSelectedIndex = selectedIndex,
                defaultBackgroundColor = colorResource(id = R.color.background_200),
                defaultBorderColor = colorResource(id = R.color.gray150),
                defaultTextColor = colorResource(id = R.color.text_default),
                packagePrefName = Util.PREF_NAME,
                hapticPrefName = Const.PREF_USE_HAPTIC,
                tutorialIndex = TutorialManager.getTutorialIndex(),
                tabIndex = tabIndex,
                navigateToFragment = { index ->
                    // 현재 탭을 다시 탭하면 최상단으로 이동
                    if (currentIndex == index) {
                        (activeFragment as? OnScrollToTopListener)?.onScrollToTop()
                        return@MainBottomNavigation // ga action 중복 등 방지
                    }

                    if (isShowMissionBtn) {
                        binding.ivMission.visibility = if (index == 0) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                    navigateToFragment(MainTab.fromIndex(index))

                    // 메뉴 탭으로 이동하면 투표수/레벨 등 갱신 처리 (원래 아래쪽에 있던 것을 부작용 발생으로 여기로 옮김)
                    if (index == MainTab.MENU.index) {
                        menuFragment?.onResume()
                    }

                    if (TutorialManager.getTutorialIndex() == TutorialBits.MAIN_SEARCH) {
                        val isVisible =
                            if (index == MainTab.RANKING.index) View.VISIBLE else View.GONE
                        binding.mainToolbar.mainToolbarMenu.lottieTutorialSearch.visibility =
                            isVisible
                    }

                    if (TutorialManager.getTutorialIndex() == TutorialBits.MAIN_GENDER) {
                        val isVisible =
                            if (index == MainTab.RANKING.index) View.VISIBLE else View.GONE
                        binding.mainToolbar.lottieMainToggle.visibility = isVisible
                    }

                    if (TutorialManager.getTutorialIndex() == TutorialBits.MAIN_FRIEND) {
                        val isVisible =
                            if (index == MainTab.RANKING.index) View.VISIBLE else View.GONE
                        binding.mainToolbar.mainToolbarMenu.lottieTutorialFriends.visibility =
                            isVisible
                    }

                    if (TutorialManager.getTutorialIndex() == TutorialBits.MENU_SETTINGS) {
                        val isVisible = if (index == MainTab.MENU.index) View.VISIBLE else View.GONE
                        binding.mainToolbar.myinfoToolbarMenu.lottieTutorialSetting.visibility =
                            isVisible
                    }

                    if (TutorialManager.getTutorialIndex() == TutorialBits.MENU_NOTIFICATION) {
                        val isVisible = if (index == MainTab.MENU.index) View.VISIBLE else View.GONE
                        binding.mainToolbar.myinfoToolbarMenu.lottieTutorialNotification.visibility =
                            isVisible
                    }
                },
                tutorialClick = {
                    mainViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                }
            )
        }
    }

    @UnstableApi
    private fun navigateToFragment(tab: MainTab, refresh: Boolean = false, intent: Intent? = null) {
        showAwardButton(tab == MainTab.RANKING && ConfigModel.getInstance(this).showAwardTab)

        binding.mainToolbar.tvDeadline.visibility = View.VISIBLE
        val fragment =
            when (tab) {
                MainTab.RANKING -> {
                    setUiActionFirebaseGoogleAnalyticsActivity(
                        GaAction.BTB_RANK.actionValue,
                        GaAction.BTB_RANK.label
                    )
                    mainRankingFragment
                        ?: supportFragmentManager.findFragmentByTag(MainRankingFragment::class.java.name) as? MainRankingFragment
                        ?: MainRankingFragment().also {
                            mainRankingFragment = it
                        }
                }

                MainTab.FAVORITE -> {
                    setUiActionFirebaseGoogleAnalyticsActivity(
                        GaAction.BTB_MY_IDOL.actionValue, GaAction.BTB_MY_IDOL.label
                    )
                    favoriteIdolFragment
                        ?: supportFragmentManager.findFragmentByTag(FavoritIdolFragment::class.java.name) as? FavoritIdolFragment
                        ?: FavoritIdolFragment().also { favoriteIdolFragment = it }
                }

                MainTab.MY_HEART -> {
                    setUiActionFirebaseGoogleAnalyticsActivity(
                        GaAction.BTB_MY_PAGE.actionValue, GaAction.BTB_MY_PAGE.label
                    )

                    mainViewModel.refreshMyHeartData()
                    myHeartInfoFragment ?: supportFragmentManager.findFragmentByTag(
                        MyheartInfoFragment::class.java.name
                    ) as? MyheartInfoFragment
                    ?: MyheartInfoFragment().also { myHeartInfoFragment = it }
                }

                MainTab.FREE_BOARD_OR_LIVE -> {
                    if (ConfigModel.getInstance(this).showLiveStreamingTab) {
                        liveFragment ?: supportFragmentManager.findFragmentByTag(
                            LiveStreamingListFragment::class.java.name
                        ) as? LiveStreamingListFragment
                        ?: LiveStreamingListFragment().also { liveFragment = it }
                    } else {
                        setUiActionFirebaseGoogleAnalyticsActivity(
                            GaAction.BTB_FREE_BOARD.actionValue, GaAction.BTB_FREE_BOARD.label
                        )
                        // 자게 열려있는 상태에서 왔다면 자게 갱신 처리
                        val tag = intent?.getIntExtra(Const.EXTRA_TAG_ID, -1) ?: -1
                        if (freeboardFragment != null && refresh) {
                            freeboardFragment?.refresh(tag)
                        }

                        freeboardFragment ?: supportFragmentManager.findFragmentByTag(
                            FreeboardFragment::class.java.name
                        ) as? FreeboardFragment
                        ?: FreeboardFragment().also { freeboardFragment = it }
                    }
                }

                MainTab.MENU -> {
                    setUiActionFirebaseGoogleAnalyticsActivity(
                        GaAction.BTB_MENU.actionValue,
                        GaAction.BTB_MENU.label
                    )
                    menuFragment
                        ?: supportFragmentManager.findFragmentByTag(MyInfoFragment::class.java.name) as? MyInfoFragment
                        ?: MyInfoFragment().also { menuFragment = it }
                }

                else -> mainRankingFragment ?: MainRankingFragment().also {
                    mainRankingFragment = it
                }
            }
        mainViewModel.setSelectedIndex(tab.index)
        currentIndex = tab.index
        // background 상태에서 종료될 때 onDestroy가 호출되지 않아서 여기서 저장해줌
        Util.setPreference(
            this,
            Const.MAIN_BOTTOM_TAB_CURRENT_INDEX,
            mainViewModel.selectedBottomNavIndex.value ?: 0
        )
        showFragment(fragment)

        mainViewModel.updateBottomTabIndex(tab.index)
    }

    private fun showFragment(fragment: Fragment) {
        if (activeFragment == fragment) return

        // 하단 버튼들 빠르게 왔다갔다 연타시 Fragment already added 예외 발생
        if (disableTab) {
            return
        }
        disableTab = true
        supportFragmentManager.beginTransaction().apply {
            activeFragment?.let {
                setMaxLifecycle(it, Lifecycle.State.STARTED)
                hide(it)
            }
            if (!fragment.isAdded) {
                add(R.id.fragment_container_view, fragment, fragment.javaClass.name)
            } else {
                show(fragment)
            }

            setMaxLifecycle(fragment, Lifecycle.State.RESUMED)

            runOnCommit {
                // 커밋 완료 후 showFragment 다시 가능하게함
                disableTab = false
            }
            commit()
        }
        activeFragment = fragment
    }

    private fun observedVM() = with(mainViewModel) {
        showWelcomeMission.observe(this@MainActivity, SingleEventObserver { isShow ->
            isShowMissionBtn = isShow ?: false
            binding.ivMission.visibility =
                if (isShowMissionBtn && currentIndex == 0) View.VISIBLE else View.GONE
        })

        lifecycleScope.launch {
            sharedAppState.hasUnreadNotification.collect { hasUnread ->
                val resId =
                    if (hasUnread) R.drawable.btn_navigation_bell_dot else R.drawable.btn_navigation_bell
                binding.mainToolbar.myinfoToolbarMenu.ivNotificationIcon.setImageResource(resId)
            }
        }
    }

    private fun updateActionBar() {
        val actionBar = supportActionBar

        if (actionBar != null) {
            var hasCoupon = false
            try {
                val account = IdolAccount.getAccount(this)
                if (account != null) {
                    hasCoupon = Util.messageParse(
                        applicationContext,
                        account.userModel?.messageInfo,
                        "C",
                    ) > 0
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            if (UtilK.hasUnreadEvent(this) || UtilK.hasUnreadNotice(this) || hasCoupon) {
                actionBar.setHomeAsUpIndicator(R.drawable.btn_navigation_menu_dot)
            } else {
                actionBar.setHomeAsUpIndicator(R.drawable.btn_navigation_menu)
            }
        }
    }

    private fun appVersionCheck() {
        // 앱버전에 따라 업데이트 팝업 띄어줌, BaseActivity에 항상 true값으로 되어있어서 바꿔줘야됨.
        FLAG_CLOSE_DIALOG = false

        val listType = object : TypeToken<List<FamilyAppModel>>() {}.type
        val appModelList: ArrayList<FamilyAppModel>? =
            IdolGson
                .getInstance()
                .fromJson(Util.getPreference(this, Const.PREF_FAMILY_APP_LIST), listType)

        try {
            if (appModelList != null) {
                // 앱버전은 고정이므로 반복문 밖선언
                val appVersion = getString(R.string.app_version)
                val splitAppVersion = appVersion.split("\\.".toRegex()).toTypedArray()

                // 각자리를 3자리씩 패딩.
                val combineAppVersion =
                    String.format(
                        Locale.US,
                        "%03d%03d%03d",
                        splitAppVersion[0].toInt(),
                        splitAppVersion[1].toInt(),
                        splitAppVersion[2].toInt(),
                    )

                for (i in appModelList.indices) {
                    val remoteVersion = appModelList[i].version
                    val splitRemoteVersion =
                        remoteVersion?.split("\\.".toRegex())?.toTypedArray() ?: break

                    val combineRemoteVersion =
                        String.format(
                            Locale.US,
                            "%03d%03d%03d",
                            splitRemoteVersion[0].toInt(),
                            splitRemoteVersion[1].toInt(),
                            splitRemoteVersion[2].toInt(),
                        )

                    if (appModelList[i].appId == AppConst.APP_ID &&
                        appModelList[i].needUpdate == "Y" &&
                        combineRemoteVersion > combineAppVersion
                    ) {
                        Util.log("Version2::$combineRemoteVersion")
                        showAppVersionCheckDialog(appModelList[i])
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showAppVersionCheckDialog(model: FamilyAppModel) {
        val url = model.updateUrl
        Util.showDefaultIdolDialogWithRedBtn2(
            this,
            null,
            getString(R.string.popup_update_app),
            getString(R.string.popup_update_app),
            R.string.update,
            R.string.btn_cancel,
            false,
            true,
            true,
            false,
            {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(this, AppLinkActivity::class.java)
                    intent.data = Uri.parse(url)
                    startActivity(intent)
                }
                Util.closeIdolDialog()
            },
            {
                Util.closeIdolDialog()
            },
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LiveTrailerSlideFragment.REQUEST_PARAM_TO_MAIN) { // 라이브 배너를 라이브 화면 갔다가 온경우.

            // main의 fragment 중에서 livestreamingListfragment를 찾아서 있으면, 해당 fragment의 onActivityResult를 호출하여,
            // 화면 refresh 진행
            for (fragment in supportFragmentManager.fragments) {
                if (fragment is LiveStreamingListFragment) {
                    fragment.onActivityResult(
                        LiveStreamingListFragment.REQUEST_CODE_LIVE_LIST,
                        resultCode,
                        data,
                    )
                    break
                }
            }
        }

        if (requestCode == REQUEST_CODE_DEEP_LINK) {
            // main으로 다시 돌아올 때 mainActivity가 재생성될 경우 deeplink를 타고 왔으면 다시 해당 로직이 실행되므로,
            // 해당 intent extra key EXTRA_NEXT_ACTIVITY를 지워준다.
            intent.removeExtra(EXTRA_NEXT_ACTIVITY)
            intent.removeExtra(PARAM_NEXT_INTENT)
        }

        Util.handleVideoAdResult(
            this,
            false,
            true,
            requestCode,
            resultCode,
            data,
            "vote_videoad",
        ) { adType ->
            videoAdUtil.onVideoSawCommon(this, true, adType, null)
        }
    }

    private fun showGoogleReviewDialog(context: Context) {
        if (reviewDialog != null && reviewDialog!!.isShowing) return

        reviewDialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow =
            WindowManager.LayoutParams().apply {
                flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
                dimAmount = 0.7f
                gravity = Gravity.CENTER
            }
        reviewDialog!!.window!!.attributes = lpWindow

        val dm = context.resources.displayMetrics
        val width = (min(dm.widthPixels, dm.heightPixels) * 0.85f).toInt()
        reviewDialog!!.window!!.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)


        val dialogBinding = DialogReviewBinding.inflate(layoutInflater)
        reviewDialog!!.setContentView(dialogBinding.root)
        reviewDialog!!.setCanceledOnTouchOutside(false)
        reviewDialog!!.setCancelable(false)

        val sp = SpannableString(dialogBinding.msg.text.toString())

        dialogBinding.msg.text = sp
        dialogBinding.btnOk.setOnClickListener {
            Util.setPreference(this, Const.PREF_REQUEST_REVIEW, true)
            reviewDialog!!.dismiss()
            Util.gotoMarket(this, packageName)
            mRequestHeartHandler.sendEmptyMessageDelayed(0, 3000)
        }
        dialogBinding.btnCancel.setOnClickListener {
            Util.setPreference(this, Const.PREF_REQUEST_REVIEW, true)
            reviewDialog!!.dismiss()
        }
        reviewDialog!!.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        reviewDialog!!.show()
    }

    private fun setToolBar() {
        binding.mainToolbar.root.visibility = View.VISIBLE
    }

    private fun setToolbarClickListener() = with(binding.mainToolbar) {
        mainToolbarMenu.ivFriends.setOnClickListener {
            if (mainToolbarMenu.lottieTutorialFriends.isVisible) return@setOnClickListener
            onClickActionBarFriend()
        }

        mainToolbarMenu.ivSearch.setOnClickListener {
            if (binding.mainToolbar.mainToolbarMenu.lottieTutorialSearch.isVisible) return@setOnClickListener
            onClickActionBarSearch()
        }

        // TODO 작업 완료 후 삭제
        myinfoToolbarMenu.ivAttendance.setOnClickListener {
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.MENU_ATTENDANCE_CHECK.actionValue,
                GaAction.MENU_ATTENDANCE_CHECK.label,
            )
            startActivity(AttendanceActivity.createIntent(this@MainActivity))
        }

        myinfoToolbarMenu.ivNotification.setOnClickListener {
            if (myinfoToolbarMenu.lottieTutorialNotification.isVisible) return@setOnClickListener
            onClickNotification()
        }

        myinfoToolbarMenu.ivSetting.setOnClickListener {
            if (myinfoToolbarMenu.lottieTutorialSetting.isVisible) return@setOnClickListener
            onClickActionBarSetting()
        }
    }

    private fun setToolbarMenuVisibility(isMyInfo: Boolean, showToggleButton: Boolean = false) {
        binding.mainToolbar.composeToggle.visibility =
            if (showToggleButton) View.VISIBLE else View.GONE

        if (isMyInfo) {
            binding.mainToolbar.myinfoToolbarMenu.root.visibility = View.VISIBLE
            binding.mainToolbar.mainToolbarMenu.root.visibility = View.GONE

            UtilK.checkNewNotification(this, messagesRepository, sharedAppState)
            return
        }

        binding.mainToolbar.myinfoToolbarMenu.root.visibility = View.GONE
        binding.mainToolbar.mainToolbarMenu.root.visibility = View.VISIBLE
    }

    override fun onPause() {
        super.onPause()
        timeThread.interrupt()

        // udp
        if (ConfigModel.getInstance(this).udp_stage > 0) {
            idolBroadcastManager.stopHeartbeat()
        }
    }

    private fun showEndPopup() {

        val isAggregatingTime =
            Util.getPreferenceBool(this, Const.PREF_IS_AGGREGATING_TIME, false)
        val account = IdolAccount.getAccount(this)

        if (account == null) {
            finish()
            return
        }

        accountManager.fetchUserInfo(
            this,
            {
                if (isFinishing || isDestroyed) {
                    return@fetchUserInfo
                }
                val userModel = account.userModel

                val dailyHeart = userModel?.weakHeart ?: 0L

                val spannableString =
                    Util.getColorText(
                        String.format(
                            getString(R.string.finish_popup_title),
                            dailyHeart.toString(),
                        ),
                        dailyHeart.toString(),
                        ContextCompat.getColor(this@MainActivity, R.color.main),
                    )

                Util.getPreference(this@MainActivity, Const.PREF_END_POPUP)
                val endPopupModels =
                    IdolGson.getInstance().fromJson(
                        Util.getPreference(this@MainActivity, Const.PREF_END_POPUP),
                        EndPopupModel::class.java,
                    )

                val adBannerDialog =
                    AdBannerDialog(
                        spannableString,
                        String.format(
                            getString(R.string.finish_popup_subtitle),
                            getString(R.string.app_name),
                        ),
                        getString(R.string.finish),
                        getString(R.string.btn_cancel),
                        (!isAggregatingTime && dailyHeart != 0L), // 집계시간이거나, 데일리하트가 0개이면 부제목을 보여주지 않습니다.
                        this@MainActivity,
                        android.R.style.Theme_Translucent_NoTitleBar,
                        endPopupModel = endPopupModels,
                        isLocalAd = true,
                        onAdOpened = { isOpened ->
                        }
                    ) {
                        finish()
                    }
                adBannerDialog.setOnCancelListener {
                    finish()
                }
                adBannerDialog.show()
            }, {
                finish()
            }
        )
    }

    override fun onDestroy() {
        super.onDestroy()

        // 채팅 서비스 제거.
        Util.log("idoltalk::MainActivity Destroyed")
        Util.setPreference(
            this,
            Const.MAIN_BOTTOM_TAB_CURRENT_INDEX,
            mainViewModel.selectedBottomNavIndex.value ?: 0
        )
        var isSocketConnected = false
        socketManager?.socket?.let {
            isSocketConnected = it.connected()
        }
        if (isSocketConnected) {
            socketManager?.disconnectSocket()
        }

        // udp
        if (ConfigModel.getInstance(this).udp_stage > 0) {
            idolBroadcastManager.disconnect()
        }

        // 움짤프사
        try {
            player1?.release()
            player2?.release()
            player3?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        IdolApplication.getInstance(this).mainActivity = null

        if (!BuildConfig.ONESTORE) {
            reviewDialog?.dismiss()
        }

        IdolApplication.STRATUP_CALLED = false

        // 앱 종료시 자동갱신 멈춤
        idolApiManager.stopTimer()
    }

    // 언어가 바뀌면 재시작
    override fun onResume() {
        super.onResume()

        // 안읽은 이벤트/공지/쿠폰 처리
        updateActionBar()

        // udp
        if (ConfigModel.getInstance(this).udp_stage > 0) {
            idolBroadcastManager.startHeartbeat()
        }

        val newLocale = Util.getPreference(this, Const.PREF_LANGUAGE)
        if ((locale == null && newLocale.isNotEmpty()) || (locale != null && locale != newLocale)) {
            val intent = intent
            finish()
            startActivity(intent)
        }

        if (timeThread.state == Thread.State.NEW) timeThread.start()

        changeAttendanceIconStatus()
    }

    private fun processAccessToken(
        uri: String?,
        approve: String?,
    ) {
        if (uri == null) {
            return
        }

        val parseUri = uri.toUri()

        val type = parseUri.getQueryParameter("type")
        val clientId = parseUri.getQueryParameter("client_id")
        val scope = parseUri.getQueryParameter("scope")
        val redirectUri = parseUri.getQueryParameter("redirect_uri")

        // 개발자들만 보는것이므로 다국어처리는 필요하지 않음.
        if (redirectUri == null) {
            IdolSnackBar.make(findViewById(android.R.id.content), "no callback url").show()
            return
        }

        lifecycleScope.launch {
            authRepository.requestAccessToken(
                type,
                clientId,
                scope,
                redirectUri,
                approve,
                { response ->
                    val dataObject = response.optJSONObject("data")
                    val gcode = response.optInt("gcode")
                    val success = response.optBoolean("success")

                    if (dataObject == null) {
                        return@requestAccessToken
                    }

                    // 권한을 못받은 사람은 팝업을 뛰워줘서 권한을 요청합니다.
                    if (!success && gcode == Gcode.REQUIRE_USER_ACCESS.value) {
                        val icon = dataObject.optString("icon")
                        val name = dataObject.optString("name")

                        val gson = IdolGson.getInstance()
                        val listType = object : TypeToken<List<AccessModel>>() {}.type
                        val accessList: ArrayList<AccessModel> =
                            gson.fromJson(
                                dataObject.optJSONArray("access_list")?.toString()
                                    ?: return@requestAccessToken,
                                listType,
                            )

                        showAuthRequestDialog(icon, name, accessList, uri)
                        return@requestAccessToken
                    }

                    val responseRedirectUri = response.optString("redirect_uri")

                    val parsedRedirectUri = responseRedirectUri.toUri()

                    val splitRedirectUri = responseRedirectUri.split("\\?".toRegex()).toTypedArray()

                    val builder = Uri.Builder().encodedPath(splitRedirectUri[0])

                    for (key in parsedRedirectUri.queryParameterNames) {
                        builder.appendQueryParameter(key, parsedRedirectUri.getQueryParameter(key))
                    }

                    val accessToken = dataObject.optString("access_token")
                    val expires = dataObject.optInt("expires")
                    val nickName = dataObject.optString("nickname")

                    builder.appendQueryParameter("access_token", accessToken)
                    builder.appendQueryParameter("expires", expires.toString())
                    builder.appendQueryParameter("nickname", nickName)

                    try {
                        val accessIntent =
                            Intent(Intent.ACTION_VIEW, builder.build().toString().toUri())
                        accessIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        accessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(accessIntent)
                    } catch (e: ActivityNotFoundException) {
                        IdolSnackBar
                            .make(
                                findViewById(android.R.id.content),
                                "Invalid redirect_uri parameter: ${builder.build()}",
                            ).show()
                        e.printStackTrace()
                    }
                },
                { throwable ->
                    IdolSnackBar.make(findViewById(android.R.id.content), throwable.message).show()
                }
            )
        }
    }

    private fun showAuthRequestDialog(
        icon: String,
        name: String,
        accessList: List<AccessModel>,
        uri: String,
    ) {
        Util.showRequestAuthDialog(
            this,
            glideRequestManager,
            icon,
            String.format(getString(R.string.popup_request_auth_title), name),
            accessList,
            {
                // 확인을 눌렀으면 다시 한번 토큰 확인 프로세스를 실행시켜준다.
                processAccessToken(uri, "Y")
                Util.closeIdolDialog()
            },
            {
                Util.closeIdolDialog()
            },
        )
    }

    private fun requestMobileAdConsent() {
        // 디버그 테스트 세팅입니다. 테스트 필요할 때 넣어주세요.
        val debugSettings =
            ConsentDebugSettings
                .Builder(this)
                .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA) // 국가 설정(영국)
                .addTestDeviceHashedId("527E2B0848C2589FE4AE03F7D666A9FD") // 기기 설정
                .build()

        val params =
            ConsentRequestParameters
                .Builder()
//        .setConsentDebugSettings(debugSettings)
                .setTagForUnderAgeOfConsent(false)
                .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this,
                ) { loadAndShowError ->
                    if (loadAndShowError != null && BuildConfig.DEBUG) {
                        // Consent gathering failed.
                        val errorCode = loadAndShowError.errorCode
                        val message = loadAndShowError.message
                        IdolSnackBar
                            .make(
                                findViewById(android.R.id.content),
                                "ErrorCode:$errorCode Message:$message",
                            ).show()
                    }

                    // Consent has been gathered.
                    if (consentInformation.canRequestAds()) {
                        initializeMobileAdsSdk()
                    }
                }
            },
            { requestConsentError ->
                // Consent gathering failed.
                if (BuildConfig.DEBUG) {
                    val errorCode = requestConsentError.errorCode
                    val message = requestConsentError.message
                    IdolSnackBar
                        .make(
                            findViewById(android.R.id.content),
                            "ErrorCode:$errorCode Message:$message",
                        ).show()
                }
            },
        )

        if (consentInformation.canRequestAds()) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        try {
            MobileAds.initialize(this) { _ ->
                // The RewardVideoManager is a singleton and will be initialized when needed.
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun changeAttendanceIconStatus() {
        val isAbleAttendance = Util.getPreferenceBool(this, Const.PREF_IS_ABLE_ATTENDANCE, false)

        val iconAttendance = ContextCompat.getDrawable(this, R.drawable.btn_navigation_attendance)
        val iconAttendanceDot =
            ContextCompat.getDrawable(this, R.drawable.btn_navigation_attendance_dot)

        binding.mainToolbar.myinfoToolbarMenu.ivInnerAttendance.setImageDrawable(if (isAbleAttendance) iconAttendanceDot else iconAttendance)
    }

    private fun setMainTutorial() {
        when (TutorialManager.getTutorialIndex()) {
            TutorialBits.MAIN_GENDER -> {
                setupLottieTutorial(binding.mainToolbar.lottieMainToggle) {
                    mainViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                    triggerClick.value = true
                }
            }

            TutorialBits.MAIN_SEARCH -> {
                setupLottieTutorial(binding.mainToolbar.mainToolbarMenu.lottieTutorialSearch) {
                    mainViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                    onClickActionBarSearch()
                }
            }

            TutorialBits.MAIN_FRIEND -> {
                setupLottieTutorial(binding.mainToolbar.mainToolbarMenu.lottieTutorialFriends) {
                    mainViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                    onClickActionBarFriend()
                }
            }

            TutorialBits.MENU_NOTIFICATION -> {
                setupLottieTutorial(binding.mainToolbar.myinfoToolbarMenu.lottieTutorialNotification) {
                    mainViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                    onClickNotification()
                }
            }

            TutorialBits.MENU_SETTINGS -> {
                setupLottieTutorial(binding.mainToolbar.myinfoToolbarMenu.lottieTutorialSetting) {
                    mainViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                    onClickActionBarSetting()
                }
            }
        }
    }

    private fun setSwitchToggleButton() {
        val currentLocale = Util.getSystemLanguage(this)
        val genderStrings = when (currentLocale) {
            "ko_KR" -> {
                getGenderString(Locale.KOREA)
            }

            "ja_JP" -> {
                getGenderString(Locale.JAPAN)
            }

            "zh_CN" -> {
                getGenderString(Locale("zh", "CN"))
            }

            "zh_TW" -> {
                getGenderString(Locale("zh", "TW"))
            }

            else -> {
                getGenderString(Locale.ENGLISH, isExceptCondition = true)
            }
        }

        val gender = Util.getPreference(this@MainActivity, Const.PREF_DEFAULT_CATEGORY)
        val maleIndex = genderStrings.indexOfFirst { it.second == Const.TYPE_MALE }
        val isInitMaleSelected =
            if (maleIndex == 0) gender == Const.TYPE_MALE else gender != Const.TYPE_MALE

        binding.mainToolbar.composeToggle.setContent {
            SwitchToggleButton(
                genderList = genderStrings,
                initialIsMaleSelected = isInitMaleSelected,
                boxBackgroundColor = colorResource(id = R.color.gray100),
                boxTextColor = colorResource(id = R.color.text_gray),
                thumbBackgroundColor = colorResource(id = R.color.text_default),
                thumbTextColor = colorResource(id = R.color.text_white_black),
                triggerClick = triggerClick,
                packagePrefName = Util.PREF_NAME,
                hapticPrefName = Const.PREF_USE_HAPTIC,
            ) { category ->
                if (binding.mainToolbar.lottieMainToggle.isVisible) return@SwitchToggleButton
                onClickGenderToggle(category)
            }
        }
    }

    private fun onClickGenderToggle(category: String) {
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.TOP_CHANGE_GENDER.actionValue,
            GaAction.TOP_CHANGE_GENDER.label,
        )

        Util.setPreference(this@MainActivity, Const.PREF_DEFAULT_CATEGORY, category)
        val intent = Intent(Const.REFRESH)
        LocalBroadcastManager.getInstance(this@MainActivity).sendBroadcast(intent)
        mainViewModel.setIsMaleGender(category == Const.TYPE_MALE)
        mainViewModel.setChangeHallOfGender(true)
    }

    private fun onClickActionBarSearch() {
        if (Util.mayShowLoginPopup(this@MainActivity)) return
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.SEARCH.actionValue,
            GaAction.SEARCH.label
        )
        startActivity(SearchHistoryActivity.createIntent(this@MainActivity))
    }

    private fun onClickActionBarFriend() {
        if (Util.mayShowLoginPopup(this@MainActivity)) return

        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.TOP_FRIEND.actionValue,
            GaAction.TOP_FRIEND.label,
        )
        when {
            "Y".equals(
                ConfigModel.getInstance(this@MainActivity).friendApiBlock,
                ignoreCase = true
            ) -> {
                Util.showDefaultIdolDialogWithBtn1(
                    this@MainActivity,
                    null,
                    getString(R.string.friend_api_block),
                ) { Util.closeIdolDialog() }
            }

            "L".equals(
                ConfigModel.getInstance(this@MainActivity).friendApiBlock,
                ignoreCase = true
            ) -> {
                Util.showDefaultIdolDialogWithBtn1(
                    this@MainActivity,
                    null,
                    getString(R.string.friend_api_limit),
                ) { Util.closeIdolDialog() }
            }

            else -> startActivity(FriendsActivity.createIntent(this@MainActivity))
        }
    }

    private fun onClickNotification() {
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.MENU_PUSH_LOG.actionValue,
            GaAction.MENU_PUSH_LOG.label,
        )

        startActivity(NotificationActivity.createIntent(this@MainActivity))
    }

    private fun onClickActionBarSetting() {
        setUiActionFirebaseGoogleAnalyticsActivity(
            GaAction.MENU_SETTING.actionValue,
            GaAction.MENU_SETTING.label,
        )

        startActivity(SettingActivity.createIntent(this@MainActivity))
    }

    private fun getGenderString(
        locale: Locale,
        isExceptCondition: Boolean = false,
    ): List<Pair<String, String>> {
        if (isExceptCondition) {// 나머지 언어일땐 무조건 M,F로 고정 해달라함.
            return listOf(
                Const.TYPE_MALE to Const.TYPE_MALE,
                Const.TYPE_FEMALE to Const.TYPE_FEMALE
            )
        }

        return listOf(
            UtilK.getLocaleStringResource(locale, R.string.male, this) to Const.TYPE_MALE,
            UtilK.getLocaleStringResource(locale, R.string.female, this) to Const.TYPE_FEMALE,
        )
    }

    fun setMissionButton(marginBottom: Int) {
        val layoutParams = binding.ivMission.layoutParams as ViewGroup.MarginLayoutParams
        layoutParams.bottomMargin = this.dpToPx(marginBottom.toFloat())

        binding.ivMission.layoutParams = layoutParams
        binding.ivMission.post {
            binding.ivMission.visibility = if (isShowMissionBtn && currentIndex == 0) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun onClose(isAllClear: Boolean) {
        isMissionPage = false

        if (isAllClear) {
            isShowMissionBtn = false
            binding.ivMission.visibility = View.GONE
        }
    }

    fun showAwardButton(show: Boolean) {
        if (show) {
            binding.ivAwards.visibility = View.VISIBLE
            // 애니메이션
            animateAwardButton()
        } else {
            binding.ivAwards.visibility = View.GONE
            awardAnimatorSet?.cancel()
            awardAnimatorSet?.removeAllListeners()
        }
    }

    private fun animateAwardButton() {
        awardAnimatorSet?.cancel()
        awardAnimatorSet?.removeAllListeners()
        awardAnimatorSet?.childAnimations?.forEach {
            it.cancel()
        }

        val awardAnimator = ObjectAnimator.ofFloat(binding.ivAwards, "rotationY", 0f, 180f).apply {
            duration = 300
        }
        val awardAnimator2 = ObjectAnimator.ofFloat(binding.ivAwards, "rotationY", 180f, 0f).apply {
            duration = 300
        }
        awardAnimatorSet = AnimatorSet().apply {
            playSequentially(awardAnimator, awardAnimator2)
        }
        awardAnimatorSet?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                binding.ivAwards.postDelayed({
                    awardAnimatorSet?.start()
                }, 2000)
            }
        })
        awardAnimatorSet?.start()
    }

    companion object {
        const val OTHER_APP_SHARE = "other_app_share"
        const val IS_DEEP_LINK_CLICK_FROM_IDOL = "is_deep_link_click_from_idol"

        // 전면 배너.
        private const val BANNER_LIST_SAVE = "bannerList_save"

        // 딥링크.
        private const val REQUEST_CODE_DEEP_LINK = 1001

        // 상단 탭 사이즈 saveinstance에 저장.
        private const val MAIN_CATEGORY_SIZE = "main_category_size"

        @JvmStatic
        fun createIntent(
            context: Context,
            share: Boolean,
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(OTHER_APP_SHARE, share)
            }

        // 딥링크로 실행될 때 task를 새롭게 실행하고 기존 task를 지운다.
        // /clickFromIdolScreen -> 앱 안에서 링크가 클릭되었는지 여부 체크
        @JvmStatic
        fun createIntentFromDeepLink(
            context: Context,
            share: Boolean,
            clickFromIdolScreen: Boolean,
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                if (clickFromIdolScreen) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                } else {
                    // 외부 링크 클릭 시에는 태스크를 clear 한다.
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                putExtra(OTHER_APP_SHARE, share)
                putExtra(IS_DEEP_LINK_CLICK_FROM_IDOL, clickFromIdolScreen)
            }

        @JvmStatic
        fun createIntent(
            context: Context,
            isSolo: Boolean,
            isMale: Boolean,
        ): Intent =
            Intent(context, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                putExtra(PARAM_IS_SOLO, isSolo)
                putExtra(PARAM_IS_MALE, isMale)
            }
    }
}