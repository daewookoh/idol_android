package net.ib.mn.activity

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.Parcelable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.view.Gravity
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.airbnb.lottie.LottieAnimationView
import com.applovin.mediation.ads.MaxRewardedAd
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkConfiguration
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.FormError
import com.google.android.ump.UserMessagingPlatform
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import jp.maio.sdk.android.MaioAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.attendance.AttendanceActivity
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.repository.AuthRepository
import net.ib.mn.core.data.repository.EmoticonRepository
import net.ib.mn.core.data.repository.MessagesRepositoryImpl
import net.ib.mn.core.data.repository.MiscRepository
import net.ib.mn.core.data.repository.SupportRepositoryImpl
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.domain.usecase.GetEventUseCase
import net.ib.mn.core.model.EndPopupModel
import net.ib.mn.core.model.FamilyAppModel
import net.ib.mn.core.model.NewPicksModel
import net.ib.mn.data_resource.DataResource
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.databinding.ActivityMainBinding
import net.ib.mn.dialog.AdBannerDialog
import net.ib.mn.domain.usecase.DeleteAllIdolUseCase
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.domain.usecase.datastore.InitAdDataPrefsUseCase
import net.ib.mn.emoticon.ZipManager
import net.ib.mn.feature.friend.FriendsActivity
import net.ib.mn.feature.search.history.SearchHistoryActivity
import net.ib.mn.fragment.CelebWelcomeMissionDialog
import net.ib.mn.fragment.FavoritIdolFragment
import net.ib.mn.fragment.FreeboardFragment
import net.ib.mn.fragment.HallOfFameFragment
import net.ib.mn.fragment.MiracleMainFragment
import net.ib.mn.fragment.MyInfoFragment
import net.ib.mn.fragment.NewRankingFragment2
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.fragment.SignupFragment
import net.ib.mn.fragment.SummaryMainFragment
import net.ib.mn.gcm.GcmUtils
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.liveStreaming.LiveStreamingListFragment
import net.ib.mn.liveStreaming.LiveTrailerSlideFragment
import net.ib.mn.model.AccessModel
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.EmoticonDetailModel
import net.ib.mn.model.EmoticonsetModel
import net.ib.mn.model.EventHeartModel
import net.ib.mn.model.FrontBannerModel
import net.ib.mn.model.StoreItemModel
import net.ib.mn.model.SupportListModel
import net.ib.mn.model.UserModel
import net.ib.mn.model.toPresentation
import net.ib.mn.onepick.OnePickMainFragment
import net.ib.mn.remote.IdolBroadcastManager
import net.ib.mn.support.SupportDetailActivity
import net.ib.mn.support.SupportInfoActivity
import net.ib.mn.support.SupportPhotoCertifyActivity
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.ApiCacheManager
import net.ib.mn.utils.AppConst
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.ExtendedDataHolder
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Gcode
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.Logger
import net.ib.mn.utils.SharedAppState
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VMDetector
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.getKSTMidnightEpochTime
import net.ib.mn.utils.setFirebaseUIAction
import net.ib.mn.viewmodel.MainRankingViewModel
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.ref.WeakReference
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale
import java.util.Objects
import java.util.TimeZone
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Consumer
import javax.inject.Inject
import kotlin.math.min

@SuppressLint("CommitTransaction")
@AndroidEntryPoint
class MainActivity : BaseActivity(), View.OnClickListener, NewRankingFragment2.FragmentListener,
    CelebWelcomeMissionDialog.WelcomeMissionDialogListener, HasFreeboard {
    enum class MainTab(val index: Int) {
        MAIN(0),
        MIRACLE(1),
        ONEPICK(2),
        HOF(3),
        FAVORITES(4);
    }

    private var binding: ActivityMainBinding? = null

    private val mainRankingViewModel: MainRankingViewModel by viewModels()

    override var freeboardFragment: FreeboardFragment? = null // 빌드오류 방지용 더미값

    @Inject
    lateinit var idolApiManager: IdolApiManager

    @Inject
    lateinit var idolBroadcastManager: IdolBroadcastManager

    @Inject
    lateinit var deleteAllIdolUseCase: DeleteAllIdolUseCase

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase

    @Inject
    lateinit var initAdDataPrefsUseCase: InitAdDataPrefsUseCase

    @JvmField
    @Inject
    var supportRepository: SupportRepositoryImpl? = null
    @Inject
    lateinit var messagesRepository: MessagesRepositoryImpl
    @Inject
    lateinit var miscRepository: MiscRepository
    @Inject
    lateinit var emoticonRepository: EmoticonRepository
    @Inject
    lateinit var authRepository: AuthRepository
    @Inject
    lateinit var articlesRepository: ArticlesRepository

    @JvmField
    @Inject
    var videoAdUtil: VideoAdUtil? = null

    @Inject
    lateinit var sharedAppState: SharedAppState
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var getEventUseCase: GetEventUseCase
    @Inject
    lateinit var accountManager: IdolAccountManager

    //뷰 관리
    private var btnTabResourceId = 0

    // 프레그먼트 관리
    private var summaryFrag: SummaryMainFragment? = null
    private var miracleMainFrag: MiracleMainFragment? = null
    private var onePickFrag: OnePickMainFragment? = null
    private var hallFrag: HallOfFameFragment? = null
    private var favoritFrag: FavoritIdolFragment? = null

    private var mGlideRequestManager: RequestManager? = null
    private val BANNER_LIST_SAVE = "bannerList_save"
    private var isShowMissionBtn = false

    private var hasNewHeartPick = false
    private var hasNewThemePick = false

    private var isMission = false

    // drawer menu
    private var myInfo: MyInfoFragment? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var drawerLayout: DrawerLayout? = null

    // toolbar
    var toolbar: Toolbar? = null
        private set
    private var menu: Menu? = null

    // 검색
    private var mSearchInput: EditText? = null
    private var mSearchBtn: ImageButton? = null
    private var mSearchClose: TextView? = null
    private var mKeyword: String? = null

    //전면 배너 받는 리스트
    private var fronbannerlist: ArrayList<FrontBannerModel>? = null

    //서포트 status(0:진행중, 1:성공, 2:실패)을 가져오기위한 모델
    private var supportModel: SupportListModel? = null

    private var consentInformation: ConsentInformation? = null
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    private val timeThread = Thread {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                try {
                    timeHandler.sendEmptyMessage(0)
                } catch (e: OutOfMemoryError) {
                    e.printStackTrace()
                }
            }
        }, 0, 1000)
    }

    private val timeHandler: Handler = TimeHandler(this)

    private fun handleMessage() {
        if (begin != null && end != null) {
            val now = (Date().time + 32400000) % 86400000
            val beginTime = (begin!!.time + 32400000) % 86400000
            val endTime = (end!!.time + 32400000) % 86400000
            val strTime: String?
            if (now in beginTime..endTime) {
                strTime = aggregatingTime
                Util.setPreference(this, Const.PREF_IS_AGGREGATING_TIME, true)
            } else {
                val time = if (endTime < now) beginTime + 86400000 else beginTime
                val deadline = time - now
                if (deadline <= 60000) {
                    strTime = String.format(aggregatingTimeFormatOne!!, 1)
                } else if (deadline <= 600000) {
                    strTime = String.format(aggregatingTimeFormatFew!!, deadline / 60000 + 1)
                } else {
                    val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                    strTime = sdf.format(deadline)
                }
                Util.setPreference(this, Const.PREF_IS_AGGREGATING_TIME, false)
            }

            mDeadline!!.text = strTime
        }
    }

    override fun onClose(isAllClear: Boolean) {
        tabIndex = MainTab.MAIN
        binding?.ivSummary?.setImageResource(R.drawable.btn_bottom_nav_ranking_off_celeb)

        if (isAllClear) {
            isShowMissionBtn = false
            summaryFrag?.showWelcomeMissionButton(false)
        } else {
            summaryFrag?.showWelcomeMissionButton(true)
        }
    }

    private class TimeHandler(activity: MainActivity) : Handler(Looper.getMainLooper()) {
        private val mActivity = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val activity = mActivity.get()
            activity?.handleMessage()
        }
    }


    // 구글 리뷰 팝업 이쪽으로 옮김
    private var reviewDialog: Dialog? = null
    private var mRequestHeartHandler: Handler? = null

    private var mBottomSheetFragment: RewardBottomSheetDialogFragment? = null

    private var eventHeartModel: EventHeartModel? = null

    // 출석/데일리팩/덕질타임/VIP 하트
    private var banners: JSONArray? = null

    private var locale: String? = null

    private var tapCount = 0


    private val MAIN_TAB = 0
    private val MIRACLE_TAB = 1
    private val HALL_TAB = 2
    private val AWARD_TAB = 3

    //
    private var tabIndex = MainTab.MAIN
    private var onGcmRegistered: SignupFragment.OnRegistered = SignupFragment.OnRegistered { id: String? ->
        // preference에 있는 값과 비교하여 변경이 있을 때에만 발송
        Util.log("FCM KEY CHANGED.")
        Util.setPreference(this@MainActivity, Const.PREF_GCM_PUSH_KEY, id)

        val deviceId = Util.getDeviceUUID(this@MainActivity)
        lifecycleScope.launch {
            usersRepository.updatePushKey(
                id,
                deviceId,
                {},
                {}
            )
        }
    }

    override fun onMoreClick() {
        summaryFrag?.moreRankingView()
    }

    override fun onPrevClick() {
        Logger.v("PAGER::onPrev")
        summaryFrag?.moreSummaryView()
    }

    override fun onPrevPageClick() {
        Logger.v("PAGER::onPrev page Click")
        summaryFrag?.onPrevClick()
    }

    override fun onNextPageClick() {
        Logger.v("PAGER::onNext page click")
        summaryFrag?.onNextClick()
    }

    override fun setRankingViewAnimationIsOn(flag: Boolean) {
        summaryFrag?.setRankingViewAnimationIsOn(flag)
    }

    private val mMessageReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Get extra data included in the Intent
            val msg = intent.getIntExtra(Const.EXTRA_DRAWER_MESSAGE, 0)
            if (msg == Const.MESSAGE_CLOSE_DRAWER) drawerLayout?.closeDrawers()
        }
    }

    private fun onBackPressedCallback() {
        if (isMission) {
            isMission = false
            supportFragmentManager.popBackStack()
            return
        }

        if (drawerLayout?.isDrawerOpen(GravityCompat.START) == true) {
            drawerLayout?.closeDrawers()
            return
        }
        if (summaryFrag != null && tabIndex == MainTab.MAIN) {
            if (summaryFrag!!.getRankingViewIsOn()) {
                summaryFrag!!.closeRankingView()
            } else {
                val isAggregatingTime =
                    Util.getPreferenceBool(this, Const.PREF_IS_AGGREGATING_TIME, false)
                val account: IdolAccount? = IdolAccount.getAccount(this@MainActivity)

                if (account == null) {
                    finish()
                    return
                }

                accountManager.fetchUserInfo(this, {
                    val userModel: UserModel? = account.userModel
                    val dailyHeart: Long = userModel?.weakHeart ?: 0

                    val spannableString: SpannableString = Util.getColorText(
                        String.format(
                            getString(R.string.finish_popup_title),
                            dailyHeart.toString()
                        ),
                        dailyHeart.toString(),
                        ContextCompat.getColor(this@MainActivity, R.color.main)
                    )

                    Util.getPreference(this@MainActivity, Const.PREF_END_POPUP)
                    val endPopupModels: EndPopupModel =
                        IdolGson.instance.fromJson(
                            Util.getPreference(
                                this@MainActivity, Const.PREF_END_POPUP
                            ), EndPopupModel::class.java
                        )

                    val adBannerDialog = AdBannerDialog(
                        spannableString,
                        String.format(
                            getString(R.string.finish_popup_subtitle),
                            getString(R.string.actor_app_name)
                        ),
                        getString(R.string.finish),
                        getString(R.string.btn_cancel),
                        (!isAggregatingTime && dailyHeart != 0L),  // 집계시간이거나, 데일리하트가 0개이면 부제목을 보여주지 않습니다.
                        this@MainActivity,
                        android.R.style.Theme_Translucent_NoTitleBar,
                        endPopupModels,
                        true,
                        { _: Boolean? ->  },
                        {
                            finish()
                        }
                    )
                    adBannerDialog.setOnCancelListener {
                        finish()
                    }
                    adBannerDialog.show()
                }, {
                    finish()
                }
                )
            }
        } else if (tabIndex != MainTab.MAIN) binding?.btnSummary?.callOnClick()
        else finish()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Logger.v("onsaveInstance 값" + savedInstanceState.getSerializable(BANNER_LIST_SAVE))

        //main으로 다시 돌아올떄  mainActivity가 재생성될경우  deeplink를 타고 왔으면  다시 해당 로직이 실행되므로,
        // 해당  intent extra key EXTRA_NEXT_ACTIVITY를 지워준다.
        intent.removeExtra(EXTRA_NEXT_ACTIVITY)
        intent.removeExtra(PARAM_NEXT_INTENT)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // E2E
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        // 밝은 배경이면 true, 어두운 배경이면 false
        WindowInsetsControllerCompat(window, window.decorView).apply {
            val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
            isAppearanceLightStatusBars = !isNightMode
            isAppearanceLightNavigationBars = !isNightMode
        }

        // 불필요한 상태 저장 방지 : 백그라운드 방치상태에서 복구될 때 RankingPageFragment들이 다시 살아난 후
        // 새로 RankingPageFragment들을 생성하고 있어서 누적되면서 뻗는 문제 처리
        val fragmentManager = supportFragmentManager
        fragmentManager.fragments.forEach(Consumer { fragment: Fragment? ->
            fragmentManager.beginTransaction().remove(
                fragment!!
            ).commitNowAllowingStateLoss()
        })

        // Initialize the Audience Network SDK
//        AudienceNetworkAds.initialize(this);
        mGlideRequestManager = Glide.with(this)

        observeState()

        // maio
        if (Const.USE_MAIO) {
            MaioAds.init(this, Const.MAIO_MEDIA_ID, null)
        }

        // admob preload
        //VideoAdManager.getInstance(this, null).requestAd();
        if (intent.getBooleanExtra(OTHER_APP_SHARE, false)) {
            Util.showDefaultIdolDialogWithBtn2(
                this,
                getString(R.string.empty_most),
                getString(R.string.lockscreen_not_available),
                { v: View? ->
                    Util.showProgress(this@MainActivity)
                    Util.closeIdolDialog()
                    startActivity(
                        Intent(this@MainActivity, FavoriteSettingActivity::class.java)
                    )
                    finish()
                },
                { Util.closeIdolDialog() })
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        // E2E Inset을 각 컴포넌트에 개별적으로 적용하여 충돌을 방지합니다.
        ViewCompat.setOnApplyWindowInsetsListener(binding!!.toolbar) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(0, topInset, 0, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding!!.tvDeadline) { view, insets ->
            val topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
            view.setPadding(0, topInset, 0, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding!!.tabContainer) { view, insets ->
            val bottomInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            val params = view.layoutParams as ViewGroup.MarginLayoutParams
            params.bottomMargin = bottomInset
            view.layoutParams = params
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding!!.llMenu) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }

        drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)

        summaryFrag = SummaryMainFragment()

        val transaction = supportFragmentManager.beginTransaction()
        transaction.add(R.id.fragment_container, summaryFrag!!, TAG_SUMMARY).commit()

        binding?.btnSummary?.callOnClick()

        if (intent.getBooleanExtra(OTHER_APP_SHARE, false)) {
            Util.showDefaultIdolDialogWithBtn2(
                this,
                getString(R.string.actor_empty_most),
                getString(R.string.actor_lockscreen_not_available),
                { v: View? ->
                    Util.showProgress(this@MainActivity)
                    Util.closeIdolDialog()
                    startActivity(
                        Intent(this@MainActivity, FavoriteSettingActivity::class.java)
                    )
                    finish()
                },
                { Util.closeIdolDialog() })
        }

        IdolApplication.getInstance(this).mainActivity = this

        // 앱 다시 시작하면 커뮤니티별 하트박스 다시 보이게
        IdolApplication.getInstance(this).mapHeartboxViewable.clear()

        UtilK.setTapJoy(this) { _: Boolean? -> }

        locale = Util.getPreference(this, Const.PREF_LANGUAGE)

        // appLovin max
        if (Const.USE_APPLOVIN && locale != null && (locale!!.startsWith("ja")) && !BuildConfig.CHINA) {
            AppLovinSdk.getInstance(this).setMediationProvider("max")
            AppLovinSdk.initializeSdk(
                this
            ) { configuration: AppLovinSdkConfiguration? ->
                // AppLovin SDK is initialized, start loading ads
                // preload ads
                val max: MaxRewardedAd =
                    MaxRewardedAd.getInstance(Const.APPLOVIN_MAX_UNIT_ID, this)
                max.setListener(null)
                max.loadAd()
            }
        }

        //MobileAd 동의 요청.
        if (!BuildConfig.CHINA) {
            requestMobileAdConsent()
        }

        // 하트박스 안나온다는 사람이 많아서 옮겨봄...
        Util.setPreference(this, Const.PREF_HEART_BOX_VIEWABLE, true) //  앱을 실행할 때 true로 설정

        val listType = object : TypeToken<NewPicksModel?>() {}.type
        val newPicks: NewPicksModel = IdolGson.instance.fromJson(
            Util.getPreference(
                this, Const.PREF_NEW_PICKS
            ), listType
        )

        hasNewHeartPick = newPicks.heartpick
        hasNewThemePick = newPicks.themepick
        if (java.lang.Boolean.TRUE == hasNewHeartPick || java.lang.Boolean.TRUE == hasNewThemePick) {
            binding?.ivNewOnePick?.setVisibility(View.VISIBLE)
        } else {
            binding?.ivNewOnePick?.setVisibility(View.GONE)
        }

        // Drawer menu
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mMessageReceiver,
            IntentFilter(Const.DRAWER_EVENT)
        )

        toolbar = findViewById(R.id.toolbar)
        mSearchInput = findViewById<EditText>(R.id.search_input)
        mSearchBtn = findViewById<ImageButton>(R.id.search_btn)
        mSearchClose = findViewById(R.id.search_close)
        mSearchClose?.setText(R.string.btn_cancel) //앱 처음 시작시 locale에 따른 text적용이 안되서, 이렇게  settext를 한번더 해줌.
        mDeadline = findViewById(R.id.tv_deadline)

        initAdData()

        mSearchInput?.setOnClickListener({ v: View? -> showSoftKeyboard() })
        mSearchInput?.setOnEditorActionListener(TextView.OnEditorActionListener { textView: TextView?, i: Int, keyEvent: KeyEvent? ->
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                searchKeyword()
            } else { // 기본 엔터키 동작
                return@OnEditorActionListener false
            }
            true
        })
        mSearchBtn?.setOnClickListener { searchKeyword() }
        mSearchClose?.setOnClickListener { searchClose() }

        toolbar?.setBackgroundResource(R.color.background)
        setSupportActionBar(toolbar)
        val padding = Util.convertDpToPixel(this, 7f).toInt()
        if (Util.isRTL(this)) {
            toolbar?.setPadding(padding, 0, 0, 0)
        } else {
            toolbar?.setPadding(0, 0, padding, 0)
        }

        val actionBar = supportActionBar

        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false)
            actionBar.setDisplayHomeAsUpEnabled(true)
            actionBar.setHomeButtonEnabled(true)
        }

        // drawer 메뉴 toolbar
        setDrawerToolbar()

        drawerLayout = findViewById(R.id.drawer_layout)
        mDrawerToggle = object : ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.drawer_open,
            R.string.drawer_close
        ) {
            private var isOpened = false

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)

                drawerView.bringToFront()
            }

            /** Called when a drawer has settled in a completely closed state.  */
            override fun onDrawerClosed(view: View) {
                super.onDrawerClosed(view)
                isOpened = false
                //                myInfo.isFragmentActive = false; // 인앱 배너 트래킹 때문에 추가
                updateActionBar()
            }

            /** Called when a drawer has settled in a completely open state.  */
            override fun onDrawerOpened(drawerView: View) {
                super.onDrawerOpened(drawerView)
                hideSoftKeyboard()
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "top_menu"
                )
                isOpened = true
                searchClose()
                myInfo?.onResume()
            }
        }

        mDrawerToggle?.toolbarNavigationClickListener = View.OnClickListener {
            drawerLayout?.openDrawer(GravityCompat.START)
        }
        // Set the drawer toggle as the DrawerListener
        drawerLayout?.setDrawerListener(mDrawerToggle)
        mDrawerToggle?.syncState() // 이걸 해줘야 햄버거 모양으로 나옴

        setMyInfoFragment()

        //
        val bundle = intent.extras
        val push: Boolean
        if (bundle != null) {
            push = bundle.getBoolean("push", false)

            val account: IdolAccount? = IdolAccount.getAccount(this)
            if (push && account != null) {
                accountManager.fetchUserInfo(this, {
                    requestEvent(savedInstanceState)
                })
            } else {
                requestEvent(savedInstanceState)
            }
        } else {
            requestEvent(savedInstanceState)
        }

        // 앱 설치 후 24시간 후에 구글리뷰 팝업
        if (!BuildConfig.ONESTORE) {
            mRequestHeartHandler = object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    super.handleMessage(msg)
                    lifecycleScope.launch {
                        usersRepository.provideHeart(
                            "review",
                            {
                            }, {
                            }
                        )
                    }
                }
            }

            val appInstall = Util.getPreferenceLong(this, Const.PREF_APP_INSTALL, 0)
            if (appInstall == 0L) {
                Util.setPreference(this, Const.PREF_APP_INSTALL, Date().time)
            } else {
                val timeDiff = Date().time - appInstall
                if (timeDiff > 24 * 60 * 60 * 1000) {
                    val review_already_request =
                        Util.getPreferenceBool(this, Const.PREF_REQUEST_REVIEW, false)
                    if (!review_already_request && !BuildConfig.CHINA) {
                        showGoolgeReviewDialog(this@MainActivity)
                    }
                }
            }
        }

        // udp
        if (ConfigModel.getInstance(this).udp_stage > 1) {
            idolBroadcastManager.setupConnection(
                applicationContext, ConfigModel.getInstance(
                    this
                ).udp_broadcast_url
            )
        }

        aggregatingTime = resources.getString(R.string.aggregating_time)
        aggregatingTimeFormatOne = resources.getString(R.string.deadline_format_one)
        aggregatingTimeFormatFew = resources.getString(R.string.deadline_format_few)
        begin = ConfigModel.getInstance(this).inactiveBegin
        end = ConfigModel.getInstance(this).inactiveEnd
        mDeadline?.visibility = View.VISIBLE


        //앱버전에따라 업데이트팝업 띄어줌 , BaseActivity에 항상 true값으로 되어있어서 바꿔줘야됨.
        FLAG_CLOSE_DIALOG = false
        appVersionCheck()

        //이모티콘 압축해제 시작.
        emoticonProcess

        if (bundle != null) {
            val isAuthRequest = bundle.getBoolean("is_auth_request", false)
            val uri = bundle.getString("uri")

            if (isAuthRequest) {
                processAccessToken(uri, null)
            }
        }

        setListener()

        // 백버튼 처리 (onBackPressed deprecated)
        onBackPressedDispatcher.addCallback { onBackPressedCallback() }

        // 푸시 설정 유도 초기화
        sharedAppState.setIncitePushHidden(false)

        UtilK.checkNewNotification(this, messagesRepository, sharedAppState)

        setTutorial()
    }

    private fun initAdData() = lifecycleScope.launch {
        val todayEpochTime = getKSTMidnightEpochTime()
        initAdDataPrefsUseCase(todayEpochTime).collectLatest { result ->
            when (result) {
                is DataResource.Success -> {
                    // no-op
                }
                is DataResource.Error -> {
                    // no-op
                }
                is DataResource.Loading -> {
                    // no-op
                }
            }
        }
    }


    private fun setTutorial() {
        when(TutorialManager.getTutorialIndex()) {
            CelebTutorialBits.MAIN_MENU_1 -> {
                var statusBarHeight = 0
                val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
                if (resourceId > 0) {
                    statusBarHeight = resources.getDimensionPixelSize(resourceId)
                }
                binding!!.lottieTutorialMenu.y += statusBarHeight

                setupLottieTutorial(binding!!.lottieTutorialMenu) {
                    updateTutorial(CelebTutorialBits.MAIN_MENU_1)
                    drawerLayout?.openDrawer(GravityCompat.START)
                }
            }
            CelebTutorialBits.MAIN_RANKING -> {
                setupLottieTutorial(binding!!.lottieTutorialRanking) {
                    updateTutorial(CelebTutorialBits.MAIN_RANKING)
                    binding?.btnSummary?.callOnClick()
                }
            }
            CelebTutorialBits.MAIN_MIRACLE -> {
                setupLottieTutorial(binding!!.lottieTutorialMiracle) {
                    updateTutorial(CelebTutorialBits.MAIN_MIRACLE)
                    binding?.btnMiracle?.callOnClick()
                }
            }
            CelebTutorialBits.MAIN_ONE_PICK -> {
                setupLottieTutorial(binding!!.lottieTutorialOnePick) {
                    updateTutorial(CelebTutorialBits.MAIN_ONE_PICK)
                    binding?.btnOnePick?.callOnClick()
                }
            }
            CelebTutorialBits.MAIN_HOF -> {
                setupLottieTutorial(binding!!.lottieTutorialHallOfFame) {
                    updateTutorial(CelebTutorialBits.MAIN_HOF)
                    binding?.btnHallOfFame?.callOnClick()
                }
            }
            CelebTutorialBits.MAIN_MY_CELEB -> {
                setupLottieTutorial(binding!!.lottieTutorialFavorite) {
                    updateTutorial(CelebTutorialBits.MAIN_MY_CELEB)
                    binding?.btnFavorite?.callOnClick()
                }
            }
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            sharedAppState.hasUnreadNotification.collect { hasUnread ->
                val resId = if(hasUnread) R.drawable.btn_navigation_bell_dot_celeb else R.drawable.btn_navigation_bell_celeb
                val menuToolbar = findViewById<Toolbar?>(R.id.toolbar_menu)
                val btnNotification = menuToolbar?.menu?.findItem(R.id.menu_notification)
                btnNotification?.setIcon(resId)
                // 메뉴 버튼에 점 표시
                updateActionBar()
            }
        }
    }

    fun updateActionBar() {
        val actionBar = supportActionBar

        if (actionBar != null) {
            var hasCoupon = false
            try {
                val account: IdolAccount? = IdolAccount.getAccount(this)
                if (account != null) {
                    hasCoupon = Util.messageParse(
                        applicationContext,
                        account.userModel?.messageInfo,
                        "C"
                    ) > 0
                }
            } catch (_: Exception) {
            }
            if (UtilK.hasUnreadEvent(this) || UtilK.hasUnreadNotice(this) || hasCoupon
                || sharedAppState.hasUnreadNotification.value == true
                ) {
                actionBar.setHomeAsUpIndicator(R.drawable.btn_navigation_menu_dot)
            } else {
                actionBar.setHomeAsUpIndicator(R.drawable.btn_navigation_menu)
            }
        }
    }

    //다운로드받은 zip파일 정보(JSON파일) 로컬 캐싱해줌.
    private fun initEmoticon() {
        //서버에서 불러온 이모티콘 zip파일 저장 경로.
        val zipFolder = File(filesDir.absolutePath + "/zipFile")

        Thread {
            try {
                val gson: Gson = IdolGson.instance
                val emoListType = object : TypeToken<ArrayList<EmoticonsetModel?>?>() {
                }.type

                val emoList: ArrayList<EmoticonsetModel> =
                    gson.fromJson<ArrayList<EmoticonsetModel>>(
                        Util.getPreference(
                            this, Const.EMOTICON_SET
                        ), emoListType
                    )

                //이모티콘 전체리스트 지워주고 다시 가져와줌.
                val emoAllInfolistType = object : TypeToken<List<EmoticonDetailModel?>?>() {
                }.type
                var emoticonAllInfoList: ArrayList<EmoticonDetailModel> =
                    ArrayList()
                val emoticonAllInfo = Util.getPreference(this, Const.EMOTICON_ALL_INFO)
                if (emoticonAllInfo.isNotEmpty()) { //빈값으로 들어있으면 removeIf 할때 exception으로 나와서 비어있나 체크해줘야됨.
                    emoticonAllInfoList = gson.fromJson<ArrayList<EmoticonDetailModel>>(
                        emoticonAllInfo,
                        emoAllInfolistType
                    )
                }
                for (i in emoList.indices) { //이모티콘 SET별로 로컬에 풀어줌.
                    if (emoList[i].isChanged) { //업데이트된 SET만 zip해제해주기.
                        //해제해주기전에 로컬 캐싱되어있는 해당 JSON SET제거해줍니다(JSON만제거하고 폴더안에있는 파일은 기존 사용자가 쓸수있기때문에 남겨둡니다).
                        val iterator: MutableIterator<EmoticonDetailModel> =
                            emoticonAllInfoList.iterator()
                        while (iterator.hasNext()) {
                            val model: EmoticonDetailModel = iterator.next()
                            if (model.emoticonSetId == emoList[i].id) {
                                iterator.remove()
                            }
                        }
                        try {
                            val manager: ZipManager? = ZipManager.getInstance(emoticonAllInfoList)
                            //zip풀어주기.
                            manager?.unzip(
                                this,
                                zipFolder.toString() + File.separator + emoList[i].id + ".zip",
                                filesDir.canonicalPath + File.separator + "unzipped",
                                emoList[i].id
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    //서버에서 zip파일 다운로드.
    private fun downLoadZipFile() {
        Thread {
            var input: InputStream? = null
            var output: OutputStream? = null
            var connection: HttpURLConnection?
            val gson: Gson = IdolGson.instance
            val emoListType = object : TypeToken<ArrayList<EmoticonsetModel?>?>() {}.type
            val emoList: ArrayList<EmoticonsetModel> = gson.fromJson(
                Util.getPreference(
                    this, Const.EMOTICON_SET
                ), emoListType
            )

            val emoticonUrl: String = ConfigModel.getInstance(this).emoticonUrl ?: ""

            //이모티콘 SET마다 서버에서 이모티콘 zip파일을 불러옵니다.
            for (i in emoList.indices) {
                try {
                    val url =
                        URL(emoticonUrl + File.separator + emoList[i].id + File.separator + emoList[i].id + ".zip")
                    connection = url.openConnection() as HttpURLConnection
                    connection.connect()

                    input = connection.inputStream

                    val zipFolder = File(filesDir.absolutePath + "/zipFile")
                    if (!zipFolder.exists()) { //zipFolder 만들어져있나 확인(zip파일은 zipFolder , zip파일 압축푼거는 unzipped폴더에 있음).
                        zipFolder.mkdir()
                    }

                    output =
                        FileOutputStream(zipFolder.toString() + File.separator + emoList[i].id + ".zip")

                    val data = ByteArray(4096)
                    var count: Int
                    while ((input.read(data).also { count = it }) != -1) {
                        output.write(data, 0, count)
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                } finally {
                    try {
                        output?.close()
                        input?.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }

            //이모티콘 zip파일 풀어주기.
            initEmoticon()
        }.start()
    }

    private val emoticonProcess: Unit
        //서버에서 이모티콘 SET불러와줘서 로컬에캐싱되있는거랑 비교.
        get() {
            val cacheEmoticonVersion = Util.getPreferenceInt(this, Const.EMOTICON_VERSION, -1)
            val remoteEmoticonVersion: Int = ConfigModel.getInstance(this).emoticonVersion

            //configs에서 가져온 이모티콘 버전이랑 같지않으면 이모티콘 불러오는 로직 실행.
            if (cacheEmoticonVersion != remoteEmoticonVersion) {
                //이모티콘 set 리스트를 불러와줍니다.
                lifecycleScope.launch {
                    emoticonRepository.getEmoticon(null,
                        { response ->
                            if (response.optBoolean("success")) {
                                val emoGson: Gson = IdolGson.instance
                                val emoListType =
                                    object : TypeToken<ArrayList<EmoticonsetModel?>?>() {}.type

                                val obj: JSONArray?
                                try {
                                    obj = response.getJSONArray("emoticon_set")
                                } catch (e: JSONException) {
                                    return@getEmoticon
                                }

                                //가장 처음 시작했을떄.
                                if (Util.getPreference(this@MainActivity, Const.EMOTICON_SET)
                                        .isEmpty()
                                ) {
                                    val emoList: ArrayList<EmoticonsetModel?> =
                                        emoGson.fromJson(
                                            obj.toString(),
                                            emoListType
                                        )

                                    for (i in emoList.indices) { //가장 처음엔 모두다 다운받아야되므로 모두 true 처리해준다.
                                        emoList[i]?.isChanged = true
                                    }

                                    //                            Collections.swap(emoList, 0,1);
                                    emoList.reverse()
                                    Util.setPreference(
                                        this@MainActivity,
                                        Const.EMOTICON_SET,
                                        emoGson.toJson(emoList)
                                    )
                                } else {
                                    try {
                                        //기존 가지고있는 이모티콘.
                                        val priorEmoList: ArrayList<EmoticonsetModel> =
                                            emoGson.fromJson<ArrayList<EmoticonsetModel>>(
                                                Util.getPreference(
                                                    this@MainActivity, Const.EMOTICON_SET
                                                ), emoListType
                                            )

                                        //서버에서 가지고온 이모티콘.
                                        val newEmoList: ArrayList<EmoticonsetModel?> =
                                            emoGson.fromJson<ArrayList<EmoticonsetModel?>>(
                                                obj.toString(),
                                                emoListType
                                            )
                                        for (i in newEmoList.indices) {
                                            newEmoList[i]?.isChanged = true
                                        }

                                        for (i in priorEmoList.indices) {
                                            for (j in newEmoList.indices) {
                                                if (newEmoList[j]?.id == priorEmoList[i].id) { //아이디가 같고.
                                                    if (newEmoList[j]?.version == priorEmoList[i].version) { //버전이똑같으면 false로 넣어서 다시 안풀어지게 해줌.
                                                        newEmoList[j]?.isChanged = false
                                                    } else {
                                                        newEmoList[j]?.isChanged = true
                                                    }
                                                }
                                            }
                                        }

                                        //                                Collections.swap(newEmoList, 0,1);
                                        Collections.reverse(newEmoList)
                                        Util.setPreference(
                                            this@MainActivity,
                                            Const.EMOTICON_SET,
                                            emoGson.toJson(newEmoList)
                                        )
                                    } catch (e: JsonSyntaxException) {
                                        return@getEmoticon
                                    }
                                }

                                //이모티콘 받아오기
                                downLoadZipFile()
                            } else {
                                UtilK.handleCommonError(this@MainActivity, response)
                            }

                            //이모티콘 버전 세팅.
                            Util.setPreference(
                                this@MainActivity,
                                Const.EMOTICON_VERSION,
                                remoteEmoticonVersion
                            )
                        }, { throwable ->
                            Toast.makeText(this@MainActivity, throwable.message, Toast.LENGTH_SHORT).show()
                        })
                }
            }
        }

    private fun appVersionCheck() {
        val listType = object : TypeToken<List<FamilyAppModel?>?>() {}.type
        val appModelList: ArrayList<FamilyAppModel> =
            IdolGson.instance.fromJson<ArrayList<FamilyAppModel>>(
                Util.getPreference(
                    this, Const.PREF_FAMILY_APP_LIST
                ), listType
            )

        try {
            //앱버전은 고정이므로 반복문 밖선언
            val appVersion = getString(R.string.app_version)
            val splitAppVersion =
                appVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()

            //각자리를 3자리씩 패딩.
            val combineAppVersion = String.format(
                Locale.US,
                "%03d%03d%03d",
                splitAppVersion[0].toInt(),
                splitAppVersion[1].toInt(),
                splitAppVersion[2].toInt()
            )

            for (i in appModelList.indices) {
                val remoteVersion: String = appModelList[i].version ?: ""
                val splitRemoteVersion =
                    remoteVersion.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }
                        .toTypedArray()

                val combineRemoteVersion = String.format(
                    Locale.US,
                    "%03d%03d%03d",
                    splitRemoteVersion[0].toInt(),
                    splitRemoteVersion[1].toInt(),
                    splitRemoteVersion[2].toInt()
                )

                if (appModelList[i].appId == AppConst.APP_ID && appModelList[i].needUpdate == "Y" && combineRemoteVersion > combineAppVersion
                ) {
                    Util.log("Version2::$combineRemoteVersion")
                    showAppVersionCheckDialog(appModelList[i])
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //drwable 메뉴에  myinfofragment를 세로 set 해줌.
    private fun setMyInfoFragment() {
        try {
            myInfo = null
            myInfo = MyInfoFragment()
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.drawer_menu, myInfo!!)
                .commit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        var isSocketConnected = false
        if (socketManager?.socket != null) {
            isSocketConnected = socketManager?.socket!!.connected()
        }
        if (socketManager != null && isSocketConnected) {
            socketManager?.disconnectSocket()
        }

        idolBroadcastManager.disconnect()

        // 움짤프사
        try {
            player1?.release()
            player2?.release()
            player3?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // drawer
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver)

        IdolApplication.getInstance(this).mainActivity = null

        if (reviewDialog != null) reviewDialog!!.dismiss()

        //앱종시 자동갱신 멈춤
        idolApiManager.stopTimer()
    }

    // 언어가 바뀌면 재시작
    private fun showAppVersionCheckDialog(model: FamilyAppModel) {
        val url: String? = model.updateUrl
        Util.showDefaultIdolDialogWithRedBtn2(
            this,
            null,
            getString(R.string.popup_update_app),
            getString(R.string.popup_update_app),
            R.string.update,
            R.string.btn_cancel,
            false,
            true,
            true, false,
            { v: View? ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: ActivityNotFoundException) {
                    val intent = Intent(this, AppLinkActivity::class.java)
                    intent.setData(Uri.parse(url))
                    startActivity(intent)
                }
                Util.closeIdolDialog()
            },
            { v: View? ->
                Util.closeIdolDialog()
            })
    }

    override fun onResume() {
        super.onResume()

        idolApiManager.startTimer()

        // 안읽은 이벤트/공지/쿠폰 처리
        updateActionBar()

        //딥링크 이동
        deepLinkScreenMove()

        val newLocale = Util.getPreference(this, Const.PREF_LANGUAGE)
        if ((locale == null && newLocale != null)
            || (locale != null && locale != newLocale)
        ) {
            val intent = intent
            finish()
            startActivity(intent)
        }
        if (timeThread.state == Thread.State.NEW) timeThread.start()

        changeAttendanceIconStatus()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == LiveTrailerSlideFragment.REQUEST_PARAM_TO_MAIN) { //라이브 배너를 라이브 화면 갔다가 온경우.

            //main의 fragment 중에서 livestreamingListfragment를 찾아서 있으면,  해당 fragment의 onActivityResult를 호출하여,
            //화면 refresh 진행

            for (fragment in supportFragmentManager.fragments) {
                if (fragment is LiveStreamingListFragment) {
                    fragment.onActivityResult(
                        LiveStreamingListFragment.REQUEST_CODE_LIVE_LIST,
                        resultCode,
                        data
                    )
                    break
                }
            }
        }

        if (requestCode == REQUEST_CODE_DEEP_LINK) {
            //main으로 다시 돌아올떄  mainActivity가 재생성될경우  deeplink를 타고 왔으면  다시 해당 로직이 실행되므로,
            //해당  intent extra key EXTRA_NEXT_ACTIVITY를 지워준다.
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
            "vote_videoad"
        ) { adType: String? ->
            videoAdUtil?.onVideoSawCommon(
                this, true, adType, null
            )
        }
    }

    override fun onPause() {
        super.onPause()
        idolApiManager.stopTimer()
        timeThread.interrupt()
        searchClose()
    }

    private fun initNavState() {
        binding?.let {
            with(it) {
                ivSummary.setImageResource(R.drawable.btn_bottom_nav_ranking_off_celeb)
                tvSummary.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dimmed))
                ivMiracle.setImageResource(R.drawable.btn_bottom_nav_miracle_off_celeb)
                tvMiracle.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dimmed))
                ivOnePick.setImageResource(R.drawable.btn_bottom_nav_onepick_off_celeb)
                tvOnePick.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dimmed))
                ivHallOfFame.setImageResource(R.drawable.btn_bottom_nav_halloffame_off_celeb)
                tvHallOfFame.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dimmed))
                ivFavorite.setImageResource(R.drawable.btn_bottom_nav_favorite_off_celeb)
                tvFavorite.setTextColor(ContextCompat.getColor(this@MainActivity, R.color.text_dimmed))
            }
        }
    }

    private fun checkTabState(v: View) {
        if (btnTabResourceId != v.id) {
            if (menu != null) searchClose()
            btnTabResourceId = v.id
            tapCount = 0
        }
    }

    private fun getTransactionOnTabNav(v: View): FragmentTransaction {
        // 순위화면 이외의 화면으로 이동하면 움짤 멈추기
        val currentFragment: Fragment? = summaryFrag?.getCurrentFragment()
        if (currentFragment != null && currentFragment is NewRankingFragment2) {
            val rankingFragment: NewRankingFragment2 = currentFragment
            if (v.id != R.id.btn_summary) {
                rankingFragment.stopPlayer()
            } else if (tabIndex != MainTab.MAIN) {
                // 순위화면으로 다시 오면
                rankingFragment.startPlayer()
                //                rankingFragment.startTopBannerPlayer();
            }
        }

        val transaction = supportFragmentManager.beginTransaction()
        for (frag in supportFragmentManager.fragments) {
            transaction.hide(frag)
        }

        return transaction
    }

    private fun checkScrollToTop(v: View) {
        when(v.id) {
            R.id.btn_summary -> {
                if(tabIndex == MainTab.MAIN) {
                    lifecycleScope.launch {
                        sharedAppState.setScrollToTop(true)
                    }
                }
            }
            R.id.btn_miracle -> {
                if(tabIndex == MainTab.MIRACLE) {
                    miracleMainFrag?.onScrollToTop()
                }
            }
            R.id.btn_one_pick -> {
                if(tabIndex == MainTab.ONEPICK) {
                    onePickFrag?.onScrollToTop()
                }
            }
            R.id.btn_hall_of_fame -> {
                if(tabIndex == MainTab.HOF) {
                    hallFrag?.onScrollToTop()
                }
            }
            R.id.btn_favorite -> {
                if(tabIndex == MainTab.FAVORITES) {
                    favoritFrag?.onScrollToTop()
                }
            }
        }
    }

    private fun setListener() {
        val summaryFrag = summaryFrag ?: return
        binding?.btnSummary?.setOnClickListener { v ->
            if (binding?.lottieTutorialRanking?.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            val transaction = getTransactionOnTabNav(v)
            initNavState()
            checkScrollToTop(v)

            tabIndex = MainTab.MAIN
            transaction.show(summaryFrag).commit()
            binding?.ivSummary?.setImageResource(R.drawable.btn_bottom_nav_ranking_on)
            binding?.tvSummary?.setTextColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.text_default
                )
            )
            summaryFrag.checkVisibility()
            setMyInfoFragment()

            setUiActionFirebaseGoogleAnalyticsActivity(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                GaAction.BTB_RANK.label
            )

            if (BuildConfig.DEBUG) {
                tapCount++
                if (tapCount >= 5) {
                    tapCount = 0
                    if (ServerUrl.HOST == ServerUrl.HOST_REAL) {
                        ServerUrl.HOST = ServerUrl.HOST_TEST
                    } else {
                        ServerUrl.HOST = ServerUrl.HOST_REAL
                    }

                    // 로컬 아이돌 싱글톤 초기화
                    lifecycleScope.launch(Dispatchers.IO) {
                        deleteAllIdolUseCase().firstOrNull()
                    }
                    // 아이돌 타임스탬프 초기화
                    Util.setPreference(this@MainActivity, Const.PREF_ALL_IDOL_UPDATE, "")
                    Util.setPreference(this@MainActivity, Const.PREF_DAILY_IDOL_UPDATE, "")
                    // 각 탭 reload 시간 초기화
                    Util.setPreference(this@MainActivity, Const.KEY_IDOLS_S, 0L)
                    Util.setPreference(this@MainActivity, Const.KEY_IDOLS_S, 0L)
                    Util.setPreference(this@MainActivity, Const.KEY_IDOLS_G, 0L)
                    Util.setPreference(this@MainActivity, Const.KEY_IDOLS_G, 0L)
                    // 나의최애 탭 초기화
                    ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)

                    Util.setPreference(this@MainActivity, Const.PREF_SERVER_URL, ServerUrl.HOST)

                    Toast.makeText(
                        this@MainActivity,
                        "Server set to " + ServerUrl.HOST,
                        Toast.LENGTH_SHORT
                    ).show()

                    val intent = Intent(this@MainActivity, StartupActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                }
            }
            checkTabState(v)
        }

        binding?.btnMiracle?.setOnClickListener { v ->
            if (binding?.lottieTutorialMiracle?.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            val transaction = getTransactionOnTabNav(v)
            initNavState()
            checkScrollToTop(v)

            setUiActionFirebaseGoogleAnalyticsActivity(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                GaAction.BTB_MIRACLE.label
            )

            if (tabIndex != MainTab.MIRACLE) {    //탭 클릭때마다 api 호출하기 때문에 해당 탭일 땐 해당 탭 클릭못하도록 막음
                if (miracleMainFrag == null) {
                    miracleMainFrag = MiracleMainFragment()
                    transaction.add(R.id.fragment_container, miracleMainFrag!!, TAG_MIRACLE)
                        .commitAllowingStateLoss()
                } else {
                    transaction.show(miracleMainFrag!!).commitAllowingStateLoss()
                }
            }
            tabIndex = MainTab.MIRACLE
            setMyInfoFragment()
            showBanner(MIRACLE_TAB)
            binding?.ivMiracle?.setImageResource(R.drawable.btn_bottom_nav_miracle_on_celeb)
            binding?.tvMiracle?.setTextColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.text_default
                )
            )
            checkTabState(v)
        }

        binding?.btnOnePick?.setOnClickListener { v ->
            if (binding?.lottieTutorialOnePick?.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            val transaction = getTransactionOnTabNav(v)
            initNavState()
            checkScrollToTop(v)

            setUiActionFirebaseGoogleAnalyticsActivity(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                GaAction.BTB_THEME_PICK.label
            )

            tabIndex = MainTab.ONEPICK
            if (onePickFrag == null) {
                onePickFrag = OnePickMainFragment()
                transaction.add(R.id.fragment_container, onePickFrag!!, TAG_THEME)
                    .commitAllowingStateLoss()
            } else {
                transaction.show(onePickFrag!!).commitAllowingStateLoss()
            }
            setMyInfoFragment()
            binding?.ivOnePick?.setImageResource(R.drawable.btn_bottom_nav_onepick_on_celeb)
            binding?.tvOnePick?.setTextColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.text_default
                )
            )
            checkTabState(v)
        }

        binding?.btnHallOfFame?.setOnClickListener { v ->
            if (binding?.lottieTutorialHallOfFame?.visibility == View.VISIBLE) {
                return@setOnClickListener
            }
            val transaction = getTransactionOnTabNav(v)
            initNavState()
            checkScrollToTop(v)

            setUiActionFirebaseGoogleAnalyticsActivity(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                GaAction.BTB_HOF.label
            )

            tabIndex = MainTab.HOF
            if (hallFrag == null) {
                hallFrag = HallOfFameFragment()
                transaction.add(R.id.fragment_container, hallFrag!!, TAG_HOF)
                    .commitAllowingStateLoss()
            } else {
                transaction.show(hallFrag!!).commitAllowingStateLoss()
            }
            setMyInfoFragment()
            showBanner(HALL_TAB)
            binding?.ivHallOfFame?.setImageResource(R.drawable.btn_bottom_nav_halloffame_on_celeb)
            binding?.tvHallOfFame?.setTextColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.text_default
                )
            )
            checkTabState(v)
        }

        binding?.btnFavorite?.setOnClickListener { v ->
            if (binding?.lottieTutorialFavorite?.visibility == View.VISIBLE) {
                return@setOnClickListener
            }

            val transaction = getTransactionOnTabNav(v)
            initNavState()
            checkScrollToTop(v)

            setUiActionFirebaseGoogleAnalyticsActivity(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                GaAction.BTB_MY_IDOL.label
            )

            tabIndex = MainTab.FAVORITES
            if (favoritFrag == null) {
                favoritFrag = FavoritIdolFragment()
                transaction.add(R.id.fragment_container, favoritFrag!!, TAG_FAVORITE)
                    .commitAllowingStateLoss()
            } else {
                transaction.show(favoritFrag!!).commitAllowingStateLoss()
            }
            setMyInfoFragment()
            binding?.ivFavorite?.setImageResource(R.drawable.btn_bottom_nav_favorite_on_celeb)
            binding?.tvFavorite?.setTextColor(
                ContextCompat.getColor(
                    baseContext,
                    R.color.text_default
                )
            )
            checkTabState(v)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.iv_mission -> {
                val dialog = CelebWelcomeMissionDialog()
                dialog.show(supportFragmentManager, "WelcomeMissionDialog")
            }
        }
        if (btnTabResourceId != v.id) {
            if (menu != null) searchClose()
            btnTabResourceId = v.id
            tapCount = 0
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_menu, menu)
        this.menu = menu

        val currentTutorialIndex= TutorialManager.getTutorialIndex()

        val searchItem = menu.findItem(R.id.action_search).actionView
        val searchIcon = searchItem?.findViewById<AppCompatImageView>(R.id.iv_icon)
        searchIcon?.let {
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.SEARCH.actionValue,
                GaAction.SEARCH.label
            )

            it.setImageResource(R.drawable.btn_navigation_search_celeb)
            it.setOnClickListener {
                if (Util.mayShowLoginPopup(this@MainActivity)) return@setOnClickListener
                startActivity(SearchHistoryActivity.createIntent(this@MainActivity))
            }
        }

        if (currentTutorialIndex == CelebTutorialBits.MAIN_SEARCH) {
            searchItem?.findViewById<LottieAnimationView>(R.id.lottie_tutorial) ?.let {
                setupLottieTutorial(it) {
                    updateTutorial(currentTutorialIndex)
                    searchIcon?.callOnClick()
                }
            }
        }

        val friendItem = menu.findItem(R.id.action_friends).actionView
        val friendIcon = friendItem?.findViewById<AppCompatImageView>(R.id.iv_icon)
        friendIcon?.let {
            it.setImageResource(R.drawable.btn_navigation_friend)
            it.setOnClickListener {
                if (Util.mayShowLoginPopup(this@MainActivity)) return@setOnClickListener

                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION, "top_friend"
                )
                startActivity(FriendsActivity.createIntent(this@MainActivity))
            }
        }

        if (currentTutorialIndex == CelebTutorialBits.MAIN_FRIEND) {
            friendItem?.findViewById<LottieAnimationView>(R.id.lottie_tutorial) ?.let {
                setupLottieTutorial(it) {
                    updateTutorial(currentTutorialIndex)
                    friendIcon?.callOnClick()
                }
            }
        }

        val myHeartItem = menu.findItem(R.id.action_myheart).actionView
        val myHeartIcon = myHeartItem?.findViewById<AppCompatImageView>(R.id.iv_icon)
        myHeartIcon?.let {
            it.setImageResource(R.drawable.btn_navigation_my_heart_celeb)
            it.setOnClickListener {
                if (Util.mayShowLoginPopup(this@MainActivity)) return@setOnClickListener

                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION, "top_myheart"
                )
                startActivity(MyHeartInfoActivity.createIntent(this@MainActivity))
            }
        }

        if (currentTutorialIndex == CelebTutorialBits.MAIN_MY_HEART) {
            myHeartItem?.findViewById<LottieAnimationView>(R.id.lottie_tutorial) ?.let {
                setupLottieTutorial(it) {
                    updateTutorial(currentTutorialIndex)
                    myHeartIcon?.callOnClick()
                }
            }
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mDrawerToggle!!.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showSoftKeyboard() {
        mSearchInput?.requestFocus()
        mSearchInput?.setCursorVisible(true)
        val imm = getSystemService(
            INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.showSoftInput(mSearchInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun hideSoftKeyboard() {
        mSearchInput?.setCursorVisible(false)
        val imm = getSystemService(
            INPUT_METHOD_SERVICE
        ) as InputMethodManager
        imm.hideSoftInputFromWindow(mSearchInput?.windowToken, 0)
    }

    private fun cleanKeyword() {
        mSearchInput?.text = null
        mSearchInput?.clearFocus()
    }

    private fun searchClose() {
        mDeadline!!.visibility = View.VISIBLE
        cleanKeyword()
        hideSoftKeyboard()
        if (menu != null) menu!!.setGroupVisible(R.id.main_menu_group, true)
    }

    private fun searchKeyword() {
        hideSoftKeyboard()

        val searchText: String = mSearchInput?.getText().toString().trim { it <= ' ' }
        if (searchText.isEmpty()) {
            mKeyword = null
            cleanKeyword()
        } else {
            mKeyword = searchText
            cleanKeyword()
            startActivity(SearchResultActivity.createIntent(applicationContext, mKeyword!!))
        }
    }

    private fun showGoolgeReviewDialog(context: Context) {
        if (reviewDialog != null && reviewDialog!!.isShowing) return

        reviewDialog = Dialog(context, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow: WindowManager.LayoutParams = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        reviewDialog!!.window!!.attributes = lpWindow
        val dm = context.resources.displayMetrics
        val width = (min(dm.widthPixels, dm.heightPixels) * 0.85f).toInt()
        reviewDialog!!.window!!.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        reviewDialog!!.setContentView(R.layout.dialog_review)
        reviewDialog!!.setCanceledOnTouchOutside(false)
        reviewDialog!!.setCancelable(false)

        val tvMsg = reviewDialog!!.findViewById<AppCompatTextView>(R.id.msg)

        // celeb
        (reviewDialog!!.findViewById<View>(R.id.title) as AppCompatTextView).setText(R.string.actor_title_review)
        (reviewDialog!!.findViewById<View>(R.id.msg2) as AppCompatTextView).setText(R.string.actor_lable_review_info2)
        tvMsg.setText(R.string.actor_lable_review_info1)

        val sp: Spannable = SpannableString(tvMsg.text.toString())

        tvMsg.setText(sp)
        val tvBtnOk = reviewDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
        tvBtnOk.setOnClickListener { v: View? ->
            Util.setPreference(this@MainActivity, Const.PREF_REQUEST_REVIEW, true)
            reviewDialog!!.dismiss()
            Util.gotoMarket(this@MainActivity, packageName)
            mRequestHeartHandler!!.sendEmptyMessageDelayed(0, 3000)
        }
        val tvBtnCancel = reviewDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
        tvBtnCancel.setOnClickListener { v: View? ->
            Util.setPreference(this@MainActivity, Const.PREF_REQUEST_REVIEW, true)
            reviewDialog!!.dismiss()
        }
        reviewDialog!!.window
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        reviewDialog!!.show()
    }

    private fun showSetMostDialog() {
        if (idolDialog != null && idolDialog!!.isShowing) return

        setFirebaseUIAction(GaAction.CHOEAE_POPUP)

        idolDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow: WindowManager.LayoutParams = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        idolDialog!!.window!!.attributes = lpWindow
        idolDialog!!.window!!
            .setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        idolDialog!!.setContentView(R.layout.dialog_not_show_two_btn)
        val dialogTvTitle = idolDialog!!.findViewById<AppCompatTextView>(R.id.title)
        dialogTvTitle.text = UtilK.getMyIdolTitle(this@MainActivity)


        idolDialog!!.setCanceledOnTouchOutside(true)
        idolDialog!!.setCancelable(true)
        val dialogTvMsg = idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
        dialogTvMsg.text = getString(R.string.actor_label_set_most)

        val cbCheckGuide = idolDialog!!.findViewById<AppCompatCheckBox>(R.id.check_guide)

        cbCheckGuide.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean -> }

        val tvBtnOk = idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
        tvBtnOk.setOnClickListener {
            if (cbCheckGuide.isChecked) {
                Util.setPreference(
                    applicationContext,
                    Const.PREF_NEVER_SHOW_SET_MOST,
                    true
                )
            }
            setFirebaseUIAction(GaAction.CHOEAE_POPUP_YES)
            idolDialog!!.cancel()
            startActivity(FavoriteSettingActivity.createIntent(this@MainActivity))
        }

        val tvBtnCancel = idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
        tvBtnCancel.setOnClickListener {
            if (cbCheckGuide.isChecked) {
                Util.setPreference(
                    applicationContext,
                    Const.PREF_NEVER_SHOW_SET_MOST,
                    true
                )
            }
            setFirebaseUIAction(GaAction.CHOEAE_POPUP_NO)
            idolDialog!!.cancel()
        }

        idolDialog!!.window
            ?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        idolDialog!!.show()
    }

    private fun requestEvent(savedInstanceState: Bundle?) {
        // savedInstanceState가 있으면 아무것도 안함.
        if (savedInstanceState != null) return

        // 앱 시작시 push key 업데이트
        GcmUtils.registerDevice(this@MainActivity, onGcmRegistered)

        // 최애 설정 유도
        if (((IdolAccount.getAccount(this)?.most != null
                    && IdolAccount.getAccount(this)?.most?.type.equals("B", ignoreCase = true))
                    )
        ) {
            if (!Util.getPreferenceBool(this, Const.PREF_NEVER_SHOW_SET_MOST, false)) {
                showSetMostDialog()
            }
        }

        if ((IdolAccount.getAccount(this)?.most == null)) {
            if (Util.getPreferenceBool(this, Const.PREF_SHOW_SEARCH_IDOL, true)) {
                showHelpSearchIdol()
            }
        }

        lifecycleScope.launch {
            getEventUseCase(
                version = getString(R.string.app_version),
                gmail = Util.getGmail(this@MainActivity),
                isVM = VMDetector.getInstance(this@MainActivity).isVM(),
                isRooted = VMDetector.getInstance(this@MainActivity).isRooted,
                deviceId = Util.getDeviceUUID(this@MainActivity)
            ).collect { response ->
                if (response.optBoolean("success")) {
                    val isFirstOpen =
                        Util.getPreferenceBool(baseContext, Const.PREF_FIRST_OPEN, false)

                    eventHeartModel = EventHeartModel()

                    // 출석/데일리팩/덕질타임/VIP 하트
                    eventHeartModel?.let {
                        it.dailyHeart = response.optInt("daily_heart")
                        it.sorryHeart = response.optInt("vip_heart")
                        it.vipMessage = response.optString("vip_message")
                        it.burning = response.optBoolean("burning")
                        it.burningTime = response.optBoolean("burningtime")
                        it.burningHeart = response.optInt("burning_heart")
                        val mostPicks = response.optString("most_picks")
                        Util.setPreference(this@MainActivity, Const.PREF_MOST_PICKS, mostPicks)

                        tryShowLoginHeart(it)
                    }

                    //banner 리스트 받기
                    try {
                        banners = response.getJSONArray("banners")
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                    Logger.v(response.toString())
                    // 언어에 따른 guide url
                    Util.setPreference(
                        this@MainActivity,
                        Const.PREF_GUIDE,
                        response.optString("guide_url")
                    )

                    isShowMissionBtn = response.optBoolean("show_welcome_mission")

                    val listType = object : TypeToken<ArrayList<FrontBannerModel?>?>() {}.type
                    try {
                        fronbannerlist =
                            IdolGson.instance.fromJson<ArrayList<FrontBannerModel>>(
                                banners.toString(),
                                listType
                            )

                        // TransactionTooLargeException 방지
                        val extras: ExtendedDataHolder = ExtendedDataHolder.getInstance()
                        extras.clear()
                        if (fronbannerlist != null) {
                            extras.putExtra("bannerList", fronbannerlist!!)
                        }

                        //MainActivity onCreate 될때는 메인 배너부터  보여줌.
                        if (!isFirstOpen) {
                            showBanner(MAIN_TAB)
                        }
                    } catch (e: NullPointerException) {
                        e.printStackTrace()
                    }


                    val progress = response.optString("progress")

                    if (progress == Const.RESPONSE_Y) {
                        if (response.optBoolean("burningtime")) {
                            Util.setPreference(this@MainActivity, Const.PREF_BURNING_TIME, true)
                        } else {
                            Util.setPreference(
                                this@MainActivity,
                                Const.PREF_BURNING_TIME,
                                false
                            )
                        }
                    }

                    if (isShowMissionBtn) {
                        summaryFrag?.showWelcomeMissionButton(true)

                        if (isFirstOpen) {
                            Util.setPreference(baseContext, Const.PREF_FIRST_OPEN, false)

                            val dialog = CelebWelcomeMissionDialog()
                            dialog.show(supportFragmentManager, "WelcomeMissionDialog")
                        }
                    }
                }
            }
        }
    }


    private fun showBanner(tabPosition: Int) {
        val never_event_list = Util.getPreference(
            this@MainActivity,
            Const.PREF_NEVER_SHOW_EVENT
        )

        //fronbannerlist에서  어느 인덱스에 있는지 확인 하기 위한 값.
        var checkIndex = -1

        //각 탭별  해당 하는 배너 데이터가 잇는지 여부를  판단한다.
        var isBannerDataExist = false


        //각  택
        try {
            if (fronbannerlist == null) {
                val extendedDataHolder: ExtendedDataHolder = ExtendedDataHolder.getInstance()
                if (extendedDataHolder.hasExtra("bannerList")) {
                    @Suppress("UNCHECKED_CAST")
                    fronbannerlist = extendedDataHolder.getExtra("bannerList") as? ArrayList<FrontBannerModel>?
                }
            }


            if (tabPosition == HALL_TAB) { //명전 탭일때
                for (i in fronbannerlist!!.indices) {
                    if (fronbannerlist!![i].type.equals("H", ignoreCase = true)) {
                        checkIndex = i
                        isBannerDataExist = true
                        break
                    }
                }
            } else if (tabPosition == MIRACLE_TAB) { //이달의 기적 탭일 때(서포트 배너가 나올 예정 - 위치는 바꼇지만, 기존대로 두번째 탭에서 나오게 하자고 하심)
                setUiActionFirebaseGoogleAnalyticsActivity(
                    Const.ANALYTICS_BUTTON_PRESS_ACTION,
                    "btn_miracle"
                )
                for (i in fronbannerlist!!.indices) {
                    if (fronbannerlist!![i].type.equals("S", ignoreCase = true)) {
                        checkIndex = i
                        isBannerDataExist = true
                        break
                    }
                }
            } else if (tabPosition == MAIN_TAB) { //메인 탭일때
                for (i in fronbannerlist!!.indices) {
                    if (fronbannerlist!![i].type.equals("M", ignoreCase = true)) {
                        checkIndex = i
                        isBannerDataExist = true
                        break
                    }
                }
            } else if (tabPosition == AWARD_TAB) {
                for (i in fronbannerlist!!.indices) {
                    if (fronbannerlist!![i].type.equals("A", ignoreCase = true)) {
                        checkIndex = i
                        isBannerDataExist = true

                        break
                    }
                }
            }


            //해당 배너 데이터가 있을때 배너를  띄어 준다.
            if (isBannerDataExist && !fronbannerlist!![checkIndex].isClosed) {
                //메모리 부족으로  엑티비티  다시 시작할때,
                //null exception 나와서 예외 처리 적용 .

                try {
                    fronbannerlist!![checkIndex].isClosed = true
                    val eventNo: String = fronbannerlist!![checkIndex].eventNum
                    val targetMenu: String = fronbannerlist!![checkIndex].targetMenu
                    val targetId: Int = fronbannerlist!![checkIndex].targetId
                    val imgUrl: String = fronbannerlist!![checkIndex].url
                    val goUrl: String = fronbannerlist!![checkIndex].goUrl
                    val readNoticeArray =
                        never_event_list.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()
                    if (never_event_list == "") {
                        showIdolGuideDialog(eventNo, imgUrl, goUrl, targetMenu, targetId)
                    } else {
                        if (!Util.isFoundString(eventNo, readNoticeArray)) {
                            showIdolGuideDialog(eventNo, imgUrl, goUrl, targetMenu, targetId)
                        }
                    }
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                }
            }
        } catch (e: NullPointerException) {
            e.printStackTrace()
        }
    }


    //딥링크로 들어왔을떄 다음 화면으로 이동
    @SuppressLint("UnsafeIntentLaunch")
    private fun deepLinkScreenMove() {
        try {
            val nextActivity = intent.getSerializableExtra(EXTRA_NEXT_ACTIVITY) as Class<*>?

            val nextIntent = nextIntent ?: return

            if (nextActivity == null) {
                return
            }

            if (nextActivity == MainActivity::class.java) {
                setTapFromAppLinkIntent(nextIntent)
                intent.removeExtra(PARAM_NEXT_INTENT)
                intent.removeExtra(EXTRA_NEXT_ACTIVITY)
                return
            }

            startActivityForResult(nextIntent, REQUEST_CODE_DEEP_LINK)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showHelpSearchIdol() {
        val tvHelpSearchIdol = findViewById<AppCompatTextView>(R.id.tv_help_search_idol)
        tvHelpSearchIdol.setText(R.string.tooltip_search_actor)
        tvHelpSearchIdol.visibility = View.VISIBLE
        tvHelpSearchIdol.setOnClickListener {
            tvHelpSearchIdol.visibility = View.GONE
            Util.setPreference(
                applicationContext,
                Const.PREF_SHOW_SEARCH_IDOL,
                false
            )
        }
    }

    private fun goToDailyPackDetail() {
        lifecycleScope.launch {
            miscRepository.getStore("P",
                { response ->
                    if (response.optBoolean("success")) {
                        val models: ArrayList<StoreItemModel> = ArrayList()
                        val skus = ArrayList<String>()
                        val array: JSONArray
                        val gson: Gson = IdolGson.instance

                        try {
                            array = response.getJSONArray("objects")
                            for (i in 0 until array.length()) {
                                val obj = array.getJSONObject(i)
                                val model: StoreItemModel = gson.fromJson(
                                    obj.toString(),
                                    StoreItemModel::class.java
                                )

                                if (model.isViewable.equals("Y", ignoreCase = true)
                                    && model.subscription.equals("Y", ignoreCase = true)
                                    && model.type.equals("A", ignoreCase = true)
                                ) {
                                    models.add(model)
                                    skus.add(model.skuCode)
                                }
                            }

                            if (models.size > 0) {
                                lifecycleScope.launch {
                                    usersRepository.getIabKey(
                                        { response ->
                                            Util.closeProgress(200)
                                            if (response.optBoolean("success")) {
                                                val key = response.optString("key")
                                                val iabHelperKey = checkKey(key)

                                                // compute your public key and store it in base64EncodedPublicKey
                                                startActivity(
                                                    SubscriptionDetailActivity.createIntent(
                                                        this@MainActivity,
                                                        models[0],
                                                        iabHelperKey
                                                    )
                                                )
                                            }
                                        }, {
                                            Util.closeProgress(100)
                                        }
                                    )
                                }
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                            Util.closeProgress(100)
                        }
                    } else {
                        Util.closeProgress(100)
                    }
                }, {
                    Util.closeProgress(100)
                    Toast.makeText(
                        this@MainActivity, R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                })
        }
    }

    private fun checkKey(key: String): String {
        val key1 = key.substring(key.length - 7, key.length)
        val data = key.substring(0, key.length - 7)
        val pKey = Util.xor(data.toByteArray(), key1.toByteArray())
        return String(pKey)
    }

    private fun showIdolGuideDialog(
        event_no: String,
        imgUrl: String,
        goUrl: String?, target_menu: String, target_id: Int
    ) {
        val eventDialog = Dialog(this, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow: WindowManager.LayoutParams = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        eventDialog.window!!.attributes = lpWindow
        eventDialog.window!!
            .setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        eventDialog.setContentView(R.layout.dialog_event)
        eventDialog.setCanceledOnTouchOutside(false)
        eventDialog.setCancelable(true)

        val imgEvent = eventDialog.findViewById<AppCompatImageView>(R.id.img_event)
        imgEvent.setOnClickListener { v: View? ->
            if (!TextUtils.isEmpty(target_menu)) {
                if (target_menu.equals("notice", ignoreCase = true)) {
                    startActivity(NoticeActivity.createIntent(this, target_id))
                } else if (target_menu.equals("event", ignoreCase = true)) {
                    startActivity(EventActivity.createIntent(this, target_id))
                } else if (target_menu.equals("idol", ignoreCase = true)) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        val idol = getIdolByIdUseCase(target_id)
                            .mapDataResource { it?.toPresentation() }
                            .awaitOrThrow()
                        idol?.let {
                            withContext(Dispatchers.Main) {
                                startActivity(CommunityActivity.createIntent(this@MainActivity, it))
                            }
                        }
                    }
                } else if (target_menu.equals("support", ignoreCase = true)) {
                    if (target_id == 0) {
                        startActivity(SupportInfoActivity.createIntent(this))
                    } else {
                        getSupportList(target_id)
                    }
                } else if (target_menu.equals(
                        "board",
                        ignoreCase = true
                    )
                ) { //자유게시판 게시글의 commentActivity로 이동한다
                    lifecycleScope.launch {
                        articlesRepository.getArticle(
                            "/api/v1/articles/$target_id/",
                            { response ->
                                // article은 KST
                                val article: ArticleModel =
                                    IdolGson.getInstance(true).fromJson<ArticleModel>(
                                        response.toString(),
                                        ArticleModel::class.java
                                    )
                                val adapterType: Int =
                                    if (article.type == "M") NewCommentAdapter.TYPE_SMALL_TALK else NewCommentAdapter.TYPE_ARTICLE
                                startActivity(
                                    NewCommentActivity.createIntent(
                                        this@MainActivity,
                                        article,
                                        0,
                                        true,
                                        adapterType
                                    )
                                )
                            },
                            {
                                Toast.makeText(
                                    this@MainActivity,
                                    getString(R.string.error_abnormal_exception),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }
            } else if (goUrl != null) {
                if (goUrl != "") {
                    try {
                        val intent = Intent(this, AppLinkActivity::class.java)
                        intent.setData(Uri.parse(goUrl))
                        startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        Util.showIdolDialogWithBtn1(
                            this@MainActivity,
                            null,
                            getString(R.string.msg_error_ok)
                        ) { v1: View? -> Util.closeIdolDialog() }

                        e.printStackTrace()
                    }
                }
            }
        }

        val cbCheckGuide = eventDialog.findViewById<AppCompatCheckBox>(R.id.check_guide)

        cbCheckGuide.setOnCheckedChangeListener { _: CompoundButton?, _: Boolean -> }

        val dialogBtnClose = eventDialog.findViewById<AppCompatButton>(R.id.btn_close)
        dialogBtnClose.setOnClickListener {
            if (cbCheckGuide.isChecked) {
                saveNeverShowEvent(event_no)
            }
            eventDialog.cancel()
        }

        eventDialog.setOnKeyListener(DialogInterface.OnKeyListener { _: DialogInterface?, keyCode: Int, _: KeyEvent? ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                eventDialog.cancel()
                return@OnKeyListener true
            }
            false
        })
        eventDialog.window!!.setBackgroundDrawable(
            ColorDrawable(Color.TRANSPARENT)
        )

        // 이미지가 다 로드되면 다이얼로그 표시
        mGlideRequestManager
            ?.load(imgUrl)
            ?.centerInside()
            ?.into(object : CustomTarget<Drawable?>() {
                override fun onResourceReady(
                    resource: Drawable,
                    transition: Transition<in Drawable?>?
                ) {
                    imgEvent.setImageDrawable(resource)
                    eventDialog.show()
                }

                override fun onLoadCleared(placeholder: Drawable?) {
                }
            })
    }

    private fun saveNeverShowEvent(eventNo: String) {
        val neverEventList = Util.getPreference(this, Const.PREF_NEVER_SHOW_EVENT)
        val neverEventTotal: String

        val readNoticeArray =
            neverEventList.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        if (neverEventList == "") {
            neverEventTotal = neverEventList + eventNo
            Util.setPreference(
                applicationContext,
                Const.PREF_NEVER_SHOW_EVENT,
                neverEventTotal
            )
        } else if (!Util.isFoundString(eventNo, readNoticeArray)) {
            neverEventTotal = "$neverEventList,$eventNo"
            Util.setPreference(
                applicationContext,
                Const.PREF_NEVER_SHOW_EVENT,
                neverEventTotal
            )
        }
    }

    private fun tryShowLoginHeart(eventHeartModel: EventHeartModel) {
        val account: IdolAccount? = IdolAccount.getAccount(this)

        val tag = "filter_attendance"
        val oldFrag = supportFragmentManager.findFragmentByTag(tag)

        checkNotNull(account)
        if (eventHeartModel.dailyHeart + eventHeartModel.sorryHeart + account.mDailyPackHeart > 0) {
            mBottomSheetFragment = RewardBottomSheetDialogFragment.newInstance(
                RewardBottomSheetDialogFragment.FLAG_LOGIN_REWARD,
                eventHeartModel
            ) { isDailyPack: Boolean ->
                if (isDailyPack) {
                    goToDailyPackDetail()
                    return@newInstance Unit
                }
                startActivity(AttendanceActivity.createIntent(this))
                Unit
            }

            if (oldFrag == null) {
                mBottomSheetFragment?.show(supportFragmentManager, tag)
            }

            return
        }

        if (eventHeartModel.burning || !eventHeartModel.burningTime) {
            return
        }

        if (eventHeartModel.burningHeart <= 0) {
            return
        }

        mBottomSheetFragment = RewardBottomSheetDialogFragment.newInstance(
            RewardBottomSheetDialogFragment.FLAG_BURNING_REWARD,
            eventHeartModel
        ) { isDailyPack: Boolean ->
            if (isDailyPack) {
                return@newInstance Unit
            }
            startActivity(AttendanceActivity.createIntent(this))
            Unit
        }


        if (oldFrag == null) {
            mBottomSheetFragment?.show(supportFragmentManager, tag)
        }
    }

    //해당 게시물의 status값(성공여부)를 가져오기위해 호출.
    private fun getSupportList(targetId: Int) {
        Thread {
            supportRepository?.callGetSupports(
                100,
                0,
                null,
                null,
                null,
                null,
                { response: JSONObject ->
                    try {
                        if (response.optBoolean("success")) {
                            val array = response.getJSONArray("objects")
                            val gson: Gson = IdolGson.getInstance(true)
                            val items: ArrayList<SupportListModel> = ArrayList<SupportListModel>()

                            if (array.length() != 0) {
                                for (i in 0 until array.length()) {
                                    items.add(
                                        gson.fromJson<SupportListModel>(
                                            array.getJSONObject(i).toString(),
                                            SupportListModel::class.java
                                        )
                                    )

                                    //targetId와 서버에서 불러온 아이디값만 비교해서 같으면 넣어준다.`
                                    if (targetId == items[i].id) {
                                        supportModel = items[i]
                                    }
                                }
                            }

                            //비동기처리 늦게 될수도 있으니까 null체크 추가 그리고 0이면 기본상세페이지, 1이면 인증샷화면으로...
                            if (supportModel == null || supportModel?.status == 0) startActivity(
                                SupportDetailActivity.createIntent(
                                    this@MainActivity, targetId
                                )
                            )
                            else if (supportModel?.status == 1) {
                                //성공은 인증샷 페이지로 가기로한다.
                                startActivity(
                                    SupportPhotoCertifyActivity.createIntent(
                                        this@MainActivity,
                                        getSupportInfo(supportModel!!)
                                    )
                                )
                            }
                        } else {
                            UtilK.handleCommonError(this@MainActivity, response)
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                },
                { throwable: Throwable ->
                    if (Util.is_log()) showMessage(throwable.message)
                }
            )
        }.start()
    }

    private fun getSupportInfo(supportListModel: SupportListModel): String {
        //서포트  관련 필요 정보를  서포트 인증샷 화면에 json 화 시켜서 넘겨준다.

        val supportInfo = JSONObject()

        try {
            if (supportListModel.idol.getName(this@MainActivity).contains("_")) {
                supportInfo.put("name", Util.nameSplit(this@MainActivity, supportListModel.idol)[0])
                supportInfo.put(
                    "group",
                    Util.nameSplit(this@MainActivity, supportListModel.idol)[1]
                )
            } else {
                supportInfo.put("name", supportListModel.idol.getName(this@MainActivity))
            }
            supportInfo.put("support_id", supportListModel.id)
            supportInfo.put("title", supportListModel.title)
            supportInfo.put("profile_img_url", supportListModel.image_url)
        } catch (e: JSONException) {
            e.printStackTrace()
        }


        return supportInfo.toString()
    }

    private fun processAccessToken(uri: String?, approve: String?) {
        if (uri == null) {
            return
        }

        val parseUri = Uri.parse(uri)

        val type = parseUri.getQueryParameter("type")
        val clientId = parseUri.getQueryParameter("client_id")
        val scope = parseUri.getQueryParameter("scope")
        val redirectUri = parseUri.getQueryParameter("redirect_uri")

        //개발자들만 보는것이므로 다국어처리는 필요하지않음.
        if (redirectUri == null) {
            IdolSnackBar.Companion.make(findViewById<View>(android.R.id.content), "no callback url")
                .show()
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

                    //권한을 못받은 사람은 팝업을 뛰워줘서 권한을 요청합니다.
                    if (!success && gcode == Gcode.REQUIRE_USER_ACCESS.value) {
                        val icon = dataObject.optString("icon")
                        val name = dataObject.optString("name")

                        val gson: Gson = IdolGson.instance
                        val listType = object : TypeToken<List<AccessModel?>?>() {
                        }.type
                        val accessList: ArrayList<AccessModel> =
                            gson.fromJson<ArrayList<AccessModel>>(
                                Objects.requireNonNull<JSONArray>(dataObject.optJSONArray("access_list"))
                                    .toString(), listType
                            )

                        showAuthRequestDialog(icon, name, accessList, uri)
                        return@requestAccessToken
                    }

                    val responseRedirectUri = response.optString("redirect_uri")

                    val parsedRedirectUri = Uri.parse(responseRedirectUri)

                    val splitRedirectUri =
                        responseRedirectUri.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }
                            .toTypedArray()

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
                            Intent(Intent.ACTION_VIEW, Uri.parse(builder.build().toString()))
                        accessIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        accessIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(accessIntent)
                    } catch (e: ActivityNotFoundException) {
                        IdolSnackBar.Companion.make(
                            findViewById<View>(android.R.id.content),
                            "Invalid redirect_uri parameter: " + builder.build().toString()
                        ).show()
                        e.printStackTrace()
                    }
                },
                { throwable ->
                    IdolSnackBar.Companion.make(findViewById<View>(android.R.id.content), throwable.message)
                        .show()
                }
            )
        }
    }

    private fun showAuthRequestDialog(
        icon: String,
        name: String,
        accessList: List<AccessModel>,
        uri: String
    ) {
        Util.showRequestAuthDialog(
            this@MainActivity,
            mGlideRequestManager,
            icon,
            String.format(getString(R.string.popup_request_auth_title), name),
            accessList,
            { v1: View? ->
                //확인을 눌렀으면 다시하번 토큰 확인 프로세스 실행시켜준다.
                processAccessToken(uri, "Y")
                Util.closeIdolDialog()
            },
            { v2: View? -> Util.closeIdolDialog() })
    }


    private fun requestMobileAdConsent() {
        //디버그 테스트 세팅입니다. 테스트 필요할때 넣어주세요.
        val debugSettings: ConsentDebugSettings = ConsentDebugSettings.Builder(this)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA) //국가 설정(영국)
            .addTestDeviceHashedId("527E2B0848C2589FE4AE03F7D666A9FD") //기기 설정.
            .build()

        val params: ConsentRequestParameters =
            ConsentRequestParameters.Builder() //            .setConsentDebugSettings(debugSettings)
                .setTagForUnderAgeOfConsent(false)
                .build()

        consentInformation = UserMessagingPlatform.getConsentInformation(this)

        consentInformation?.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(
                    this
                ) { loadAndShowError: FormError? ->
                    if (loadAndShowError != null && BuildConfig.DEBUG) {
                        // Consent gathering failed.
                        val errorCode: Int = loadAndShowError.errorCode
                        val message: String = loadAndShowError.message
                        IdolSnackBar.make(
                            findViewById<View>(android.R.id.content),
                            "ErrorCode:" + errorCode + "Message:" + message
                        ).show()
                    }
                    // Consent has been gathered.
                    if (consentInformation?.canRequestAds() == true) {
                        initializeMobileAdsSdk()
                    }
                }
            },
            { requestConsentError: FormError ->
                // Consent gathering failed.
                if (BuildConfig.DEBUG) {
                    val errorCode: Int = requestConsentError.errorCode
                    val message: String = requestConsentError.message
                    IdolSnackBar.make(
                        findViewById<View>(android.R.id.content),
                        "ErrorCode:" + errorCode + "Message:" + message
                    ).show()
                }
            })

        if (consentInformation?.canRequestAds() == true) {
            initializeMobileAdsSdk()
        }
    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.getAndSet(true)) {
            return
        }

        try {
            MobileAds.initialize(this@MainActivity) {
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun setDrawerToolbar() {
        // drawer 메뉴 toolbar
        val menuToolbar = findViewById<Toolbar>(R.id.toolbar_menu)
        menuToolbar.setNavigationIcon(R.drawable.btn_navigation_back)
        menuToolbar.setTitle(R.string.title_tab_menu)
        menuToolbar.setNavigationOnClickListener { v: View? ->
            drawerLayout?.closeDrawers()
        }
        // 카테고리
        menuToolbar.inflateMenu(R.menu.drawer_menu)
        val btnNotification = menuToolbar.menu.findItem(R.id.menu_notification)
        val btnAttendance = menuToolbar.menu.findItem(R.id.menu_attendance_check)

        changeAttendanceIconStatus()

        btnNotification.setOnMenuItemClickListener { item: MenuItem? ->
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.MENU_PUSH_LOG.actionValue,
                GaAction.MENU_PUSH_LOG.label
            )
            startActivity(NotificationActivity.createIntent(this@MainActivity))
            true
        }
        btnAttendance.setOnMenuItemClickListener { item: MenuItem? ->
            setUiActionFirebaseGoogleAnalyticsActivity(
                GaAction.MENU_ATTENDANCE_CHECK.actionValue,
                GaAction.MENU_ATTENDANCE_CHECK.label
            )
            startActivity(AttendanceActivity.createIntent(this@MainActivity))
            true
        }
    }

    private fun changeAttendanceIconStatus() {
        val isAbleAttendance = Util.getPreferenceBool(this, Const.PREF_IS_ABLE_ATTENDANCE, false)
        
        val btnAttendance = binding?.toolbarMenu?.menu?.findItem(R.id.menu_attendance_check)

        val iconAttendance = ContextCompat.getDrawable(this, R.drawable.btn_navigation_attendance)
        val iconAttendanceDot =
            ContextCompat.getDrawable(this, R.drawable.btn_navigation_attendance_dot_celeb)
        btnAttendance?.setIcon(if (isAbleAttendance) iconAttendanceDot else iconAttendance)
    }

    private fun setTapFromAppLinkIntent(nextIntent: Intent) {
        if (nextIntent.getBooleanExtra(EXTRA_IS_HOF, false)) { //명전탭.
            binding?.btnHallOfFame?.callOnClick()
        } else if (nextIntent.getBooleanExtra(EXTRA_THEME_PICK, false)) {
            binding?.btnOnePick?.callOnClick()
        } else if (nextIntent.getBooleanExtra(EXTRA_IS_HEART, false)) {
            binding?.btnOnePick?.callOnClick()
        } else if (nextIntent.getBooleanExtra(
                EXTRA_IS_MENU,
                false
            )
        ) {
            drawerLayout?.openDrawer(GravityCompat.START)
        } else if (nextIntent.getBooleanExtra(EXTRA_IS_MIRACLE, false)) {
            binding?.btnMiracle?.callOnClick()
        } else if (nextIntent.getBooleanExtra(EXTRA_IS_MY_HEART_INFO, false)) {
            if (Util.mayShowLoginPopup(this)) return

            startActivity(MyHeartInfoActivity.createIntent(this))
        } else {
            // no-op
        }
    }

    private fun updateTutorial(tutorialIndex: Int) = lifecycleScope.launch {
        usersRepository.updateTutorial(
            tutorialIndex = tutorialIndex,
            listener = { response ->
                if (response.optBoolean("success")) {
                    Logger.d("Tutorial updated successfully: $tutorialIndex")
                    val bitmask = response.optLong("tutorial", 0L)
                    TutorialManager.init(bitmask)
                } else {
                    Toast.makeText(this@MainActivity, response.toString(), android.widget.Toast.LENGTH_SHORT).show()
                }
            },
            errorListener = { throwable ->
                Toast.makeText(this@MainActivity, throwable.message ?: "Error updating tutorial", android.widget.Toast.LENGTH_SHORT).show()
            }
        )
    }

    @get:SuppressLint("UnsafeIntentLaunch")
    private val nextIntent: Intent?
        get() {
            try {
                val intent = intent
                val nextIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(PARAM_NEXT_INTENT, Intent::class.java)
                } else {
                    intent.getParcelableExtra<Parcelable>(PARAM_NEXT_INTENT) as Intent?
                }

                return nextIntent
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // 다른 엑티비티에서 딥링크로 넘어왔을때 (예: 알림모아보기->뭔가 누른 경우)
        // drawer 닫기
        drawerLayout?.closeDrawers()

        val nextActivity = intent.getSerializableExtra(EXTRA_NEXT_ACTIVITY) as Class<*>?

        if (nextActivity == MainActivity::class.java) {
            setTapFromAppLinkIntent(intent)
        }
    }

    companion object {
        //딥링크로 실행될때 task 새롭게 실행해주고 기존 task들 지워준다. /clickFromIdolScreen -> 앱안에서 링크 클릭되었는지 여부 체크
        fun createIntentFromDeepLink(
            context: Context?,
            share: Boolean?,
            clickFromIdolScreen: Boolean
        ): Intent {
            val intent = Intent(context, MainActivity::class.java)
            if (clickFromIdolScreen) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            } else { //외부 링크 클릭시에는  태스크 clear  해줌.
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            intent.putExtra(OTHER_APP_SHARE, share)
            intent.putExtra(IS_DEEP_LINK_CLICK_FROM_IDOL, clickFromIdolScreen)
            return intent
        }


        fun createIntent(context: Context?, isSolo: Boolean, isMale: Boolean): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            intent.putExtra(PARAM_IS_SOLO, isSolo)
            intent.putExtra(PARAM_IS_MALE, isMale)
            return intent
        }

        private var idolDialog: Dialog? = null
        private const val PARAM_IS_SOLO = "paramIsSolo"
        private const val PARAM_IS_MALE = "paramIsMale"

        const val IS_DEEP_LINK_CLICK_FROM_IDOL: String = "click_from_idol"
        private const val REQUEST_CODE_DEEP_LINK = 1001
        private var mDeadline: AppCompatTextView? = null
        private var begin: Date? = null
        private var end: Date? = null
        private var aggregatingTime: String? = null
        private var aggregatingTimeFormatOne: String? = null
        private var aggregatingTimeFormatFew: String? = null

        private const val OTHER_APP_SHARE = "share"

        const val TAG_SUMMARY: String = "summary"
        const val TAG_MIRACLE: String = "miracle"
        const val TAG_THEME: String = "theme"
        const val TAG_HOF: String = "hof"
        const val TAG_FAVORITE: String = "favorite"

        fun createIntent(context: Context?, share: Boolean?): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            intent.putExtra(OTHER_APP_SHARE, share)
            return intent
        }
    }
}
