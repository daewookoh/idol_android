package net.ib.mn.feature.rookie

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.domain.usecase.GetChartIdolIdsUseCase
import net.ib.mn.databinding.FragmentRookieRankingBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.feature.generic.GenericRankingAdapter
import net.ib.mn.feature.generic.GenericRankingClickListener
import net.ib.mn.feature.generic.GenericRankingViewModel
import net.ib.mn.feature.generic.GenericRankingViewModelFactory
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.core.model.ChartModel
import net.ib.mn.fragment.MiracleRankingFragment
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.preventUnwantedHorizontalScroll
import net.ib.mn.utils.livedata.SingleEventObserver
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@AndroidEntryPoint
class RookieRankingFragment : BaseFragment(), OnScrollToTopListener,
    BaseDialogFragment.DialogResultHandler{

    @Inject
    lateinit var getChartIdolIdsUseCase: GetChartIdolIdsUseCase

    @Inject
    lateinit var getIdolsByIdsUseCase: GetIdolsByIdsUseCase
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var accountManager: IdolAccountManager

    private lateinit var binding: FragmentRookieRankingBinding
    private lateinit var viewModel: GenericRankingViewModel

    private lateinit var chartModel: ChartModel

    private lateinit var rankingAdapter: GenericRankingAdapter
    private lateinit var models: ArrayList<IdolModel>

    private lateinit var animator: SimpleItemAnimator
    private lateinit var dateFormat: SimpleDateFormat

    private var isScrolling = false

    private var timerJob: Job? = null
    private val refreshIntervalSeconds = if (BuildConfig.DEBUG) 5L else 10L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userVisibleHint = false // true가 기본값이라 fragment가 보이지 않는 상태에서도 visible한거로 처리되는 문제 방지

        arguments?.let {
            chartModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelable(ARG_CHART_MODEL, ChartModel::class.java) ?: ChartModel()
            } else {
                it.getParcelable(ARG_CHART_MODEL) ?: ChartModel()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRookieRankingBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        val factory = GenericRankingViewModelFactory(
            requireContext(),
            SavedStateHandle(),
            getChartIdolIdsUseCase,
            getIdolsByIdsUseCase,
            usersRepository,
            chartModel.code ?: "")
        viewModel = ViewModelProvider(this, factory)[GenericRankingViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        observeVM()
    }

    override fun onResume() {
        super.onResume()

        viewModel.refreshIfNeeded() // 타이밍 문제로 목록을 DB에서 못가져온 경우 대비
        if (RookieContainerFragment.tabCheck) startTimer()
    }

    override fun onPause() {
        super.onPause()

        if (RookieContainerFragment.tabCheck) stopTimer()
    }

    override fun onScrollToTop() {
        if(!::binding.isInitialized) return
        binding.rvRanking.scrollToPosition(0)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initUI() {
        models = ArrayList()

        dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        binding.rvRanking.layoutManager =
            LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)
        if (Util.getPreferenceBool(context, Const.PREF_ANIMATION_MODE, false)) {
            animator = DefaultItemAnimator()
            animator.supportsChangeAnimations = true
            binding.rvRanking.itemAnimator = animator
        } else {
            binding.rvRanking.itemAnimator = null
        }

        // 막다른 곳에서 스크롤할 때 삐딱하게 스크롤하면 좌우로 넘어감 방지
        binding.rvRanking.preventUnwantedHorizontalScroll()

        rankingAdapter = GenericRankingAdapter(
            models,
            onClickListener = object : GenericRankingClickListener {
                override fun onItemClicked(item: IdolModel?) {
                    if (Util.mayShowLoginPopup(activity) || activity == null) return

                    startActivityForResult(
                        CommunityActivity.createIntent(requireActivity(), item?: return),
                        RequestCode.COMMUNITY_OPEN.value
                    )
                }

                override fun onVote(item: IdolModel) {
                    viewModel.onVote(item)
                }
            },
            mGlideRequestManager
        )

        rankingAdapter.setHasStableIds(true)
        binding.rvRanking.apply {
            adapter = rankingAdapter
            addItemDecoration(divider)
            setHasFixedSize(true)
            setItemViewCacheSize(20) // 이걸 해줘야 상위 아이템 투표수 애니메이션이 제대로 나옴

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                    super.onScrollStateChanged(recyclerView, newState)
                    if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                        isScrolling = false
                        mGlideRequestManager.resumeRequests()
                    } else {
                        isScrolling = true
                        mGlideRequestManager.pauseRequests()
                    }
                }
            })
        }
    }

    private fun isShowEmptyView(isEmpty: Boolean) = with(binding) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvRanking.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun observeVM() = with(viewModel) {
        rankingList.observe(viewLifecycleOwner, SingleEventObserver {

            isShowEmptyView(it.isEmpty())

            rankingAdapter.apply {
                setItems(it)
            }
        })

        errorToast.observe(viewLifecycleOwner, SingleEventObserver {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })

        errorToastWithCode.observe(viewLifecycleOwner, SingleEventObserver {
            val msg = if (it == Const.RESPONSE_IS_ACTIVE_TIME_1) {
                getString(R.string.response_users_is_active_time_over)
            } else {
                getString(R.string.msg_not_able_vote)
            }

            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        })

        inActiveVote.observe(viewLifecycleOwner, SingleEventObserver {
            val unableUseTime = String.format(
                getString(R.string.msg_unable_use_vote), it.first, it.second
            )
            Util.showIdolDialogWithBtn1(
                activity,
                null,
                unableUseTime
            ) { Util.closeIdolDialog() }
        })

        voteHeart.observe(viewLifecycleOwner, SingleEventObserver {
            val dialogFragment = VoteDialogFragment.getIdolVoteInstance(it.first, it.second, it.third)
            dialogFragment.setTargetFragment(this@RookieRankingFragment, RequestCode.RANKING_VOTE.value)
            dialogFragment.setActivityRequestCode(RequestCode.RANKING_VOTE.value)
            dialogFragment.show(parentFragmentManager, "vote")
        })
    }

    fun startTimer() {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                if (!isScrolling) {
                    viewModel.updateData(getIdolsByIdsUseCase)
                }
                delay(TimeUnit.SECONDS.toMillis(refreshIntervalSeconds))
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.RANKING_VOTE.value && resultCode == BaseDialogFragment.RESULT_OK) {
            val heart = data!!.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
            if (heart > 0) {
                val idol = data.getSerializableExtra(VoteDialogFragment.PARAM_IDOL_MODEL) as? IdolModel
                val heart = data.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
                UtilK.checkLevelUp(baseActivity, accountManager, idol, heart)

                // TOOD unman new
                viewModel.updateData(getIdolsByIdsUseCase)
                // 투표 후 투표수 즉시 갱신
//                idol?.let {
//                    IdolApiManager.getInstance(requireContext()).updateHeartTop3(arrayListOf(it.getId())) {
//                        viewModel.updateData()
//                    }
//                }
            } else {
                Util.closeProgress()
            }
        }
    }

    companion object {
        const val TAG = "RookieRanking"
        private const val ARG_CHART_MODEL = "chart_model"

        fun newInstance(chartModel: ChartModel): RookieRankingFragment {
            val fragment = RookieRankingFragment()
            val args = Bundle()
            args.putParcelable(ARG_CHART_MODEL, chartModel)
            fragment.arguments = args
            return fragment
        }
    }
}