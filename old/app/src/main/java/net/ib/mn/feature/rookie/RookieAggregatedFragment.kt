package net.ib.mn.feature.rookie

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.core.domain.usecase.GetChartRanksUseCase
import net.ib.mn.databinding.FragmentRookieAggregatedBinding
import net.ib.mn.feature.generic.GenericAggregatedAdapter
import net.ib.mn.feature.generic.GenericAggregatedViewModel
import net.ib.mn.feature.generic.GenericAggregatedViewModelFactory
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.core.model.ChartModel
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.Toast
import net.ib.mn.utils.ext.preventUnwantedHorizontalScroll
import net.ib.mn.utils.livedata.SingleEventObserver
import javax.inject.Inject

@AndroidEntryPoint
class RookieAggregatedFragment: BaseFragment(), OnScrollToTopListener {

    private lateinit var binding: FragmentRookieAggregatedBinding
    private lateinit var viewModel: GenericAggregatedViewModel
    private val rookieContainerViewModel: RookieContainerViewModel by activityViewModels()

    private lateinit var chartModel: ChartModel

    private lateinit var aggregatedAdapter: GenericAggregatedAdapter
    @Inject
    lateinit var getChartRanksUseCase: GetChartRanksUseCase

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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentRookieAggregatedBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this

        val factory = GenericAggregatedViewModelFactory(
            requireContext(),
            SavedStateHandle(),
            getChartRanksUseCase,
            chartModel.code ?: "")
        viewModel = ViewModelProvider(this, factory)[GenericAggregatedViewModel::class.java]

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        observedVM()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getAggregatedRanking()
    }

    override fun onScrollToTop() {
        if(!::binding.isInitialized) return
        binding.rvRanking.scrollToPosition(0)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initUI() {
        val divider = DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        binding.rvRanking.layoutManager = LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)

        aggregatedAdapter = GenericAggregatedAdapter(
            arrayListOf()
        )
        aggregatedAdapter.setHasStableIds(true)

        binding.rvRanking.apply {
            adapter = aggregatedAdapter
            itemAnimator = null
            addItemDecoration(divider)
            setHasFixedSize(true)
        }

        // 막다른 곳에서 스크롤할 때 삐딱하게 스크롤하면 좌우로 넘어감 방지
        binding.rvRanking.preventUnwantedHorizontalScroll()
    }

    private fun observedVM() = with(viewModel) {
        rankingList.observe(viewLifecycleOwner, Observer {

            isShowEmptyView(it.isEmpty())

            aggregatedAdapter.apply {
                setItems(it)
                rookieContainerViewModel.setFirstPlaceRankerName(if (it.isEmpty()) "" else it.first().name)
            }
        })

        errorToast.observe(viewLifecycleOwner, SingleEventObserver {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        })
    }

    private fun isShowEmptyView(isEmpty: Boolean) = with(binding) {
        tvEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        rvRanking.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    companion object {
        const val TAG = "RookieAggregated"
        private const val ARG_CHART_MODEL = "chart_model"

        fun newInstance(chartModel: ChartModel): RookieAggregatedFragment {
            val fragment = RookieAggregatedFragment()
            val args = Bundle()
            args.putParcelable(ARG_CHART_MODEL, chartModel)
            fragment.arguments = args
            return fragment
        }
    }
}