/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __jungSangMin__ jnugg0819@gmail.com
 * Description:
 *
 * */

package net.ib.mn.ranking

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.util.SparseIntArray
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.widget.HorizontalScrollView
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatCheckBox
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_IDOL_STATUS_CHANGE
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_IS_HEART
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_IS_HOF
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_IS_IMAGEPICK
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_IS_LIVE
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_IS_MIRACLE
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_IS_ROOKIE
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_NEXT_ACTIVITY
import net.ib.mn.activity.BaseActivity.Companion.EXTRA_ONEPICK_STATUS
import net.ib.mn.activity.BaseActivity.Companion.PARAM_IS_MALE
import net.ib.mn.activity.BaseActivity.Companion.PARAM_IS_SOLO
import net.ib.mn.activity.BaseActivity.Companion.PARAM_NEXT_INTENT
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.EventActivity
import net.ib.mn.activity.FavoriteSettingActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.activity.NewCommentActivity
import net.ib.mn.activity.NoticeActivity
import net.ib.mn.activity.StartupActivity
import net.ib.mn.activity.SubscriptionDetailActivity
import net.ib.mn.adapter.NewCommentAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.attendance.AttendanceActivity
import net.ib.mn.awards.AwardsGuideFragment
import net.ib.mn.awards.AwardsMainFragment
import net.ib.mn.awards.AwardsResultFragment
import net.ib.mn.core.data.api.ServerUrl
import net.ib.mn.core.data.model.MainChartModel
import net.ib.mn.core.data.repository.article.ArticlesRepository
import net.ib.mn.core.model.ChartModel
import net.ib.mn.core.model.NewPicksModel
import net.ib.mn.databinding.FragmentMainRankingBinding
import net.ib.mn.databinding.MainTopTapBinding
import net.ib.mn.feature.rookie.RookieContainerFragment
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.fragment.HallOfFameFragment
import net.ib.mn.fragment.HeartPickFragment
import net.ib.mn.fragment.MiracleMainFragment
import net.ib.mn.fragment.NewRankingFragment
import net.ib.mn.fragment.NewRankingFragment.Companion.ARG_FEMALE_CHART_CODE
import net.ib.mn.fragment.NewRankingFragment.Companion.ARG_MALE_CHART_CODE
import net.ib.mn.fragment.NewSoloRankingFragment
import net.ib.mn.fragment.RewardBottomSheetDialogFragment
import net.ib.mn.link.AppLinkActivity
import net.ib.mn.liveStreaming.LiveStreamingListFragment
import net.ib.mn.model.ArticleModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.EventHeartModel
import net.ib.mn.model.FrontBannerModel
import net.ib.mn.onepick.OnePickMainFragment
import net.ib.mn.support.SupportDetailActivity
import net.ib.mn.support.SupportInfoActivity
import net.ib.mn.support.SupportPhotoCertifyActivity
import net.ib.mn.tutorial.TutorialBits
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.tutorial.setupLottieTutorial
import net.ib.mn.utils.ApiCacheManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.ExtendedDataHolder
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Logger
import net.ib.mn.utils.MainTypeCategory
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.getSerializableData
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.setFirebaseScreenViewEvent
import net.ib.mn.utils.setFirebaseUIAction
import net.ib.mn.viewmodel.MainRankingViewModel
import net.ib.mn.viewmodel.MainViewModel
import javax.inject.Inject


/**
 * @see
 * */

@AndroidEntryPoint
class MainRankingFragment : BaseFragment(), OnScrollToTopListener {
    private val mainViewModel: MainViewModel by activityViewModels()

    private var _binding: FragmentMainRankingBinding? = null
    private val binding get() = _binding!!

    @Inject
    lateinit var articlesRepository: ArticlesRepository

    private val mainRankingViewModel: MainRankingViewModel by viewModels()
    // 상단 탭.

    private var tapCount: Int = 0
    private lateinit var topNavigationMenuAdapter: TopNavigationMenuAdapter

    // 하트픽.
    private var hasNewHeartPick: Boolean = false
    private var hasNewOnePick: Boolean = false

    private var awardsPosition: Int = -1

    private var fronbannerlist: ArrayList<FrontBannerModel> = ArrayList()

    private var idolDialog: Dialog? = null

    private val tabViewList = arrayListOf<View>()

    private var defaultTabIndex: Int = 0

    private val scrollChangedListener = ViewTreeObserver.OnScrollChangedListener {
        val binding = _binding ?: return@OnScrollChangedListener
        val tabStrip = binding.tabContainer.getChildAt(0)
        val scrollView = tabStrip?.parent as? HorizontalScrollView ?: return@OnScrollChangedListener

        val scrollX = scrollView.scrollX
        val maxScroll = tabStrip.width - scrollView.width

        if (scrollX <= 0 || scrollX >= maxScroll) {
            binding.vLeftFade.visibility = View.GONE
            binding.vRightFade.visibility = View.GONE
        } else {
            binding.vLeftFade.visibility = View.VISIBLE
            binding.vRightFade.visibility = View.VISIBLE
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_main_ranking, container, false)
        binding.lifecycleOwner = this

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSet()
        observedVM(savedInstanceState)
        observedMainRankingVM()
        resetTabWidth(savedInstanceState)
    }

    private fun resetTabWidth(savedInstanceState: Bundle?) {
        if (savedInstanceState == null) {
            return
        }

        val tabWidthsBundle = savedInstanceState.getBundle("tabWidths")
        val tabWidths = SparseIntArray()
        tabWidthsBundle?.keySet()?.forEach { key ->
            key.toIntOrNull()?.let { intKey ->
                tabWidths.put(intKey, tabWidthsBundle.getInt(key))
            }
        }
        mainViewModel.tabWidths.value = tabWidths
    }

    override fun onDestroyView() {
        super.onDestroyView()

        _binding?.let { binding ->
            val tabStrip = binding.tabContainer.getChildAt(0)
            val scrollView = tabStrip?.parent as? HorizontalScrollView
            scrollView?.viewTreeObserver?.apply {
                if (isAlive) {
                    removeOnScrollChangedListener(scrollChangedListener)
                }
            }

            binding.pager.unregisterOnPageChangeCallback(viewPagerChangeCallBack)
        }

        _binding = null
    }

    override fun onScrollToTop() {
        scrollToTop()
    }

    private fun initSet() {
        mainViewModel.getLiveChart(requireContext(), this@MainRankingFragment)
        mainViewModel.resetPresentData()

        // 하트픽, 원픽 신규 여부 세팅.
        setNewPicks()

        // 최애 설정 팝업 세팅.
        showSetMostDialog()

        //상단탭 세팅
        topNavigationMenuAdapter = TopNavigationMenuAdapter(this)

        setTabScrollListener()
    }

    private fun setTabScrollListener() {
        val tabStrip = binding.tabContainer.getChildAt(0)
        val scrollView = tabStrip?.parent as? HorizontalScrollView

        scrollView?.viewTreeObserver?.addOnScrollChangedListener(scrollChangedListener)
    }

    private fun observedVM(savedInstanceState: Bundle?) = with(mainViewModel) {
        liveChart.observe(viewLifecycleOwner, Observer {
            Logger.v("context :: rerset all")
            createChartMenu(it.first, it.second, it.third)
            if (savedInstanceState == null) {
                showBanner(MAIN_SOLO_TAB)
            }
            setViewPager()
            setDefaultTab(it.third)

            // 탑 메뉴가 준비된 이후 푸시 탭 이동 처리
            intentOfLinkObserve.observe(viewLifecycleOwner, SingleEventObserver { intent ->
                setDefaultTab(intent)
            })

        })

        errorToast.observe(viewLifecycleOwner, SingleEventObserver { msg ->
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show()
        })

        eventBanner.observe(viewLifecycleOwner, SingleEventObserver { eventModel ->
            // 로그인 하트 띄우기.
            tryShowLoginHeart(eventModel)

            // 탭 클릭시 띄워줄 배너 세팅.
            fronbannerlist = eventModel?.banners ?: arrayListOf()
            with(binding.pager) {
                if (currentItem == MAIN_SOLO_TAB || currentItem == MAIN_GROUP_TAB) {
                    if (savedInstanceState == null) {
                        showBanner(binding.pager.currentItem)
                    }
                }
            }
        })

        moveToSupportScreen.observe(viewLifecycleOwner, SingleEventObserver { supportListModel ->
            when (supportListModel.status) {
                0 -> {
                    SupportDetailActivity.createIntent(
                        context ?: return@SingleEventObserver,
                        supportListModel.id,
                    )
                }

                1 -> {
                    context?.let {
                        SupportPhotoCertifyActivity.createIntent(
                            context ?: return@SingleEventObserver,
                            mainViewModel.getSupportInfo(supportListModel, it),
                        )
                    }
                }
            }
        })

        moveToSubscriptionScreen.observe(viewLifecycleOwner, SingleEventObserver {
            startActivity(
                SubscriptionDetailActivity.createIntent(
                    context ?: return@SingleEventObserver,
                    it.first,
                    it.second,
                ),
            )
        })

        isMaleGender.observe(viewLifecycleOwner, SingleEventObserver { isMale ->
            if (isMale) {
                setMaleCategory()
                return@SingleEventObserver
            }

            setFemaleCategory()
        })

    }

    private fun observedMainRankingVM() = with(mainRankingViewModel) {
        idol.observe(viewLifecycleOwner, SingleEventObserver {
            startActivity(
                CommunityActivity.createIntent(
                    context ?: return@SingleEventObserver, it
                )
            )
        })
    }

    private var mainRankingChartList = arrayListOf<String>()

    private fun createChartMenu(mainChartModel: MainChartModel, chartList: Map<String, List<ChartModel>>, intent: Intent?) {
        // 뷰가 재생성될 때 (예:다크모드) 기존 탭 초기화
        topNavigationMenuAdapter.clear()
        mainViewModel.clearData()
        tabViewList.clear()

        var isMale = mainViewModel.isMaleGender.value?.peekContent()
        if (isMale == null) {
            isMale = Util.getPreference(
                requireContext(),
                Const.PREF_DEFAULT_CATEGORY
            ) == Const.TYPE_MALE

            mainViewModel.setIsMaleGender(isMale)
        }

        val list = if (isMale) mainChartModel.males else mainChartModel.females

        list.forEachIndexed { index, chartCodeInfo ->
            mainRankingChartList.add(chartCodeInfo.code)

            val bundle = if (isMale) {
                Bundle().apply {
                    putParcelable(ARG_MALE_CHART_CODE, chartCodeInfo)
                    putParcelable(ARG_FEMALE_CHART_CODE, mainChartModel.females[index])
                }
            } else {
                Bundle().apply {
                    putParcelable(ARG_MALE_CHART_CODE, mainChartModel.males[index])
                    putParcelable(ARG_FEMALE_CHART_CODE, chartCodeInfo)
                }
            }

            createMenu(
                0,
                if (Util.getPreference(
                        context,
                        Const.PREF_DEFAULT_CATEGORY,
                    ) == Const.TYPE_MALE
                ) {
                    chartCodeInfo.name
                } else {
                    mainChartModel.females[index].name
                },
                NewSoloRankingFragment::class.java,
                bundle,
                GaAction.MAIN_LEAGUE_TAB,
                MainTypeCategory.S.name,
            )
        }

        val tutorialIndex = TutorialManager.getTutorialIndex()
        chartList.forEach {
            when (it.key) {
                // 기적
                "M" -> {
                    val bundle =
                        Bundle().apply {
                            putParcelableArrayList(MiracleMainFragment.ARG_CHART_MODEL, ArrayList(chartList[it.key] ?: emptyList()))
                        }

                    createMenu(R.string.miracle, "",  MiracleMainFragment::class.java, bundle, null, null, tutorialIndex == TutorialBits.MAIN_MIRACLE, TutorialBits.MAIN_MIRACLE)
                }
                // 루키
                "R" -> {
                    val bundle =
                        Bundle().apply {
                            putParcelableArrayList(RookieContainerFragment.ARG_CHART_MODEL, ArrayList(chartList[it.key] ?: emptyList()))
                        }

                    createMenu(R.string.rookie, "",  RookieContainerFragment::class.java, bundle, null, null)
                }
            }
        }

//        if (ConfigModel.getInstance(context).showLiveStreamingTab) {
//            createMenu(R.string.menu_live, "",  LiveStreamingListFragment::class.java, null, null, null)
//        }

        createMenu(R.string.heartpick, "",  HeartPickFragment::class.java, null, null, null, tutorialIndex == TutorialBits.MAIN_HEART_PICK, TutorialBits.MAIN_HEART_PICK)

        val nextIntent = mainViewModel.getNextIntent(intent)

        // 원픽 딥링크 번들값 설정.
        if (nextIntent?.getStringExtra(EXTRA_ONEPICK_STATUS) != null) {
            val imageBundle =
                Bundle().apply {
                    putBoolean(
                        EXTRA_IS_IMAGEPICK,
                        nextIntent.getBooleanExtra(EXTRA_IS_IMAGEPICK, false),
                    )
                }

            createMenu(R.string.onepick, "", OnePickMainFragment::class.java, imageBundle, null, null, tutorialIndex == TutorialBits.MAIN_ONE_PICK, TutorialBits.MAIN_ONE_PICK)
        } else {
            createMenu(R.string.onepick, "",  OnePickMainFragment::class.java, null, null, null, tutorialIndex == TutorialBits.MAIN_ONE_PICK, TutorialBits.MAIN_ONE_PICK)
        }


        val hallOfFameBundle =
            Bundle().apply {
                putParcelableArrayList(HallOfFameFragment.ARG_MALE_CHART_CODE, ArrayList(mainChartModel.males))
                putParcelableArrayList(HallOfFameFragment.ARG_FEMALE_CHART_CODE, ArrayList(mainChartModel.females))
            }

        createMenu(
            R.string.title_tab_hof,
            "",
            HallOfFameFragment::class.java,
            hallOfFameBundle,
            GaAction.LIST_HOF,
            null,
        )
    }

    // TODO 기적 등의 차트코드 응답도 최신걸로 적용되면 textResId 삭제예정
    private fun createMenu(
        textResId: Int,
        text: String,
        klass: Class<out BaseFragment>,
        args: Bundle?,
        gaAction: GaAction?,
        type: String?,
        isShowTutorial: Boolean = false,
        tutorialIndex: Int = TutorialBits.NO_TUTORIAL,
    ) {
        val tabView: MainTopTapBinding = DataBindingUtil.inflate(
            LayoutInflater.from(context),
            R.layout.main_top_tap,
            null,
            false
        )

        when {
            args?.getBoolean("award", false) == true -> {
                tabView.clRoot.background = context?.let { ContextCompat.getDrawable(it, R.drawable.btn_awards_main_menu_on) }
                tabView.clRoot.tag = "award"
            }

            textResId == R.string.heartpick || textResId == R.string.onepick -> {
                setVisibilityNewBtn(tabView.tvNew, textResId)
                tabView.tvTitle.text = getString(textResId)
            }
            else -> {
                tabView.tvTitle.text = if (textResId == 0) {
                    text
                } else {
                    getString(textResId)
                }
            }
        }
        val tabIdx = mainViewModel.getTopNavigationTab().size

        if (isShowTutorial) {
            setupLottieTutorial(tabView.lottieTutorialRankingTab) {
                mainViewModel.updateTutorial(tutorialIndex)
                tabClick(gaAction, type, tabIdx)
            }
        }

        tabView.root.setOnClickListener {
            if (tabView.lottieTutorialRankingTab.isVisible) return@setOnClickListener
            tabClick(gaAction, type, tabIdx)
        }

        tabView.root.id = tabIdx // 탭별 id 세팅 -> buildmenu에서 addview 할때 필요.
        tabViewList.add(tabView.root)
        mainViewModel.addTopNavigationTab(tabView.root)
        topNavigationMenuAdapter.add(klass, args?.apply {
            putInt("fragment_index", tabIdx) // 고유 인덱스 추가, 필요시 사용
        })
    }

    private fun tabClick(gaAction: GaAction?, type: String? = null, tabIdx: Int) {
        Logger.v("---------- :: ------------------")
        gaAction?.let {
            val category = Util.getPreference(context, Const.PREF_DEFAULT_CATEGORY)
            if (!TextUtils.isEmpty(type) &&
                !TextUtils.isEmpty(category) &&
                !TextUtils.isEmpty(
                    NewRankingFragment.league,
                )
            ) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    gaAction.actionValue,
                    UtilK.getFirebaseLabel(
                        MainTypeCategory.valueOf(category).value,
                        MainTypeCategory.valueOf(type ?: "M").value,
                        "main_tab",
                    ),
                )
            } else {
                setUiActionFirebaseGoogleAnalyticsFragment(gaAction.actionValue, gaAction.label)
            }
        }

        if (mainViewModel.getCurrentNavigationTabIdx() == tabIdx) {
            scrollToTop()
            goTestServer()
        } else {
            val category = Util.getPreference(context, Const.PREF_DEFAULT_CATEGORY)

            type?.let {
                val action = when (it) {
                    "S" -> if (category == "M") GaAction.RANKING_BOY_INDV else GaAction.RANKING_GIRL_INDV
                    "G" -> if (category == "M") GaAction.RANKING_BOY_GROUP else GaAction.RANKING_GIRL_GROUP
                    else -> null
                }

                action?.let { act ->
                    setFirebaseScreenViewEvent(act, this::class.simpleName)
                }
            }
            onTabClicked(tabIdx, true)
            tapCount = 0
        }
    }

    fun getDefaultTabIndex(): Int = defaultTabIndex

    private fun goTestServer() {

        if (!BuildConfig.DEBUG || mainViewModel.getCurrentNavigationTabIdx() != 0) {
            return
        }

        tapCount++
        if (tapCount >= 5) {
            tapCount = 0
            // retrofit
            ServerUrl.HOST = if (ServerUrl.HOST == ServerUrl.HOST_REAL) ServerUrl.HOST_TEST else ServerUrl.HOST_REAL

            mainRankingViewModel.clearDatabase()

            // 아이돌 타임스탬프 초기화
            Util.setPreference(context, Const.PREF_ALL_IDOL_UPDATE, "")
            Util.setPreference(context, Const.PREF_DAILY_IDOL_UPDATE, "")
            // 각 탭 reload 시간 초기화
            Util.setPreference(context, Const.KEY_IDOLS_S + "M", 0L)
            Util.setPreference(context, Const.KEY_IDOLS_S + "F", 0L)
            Util.setPreference(context, Const.KEY_IDOLS_G + "M", 0L)
            Util.setPreference(context, Const.KEY_IDOLS_G + "F", 0L)
            // 나의 최애 탭 초기화
            ApiCacheManager.getInstance().clearCache(Const.KEY_FAVORITE)

            Util.setPreference(context, Const.PREF_SERVER_URL, ServerUrl.HOST)

            Toast
                .makeText(context, "Server set to " + ServerUrl.HOST, Toast.LENGTH_SHORT)
                .show()

            val intent = Intent(context, StartupActivity::class.java)
            intent.flags =
                Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            activity?.finish()
        }
    }

    private fun setVisibilityNewBtn(
        tab: View,
        textResId: Int,
    ) {
        val hasPick = if (textResId == R.string.heartpick) hasNewHeartPick else hasNewOnePick
        tab.visibility = if (hasPick) View.VISIBLE else View.GONE
    }

    private fun setNewPicks() {
        val listType = object : TypeToken<NewPicksModel>() {}.type
        val newPicks: NewPicksModel? =
            IdolGson
                .getInstance()
                .fromJson(Util.getPreference(context, Const.PREF_NEW_PICKS), listType)

        if (newPicks != null) {
            hasNewHeartPick = newPicks.heartpick ?: false
            hasNewOnePick = (newPicks.onepick == true || newPicks.themepick == true)
        }
    }

    // 뷰페이저 설정
    private fun setViewPager() {
        // 현재 페이지 좌, 우로 3만큼 페이지를 그려 놓음
        binding.pager.offscreenPageLimit = 3 // 하트픽/원픽 선택시 명전 나오는 현상이 있어 1 -> 3으로 변경
        binding.pager.adapter = topNavigationMenuAdapter

        binding.tabContainer.tabMode = TabLayout.MODE_SCROLLABLE
        TabLayoutMediator(binding.tabContainer, binding.pager) { tab, position ->
            // 커스텀 뷰를 설정하고 레이아웃 매개변수 조정
            tab.customView = createCustomTabView(position)
        }.attach()

        binding.pager.registerOnPageChangeCallback(viewPagerChangeCallBack)
    }

    private fun createCustomTabView(position: Int): View {
        return tabViewList[position].apply {
            layoutParams = ConstraintLayout.LayoutParams(
                if (this.rootView.tag == "award") ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
                bottomMargin = Util.convertDpToPixel(context, 4f).toInt()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val tabWidths = mainViewModel.tabWidths.value ?: return
        val tabWidthsBundle = Bundle().apply {
            for (i in 0 until tabWidths.size()) {
                putInt(tabWidths.keyAt(i).toString(), tabWidths.valueAt(i))
            }
        }
        outState.putBundle("tabWidths", tabWidthsBundle)
    }

    private val viewPagerChangeCallBack = object : ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            onTabClicked(position, false)
        }
    }

    private fun onTabClicked(
        position: Int,
        updatePager: Boolean,
    ) = with(mainViewModel) {
        val topNavigationTab = getTopNavigationTab()
        val currentNavigationTabIdx = getCurrentNavigationTabIdx()

        topNavigationTab.forEachIndexed { index, view ->
            if (index == position) {
                context?.let {
                    view.findViewById<AppCompatTextView>(R.id.tv_title)
                        .setTextColor(it.getColor(R.color.main))
                }
            } else {
                context?.let {
                    view.findViewById<AppCompatTextView>(R.id.tv_title)
                        .setTextColor(it.getColor(R.color.text_dimmed))
                }
            }
        }

        setCurrentNavigationTabIdx(position)

        if (updatePager) {
            binding.pager.currentItem = position
        } else {
            showBanner(currentNavigationTabIdx)
        }

        if (position == 0 || position == 1) {
            val fragmentTag = "fragment_$position"
            val page = childFragmentManager.fragments.firstOrNull {
                it is NewRankingFragment && it.arguments?.getString("fragment_tag") == fragmentTag
            } as? NewRankingFragment
            page?.onFragmentSelected()
        }
    }

    private fun setDefaultTab(intent: Intent?) = with(mainViewModel) {
        // 딥링크로 넘어온 화면이 MainActivity라면
        val nextActivity = intent?.getSerializableData<Class<*>>(EXTRA_NEXT_ACTIVITY)

        if (nextActivity == MainActivity::class.java) {
            setTapFromAppLinkIntent(intent)
            intent.removeExtra(PARAM_NEXT_INTENT)
            intent.removeExtra(EXTRA_NEXT_ACTIVITY)
            return
        }

        // 디폴트는 남자 개인
        setCurrentNavigationTabIdx(0)

        val listType = object : TypeToken<ArrayList<String>>() {}.type
        val gson = IdolGson.getInstance()
        val preferenceJson = Util.getPreference(requireContext(), Const.PREF_MOST_CHART_CODE)

        val mostChartCodes: ArrayList<String> = if (preferenceJson.isNullOrEmpty()) {
            arrayListOf()
        } else {
            gson.fromJson(preferenceJson, listType)
        }

        val firstMatch = mostChartCodes.firstOrNull { it in mainRankingChartList }
        val indexInMainRankingChartList = firstMatch?.let { mainRankingChartList.indexOf(it) } ?: 0

        setCurrentNavigationTabIdx(indexInMainRankingChartList)

        defaultTabIndex = indexInMainRankingChartList

        topNavigationMenuAdapter.notifyDataSetChanged()
        onTabClicked(indexInMainRankingChartList, true)
    }

    private fun setTapFromAppLinkIntent(nextIntent: Intent?) = with(mainViewModel) {
        if (!this@MainRankingFragment::topNavigationMenuAdapter.isInitialized || nextIntent == null) {
            return
        }

        val configModel = ConfigModel.getInstance(context)

        // 메인화면 딥링크 뷰페이저 이동하는곳은 여기다 명시.
        when {
//            configModel.showAwardTab &&
//                nextIntent.getBooleanExtra(
//                    EXTRA_IS_AWARD,
//                    false,
//                ) -> { // 어워드 탭이 보일경우에만 이동해주기.
//                for (i in topNavigationMenuAdapter.mMenus.indices) {
//                    if (topNavigationMenuAdapter.mMenus[i].klass == AwardsMainFragment::class.java ||
//                        topNavigationMenuAdapter.mMenus[i].klass == AwardsGuideFragment::class.java ||
//                        topNavigationMenuAdapter.mMenus[i].klass == AwardsResultFragment::class.java
//                    ) {
//                        setCurrentNavigationTabIdx(i)
//                        break
//                    }
//                }
//            }

            nextIntent.getBooleanExtra(EXTRA_IS_HOF, false) -> { // 명전탭.
                for (i in topNavigationMenuAdapter.mMenus.indices) {
                    if (topNavigationMenuAdapter.mMenus[i].klass == HallOfFameFragment::class.java) {
                        setCurrentNavigationTabIdx(i)
                        break
                    }
                }
            }

//            configModel.showLiveStreamingTab &&
//                nextIntent.getBooleanExtra(
//                    EXTRA_IS_LIVE,
//                    false,
//                ) -> { // 라이브탭(어워드 탭과 동일).
//                for (i in topNavigationMenuAdapter.mMenus.indices) {
//                    if (topNavigationMenuAdapter.mMenus[i].klass == LiveStreamingListFragment::class.java) {
//                        setCurrentNavigationTabIdx(i)
//                        break
//                    }
//                }
//            }

            nextIntent.getStringExtra(EXTRA_ONEPICK_STATUS) != null -> {
                for (i in topNavigationMenuAdapter.mMenus.indices) {
                    if (topNavigationMenuAdapter.mMenus[i].klass == OnePickMainFragment::class.java) {
                        setCurrentNavigationTabIdx(i)
                        break
                    }
                }
            }

            nextIntent.getBooleanExtra(EXTRA_IDOL_STATUS_CHANGE, false) -> {
                val isMale = nextIntent.getBooleanExtra(PARAM_IS_MALE, false)
                val categoryFromLink = if (isMale) "M" else "F"
                val isSolo = nextIntent.getBooleanExtra(PARAM_IS_SOLO, false)
                val tabIdx = if (isSolo) 0 else 1
                setCurrentNavigationTabIdx(tabIdx)

                val category = Util.getPreference(context, Const.PREF_DEFAULT_CATEGORY)
                if (!category.equals(categoryFromLink, ignoreCase = true)) {
                    mainViewModel.setIsMaleGender(category == "M")
                    mainViewModel.setChangeHallOfGender(true)
                }
            }

            nextIntent.getBooleanExtra(EXTRA_IS_MIRACLE, false) -> {
                for (i in topNavigationMenuAdapter.mMenus.indices) {
                    if (topNavigationMenuAdapter.mMenus[i].klass == MiracleMainFragment::class.java) {
                        setCurrentNavigationTabIdx(i)
                        break
                    }
                }
            }

            nextIntent.getBooleanExtra(EXTRA_IS_HEART, false) -> {
                for (i in topNavigationMenuAdapter.mMenus.indices) {
                    if (topNavigationMenuAdapter.mMenus[i].klass == HeartPickFragment::class.java) {
                        setCurrentNavigationTabIdx(i)
                        break
                    }
                }
            }

            nextIntent.getBooleanExtra(EXTRA_IS_ROOKIE, false) -> {
                for (i in topNavigationMenuAdapter.mMenus.indices) {
                    if (topNavigationMenuAdapter.mMenus[i].klass == RookieContainerFragment::class.java) {
                        setCurrentNavigationTabIdx(i)
                        break
                    }
                }
            }
        }

        topNavigationMenuAdapter.notifyDataSetChanged()
        onTabClicked(getCurrentNavigationTabIdx(), true)
    }

    private fun showBanner(tabPosition: Int) {
        val neverEventList = Util.getPreference(context, Const.PREF_NEVER_SHOW_EVENT)

        // fronbannerlist에서 어느 인덱스에 있는지 확인하기 위한 값.
        var checkIndex = -1

        // 각 탭별 해당하는 배너 데이터가 있는지 여부를 판단한다.
        var isBannerDataExist = false

        // 클릭한 탭 클래스 이름

        if (topNavigationMenuAdapter.mMenus.size <= 0) {
            return
        }

        val clickClass = topNavigationMenuAdapter.getFragmentSimpleName(tabPosition)

        try {
            if (fronbannerlist.isEmpty()) {
                val extendedDataHolder = ExtendedDataHolder.getInstance()
                if (extendedDataHolder.hasExtra("bannerList")) {
                    fronbannerlist =
                        extendedDataHolder.getExtra("bannerList") as ArrayList<FrontBannerModel>
                }

                return
            }

            when {
                clickClass == HALL_CLASS -> { // 명전 탭일 때
                    for (i in fronbannerlist.indices) {
                        if (fronbannerlist[i].type.equals("H", ignoreCase = true)) {
                            checkIndex = i
                            isBannerDataExist = true
                            break
                        }
                    }
                }

                clickClass == ONE_PICK_CLASS -> { // 원픽 탭일 때, (기존에 서포트 탭이었는데 원픽 탭으로 바뀌었지만 서포트 배너가 나오게 유지)
                    for (i in fronbannerlist.indices) {
                        if (fronbannerlist[i].type.equals("S", ignoreCase = true)) {
                            checkIndex = i
                            isBannerDataExist = true
                            break
                        }
                    }
                }

                (tabPosition == MAIN_SOLO_TAB) || (tabPosition == MAIN_GROUP_TAB) -> { // 메인 탭일 때
                    for (i in fronbannerlist.indices) {
                        if (fronbannerlist[i].type.equals("M", ignoreCase = true)) {
                            checkIndex = i
                            isBannerDataExist = true
                            break
                        }
                    }
                }

                clickClass == AWARD_CLASS || clickClass == AWARD_GUIDE_CLASS || clickClass == AWARD_RESULT_CLASS -> { // 어워즈 탭일 때
                    for (i in fronbannerlist.indices) {
                        if (fronbannerlist[i].type.equals("A", ignoreCase = true)) {
                            checkIndex = i
                            isBannerDataExist = true
                            break
                        }
                    }
                }
            }

            // 해당 배너 데이터가 있을 때 배너를 띄워 준다.
            if (isBannerDataExist && !fronbannerlist[checkIndex].isClosed) {
                // 메모리 부족으로 엑티비티 다시 시작할 때,
                // null exception 나와서 예외 처리 적용.
                try {
                    fronbannerlist[checkIndex].isClosed = true
                    val eventNo = fronbannerlist[checkIndex].eventNum
                    val targetMenu = fronbannerlist[checkIndex].targetMenu
                    val targetId = fronbannerlist[checkIndex].targetId
                    val imgUrl = fronbannerlist[checkIndex].url
                    val goUrl = fronbannerlist[checkIndex].goUrl
                    val readNoticeArray = neverEventList.split(",").toTypedArray()
                    if (neverEventList == "") {
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

    private fun setMaleCategory() = with(binding.tabContainer) {
        mainViewModel.mainChartModel?.males?.forEachIndexed { index, chartCodeInfo ->
            getTabAt(index)?.customView?.findViewById<AppCompatTextView>(R.id.tv_title)?.text =
                chartCodeInfo.name
        }
    }

    private fun setFemaleCategory() = with(binding.tabContainer) {
        mainViewModel.mainChartModel?.females?.forEachIndexed { index, chartCodeInfo ->
            getTabAt(index)?.customView?.findViewById<AppCompatTextView>(R.id.tv_title)?.text =
                chartCodeInfo.name
        }
    }

    // 탭을 클릭했을 때 해당 클래스인 경우 scrollToTop 작동
    private fun scrollToTop() {
        // 앱 시작하자마자 연타시 크래시 방지
        try {
            val klass =
                topNavigationMenuAdapter.mMenus[mainViewModel.getCurrentNavigationTabIdx()].klass

            val currentFragment = childFragmentManager.findFragmentByTag("f${binding.pager.currentItem}")

            if(currentFragment is OnScrollToTopListener) {
                currentFragment.onScrollToTop()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun tryShowLoginHeart(eventHeartModel: EventHeartModel?) {
        if (eventHeartModel == null) return

        val account = IdolAccount.getAccount(context)

        val tag = "filter_attendance"
        val oldFrag = childFragmentManager.findFragmentByTag(tag)

        val rewardBottomSheetFragment: RewardBottomSheetDialogFragment

        account?.let {
            if (eventHeartModel.dailyHeart + eventHeartModel.sorryHeart + it.mDailyPackHeart > 0) {
                rewardBottomSheetFragment =
                    RewardBottomSheetDialogFragment.newInstance(
                        RewardBottomSheetDialogFragment.FLAG_LOGIN_REWARD,
                        eventHeartModel,
                    ) { isDailyPack ->
                        if (isDailyPack) {
                            (activity as? MainActivity)?.let { mainActivity ->
                                mainViewModel.goToDailyPackDetail(mainActivity)
                            }
                        } else {
                            startActivity(
                                AttendanceActivity.createIntent(
                                    context ?: return@newInstance
                                )
                            )
                        }
                    }

                if (oldFrag == null) {
                    rewardBottomSheetFragment.show(childFragmentManager, tag)
                }

                return
            }

            if (eventHeartModel.burning || !eventHeartModel.burningTime) {
                return
            }

            if (eventHeartModel.burningHeart <= 0) {
                return
            }

            rewardBottomSheetFragment =
                RewardBottomSheetDialogFragment.newInstance(
                    RewardBottomSheetDialogFragment.FLAG_BURNING_REWARD,
                    eventHeartModel,
                ) { isDailyPack ->
                    if (!isDailyPack) {
                        startActivity(
                            AttendanceActivity.createIntent(
                                context ?: return@newInstance
                            )
                        )
                    }
                    Unit
                }

            if (oldFrag == null) {
                rewardBottomSheetFragment.show(childFragmentManager, tag)
            }
        }
    }

    private fun showIdolGuideDialog(
        eventNo: String,
        imgUrl: String,
        goUrl: String?,
        targetMenu: String?,
        targetId: Int,
    ) {
        val eventDialog = Dialog(context ?: return, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow =
            WindowManager.LayoutParams().apply {
                flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
                dimAmount = 0.7f
                gravity = Gravity.CENTER
            }
        eventDialog.window?.attributes = lpWindow
        eventDialog.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        eventDialog.setContentView(R.layout.dialog_event)
        eventDialog.setCanceledOnTouchOutside(false)
        eventDialog.setCancelable(true)

        val cbCheckGuide = eventDialog.findViewById<AppCompatCheckBox>(R.id.check_guide)
        val dialogBtnClose = eventDialog.findViewById<AppCompatButton>(R.id.btn_close)
        val imgEvent = eventDialog.findViewById<AppCompatImageView>(R.id.img_event)

        imgEvent.setOnClickListener {
            if (!TextUtils.isEmpty(targetMenu)) {
                when {
                    targetMenu.equals("notice", ignoreCase = true) -> {
                        startActivity(NoticeActivity.createIntent(context, targetId))
                    }

                    targetMenu.equals("event", ignoreCase = true) -> {
                        startActivity(EventActivity.createIntent(context, targetId))
                    }

                    targetMenu.equals("idol", ignoreCase = true) -> {
                        mainRankingViewModel.getIdolById(targetId)
                    }

                    targetMenu.equals("support", ignoreCase = true) -> {
                        if (targetId == 0) {
                            startActivity(SupportInfoActivity.createIntent(context))
                        } else {
                            context?.let { mainViewModel.getSupportList(targetId, it) }
                        }
                    }

                    targetMenu.equals("board", ignoreCase = true) -> {
                        lifecycleScope.launch {
                            articlesRepository.getArticle(
                                "/api/v1/articles/$targetId/",
                                { response ->
                                    val article =
                                        IdolGson
                                            .getInstance(true)
                                            .fromJson(response.toString(), ArticleModel::class.java)
                                    val adapterType =
                                        if (article.type == "M") NewCommentAdapter.TYPE_SMALL_TALK else NewCommentAdapter.TYPE_ARTICLE
                                    startActivity(
                                        NewCommentActivity.createIntent(
                                            context,
                                            article,
                                            0,
                                            true,
                                            adapterType,
                                        ),
                                    )
                                },
                                {
                                    Toast
                                        .makeText(
                                            context,
                                            getString(R.string.error_abnormal_exception),
                                            Toast.LENGTH_SHORT,
                                        ).show()
                                }
                            )
                        }
                    }
                }
            } else if (!goUrl.isNullOrEmpty()) {
                try {
                    val intent =
                        Intent(context, AppLinkActivity::class.java).apply {
                            data = Uri.parse(goUrl)
                        }
                    startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Util.showIdolDialogWithBtn1(context, null, getString(R.string.msg_error_ok)) {
                        Util.closeIdolDialog()
                    }
                    e.printStackTrace()
                }
            }
        }

        cbCheckGuide.setOnCheckedChangeListener { _, isChecked ->
            // nothing
        }

        dialogBtnClose.setOnClickListener {
            if (cbCheckGuide.isChecked) {
                saveNeverShowEvent(eventNo)
            }
            eventDialog.cancel()
        }

        eventDialog.setOnKeyListener { _, keyCode, _ ->
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                eventDialog.cancel()
                true
            } else {
                false
            }
        }
        eventDialog.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))

        // 이미지가 다 로드되면 다이얼로그 표시
        mGlideRequestManager
            .load(imgUrl)
            .centerInside()
            .into(
                object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?,
                    ) {
                        imgEvent.setImageDrawable(resource)
                        eventDialog.show()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {}
                },
            )
    }

    private fun saveNeverShowEvent(eventNo: String) {
        val neverEventList = Util.getPreference(context, Const.PREF_NEVER_SHOW_EVENT)
        val neverEventTotal: String

        val readNoticeArray = neverEventList.split(",")

        if (neverEventList == "") {
            neverEventTotal = neverEventList + eventNo
            Util.setPreference(context, Const.PREF_NEVER_SHOW_EVENT, neverEventTotal)
        } else if (!Util.isFoundString(eventNo, readNoticeArray.toTypedArray())) {
            neverEventTotal = "$neverEventList,$eventNo"
            Util.setPreference(context, Const.PREF_NEVER_SHOW_EVENT, neverEventTotal)
        }
    }


    private fun showSetMostDialog() {

        val account = IdolAccount.getAccount(context)
        val isNoExistMost = account?.most?.type.equals("B", ignoreCase = true) ||
            account?.most == null
        val isSetNeverShowMost =
            Util.getPreferenceBool(context, Const.PREF_NEVER_SHOW_SET_MOST, false)

        if (!isNoExistMost || isSetNeverShowMost) {
            return
        }

        if (idolDialog != null && idolDialog!!.isShowing) return

        setFirebaseUIAction(GaAction.CHOEAE_POPUP)

        idolDialog = Dialog(context ?: return, android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow =
            WindowManager.LayoutParams().apply {
                flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
                dimAmount = 0.7f
                gravity = Gravity.CENTER
            }
        idolDialog!!.window?.attributes = lpWindow
        idolDialog!!.window?.setLayout(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
        )

        idolDialog!!.setContentView(R.layout.dialog_not_show_two_btn)
        val dialogTvTitle = idolDialog!!.findViewById<AppCompatTextView>(R.id.title)
        val dialogTvMsg = idolDialog!!.findViewById<AppCompatTextView>(R.id.message)
        val tvBtnOk = idolDialog!!.findViewById<AppCompatButton>(R.id.btn_ok)
        val tvBtnCancel = idolDialog!!.findViewById<AppCompatButton>(R.id.btn_cancel)
        val cbCheckGuide = idolDialog!!.findViewById<AppCompatCheckBox>(R.id.check_guide)

        dialogTvTitle.text = UtilK.getMyIdolTitle(context)

        idolDialog!!.setCanceledOnTouchOutside(true)
        idolDialog!!.setCancelable(true)
        dialogTvMsg.text = getString(R.string.label_set_most)

        cbCheckGuide.setOnCheckedChangeListener { _, _ ->
            // nothing
        }

        tvBtnOk.setOnClickListener {
            if (cbCheckGuide.isChecked) {
                Util.setPreference(context, Const.PREF_NEVER_SHOW_SET_MOST, true)
            }
            setFirebaseUIAction(GaAction.CHOEAE_POPUP_YES)
            idolDialog!!.cancel()
            startActivity(FavoriteSettingActivity.createIntent(context))
        }

        tvBtnCancel.setOnClickListener {
            if (cbCheckGuide.isChecked) {
                Util.setPreference(context, Const.PREF_NEVER_SHOW_SET_MOST, true)
            }
            setFirebaseUIAction(GaAction.CHOEAE_POPUP_NO)
            idolDialog!!.cancel()
        }

        idolDialog!!.window?.setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
        idolDialog!!.show()
    }

    fun getCurrentFragmentTag(): String? {
        val position = binding.pager.currentItem
        val tag = "android:switcher:${binding.pager.id}:$position"
        return tag
    }

    override fun onResume() {
        super.onResume()
        notifyChildFragments("onResume")
    }

    override fun onPause() {
        super.onPause()
        notifyChildFragments("onPause")
    }

    private fun notifyChildFragments(event: String) {
        // 하위 프래그먼트에 이벤트 전달
        childFragmentManager.fragments.forEach { fragment ->
            if (fragment is LifecycleAwareFragment) {
                fragment.onParentFragmentEvent(event)
            }
        }
    }

    interface LifecycleAwareFragment {
        fun onParentFragmentEvent(event: String)
    }

    companion object {
        private const val MAIN_SOLO_TAB = 0
        private const val MAIN_GROUP_TAB = 1

        private val ONE_PICK_CLASS: String? = OnePickMainFragment::class.java.simpleName
        private val AWARD_CLASS: String? = AwardsMainFragment::class.java.simpleName
        private val AWARD_GUIDE_CLASS: String? = AwardsGuideFragment::class.java.simpleName
        private val AWARD_RESULT_CLASS: String? = AwardsResultFragment::class.java.simpleName
        private val HALL_CLASS: String? = HallOfFameFragment::class.java.simpleName
        private val HEART_PICK_CLASS: String? = HeartPickFragment::class.java.simpleName
    }
}
