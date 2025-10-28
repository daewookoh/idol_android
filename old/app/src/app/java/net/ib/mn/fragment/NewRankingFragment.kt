package net.ib.mn.fragment

import android.animation.Animator
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.MainActivity
import net.ib.mn.adapter.NewRankingAdapter
import net.ib.mn.core.data.model.ChartCodeInfo
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.data.repository.idols.IdolsRepository
import net.ib.mn.core.designsystem.toast.MostToast
import net.ib.mn.core.domain.usecase.GetChartIdolIdsUseCase
import net.ib.mn.databinding.NewFragmentRankingBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.model.IdolModel
import net.ib.mn.ranking.MainRankingFragment
import net.ib.mn.remote.IdolBroadcastManager
import net.ib.mn.tutorial.TutorialManager
import net.ib.mn.utils.Const
import net.ib.mn.utils.EventBus
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.Logger
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.preventUnwantedHorizontalScroll
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.throttleFirst
import net.ib.mn.viewmodel.MainViewModel
import net.ib.mn.viewmodel.NewRankingViewModel
import javax.inject.Inject

@AndroidEntryPoint
abstract class NewRankingFragment : BaseFragment(),
    BaseDialogFragment.DialogResultHandler,
    NewRankingAdapter.OnClickListener,
    MainRankingFragment.LifecycleAwareFragment,
    OnScrollToTopListener {

    private lateinit var binding: NewFragmentRankingBinding

    private var mRankingAdapter: NewRankingAdapter? = null
    lateinit var rvRanking: RecyclerView

    private val newRankingViewModel: NewRankingViewModel by viewModels()

    @Inject
    lateinit var getChartIdolIdsUseCase: GetChartIdolIdsUseCase

    @Inject
    lateinit var idolBroadcastManager: IdolBroadcastManager

    @Inject
    lateinit var idolApiManager: IdolApiManager
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var idolsRepository: IdolsRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    protected abstract fun getLoaderId(): Int
    protected lateinit var displayErrorHandler: Handler
    private lateinit var eventHeartDialog: Dialog

    protected lateinit var models: ArrayList<IdolModel> // 남/여/전체 순위 리스트

    protected lateinit var type: String // 개인/그룹 타입
    private var isScrolling = false

    lateinit var maleChartCode: ChartCodeInfo
    lateinit var femaleChartCode: ChartCodeInfo

    private var scrollPosition = 0
    private var isInit = true

    protected var timerHandler: Handler? = null // 10초 자동갱신 타이머
    protected var timerRunnable: Runnable? = null
    protected val refreshInterval: Long = if (BuildConfig.DEBUG) 5 else 10    // 10초 갱신

    private var isRefresh = false

    private val mainViewModel: MainViewModel by activityViewModels()

    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            // 메뉴에서 카테고리 변경시 처리
            if (intent.action!!.equals(Const.REFRESH, ignoreCase = true)) {
                showEmptyView()

                isRefresh = true

                category = Util.getPreference(activity!!, Const.PREF_DEFAULT_CATEGORY)

                // 현재 보여지는 상태라면 reload
                mapExpanded.clear()
                mRankingAdapter?.needUpdate = true
                if (isVisible) {
                    val chartCode = if (mainViewModel.isMaleGender.value?.peekContent() != false) {
                        maleChartCode.code
                    } else {
                        femaleChartCode.code
                    }
                    mRankingAdapter?.updateChartCode(chartCode)
                    newRankingViewModel.mainChartIdols(chartCode)
                }

                return
            }

            if (!Const.USE_ANIMATED_PROFILE || !isVisible) return

            val index = intent.getIntExtra("index", 0)
            // 움짤 주소가 있을 때에만 처리
            try {
                if (models.size > 0) {
                    if (index == 0
                        && playerView1 != null
                        && hasVideo(playerView1)
                    ) {
                        (playerView1?.parent as ViewGroup)
                            .findViewById<View>(R.id.photo1).visibility = View.INVISIBLE
                        playerView1?.visibility = View.VISIBLE
                    } else if (index == 1
                        && playerView2 != null
                        && hasVideo(playerView2)
                    ) {
                        (playerView2?.parent as ViewGroup)
                            .findViewById<View>(R.id.photo2).visibility = View.INVISIBLE
                        playerView2?.visibility = View.VISIBLE
                    } else if (index == 2
                        && playerView3 != null
                        && hasVideo(playerView3)
                    ) {
                        (playerView3?.parent as ViewGroup)
                            .findViewById<View>(R.id.photo3).visibility = View.INVISIBLE
                        playerView3?.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGlideRequestManager = Glide.with(this)
        displayErrorHandler = @SuppressLint("HandlerLeak") object : Handler() {

            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                try {
                    val responseMsg = msg.obj as String
                    Toast.makeText(activity, responseMsg, Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.new_fragment_ranking, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            maleChartCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_MALE_CHART_CODE, ChartCodeInfo::class.java) ?: return
            } else {
                it.getParcelable(ARG_MALE_CHART_CODE) ?: return
            }

            femaleChartCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_FEMALE_CHART_CODE, ChartCodeInfo::class.java) ?: return
            } else {
                it.getParcelable(ARG_FEMALE_CHART_CODE) ?: return
            }
        }

        // onCreate에서 옮김 (chartCode 변수 lateinit 문제)
        lifecycleScope.launch {
            EventBus.receiveEvent<Boolean>(Const.BROADCAST_MANAGER_MESSAGE)
                .throttleFirst(1000)
                .collect { result ->
                    if (result) {
                        val chartCode =
                            if (mainViewModel.isMaleGender.value?.peekContent() != false) {
                                maleChartCode.code
                            } else {
                                femaleChartCode.code
                            }
                        newRankingViewModel.changeIdolList(chartCode)
                    }
                }
        }

        init()
        observedVM()
    }

    private fun observedVM() {
        mainViewModel.bottomTabIndex.observe(viewLifecycleOwner) { index ->
            when (index) {
                1, 2, 3 -> {
                    stopTimer()
                    stopExoPlayer(playerView1)
                    stopExoPlayer(playerView2)
                    stopExoPlayer(playerView3)
                }

                0 -> {
                    startTimer()

                    if (isInit) return@observe
                    mRankingAdapter?.needUpdate = true
                    val chartCode = if (mainViewModel.isMaleGender.value?.peekContent() != false) {
                        maleChartCode.code
                    } else {
                        femaleChartCode.code
                    }
                    mRankingAdapter?.updateChartCode(chartCode)
                    newRankingViewModel.mainChartIdols(chartCode = chartCode)
                }
            }
        }

        newRankingViewModel.rankingList.observe(viewLifecycleOwner, SingleEventObserver { idols ->
            if (idols.isNotEmpty()) {
                applyItems(idols) {
                    showRankingWithMyFavToast(idols)
                }
            } else {
                showEmptyView()
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        models = ArrayList()
        rvRanking = binding.rvRanking
        rvRanking.setHasFixedSize(true)
        rvRanking.setItemViewCacheSize(40)
        category = Util.getPreference(activity, Const.PREF_DEFAULT_CATEGORY)

        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)
        rvRanking.itemAnimator = null

        // 막다른 곳에서 스크롤할 때 삐딱하게 스크롤하면 좌우로 넘어감 방지
        rvRanking.preventUnwantedHorizontalScroll()

        mRankingAdapter = NewRankingAdapter(requireActivity(), this, mGlideRequestManager,
            animationMode = Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false),
            mListener = this,
            league = league ?: Const.LEAGUE_S,
            lottieListener = object : NewRankingAdapter.LottieListener {
                override fun lottieListener(view: LottieAnimationView) {
                    view.postDelayed({
                        view.setAnimation("animation_heartbar_android.json")
                        view.playAnimation()
                    }, 300)  //맨처음 진입 시 버벅여서 딜레이 살짝 줌

                    view.addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(p0: Animator) {}
                        override fun onAnimationEnd(p0: Animator) {
                            view.pauseAnimation()
                            p0.removeAllListeners()
                        }

                        override fun onAnimationCancel(p0: Animator) {}
                        override fun onAnimationRepeat(p0: Animator) {}
                    })

                    //로티 애니메이션이 완료가 되지 않았는데 스크롤을 할 경우, addAnimatorListener가 작동하지 않아 강제로 해당 view의 로티 pause 시키고 progress 상태 처음으로 되돌림(cancelAnimation, clearAnimation 다 작동하지 않음)
                    //해당 작업을 하지 않으면, addAnimatorListener가 작동하지 않을 경우(빠르게 스크롤 할 경우), 하단으로 내렸다가 다시 올릴 경우에 애니메이션이 또 작동함 (onAnimationEnd 작업이 실행되어야만 애니매이션 반복이 되지 않음)
                    rvRanking.addOnScrollListener(object : RecyclerView.OnScrollListener() {
                        override fun onScrollStateChanged(
                            recyclerView: RecyclerView,
                            newState: Int
                        ) {
                            super.onScrollStateChanged(recyclerView, newState)
                            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                                isScrolling = false
                                mGlideRequestManager.resumeRequests() // 스크롤 멈추면 로딩 재개
                            } else {
                                isScrolling = true
                                mGlideRequestManager.pauseRequests() // 스크롤 중에는 로딩 중단
                            }

                            if ((rvRanking.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() > SHOW_ANIMATION_LAST_INDEX) {
                                view.pauseAnimation()
                                view.progress = 0f  //0으로 돌리지 않으면 애니메이션 이어서 시작하기 때문에 0으로 초기화
                            }
                        }

                        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                            super.onScrolled(recyclerView, dx, dy)

                            if ((rvRanking.layoutManager as LinearLayoutManager).findFirstVisibleItemPosition() >= scrollPosition - MY_FAV_TOAST_VISIBLE_MINUS_COUNT) {

                                binding.composeFavToast.visibility = View.GONE
                            }
                        }
                    })
                }
            },
            lifecycleScope = lifecycleScope,
            idolsRepository = idolsRepository,
            isTutorialAdapter = "f${(parentFragment as MainRankingFragment).getDefaultTabIndex()}" == tag
        ) { tutorialIndex ->
            newRankingViewModel.updateTutorial(tutorialIndex)
        }


        mRankingAdapter?.setHasStableIds(true)
        rvRanking.apply {
            adapter = mRankingAdapter
            addItemDecoration(divider)
            setHasFixedSize(true)
        }

        isInit = true

        var isMale = mainViewModel.isMaleGender.value?.peekContent()
        if (isMale == null) {
            isMale = Util.getPreference(
                requireContext(),
                Const.PREF_DEFAULT_CATEGORY
            ) == Const.TYPE_MALE

            mainViewModel.setIsMaleGender(isMale)
        }

        if (isMale) {
            mRankingAdapter?.updateChartCode(maleChartCode.code)
            newRankingViewModel.mainChartIdols(maleChartCode.code)
        } else {
            mRankingAdapter?.updateChartCode(femaleChartCode.code)
            newRankingViewModel.mainChartIdols(femaleChartCode.code)
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    override fun onPause() {
        super.onPause()

        Util.log("RankingFragment onPause")

        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(mBroadcastReceiver)

        mRankingAdapter?.clearAnimation()

        // 움짤 멈추기
        if (Const.USE_ANIMATED_PROFILE) {
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
        }
    }

    override fun onStop() {
        super.onStop()

        Util.log("NewRankingFragment onStop")
    }

    override fun onResume() {
        super.onResume()
        Util.log("NewRankingFragment onResume")

        val filter = IntentFilter()

        filter.addAction(Const.REFRESH)
        if (Const.USE_ANIMATED_PROFILE) {
            filter.addAction(Const.PLAYER_START_RENDERING)
        }
        LocalBroadcastManager.getInstance(requireContext())
            .registerReceiver(mBroadcastReceiver, filter)

        if (Const.USE_ANIMATED_PROFILE) {
            // 움짤 다시 재생
            Handler().postDelayed({
                startExoPlayer(playerView1)
                startExoPlayer(playerView2)
                startExoPlayer(playerView3)
            }, 200)
        }

        val chartCode = if (mainViewModel.isMaleGender.value?.peekContent() != false) {
            maleChartCode.code
        } else {
            femaleChartCode.code
        }

        mRankingAdapter?.updateChartCode(chartCode)
        newRankingViewModel.checkChangeGender(chartCode)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.RANKING_VOTE.value
            && resultCode == BaseDialogFragment.RESULT_OK
        ) {
            val heart = data!!.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
            if (heart > 0) {
                newRankingViewModel.refreshData()

                val eventHeart = data?.getStringExtra(PARAM_EVENT_HEART)
                if (!eventHeart.isNullOrEmpty()) {
                    showEventDialog(eventHeart)
                }

                // 레벨업 체크
                if (data != null) {
                    val idol: IdolModel =
                        data.getSerializableExtra(VoteDialogFragment.PARAM_IDOL_MODEL) as IdolModel
                    UtilK.checkLevelUp(baseActivity, accountManager, idol, heart)
                }
            } else {
                Util.closeProgress()
            }
        }
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        // 최애가 그룹인 경우 뷰 생성 전에 이쪽을 타서 방어
        if (!isAdded) {
            // 앱 시작 후 방치시 자동갱신 안되서
            if (isVisible) {
                startTimer()
            }
            return
        }

        if (isVisible) {
            idolApiManager.startTimer()
        } else {
            idolApiManager.stopTimer()
        }

        // 펼친거 다시 닫기
        mapExpanded.clear()

        if (isVisible) {
            // 다른탭 갔다오면 1위 움짤프사 다시 재생되게
            mRankingAdapter?.needUpdate = true

            // 화면 전환하면 카테고리 다시 저장한 값으로 초기화
            if (activity != null && isAdded) {
                category = Util.getPreference(activity, Const.PREF_DEFAULT_CATEGORY)
            }

            if (getLoaderId() == Const.GROUP_ID) {
                if (binding.tvLoadData.visibility == View.VISIBLE) {
                    val chartCode = if (category == Const.TYPE_MALE) {
                        maleChartCode.code
                    } else {
                        femaleChartCode.code
                    }

                    newRankingViewModel.mainChartIdols(chartCode)
                } else {
                    if (isScrolling) return
                    newRankingViewModel.refreshData()
                }
            } else {
                if (isScrolling) return
                newRankingViewModel.refreshData()
            }

            mRankingAdapter?.hasExpanded = false
            startTimer()
        } else {
            stopTimer()
            mRankingAdapter?.needUpdate = false
        }
    }

    fun startTimer() {
        if (timerRunnable != null) {
            timerHandler?.removeCallbacks(timerRunnable!!)
        }

        timerRunnable = Runnable {
            try {
                if (isScrolling) return@Runnable
                newRankingViewModel.refreshData()
            } finally {
                timerHandler?.postDelayed(timerRunnable!!, refreshInterval * 1000)
            }
        }

        timerHandler = Handler(Looper.getMainLooper())
        timerHandler?.postDelayed(timerRunnable!!, refreshInterval * 1000)

        Util.log("*** startTimer $this")
    }

    fun stopTimer() {
        if (timerRunnable != null) {
            timerHandler?.removeCallbacks(timerRunnable!!)
        }

        Util.log("*** stopTimer $this")
    }

    protected fun applyItems(
        items: ArrayList<IdolModel>,
        dbIdolsCallBack: (ArrayList<IdolModel>) -> Unit = {}
    ) {
        val recyclerViewState = rvRanking.layoutManager?.onSaveInstanceState()

        mRankingAdapter?.apply {
            updateMostIdol()
            setItems(items, league ?: Const.LEAGUE_S)
        }

        dbIdolsCallBack(items)

        if (isRefresh) {
            rvRanking.postDelayed({
                isRefresh = false
                rvRanking.layoutManager?.onRestoreInstanceState(recyclerViewState)
            }, 200)
        }

        if (items.isEmpty()) {
            showEmptyView()
        } else {
            hideEmptyView()
        }
    }

    //S리그 화면 보여질 경우 로티를 실행하라
    protected fun startLottie() {
        mRankingAdapter?.startLottieAnimation()
    }

    private fun showEmptyView() {
        if (league == Const.LEAGUE_A) {
            binding.tvLoadData.text = getString(R.string.label_league_ended)
        }
        binding.tvLoadData.visibility = View.VISIBLE
        rvRanking.visibility = View.GONE
    }

    private fun hideEmptyView() {
        binding.tvLoadData.visibility = View.GONE
        rvRanking.visibility = View.VISIBLE
    }

    private fun showEventDialog(eventHeart: String) {
        eventHeartDialog = Dialog(requireActivity(), android.R.style.Theme_Translucent_NoTitleBar)

        val lpWindow = WindowManager.LayoutParams()
        lpWindow.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND
        lpWindow.dimAmount = 0.7f
        lpWindow.gravity = Gravity.CENTER
        eventHeartDialog.window!!.attributes = lpWindow
        eventHeartDialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val btnOk: AppCompatButton = eventHeartDialog.findViewById(R.id.btn_ok)
        val msg: AppCompatTextView = eventHeartDialog.findViewById(R.id.message)

        eventHeartDialog.setContentView(R.layout.dialog_surprise_heart)
        eventHeartDialog.setCanceledOnTouchOutside(false)
        eventHeartDialog.setCancelable(true)
        btnOk.setOnClickListener { eventHeartDialog.cancel() }
        msg.text = String.format(getString(R.string.msg_surprise_heart), eventHeart)
        eventHeartDialog.window!!
            .setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        eventHeartDialog.show()
    }

    private fun showError(title: String, text: String?) {
        Util.showDefaultIdolDialogWithBtn1(
            activity,
            title,
            text
        ) { Util.closeIdolDialog() }
    }

    open fun onFragmentSelected() {}

    private fun openCommunity(idol: IdolModel) {
        if (Util.mayShowLoginPopup(activity) || activity == null) return

        startActivity(CommunityActivity.createIntent(requireActivity(), idol))
    }

    private fun voteHeart(idol: IdolModel, totalHeart: Long, freeHeart: Long) {
        val dialogFragment = VoteDialogFragment.getIdolVoteInstance(idol, totalHeart, freeHeart)
        dialogFragment.setTargetFragment(this, RequestCode.RANKING_VOTE.value)
        dialogFragment.show(parentFragmentManager, "vote")
    }

    override fun onItemClicked(item: IdolModel) {
        openCommunity(item)
    }

    override fun onVote(item: IdolModel) {
        if (Util.mayShowLoginPopup(baseActivity)) {
            return
        }

        setUiActionFirebaseGoogleAnalyticsFragment(GaAction.VOTE.actionValue, GaAction.VOTE.label)
        Util.showProgress(activity)
        lifecycleScope.launch {
            usersRepository.isActiveTime(
                { response ->
                    Util.closeProgress()

                    if (response.optBoolean("success")) {
                        val gcode = response.optInt("gcode")
                        if (response.optString("active") == Const.RESPONSE_Y) {
                            if (response.optInt("total_heart") == 0) {
                                Util.showChargeHeartDialog(activity)
                            } else {
                                if (response.optString("vote_able")
                                        .equals(Const.RESPONSE_Y, ignoreCase = true)
                                ) {
                                    voteHeart(
                                        item,
                                        response.optLong("total_heart"),
                                        response.optLong("free_heart")
                                    )
                                } else {

                                    if (gcode == Const.RESPONSE_IS_ACTIVE_TIME_1) {
                                        Toast.makeText(
                                            activity,
                                            getString(R.string.response_users_is_active_time_over),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            activity,
                                            getString(R.string.msg_not_able_vote),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        } else {
                            val start = Util.convertTimeAsTimezone(response.optString("begin"))
                            val end = Util.convertTimeAsTimezone(response.optString("end"))
                            val unableUseTime = String.format(
                                getString(R.string.msg_unable_use_vote), start, end
                            )
                            Util.showIdolDialogWithBtn1(
                                activity,
                                null,
                                unableUseTime
                            ) { Util.closeIdolDialog() }
                        }
                    } else { // success is false!
                        UtilK.handleCommonError(activity, response)
                    }
                }, {
                    Util.closeProgress()
                    Toast.makeText(activity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT)
                        .show()

                }
            )
        }
    }

    // 프사 눌러 펼치기
    override fun onPhotoClicked(item: IdolModel, position: Int) {
        if (category.isEmpty()) {
            Logger.v("onPhotoClicked :: it's null")
            return
        }

        rvRanking.postDelayed({
            // get clicked item view
            val targetView = rvRanking.findViewHolderForAdapterPosition(position)?.itemView
            // get window location
            val location = IntArray(2)

            if (targetView != null) {
                targetView.getLocationInWindow(location)
                // expanded height
                val viewHeight = targetView.height
                val viewWidth = targetView.width
                val targetY = location[1] - (viewWidth / 3)
                val targetHeight = viewHeight + viewWidth / 3
                val targetBottom = targetY + targetHeight
                rvRanking.getLocationInWindow(location)
                val listviewTop = location[1]
                val listviewBottom = listviewTop + rvRanking.height
                // check if target bottom is under listview's bottom
                if (targetBottom > listviewBottom) {
                    rvRanking.smoothScrollBy(targetBottom - listviewBottom + viewHeight, 200)
                }
            }

            // 펼친거 모두 닫으면 움짤프사 다시 재생
            if (mapExpanded.values.all { !it }) {
                mRankingAdapter?.notifyItemChanged(0)
            }
        }, 300)
    }

    private fun showRankingWithMyFavToast(idols: ArrayList<IdolModel>) {
        val account = IdolAccount.getAccount(context)
        val mostId = account?.most?.getId()

        val mostIdol = idols.find { it.getId() == mostId }
        val hasShownMyFavToast =
            Util.getPreferenceBool(context ?: return, Const.PREF_HAS_SHOWN_MY_FAV_TOAST, true)

        if (mostIdol == null || hasShownMyFavToast) {
            binding.composeFavToast.visibility = View.GONE
            return
        }

        if (mostIdol.rank < MY_FAV_TOAST_MIN_RANK_LIMIT) {
            binding.composeFavToast.visibility = View.GONE
            return
        }

        binding.composeFavToast.visibility = View.VISIBLE
        binding.composeFavToast.post {
            if (activity == null) return@post

            // 메인 화면에 웰컴 미션 버튼 세팅 (최애 토스트 때문에 여기다 배치 구조 바꾸긴 해야할듯)
            val missionBtnBottomMargin =
                if (binding.composeFavToast.visibility == View.VISIBLE) 72 else 12
            (activity as MainActivity).setMissionButton(missionBtnBottomMargin)
        }

        scrollPosition = idols.indexOfFirst { it.getId() == mostId }

        getFirstItemViewHeight { offset ->
            setMyFavToast(scrollPosition, offset)
        }
    }


    private fun setMyFavToast(scrollPosition: Int, offset: Int) {
        binding.composeFavToast.setContent {
            MostToast(
                mainIcon = painterResource(id = R.drawable.icon_toast_heart),
                backGroundColor = colorResource(id = R.color.main100),
                borderColor = colorResource(id = R.color.main300),
                title = getString(R.string.banner_go_myidol_title),
                titleColor = colorResource(id = R.color.text_default),
                underLineText = getString(R.string.banner_go_myidol_btn),
                underLineTextColor = colorResource(id = R.color.main_light)
            ) {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.RANKING_MY_IDOL.actionValue,
                    GaAction.RANKING_MY_IDOL.label
                )
                binding.rvRanking.post {
                    val targetPosition = (scrollPosition).coerceAtLeast(0)
                    val layoutManager = binding.rvRanking.layoutManager as LinearLayoutManager
                    val itemHeight =
                        resources.getDimensionPixelSize(R.dimen.main_ranking_item_height)
                    layoutManager.scrollToPositionWithOffset(targetPosition, itemHeight / 2)

                    binding.composeFavToast.visibility = View.GONE
                    binding.composeFavToast.post {
                        val missionBtnBottomMargin =
                            if (binding.composeFavToast.visibility == View.VISIBLE) 72 else 12
                        (activity as MainActivity).setMissionButton(missionBtnBottomMargin)
                    }

                    Util.setPreference(context, Const.PREF_HAS_SHOWN_MY_FAV_TOAST, true)
                }
            }
        }
    }

    private fun getFirstItemViewHeight(offsetCall: (Int) -> Unit = {}) {
        binding.rvRanking.post {
            // 첫 번째 아이템을 가져와서 높이를 구해줍니다. 해당 포지션의 아이템은 아직 그려지지 않아서 높이를 못가져옴.
            val viewHolder =
                binding.rvRanking.findViewHolderForAdapterPosition(1) ?: return@post

            val itemView = viewHolder.itemView

            itemView.viewTreeObserver?.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    itemView.viewTreeObserver.removeOnGlobalLayoutListener(this)

                    val height = itemView.height
                    offsetCall(height)
                }
            })
        }
    }

    override fun onParentFragmentEvent(event: String) {
        // 다른 액티비티로 갔을때 대비.. onVisibilityChaned는 상단 탭 상에서는 동작함.
        when (event) {
            "onPause" -> {
                stopTimer()
            }

            "onResume" -> {
                startTimer()
            }
        }
    }

    override fun onScrollToTop() {
        val layoutManager = rvRanking.layoutManager as LinearLayoutManager
        layoutManager.scrollToPosition(0)
    }

    companion object {
        const val ARG_MALE_CHART_CODE = "maleChartCode"
        const val ARG_FEMALE_CHART_CODE = "femaleChartCode"

        // 카테고리 처리
        @JvmStatic
        public var category: String = "" // 카테고리는 앱 내에서 공용으로 사용
        var league: String? = null //리그 상태

        const val PARAM_EVENT_HEART = "paramEventHeart"
        const val SHOW_ANIMATION_LAST_INDEX = 3     //애니메이션 보여줘야하는 View의 마지막 인덱스

        const val MALE = "M"
        const val MY_FAV_TOAST_MIN_RANK_LIMIT = 6
        private const val MY_FAV_TOAST_VISIBLE_MINUS_COUNT = 4
    }
}
