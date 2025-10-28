/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.activity

import android.animation.Animator
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.viewModels
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media3.common.util.UnstableApi
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ib.mn.BuildConfig
import net.ib.mn.IdolApplication
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.adapter.CommunityPagerAdapter
import net.ib.mn.admanager.AdManager
import net.ib.mn.chatting.ChattingRoomListFragment
import net.ib.mn.databinding.ActivityCommunityBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.IdolCommunityDialogFragment
import net.ib.mn.fragment.CommunityFragment
import net.ib.mn.fragment.ScheduleFragment
import net.ib.mn.model.IdolModel
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.data_resource.awaitOrThrow
import net.ib.mn.data_resource.mapDataResource
import net.ib.mn.domain.usecase.GetIdolByIdUseCase
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.model.toPresentation
import net.ib.mn.schedule.IdolSchedule
import net.ib.mn.smalltalk.SmallTalkFragment
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.CelebTutorialBits
import net.ib.mn.utils.Const
import net.ib.mn.utils.IdolSnackBar
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Logger
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.UploadSingleton
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.VideoAdUtil
import net.ib.mn.utils.ext.applyNavigationBarInset
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.ext.getUiColor
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.setColorFilterSrcIn
import net.ib.mn.utils.setMargins
import net.ib.mn.viewmodel.CommunityActivityViewModel
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.math.abs

@UnstableApi
@AndroidEntryPoint
class CommunityActivity : BaseActivity(), OnClickListener, BaseDialogFragment.DialogResultHandler {

    lateinit var binding: ActivityCommunityBinding
    private lateinit var mGlideRequestManager: RequestManager

    // BottomSheet에서 가져다쓰고 있어서 public
    lateinit var communityFragment: CommunityFragment
    lateinit var smallTalkFragment: SmallTalkFragment

    lateinit var chattingRoomListFragment: ChattingRoomListFragment
    lateinit var scheduleFragment: ScheduleFragment

    private var fragments = arrayListOf<Fragment>()

    private var refreshJob: Job? = null

    private val communityActivityViewModel: CommunityActivityViewModel by viewModels()

    @Inject
    lateinit var idolApiManager: IdolApiManager

    @Inject
    lateinit var getIdolByIdUseCase: GetIdolByIdUseCase

    @Inject
    lateinit var videoAdUtil: VideoAdUtil

    private val mBroadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (!Const.USE_ANIMATED_PROFILE) return

            val index = intent.getIntExtra("index", 0)

            val views = listOf(
                binding.communityHeader.headerPlayerview1 to binding.communityHeader.photo1,
                binding.communityHeader.headerPlayerview2 to binding.communityHeader.photo2,
                binding.communityHeader.headerPlayerview3 to binding.communityHeader.photo3
            )

            if (views[index].first.visibility != View.VISIBLE || views[index].second.visibility != View.INVISIBLE) {
                views[index].second.visibility = View.INVISIBLE
                views[index].first.visibility = View.VISIBLE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityBinding.inflate(layoutInflater)

        setupEdgeToEdge()
        setContentView(binding.root)

        init()
        setListenerEvent()
    }

    private fun setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding.clCommunity.applySystemBarInsets()

        binding.btnWrite.applyNavigationBarInset()
    }

    private fun setListenerEvent() {
        binding.appBarLayout.addOnOffsetChangedListener { appBarLayout, verticalOffset ->
            if (abs(verticalOffset) - appBarLayout.totalScrollRange == 0) {
                if (binding.toolbar.toolbarCommunity.visibility != View.VISIBLE) {    //여러번 호출해서 안 보일 때 한번만 호출하도록 추가
                    baseStopExoPlayer(binding.communityHeader.headerPlayerview1)
                    baseStopExoPlayer(binding.communityHeader.headerPlayerview2)
                    baseStopExoPlayer(binding.communityHeader.headerPlayerview3)
                }
                // Collapsed
                binding.toolbar.toolbarCommunity.visibility = View.VISIBLE
            } else {
                if (binding.toolbar.toolbarCommunity.visibility != View.GONE) {  //여러번 호출해서 보일때 한번만 호출하도록 추가
                    startExoPlayers()
                }
                // Expand
                binding.toolbar.toolbarCommunity.visibility = View.GONE
            }
        }
    }

    private fun init() {
        mGlideRequestManager = Glide.with(this)

        communityActivityViewModel.handleIntent(intent)

        setDataOfVM()
        getDataFromVM()
        if (BuildConfig.CELEB) {
            setCelebTutorial(true)
        } else {
            setTutorial()
        }
        setDefaultTab()
        binding.tabLayout.post {
            showLottieOnTabs(binding.tabLayout, this)
        }
        selectTabIcon()
        setToolBar()
        profileInit()
        setPurchasedDailyPackStatus()
        communityActivityViewModel.loadFavorites(this)

        with(binding) {
            communityHeader.more.setOnClickListener(this@CommunityActivity)
            toolbar.more.setOnClickListener(this@CommunityActivity)
            toolbar.rlBtnBack.setOnClickListener(this@CommunityActivity)
            communityHeader.profile.setOnClickListener(this@CommunityActivity)
            communityHeader.btnBack.setOnClickListener(this@CommunityActivity)
            clToolTip.setOnClickListener(this@CommunityActivity)
            binding.btnWrite.setOnClickListener(this@CommunityActivity)
        }
    }

    private fun setToolBar() {
        UtilK.setName(
            this@CommunityActivity,
            communityActivityViewModel.idolModel.value!!,
            binding.toolbar.name,
            binding.toolbar.group
        )
    }

    override fun onDestroy() {
        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.destroy()
        }

        super.onDestroy()
        try {
            player1?.release()
            player2?.release()
            player3?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        IdolSchedule.getInstance().clearSchedule()
    }

    override fun onStop() {
        FLAG_CLOSE_DIALOG = false
        super.onStop()
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver)
        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.pause()
        }
        // 움짤 멈추기
        if (Const.USE_ANIMATED_PROFILE) {
            baseStopExoPlayer(binding.communityHeader.headerPlayerview1)
            baseStopExoPlayer(binding.communityHeader.headerPlayerview2)
            baseStopExoPlayer(binding.communityHeader.headerPlayerview3)
        }

        idolApiManager.stopTimer()
        stopTimer()

        super.onPause()
    }

    override fun onResume() {
        if (!BuildConfig.CHINA) {
            AdManager.getInstance().adManagerView?.resume()
        }

        //상단 CollapsingToolbarLayout Expand상태일때 , 백그라운드에서 다시 돌아올때 플레이어 재실행
        if (binding.toolbar.toolbarCommunity.visibility == View.GONE) {
            startExoPlayers()
        }
        val filter = IntentFilter()
        if (Const.USE_ANIMATED_PROFILE) {
            filter.addAction(Const.PLAYER_START_RENDERING)
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(
            mBroadcastReceiver,
            filter
        )

        idolApiManager.startTimer()
        startTimer()

        super.onResume()
    }

    private fun setDataOfVM() {
        communityActivityViewModel.apply {
            setIdolAccount(this@CommunityActivity)
            setIsMost()
            setIsShowChattingTab()
            setMostCount(this@CommunityActivity)
        }
    }

    private fun getDataFromVM() {
        communityActivityViewModel.apply {
            mostCount.observe(
                this@CommunityActivity,
                SingleEventObserver {
                    with(binding.communityHeader) {
                        ivMostCount.visibility = View.VISIBLE
                        tvMostCount.text = it
                    }
                }
            )

            changeTop3.observe(
                this@CommunityActivity,
                SingleEventObserver {
                    communityActivityViewModel.idolModel.value?.let { it1 ->
                        setProfileThumb(it1)
                        setBanner(it1)
                    }
                },
            )
            changeMost.observe(
                this@CommunityActivity,
                SingleEventObserver {
                    communityActivityViewModel.setIdolAccount(this@CommunityActivity)
                    communityActivityViewModel.setIsShowChattingTab()
                    changeChattingTab()
                }
            )

            //검색결과에서 넘어올 경우, category 값을 넘겨주는데 넘어온 값에 따라 해당 화면 보여주도록 처리
            intentcategory.observe(
                this@CommunityActivity,
                SingleEventObserver {
                    if (!it.isNullOrEmpty()) {
                        when (it) {
                            CATEGORY_COMMUNITY -> {
                                binding.viewPager2.setCurrentItem(fragments.indexOf(communityFragment), false)
                            }
                            CATEGORY_IDOLTALK -> {
                                binding.viewPager2.setCurrentItem(fragments.indexOf(chattingRoomListFragment), false)

                            }
                            CATEGORY_SCHEDULE -> {
                                binding.viewPager2.setCurrentItem(fragments.indexOf(scheduleFragment), false)
                            }
                            CATEGORY_SMALL_TALK -> {
                                binding.viewPager2.setCurrentItem(fragments.indexOf(smallTalkFragment), false)
                            }
                        }
                    }
                }
            )

            //NewIntent 탈 경우 처리
            newIntentCategory.observe(
                this@CommunityActivity,
                SingleEventObserver {
                    if(!it.isNullOrEmpty()) {
                        when (it) {
                            CATEGORY_COMMUNITY -> {
                                binding.viewPager2.setCurrentItem(fragments.indexOf(communityFragment), false)
                                communityActivityViewModel.recentCommunityArticle()
                            }

                            CATEGORY_SMALL_TALK -> {
                                binding.viewPager2.setCurrentItem(fragments.indexOf(smallTalkFragment), false)
                                communityActivityViewModel.recentSmallTalkArticle()
                            }
                        }
                    }
                }
            )

            push.observe(
                this@CommunityActivity,
                SingleEventObserver {
                    if(it == true) {
                        communityActivityViewModel.setPushType(intent.getStringExtra(PUSH_TYPE))
                    }
                }
            )

            // 푸시 로 넘어온 경우,  채팅 푸시로 넘어온건지 스케쥴 푸시로 넘어온건지 비교하여 해당 하는 탭을  보여준다.
            pushType.observe(
                this@CommunityActivity,
                SingleEventObserver {
                    when (it) {
                        Const.PUSH_CHANNEL_ID_SCHEDULE -> {
                            binding.viewPager2.setCurrentItem(fragments.indexOf(scheduleFragment), false)
                        }

                        Const.PUSH_CHANNEL_ID_CHATTING_MSG_RENEW -> {
                            binding.viewPager2.setCurrentItem(fragments.indexOf(chattingRoomListFragment), false)
                        }
                    }
                }
            )
        }
    }

    fun startTimer() {
        refreshJob?.cancel()

        refreshJob = lifecycleScope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                val idolModel = communityActivityViewModel.idolModel.value
                if (idolModel == null) {
                    delay(5000L)
                    continue
                }

                try {
                    val updateIdol = withContext(Dispatchers.IO) {
                        getIdolByIdUseCase(idolModel.getId())
                            .mapDataResource { it?.toPresentation() }
                            .awaitOrThrow()
                    }

                    updateIdol?.let {
                        setProfileThumb(it)
                        setBanner(it)
                    }

                } catch (e: Exception) {
                    Logger.e("startTimer: error while updating idolModel - ${e.message}")
                }

                delay(5_000L)
            }
        }
    }

    fun stopTimer() {
        refreshJob?.cancel()
        refreshJob = null
    }

    override fun onBackPressed() {
        try {
            if (IdolApplication.getInstance(this).mainActivity == null &&
                !intent.getBooleanExtra(MainActivity.IS_DEEP_LINK_CLICK_FROM_IDOL, false)
            ) {
                // 딥링크 이동할때 main 없어지는데 이때는 Main 을 다시 살릴 필요없으므로, removeExtra 시킨다.
                intent.removeExtra(MainActivity.IS_DEEP_LINK_CLICK_FROM_IDOL)
                startActivity(MainActivity.createIntent(this, false))
                finish()
            } else {
                super.onBackPressed()
            }
        } catch (e: Exception) {
            super.onBackPressed()
        }
    }

    // 최애인지에 따라 보여줄 Fragment return 해주는 함수
    private fun addFragment(): List<Fragment> {
        communityFragment = CommunityFragment()
        smallTalkFragment = SmallTalkFragment()
        chattingRoomListFragment = ChattingRoomListFragment()
        scheduleFragment = ScheduleFragment()

        fragments = arrayListOf(
            communityFragment,
            smallTalkFragment,
            chattingRoomListFragment,
            scheduleFragment,
        )
        if (!communityActivityViewModel.isShowChattingTab()) {
            fragments.remove(chattingRoomListFragment)
        }

        for (fragment in fragments) {
            val bundle = Bundle()
            bundle.putSerializable(PARAM_IDOL, communityActivityViewModel.idolModel.value)
            fragment.arguments = bundle
        }
        return fragments.distinct()
    }

    private fun setDefaultTab() {
        if (BuildConfig.CELEB) {
            mType = UtilK.getTypeList(
                this,
                communityActivityViewModel.idolModel.value?.type + communityActivityViewModel.idolModel.value?.category
            )
        }
        
        binding.viewPager2.apply {
            adapter = communityActivityViewModel.idolModel.value?.let {
                CommunityPagerAdapter(
                    this@CommunityActivity, addFragment(),
                    it
                )
            }
            isUserInputEnabled = false // 좌우 스크롤 막는 코드
            offscreenPageLimit = fragments.size - 1
        }

        val titles = getTabTitles()

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.text = titles[position]
        }.attach()

        if (communityActivityViewModel.idolModel.value?.category == "B") {
            binding.clTabLayout.visibility = View.GONE
        }
    }

    private fun showLottieOnTabs(tabLayout: TabLayout, context: Context) {

        val index = if (BuildConfig.CELEB) {
            when(TutorialManager.getTutorialIndex()) {
                CelebTutorialBits.COMMUNITY_FAN_TALK -> 1
                CelebTutorialBits.COMMUNITY_CHAT -> if (tabLayout.tabCount == 4) 2 else -1
                CelebTutorialBits.COMMUNITY_SCHEDULE -> if (tabLayout.tabCount == 4) 3 else 2
                else -> -1
            }
        } else {
            when(TutorialManager.getTutorialIndex()) {
                TutorialBits.COMMUNITY_FANTALK -> 1
                TutorialBits.COMMUNITY_CHAT -> if (tabLayout.tabCount == 4) 2 else -1
                TutorialBits.COMMUNITY_SCHEDULE -> if (tabLayout.tabCount == 4) 3 else 2
                else -> -1
            }
        }

        val slidingTabStrip = tabLayout.getChildAt(0) as? ViewGroup ?: return

        for (i in 0 until tabLayout.tabCount) {
            if (i != index) continue

            val tabView = slidingTabStrip.getChildAt(i) as? ViewGroup ?: continue

            // Lottie가 이미 있는 경우 중복 추가 방지
            if (tabView.findViewWithTag<View>("lottie_$i") != null) continue

            val lottie = LottieAnimationView(context).apply {
                setAnimation("tutorial_heart.json")
                repeatCount = LottieDrawable.INFINITE
                playAnimation()

                val size = TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP,
                    28f,
                    context.resources.displayMetrics
                ).toInt()

                layoutParams = FrameLayout.LayoutParams(
                    size,
                    size,
                    Gravity.CENTER
                )
                tag = "lottie_$i"

                setOnClickListener {
                    setAnimation("tutorial_heart_touch.json")
                    repeatCount = 0
                    playAnimation()

                    removeAllAnimatorListeners()
                    addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}

                        override fun onAnimationEnd(animation: Animator) {
                            communityActivityViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                            handleTabSelection(i)

                            removeAllAnimatorListeners()
                            cancelAnimation()
                            progress = 0f // 상태 초기화
                            visibility = View.GONE
                        }

                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                }
            }

            // ✅ 기존 tabView 제거
            slidingTabStrip.removeView(tabView)

            val frameLayout = FrameLayout(context).apply {
                layoutParams = tabView.layoutParams
                tabView.layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addView(tabView) // 제거 후 안전하게 추가
                addView(lottie)
            }

            // ✅ frameLayout을 해당 위치에 다시 삽입
            slidingTabStrip.addView(frameLayout, i)
        }
    }

    private fun handleTabSelection(tabIndex: Int) {
        binding.viewPager2.currentItem = tabIndex

        val tab = binding.tabLayout.getTabAt(tabIndex)
        if (BuildConfig.CELEB) {
            tab?.icon?.setColorFilterSrcIn(
                Color.parseColor(mType?.getUiColor(this@CommunityActivity))
            )
        }

        binding.lottieTutorialCommunityWrite.visibility = View.GONE

        if (tabIndex > 1) {
            binding.lottieTutorialCommunityWrite.visibility = View.GONE
        } else {
            if (BuildConfig.CELEB) {
                setCelebTutorial()
            } else {
                setTutorial()
            }
        }

        // 채팅 탭 관련 UI 처리
        if (tabIndex == 2 && currentFragment(2) == chattingRoomListFragment) {
            binding.btnWrite.setBackgroundResource(R.drawable.btn_add_chat)
            if (Util.getPreferenceBool(
                    this@CommunityActivity,
                    Const.PREF_SHOW_CREATE_CHATTING_ROOM,
                    true
                )
            ) {
                binding.clToolTip.visibility = View.VISIBLE
            }
        } else {
            // 다른 탭에서는 전경 아이콘을 지워 배경만 보이게 합니다.
            binding.btnWrite.setBackgroundResource(R.drawable.btn_write_contents)
            binding.clToolTip.visibility = View.GONE
        }
    }

    // tab 클릭 시 아이콘 색 변경하는 함수
    private fun selectTabIcon() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                if (BuildConfig.CELEB) {
                    if (TutorialManager.getTutorialIndex() == CelebTutorialBits.COMMUNITY_WRITE) {
                        binding.lottieTutorialCommunityWrite.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                    } else if (TutorialManager.getTutorialIndex() == CelebTutorialBits.FAN_TALK_WRITE) {
                        binding.lottieTutorialCommunityWrite.visibility = if (tab.position == 1) View.VISIBLE else View.GONE
                    }
                } else {
                    if (TutorialManager.getTutorialIndex() == TutorialBits.COMMUNITY_WRITE) {
                        binding.lottieTutorialCommunityWrite.visibility = if (tab.position == 0) View.VISIBLE else View.GONE
                    } else if (TutorialManager.getTutorialIndex() == TutorialBits.COMMUNITY_FAN_TALK_WRITE) {
                        binding.lottieTutorialCommunityWrite.visibility = if (tab.position == 1) View.VISIBLE else View.GONE
                    }
                }

                val lottieView = binding.tabLayout
                    .getChildAt(0)
                    .findViewWithTag<LottieAnimationView>("lottie_${tab.position}")

                if (lottieView?.isVisible == true) {
                    lottieView.performClick() // Lottie의 setOnClickListener 동작 실행
                    return
                }

                binding.viewPager2.currentItem = tab.position
                if (BuildConfig.CELEB) {
                    tab.icon?.setColorFilterSrcIn(
                        Color.parseColor(mType?.getUiColor(this@CommunityActivity))
                    )
                }
                // 작성 버튼 툴팁은 채팅 탭에만 존재 하는데 채팅방 탭을 클릭 했을 때 툴팁이 나오는 상황인 지 아닌 지에 따라 처리
                if (tab.position == 2 && currentFragment(2) == chattingRoomListFragment) {
                    binding.btnWrite.setBackgroundResource(R.drawable.btn_add_chat)
                    binding.btnWrite.setImageDrawable(null)
                    if (Util.getPreferenceBool(
                            this@CommunityActivity,
                            Const.PREF_SHOW_CREATE_CHATTING_ROOM,
                            true
                        )
                    ) {
                        binding.clToolTip.visibility = View.VISIBLE
                    }
                } else {
                    binding.btnWrite.setBackgroundResource(R.drawable.btn_write_contents)
                    binding.btnWrite.setImageDrawable(null)
                    binding.clToolTip.visibility = View.GONE
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                if (tab != null) {
                    communityActivityViewModel.clickTab(tab.position)
                }
            }
        })
    }

    // 현재 fragment 반환해주는 함수
    private fun currentFragment(position: Int): Fragment {
        return when (position) {
            0 -> {
                communityFragment
            }

            1 -> {
                smallTalkFragment
            }

            2 -> {
                if (!communityActivityViewModel.isShowChattingTab()) {
                    scheduleFragment
                } else {
                    chattingRoomListFragment
                }
            }

            else -> {
                scheduleFragment
            }
        }
    }

    private fun setPurchasedDailyPackStatus() {
        // 데일리팩 구독 여부에 따라 광고 세팅
        setPurchasedDailyPackFlag(communityActivityViewModel.idolAccount)
        if (!communityActivityViewModel.getPurchasedDailyPack() && !BuildConfig.CHINA) {
            val adManager = AdManager.getInstance()
            with(adManager) {
                setAdManagerSize(this@CommunityActivity, binding.viewPager2)
                setAdManager(this@CommunityActivity)
                loadAdManager()
            }
        }
    }

    private fun profileInit() {
        with(binding.communityHeader) {
            UtilK.setName(
                this@CommunityActivity,
                communityActivityViewModel.idolModel.value!!,
                name,
                group
            )
            setProfileThumb(communityActivityViewModel.idolModel.value!!)
            setBanner(communityActivityViewModel.idolModel.value!!)

            // 비밀의 방 더보기 버튼 숨김처리 - 운영팀 요청(25.09.04)
            more.visibility = if (communityActivityViewModel.idolModel.value!!.getId() == Const.NON_FAVORITE_IDOL_ID) View.GONE else View.VISIBLE

            when (communityActivityViewModel.idolModel.value!!.anniversary) {
                Const.ANNIVERSARY_BIRTH -> {
                    if (communityActivityViewModel.idolModel.value!!.type == "S" || BuildConfig.CELEB) {
                        badgeBirth.visibility = View.VISIBLE
                        badgeDebut.visibility = View.GONE
                    } else { // 그룹은 데뷔뱃지로 보여주기
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
                    val memorialDayCount = if (Util.isRTL(this@CommunityActivity)) {
                        NumberFormat.getNumberInstance(LocaleUtil.getAppLocale(this@CommunityActivity)).format(
                            communityActivityViewModel.idolModel.value?.anniversaryDays,
                        )
                    } else {
                        communityActivityViewModel.idolModel.value?.anniversaryDays.toString()
                    }
                    badgeMemorialDay.text = memorialDayCount.replace(("[^\\d.]").toRegex(), "")
                        .plus(getString(R.string.lable_day))
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
            communityActivityViewModel.idolModel.value?.let { idolModel ->
                with(idolModel) {
                    UtilK.profileRoundBorder(
                        miracleCount,
                        fairyCount,
                        angelCount,
                        photoBorder
                    )
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnWrite -> {
                if (binding.lottieTutorialCommunityWrite.isVisible) return
                onClickWrite()
            }

            binding.communityHeader.more, binding.toolbar.more -> {
                if (binding.communityHeader.lottieTutorialCommunityMore.isVisible) return
                onClickMore()
            }

            binding.toolbar.rlBtnBack -> {
                finish()
            }

            binding.communityHeader.profile -> {
                if (binding.communityHeader.lottieTutorialCommunityProfile.isVisible) return
                onClickCommunityHeaderProfile()
            }

            binding.communityHeader.btnBack -> {
                finish()
            }

            binding.clToolTip -> {
                binding.clToolTip.visibility = View.GONE
                Util.setPreference(this, Const.PREF_SHOW_CREATE_CHATTING_ROOM, false)
            }
        }
    }

    private fun onClickCommunityHeaderProfile() {
        if (BuildConfig.CELEB) {
            showIdolCommunityDialog()
        } else {
            startActivity(
                WikiActivity.createIntent(
                    this,
                    communityActivityViewModel.idolModel.value!!,
                ),
            )
        }
    }

    private fun showIdolCommunityDialog() {
        val showDialogIdolCommunity = IdolCommunityDialogFragment.getInstance()
        showDialogIdolCommunity.setActivityRequestCode(RequestCode.CHAT_ROOM_CREATE.value)
        showDialogIdolCommunity.show(supportFragmentManager, "show_dialog_idol_community")
    }

    private fun setPurchasedDailyPackFlag(account: IdolAccount?) {
        communityActivityViewModel.setPurchasedDailyPack(false)
        account?.userModel?.subscriptions?.forEach { mySubscription ->
            if (mySubscription.familyappId == 1 ||
                mySubscription.familyappId == 2 ||
                mySubscription.skuCode == Const.STORE_ITEM_DAILY_PACK
            ) {
                communityActivityViewModel.setPurchasedDailyPack(true)
                return@forEach
            }
        }
    }

    private fun setBanner(idol: IdolModel) {
        val imageUrlList = UtilK.getTop3ImageUrl(this, idol)

        val resizeImageUrls = listOf(
            if (imageUrlList[0].isNullOrEmpty()) idol.imageUrl else imageUrlList[0],
            if (imageUrlList[1].isNullOrEmpty()) idol.imageUrl2 else imageUrlList[1],
            if (imageUrlList[2].isNullOrEmpty()) idol.imageUrl3 else imageUrlList[2]
        )

        val photoViews = listOf(
            binding.communityHeader.photo1,
            binding.communityHeader.photo2,
            binding.communityHeader.photo3
        )
        resizeImageUrls.forEachIndexed { index, url ->
            Util.loadGif(mGlideRequestManager, url, photoViews[index])
        }

        if (idol.imageUrl.isNullOrEmpty()) {
            mGlideRequestManager.load(Util.noProfileThemePickImage(idol.getId()))
                .into(binding.communityHeader.photo1)
            mGlideRequestManager.load(Util.noProfileThemePickImage(idol.getId()))
                .into(binding.communityHeader.photo2)
            mGlideRequestManager.load(Util.noProfileThemePickImage(idol.getId()))
                .into(binding.communityHeader.photo3)
        }
    }

    // 최애 바꿨을 경우 채팅 탭 visibility 바뀌어서 다시 세팅
    private fun changeChattingTab() {
        // Lottie 등으로 View 계층이 변경된 후 탭을 재구성할 때 발생할 수 있는 ClassCastException을 방지하기 위해 모든 View를 제거
        (binding.tabLayout.getChildAt(0) as? ViewGroup)?.removeAllViews()

        binding.viewPager2.apply {
            adapter = CommunityPagerAdapter(
                this@CommunityActivity,
                addFragment(),
                communityActivityViewModel.idolModel.value!!
            )
            isUserInputEnabled = false // 좌우 스크롤 막는 코드
            offscreenPageLimit = fragments.size - 1
        }

        val titles = getTabTitles()

        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            tab.text = titles[position]
        }.attach()

        selectTabIcon()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            RequestCode.ENTER_WIDE_PHOTO.value -> {
                if (resultCode == RESULT_CANCELED && !communityActivityViewModel.getPurchasedDailyPack() && !BuildConfig.CHINA) {
                    AdManager.getInstance().loadAdManager()
                }
            }
        }
    }

    @OptIn(UnstableApi::class)
    private fun startExoPlayers() {
        val playerViewList = listOf(
            binding.communityHeader.headerPlayerview1,
            binding.communityHeader.headerPlayerview2,
            binding.communityHeader.headerPlayerview3,
        )

        val imageViewList = listOf(
            binding.communityHeader.photo1,
            binding.communityHeader.photo2,
            binding.communityHeader.photo3,
        )

        val imageUrlList = listOf(
            communityActivityViewModel.idolModel.value?.imageUrl,
            communityActivityViewModel.idolModel.value?.imageUrl2,
            communityActivityViewModel.idolModel.value?.imageUrl3,
        )

        playerViewList.indices.forEach { index ->
            // 깜빡임 현상 방지를 위해 움짤이 있을 때에만 명시적으로 플레이어 실행
            if(UtilK.canPlayUmjjal(imageUrlList[index])) {
                Handler(Looper.getMainLooper()).postDelayed({
                    basePlayExoPlayer(
                        index,
                        playerViewList[index],
                        imageViewList[index],
                        imageUrlList[index]
                    )
                }, 200)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == MEZZO_PLAYER_REQ_CODE) {
            Util.handleVideoAdResult(
                this,
                false,
                true,
                requestCode,
                resultCode,
                data,
                "community_videoad",
            ) { adType ->
                videoAdUtil.onVideoSawCommon(this, true, adType, null)
            }
        }
    }

    //해당화면에 있을 때 Intent값이 넘어왔을 경우,
    // 최신순으로 게시글 바꾸기 위해

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        communityActivityViewModel.setNewIntentCategory(intent.getStringExtra(PARAM_CATEGORY))
        val isRecent = intent.getBooleanExtra(PARAM_ARTICLE_RECENT, false)
        communityActivityViewModel.setIsRecent(isRecent)
    }

    private fun setProfileThumb(idol: IdolModel) {
        val top3Ids = UtilK.getTop3Ids(idol)

        var profileThumb: String? = ""

        if (top3Ids[0].isNullOrEmpty()) {
            profileThumb = idol.imageUrl

            // 다이얼로그 프로필 용 썸네일
            communityActivityViewModel.setDialogProfileThumb(idol.imageUrl)
        } else {
            profileThumb = UtilK.top1ImageUrl(this, idol, Const.IMAGE_SIZE_LOWEST)

            // 다이얼로그 프로필 용 썸네일
            communityActivityViewModel.setDialogProfileThumb(
                UtilK.top1ImageUrl(
                    this,
                    idol,
                    Const.IMAGE_SIZE_MEDIUM
                )
            )
        }

        val idolId = idol.getId()
        mGlideRequestManager
            .load(profileThumb)
            .apply(RequestOptions.circleCropTransform())
            .fallback(Util.noProfileImage(idolId))
            .error(Util.noProfileImage(idolId))
            .dontAnimate()
            .into(binding.communityHeader.photo)
    }

    private fun getTabTitles() : List<String> {
        val currentIdol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(PARAM_IDOL, IdolModel::class.java)
        } else {
            intent.getSerializableExtra(PARAM_IDOL) as IdolModel
        }

        var fandomName = currentIdol?.getFdName(this)
        fandomName = if (fandomName.isNullOrEmpty()) {
            getString(R.string.community_board)
        } else {
            getString(R.string.community_board2, fandomName)
        }
        // 탭 제목을 동적으로 구성
        val titles = mutableListOf(getString(R.string.community_feed), fandomName)
        if (communityActivityViewModel.isShowChattingTab()) {
            titles += getString(R.string.community_chat)
        }
        titles += getString(R.string.community_schedule)

        return titles
    }

    private fun onClickWrite() {
        when (currentFragment(binding.viewPager2.currentItem)) {
            communityFragment -> {
                if (UploadSingleton.isLive()) {
                    IdolSnackBar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.upload_in_progress)
                    ).show()
                    return
                }
                communityActivityViewModel.articleWrite()
            }

            smallTalkFragment -> {
                if (UploadSingleton.isLive()) {
                    IdolSnackBar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.upload_in_progress)
                    ).show()
                    return
                }
                communityActivityViewModel.smallTalkWrite()
            }

            chattingRoomListFragment -> {
                communityActivityViewModel.chattingRoomWrite()
            }

            else -> {
                communityActivityViewModel.scheduleWrite()
            }
        }
    }

    private fun onClickMore() {
        if (!Util.mayShowLoginPopup(this@CommunityActivity)) {
            setUiActionFirebaseGoogleAnalyticsActivity(
                Const.ANALYTICS_BUTTON_PRESS_ACTION,
                "community_menu",
            )
            showIdolCommunityDialog()
        }
    }

    private fun setTutorial() {
        when(TutorialManager.getTutorialIndex()) {
            TutorialBits.COMMUNITY_WIKI -> {
                setupLottieTutorial(binding.communityHeader.lottieTutorialCommunityProfile) {
                    communityActivityViewModel.updateTutorial(TutorialBits.COMMUNITY_WIKI)
                    onClickCommunityHeaderProfile()
                }
            }
            TutorialBits.COMMUNITY_WRITE, TutorialBits.COMMUNITY_FAN_TALK_WRITE -> {
                setupLottieTutorial(binding.lottieTutorialCommunityWrite) {
                    communityActivityViewModel.updateTutorial(TutorialManager.getTutorialIndex())
                    onClickWrite()
                }
                if (TutorialManager.getTutorialIndex() == TutorialBits.COMMUNITY_FAN_TALK_WRITE) {
                    binding.lottieTutorialCommunityWrite.visibility = View.GONE
                }
            }
            TutorialBits.COMMUNITY_MORE -> {
                setupLottieTutorial(binding.communityHeader.lottieTutorialCommunityMore) {
                    communityActivityViewModel.updateTutorial(TutorialBits.COMMUNITY_MORE)
                    onClickMore()
                }
            }
        }
    }

    private fun setCelebTutorial(isInitial: Boolean = false) {
        when(val tutorialIndex = TutorialManager.getTutorialIndex()) {
            CelebTutorialBits.COMMUNITY_WRITE -> {
                setupLottieTutorial(binding.lottieTutorialCommunityWrite) {
                    communityActivityViewModel.updateTutorial(tutorialIndex)
                    onClickWrite()
                }
            }
            CelebTutorialBits.FAN_TALK_WRITE -> {
                setupLottieTutorial(binding.lottieTutorialCommunityWrite) {
                    communityActivityViewModel.updateTutorial(tutorialIndex)
                    onClickWrite()
                }

                if (isInitial) {
                    binding.lottieTutorialCommunityWrite.visibility = View.GONE
                }
            }
            CelebTutorialBits.COMMUNITY_MORE -> {
                setupLottieTutorial(binding.communityHeader.lottieTutorialCommunityMore) {
                    communityActivityViewModel.updateTutorial(tutorialIndex)
                    onClickMore()
                }
            }
        }
    }

    companion object {
        const val PARAM_ARTICLE = "article"
        const val PARAM_ARTICLE_POSITION = "article_position"
        const val PARAM_ARTICLE_REPORT_POSITION = "article_report_position"
        const val PARAM_IDOL = "idol"
        const val PARAM_CATEGORY = "category"
        const val PARAM_ARTICLE_RECENT = "article_recent"
        const val CATEGORY_COMMUNITY = "community"
        const val CATEGORY_IDOLTALK = "idoltalk"
        const val CATEGORY_SCHEDULE = "schedule"
        const val CATEGORY_SMALL_TALK = "smalltalk"
        const val PARAM_EVENT_HEART = "event_heart"
        const val PUSH = "push"
        const val PUSH_TYPE = "push_type"
        const val PARAM_IS_WALLPAPER = "is_wallpaper"

        private const val REQUEST_CODE_EDIT_SCHEDULE = 16
        var mType: TypeListModel? = null

        @JvmStatic
        var isFavoriteChanged = false

        @JvmStatic
        fun createIntent(context: Context, model: IdolModel?): Intent {
            val intent = Intent(context, CommunityActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            return intent
        }

        //게시글 작성 후 게시글 정렬이 최신으로 바꾸기 위함
        @JvmStatic
        fun createIntent(
            context: Context,
            model: IdolModel,
            category: String,
            isRecent: Boolean
        ): Intent {
            val intent = Intent(context, CommunityActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            intent.putExtra(PARAM_CATEGORY, category)
            intent.putExtra(PARAM_ARTICLE_RECENT, isRecent)
            return intent
        }

        @JvmStatic
        fun createIntent(context: Context, model: IdolModel, category: String): Intent {
            val intent = Intent(context, CommunityActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            intent.putExtra(PARAM_CATEGORY, category)
            return intent
        }

        @JvmStatic
        fun createIntent(
            context: Context,
            model: IdolModel,
            push: Boolean,
            pushType: String
        ): Intent {
            val intent = Intent(context, CommunityActivity::class.java)
            intent.putExtra(PUSH, push)
            intent.putExtra(PUSH_TYPE, pushType)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            return intent
        }

        fun createIntent(
            context: Context,
            model: IdolModel,
            isWallpaper: Boolean
        ): Intent {
            val intent = Intent(context, CommunityActivity::class.java)
            intent.putExtra(PARAM_IDOL, model as Parcelable?)
            intent.putExtra(PARAM_IS_WALLPAPER, isWallpaper)
            return intent
        }
    }
}