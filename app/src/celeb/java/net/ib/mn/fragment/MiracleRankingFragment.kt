package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
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
import net.ib.mn.account.IdolAccount
import net.ib.mn.account.IdolAccountManager
import net.ib.mn.activity.CommunityActivity
import net.ib.mn.activity.WebViewActivity
import net.ib.mn.adapter.MiracleRankingAdapter
import net.ib.mn.core.data.repository.UsersRepository
import net.ib.mn.core.domain.usecase.GetChartIdolIdsUseCase
import net.ib.mn.databinding.FragmentMiracleRankingBinding
import net.ib.mn.dialog.BaseDialogFragment
import net.ib.mn.dialog.VoteDialogFragment
import net.ib.mn.feature.generic.GenericAggregatedViewModel
import net.ib.mn.feature.generic.GenericAggregatedViewModelFactory
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.core.model.ChartModel
import net.ib.mn.domain.usecase.GetAllIdolsUseCase
import net.ib.mn.domain.usecase.GetIdolsByIdsUseCase
import net.ib.mn.model.ConfigModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.RequestCode
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.MiracleRankingViewModel
import net.ib.mn.viewmodel.MiracleRankingViewModelFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * 이달의 기적 실시간 순위 보여주는 fragment
 */
// TODO: 2022/10/24
@AndroidEntryPoint
open class MiracleRankingFragment : BaseFragment(),
    BaseDialogFragment.DialogResultHandler,
    OnScrollToTopListener {

    @Inject
    lateinit var getIdolsByIdsUseCase: GetIdolsByIdsUseCase
    @Inject
    lateinit var usersRepository: UsersRepository
    @Inject
    lateinit var getChartIdolIdsUseCase: GetChartIdolIdsUseCase
    @Inject
    lateinit var accountManager: IdolAccountManager

    private lateinit var binding: FragmentMiracleRankingBinding
    private lateinit var viewModel: MiracleRankingViewModel

    private lateinit var chartModel: ChartModel

    private lateinit var mRankingAdapter: MiracleRankingAdapter
    private lateinit var models: ArrayList<IdolModel>

    protected lateinit var animator: SimpleItemAnimator
    private lateinit var dateFormat: SimpleDateFormat

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMiracleRankingBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        val factory = MiracleRankingViewModelFactory(
            requireActivity(),
            SavedStateHandle(),
            chartModel.code ?: "",
            getIdolsByIdsUseCase,
            getChartIdolIdsUseCase,
            usersRepository
        )
        viewModel = ViewModelProvider(this, factory)[MiracleRankingViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        observeVM()
    }

    override fun onResume() {
        super.onResume()
        if (MiracleMainFragment.tabCheck) {   //해당탭이 보이고있을 때 onResume시
            startTimer()
        }
    }

    override fun onPause() {
        super.onPause()
        if (MiracleMainFragment.tabCheck) {  //해당탭이 보이고 있을 때 onPause시
            stopTimer()
        }
    }

    private fun initUI() {
        models = ArrayList()

        dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")

        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
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

        mRankingAdapter = MiracleRankingAdapter(
            requireActivity(),
            models,
            chartModel,
            onClickListener = object : MiracleRankingAdapter.OnClickListener {
                override fun onItemClicked(item: IdolModel?) {
                    openCommunity(item ?: return)
                }

                override fun onVote(item: IdolModel) {
                    viewModel.onVote(item)
                }

                override fun onInfoClicked() {
                    openInfoScreen()
                }
            }
        )

        mRankingAdapter.setHasStableIds(true)
        binding.rvRanking.apply {
            adapter = mRankingAdapter
            addItemDecoration(divider)
            setHasFixedSize(true)
        }
    }

    private fun isShowEmptyView(isEmpty: Boolean) = with(binding) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvRanking.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun observeVM() = with(viewModel) {
        rankingList.observe(viewLifecycleOwner, SingleEventObserver {

            isShowEmptyView(it.isEmpty())

            mRankingAdapter.apply {
                setItems(it, chartModel)
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
            val dialogFragment =
                VoteDialogFragment.getIdolVoteInstance(it.first, it.second, it.third)
            dialogFragment.setActivityRequestCode(RequestCode.RANKING_VOTE.value)
            dialogFragment.show(childFragmentManager, "vote")
        })
    }

    fun startTimer() {
        timerJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                viewModel.updateData()
                delay(TimeUnit.SECONDS.toMillis(refreshIntervalSeconds))
            }
        }
    }

    fun stopTimer() {
        timerJob?.cancel()
    }

    //커뮤니티 들어갔을 경우
    private fun openCommunity(idol: IdolModel) {
        if (Util.mayShowLoginPopup(activity) || activity == null) return

        startActivityForResult(
            CommunityActivity.createIntent(requireActivity(), idol),
            RequestCode.COMMUNITY_OPEN.value
        )
    }

    fun openInfoScreen() {
        val id = ConfigModel.getInstance(requireActivity()).showMiracleInfo

        setUiActionFirebaseGoogleAnalyticsFragment(
            Const.ANALYTICS_BUTTON_PRESS_ACTION,
            GaAction.MIRACLE_INFO.label
        )
        startActivity(
            WebViewActivity.createIntent(requireActivity(), Const.TYPE_EVENT,
            id,
            getString(R.string.title_miracle_month)))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        //커뮤에서 투표했을시
        if (requestCode == RequestCode.COMMUNITY_OPEN.value && resultCode == Activity.RESULT_OK) {

            viewModel.updateData()
        }
    }

    //투표 후 디비 저장된 데이터 호출 및 레벨업 체크
    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == RequestCode.RANKING_VOTE.value && resultCode == BaseDialogFragment.RESULT_OK) {
            val heart = data!!.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
            if (heart > 0) {
//                loadResource()
                // 레벨업 체크
                if (data != null) {

                    val idol: IdolModel =
                        data.getSerializableExtra(VoteDialogFragment.PARAM_IDOL_MODEL) as IdolModel
                    val heart = data.getLongExtra(VoteDialogFragment.PARAM_HEART, 0)
                    UtilK.checkLevelUp(baseActivity, accountManager, idol, heart)
                }
            } else {
                Util.closeProgress()
            }
        }
    }

    companion object {
        const val TAG = "MiracleRanking"
        private const val ARG_CHART_MODEL = "chart_model"

        fun newInstance(chartModel: ChartModel): MiracleRankingFragment {
            val fragment = MiracleRankingFragment()
            val args = Bundle()
            args.putParcelable(ARG_CHART_MODEL, chartModel)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onScrollToTop() {
        if(!::binding.isInitialized) return

        binding.rvRanking.scrollToPosition(0)
    }
}
