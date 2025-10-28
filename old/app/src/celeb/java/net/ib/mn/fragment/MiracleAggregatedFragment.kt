package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.account.IdolAccount
import net.ib.mn.activity.WebViewActivity
import net.ib.mn.adapter.MiracleAggregatedAdapter
import net.ib.mn.databinding.FragmentMiracleRankingBinding
import net.ib.mn.feature.generic.GenericAggregatedViewModel
import net.ib.mn.feature.generic.GenericAggregatedViewModelFactory
import net.ib.mn.link.enum.LinkStatus
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.core.domain.usecase.GetChartRanksUseCase
import net.ib.mn.core.model.ChartModel
import net.ib.mn.model.ConfigModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.link.LinkUtil
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.MiracleAggregatedViewModel
import net.ib.mn.viewmodel.MiracleAggregatedViewModelFactory
import java.util.Locale
import javax.inject.Inject


/**
 * 이달의 기적 누적 순위 보여주는 fragment
 */
@AndroidEntryPoint
class MiracleAggregatedFragment : BaseFragment(), OnScrollToTopListener {
    // view model에서 주입이 안되는 상황이라 일단 여기서 주입 후 factory로 넘겨주기
    @Inject
    lateinit var getChartRanksUseCase: GetChartRanksUseCase

    private lateinit var binding: FragmentMiracleRankingBinding
    private lateinit var viewModel: GenericAggregatedViewModel

    private lateinit var chartModel: ChartModel

    private lateinit var rankingAdapter: MiracleAggregatedAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

        val factory = GenericAggregatedViewModelFactory(
            requireActivity(),
            SavedStateHandle(),
            getChartRanksUseCase,
            chartModel.code ?: ""
        )
        viewModel = ViewModelProvider(requireActivity(), factory)[GenericAggregatedViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        observedVM()
    }

    private fun initUI() {
        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        binding.rvRanking.layoutManager =
            LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)
        rankingAdapter = MiracleAggregatedAdapter(
            arrayListOf(),
            chartModel,
            onClickListener = object : MiracleAggregatedAdapter.OnClickListener {
                override fun onInfoClicked() {
                    val id = ConfigModel.getInstance(requireActivity()).showMiracleInfo

                    setUiActionFirebaseGoogleAnalyticsFragment(
                        Const.ANALYTICS_BUTTON_PRESS_ACTION,
                        GaAction.MIRACLE_INFO.label
                    )
                    startActivity(
                        WebViewActivity.createIntent(
                            requireActivity(), Const.TYPE_EVENT,
                            id,
                            getString(R.string.title_miracle_month)
                        )
                    )
                }
            })

        rankingAdapter.setHasStableIds(true)
        binding.rvRanking.apply {
            itemAnimator = null
        }

        binding.rvRanking.apply {
            adapter = rankingAdapter
            itemAnimator = null
            addItemDecoration(divider)
            setHasFixedSize(true)
        }
    }

    private fun observedVM() = with(viewModel) {
        rankingList.observe(viewLifecycleOwner, Observer {

            isShowEmptyView(it.isEmpty())

            rankingAdapter.apply {
                setItems(it)
            }
        })

        errorToast.observe(viewLifecycleOwner, SingleEventObserver {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })
    }

    private fun stopPlayer() {
        if (Const.USE_ANIMATED_PROFILE) {
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
        }
    }

    override fun onPause() {
        super.onPause()

        // 움짤 멈추기
        stopPlayer()
    }

    override fun onResume() {
        super.onResume()

        if(::viewModel.isInitialized) {
            viewModel.getAggregatedRanking()
        }
    }

    private fun isShowEmptyView(isEmpty: Boolean) = with(binding) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvRanking.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    override fun onScrollToTop() {
        if(!::binding.isInitialized) return

        binding.rvRanking.scrollToPosition(0)
    }

    companion object {
        const val TAG = "MiracleAggregated"
        private const val ARG_CHART_MODEL = "chart_model"

        fun newInstance(chartModel: ChartModel): MiracleAggregatedFragment {
            val fragment = MiracleAggregatedFragment()
            val args = Bundle()
            args.putParcelable(ARG_CHART_MODEL, chartModel)
            fragment.arguments = args
            return fragment
        }
    }
}
