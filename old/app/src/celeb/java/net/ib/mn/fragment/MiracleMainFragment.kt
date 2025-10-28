package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.databinding.FragmentMiracleMainBinding
import net.ib.mn.feature.generic.GenericAggregatedViewModel
import net.ib.mn.feature.generic.GenericAggregatedViewModelFactory
import net.ib.mn.idols.IdolApiManager
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.domain.usecase.GetChartRanksUseCase
import net.ib.mn.core.model.ChartModel
import net.ib.mn.remote.IdolBroadcastManager
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.utils.trimNewlineWhiteSpace
import net.ib.mn.viewmodel.MiracleMainViewModel
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MiracleMainFragment : BaseFragment(), View.OnClickListener, OnScrollToTopListener {
    private lateinit var binding: FragmentMiracleMainBinding
    private val viewModel: MiracleMainViewModel by viewModels()
    private lateinit var genericAggregatedViewModel: GenericAggregatedViewModel

    private lateinit var realTimeChartModel: ChartModel
    private var accumulateChartModel: ChartModel? = null

    private var isFromAlternate = false

    @Inject
    lateinit var idolBroadcastManager: IdolBroadcastManager

    @Inject
    lateinit var idolApiManager: IdolApiManager

    @Inject
    lateinit var getChartRanksUseCase: GetChartRanksUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        userVisibleHint = false // true가 기본값이라 fragment가 보이지 않는 상태에서도 visible한거로 처리되는 문제 방지

        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMiracleMainBinding.inflate(inflater, container, false)
        binding.apply {
            lifecycleOwner = this@MiracleMainFragment
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        observedVM()
    }

    override fun onResume() {
        super.onResume()

        if (this::genericAggregatedViewModel.isInitialized) {
            genericAggregatedViewModel.getAggregatedRanking()
        }
        idolBroadcastManager.startHeartbeat()
    }

    // 셀럽은 onHiddenChanged가 불림
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        if (BuildConfig.CELEB) {
            handleVisibility(!hidden)
        }
    }

    // 애돌이는 onVisibilityChanged가 불림
    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        if (!BuildConfig.CELEB) {
            handleVisibility(isVisible)
        }
    }

    private fun initUI() = with(binding) {
        btnAggregated.setOnClickListener(this@MiracleMainFragment)
        btnRealtime.setOnClickListener(this@MiracleMainFragment)

        binding.btnRealtime.isSelected = true

        if (isFromAlternate) {
            handleVisibility(true)
        }
    }

    private fun observedVM() = with(viewModel) {
        miracleChartModel.observe(viewLifecycleOwner, SingleEventObserver {
            if (it.isEmpty()) {
                binding.layoutContent.visibility = View.GONE
                binding.tvEmpty.visibility = View.VISIBLE
                return@SingleEventObserver
            }
            setChartModel(it)
            createFragment()

            val genericFactory = GenericAggregatedViewModelFactory(
                requireActivity(),
                SavedStateHandle(),
                getChartRanksUseCase,
                accumulateChartModel?.code ?: realTimeChartModel.code ?: ""
            )
            genericAggregatedViewModel = ViewModelProvider(
                requireActivity(),
                genericFactory
            )[GenericAggregatedViewModel::class.java]

            genericAggregatedViewModel.rankingList.observe(viewLifecycleOwner, Observer { list ->
                setCLickListener(list)
            })
        })
    }

    private fun setChartModel(chartModelList: List<ChartModel>) {
        when (chartModelList.size) {
            1 -> realTimeChartModel = chartModelList.first()
            else -> {
                chartModelList.forEach { chartModel ->
                    when (chartModel.aggregateType.firstOrNull()) {
                        TYPE_ACCUMULATE -> accumulateChartModel = chartModel
                        TYPE_DAILY -> realTimeChartModel = chartModel
                    }
                }
            }
        }
    }

    private fun handleVisibility(isVisible: Boolean) {
        val fm = fm()

        //해당 프래그먼트 없을경우 대비.
        if (fm.findFragmentByTag(MiracleRankingFragment.TAG) == null ||
            fm.findFragmentByTag(MiracleAggregatedFragment.TAG) == null
        ) {
            createFragment()
            return
        }

        val miracleRankingFrag = fm.findFragmentByTag(MiracleRankingFragment.TAG) as MiracleRankingFragment

        if (isVisible) {
            if (!BuildConfig.CELEB) {
                idolApiManager.startTimer()
            }

            if (tabCheck) {   //기적 탭이 눌려있고, 실시간 순위 버튼을 누르고 있다면
                miracleRankingFrag.startTimer()
            }
        } else {
            idolApiManager.stopTimer()
            miracleRankingFrag.stopTimer()
        }
    }

    override fun onClick(v: View?) {
        val fm = fm()
        when (v?.id) {
            binding.btnAggregated.id -> {
                if (!binding.btnAggregated.isSelected) {
                    binding.btnAggregated.isSelected = true
                    binding.btnRealtime.isSelected = false
                    binding.btnAggregated.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.main
                        )
                    )
                    binding.btnRealtime.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.gray200
                        )
                    )
                    binding.miracleAggregated.visibility = View.VISIBLE
                    binding.miracleRealtime.visibility = View.GONE

                    //이달의 기적 실시간순위 타이머 돌던 것 제거
                    val miracleRankingFrag =
                        fm.findFragmentByTag(MiracleRankingFragment.TAG) as? MiracleRankingFragment
                    miracleRankingFrag?.stopTimer()

                    tabCheck = false
                }
            }

            binding.btnRealtime.id -> {
                if (!binding.btnRealtime.isSelected) {
                    binding.btnAggregated.isSelected = false
                    binding.btnRealtime.isSelected = true
                    binding.btnAggregated.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.gray200
                        )
                    )
                    binding.btnRealtime.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.main
                        )
                    )
                    binding.miracleAggregated.visibility = View.GONE
                    binding.miracleRealtime.visibility = View.VISIBLE
                    val miracleRankingFrag =
                        fm.findFragmentByTag(MiracleRankingFragment.TAG) as? MiracleRankingFragment
                    miracleRankingFrag?.startTimer()
                    tabCheck = true
                }
            }
        }
    }


    private fun createFragment() {
        if (!::realTimeChartModel.isInitialized) return

        val fm = fm()
        val fragmentTransaction = fm.beginTransaction()

        if (fm.findFragmentByTag(MiracleRankingFragment.TAG)?.isAdded != true) {
            val miracleRankingFrag = MiracleRankingFragment.newInstance(realTimeChartModel)
            fragmentTransaction.add(
                R.id.miracle_ranking_realtime,
                miracleRankingFrag,
                MiracleRankingFragment.TAG
            )
        }
        if (fm.findFragmentByTag(MiracleAggregatedFragment.TAG)?.isAdded != true) {
            val miracleAggregatedFrag = MiracleAggregatedFragment.newInstance(accumulateChartModel ?: realTimeChartModel)
            fragmentTransaction.add(
                R.id.miracle_ranking_aggregated,
                miracleAggregatedFrag,
                MiracleAggregatedFragment.TAG
            )
        }
        fragmentTransaction.commitAllowingStateLoss() // 크래시 발생하여 commitAllowingStateLoss()로 변경. 저장하는 state가 없으니 이래도 될듯.

        tabCheck = true
    }

    fun fm(): FragmentManager {
        return childFragmentManager
    }

    @SuppressLint("StringFormatMatches")
    private fun setCLickListener(rankingList: ArrayList<AggregateRankModel>) = with(binding) {
        ivShare.setOnClickListener {

            val insertChartModel = accumulateChartModel ?: realTimeChartModel
            val msg = if (!isExistMyMost(rankingList)) {
                val ranks = (0..2).map { index ->
                    val name = rankingList.getOrNull(index)?.name ?: ""
                    val rank = rankingList.getOrNull(index)?.scoreRank ?: ""
                    Pair(name, rank)
                }

                String.format(
                    Locale.getDefault(),
                    if (BuildConfig.CELEB) getString(R.string.celeb_miracle_n_share_msg) else getString(R.string.miracle_n_share_msg),
                    insertChartModel.targetMonth,
                    *ranks.flatMap { (name, rank) ->
                        listOf(String.format(getString(R.string.rank_format), rank), name)
                    }.toTypedArray()
                )
            } else {
                val account = IdolAccount.getAccount(context)
                val rank = rankingList.find { it.idolId == account?.most?.getId() }?.scoreRank

                String.format(
                    Locale.getDefault(),
                    if (BuildConfig.CELEB) getString(R.string.celeb_miracle_share_msg) else getString(
                        R.string.miracle_share_msg
                    ),
                    insertChartModel.targetMonth,
                    account?.most?.getName(context),
                    rank.toString(),
                    insertChartModel.targetMonth
                )
            }

            setUiActionFirebaseGoogleAnalyticsFragment(
                GaAction.MIRACLE_SHARE.actionValue,
                GaAction.MIRACLE_SHARE.label
            )

            val url = LinkUtil.getAppLinkUrl(
                context = context ?: return@setOnClickListener,
                params = listOf(LinkStatus.MIRACLE.status)
            )

            UtilK.linkStart(context = context, url = url, msg = msg.trimNewlineWhiteSpace())
        }
    }

    private fun isExistMyMost(idols: ArrayList<AggregateRankModel>): Boolean {
        val account = IdolAccount.getAccount(context)
        return idols.any { it.idolId == account?.most?.getId() }
    }

    override fun onScrollToTop() {
        (fm().findFragmentByTag(MiracleRankingFragment.TAG) as? MiracleRankingFragment)?.onScrollToTop()
        (fm().findFragmentByTag(MiracleAggregatedFragment.TAG) as? MiracleAggregatedFragment)?.onScrollToTop()
    }

    companion object {
        const val ARG_CHART_MODEL = "chart_model"
        const val TYPE_ACCUMULATE = "A"
        const val TYPE_DAILY = "D"

        //하단 탭 눌렸는지 체크
        var tabCheck = true
    }
}
