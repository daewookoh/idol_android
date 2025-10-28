package net.ib.mn.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.recyclerview.widget.DividerItemDecoration
import com.addisonelliott.segmentedbutton.SegmentedButton
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.HallOfFameAggHistoryLeagueActivity
import net.ib.mn.activity.LevelHeartGuideActivity
import net.ib.mn.adapter.HallOfFameAggAdapter
import net.ib.mn.databinding.FragmentHalloffameAggBinding
import net.ib.mn.core.data.model.AggregateRankModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.MainTypeCategory
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.preventUnwantedHorizontalScroll
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.HallOfFameViewModel
import net.ib.mn.viewmodel.MainViewModel

/**
 * Copyright 2023-01-4,수,17:6. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 명예전당 개인/그룹 누적 Fragment
 *
 **/
@AndroidEntryPoint
class HallOfFameAggFragment : BaseFragment(), HallOfFameAggAdapter.OnClickListener,
    View.OnClickListener {

    private lateinit var binding: FragmentHalloffameAggBinding

    private var hallOfFameAggAdapter: HallOfFameAggAdapter? = null
    private val hallOfFameViewModel: HallOfFameViewModel by viewModels({ requireParentFragment() })
    private val mainViewModel: MainViewModel by activityViewModels()

    private var currentSegmentIndex = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_halloffame_agg, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
        setSegmentGroupView()
        getDataFromVM()
        setInitState()
    }


    private fun setInitState() {
        val isMale = mainViewModel.isMaleGender.value?.peekContent() ?: true
        hallOfFameViewModel.getAggregatedRanking(
            requireActivity(),
            hallOfFameViewModel.getChartCode(0, isMale)
        )
    }

    private fun getDataFromVM() {
        hallOfFameViewModel.rankingList.observe(viewLifecycleOwner, SingleEventObserver {
            applyAggItems(it.toMutableList())
        })

        hallOfFameViewModel.idol.observe(viewLifecycleOwner, SingleEventObserver {
            val isMale = mainViewModel.isMaleGender.value?.peekContent() ?: true
            startActivity(
                HallOfFameAggHistoryLeagueActivity.createIntent(
                    context,
                    it,
                    hallOfFameViewModel.getChartCode(currentSegmentIndex, isMale)
                )
            )
        })
    }

    // 누적순위란? 페이지 이동
    fun openInfoScreen() {
        setUiActionFirebaseGoogleAnalyticsFragment(
            GaAction.HELP_HISTORY.actionValue,
            GaAction.HELP_HISTORY.label
        )
        startActivity(Intent(activity, LevelHeartGuideActivity::class.java))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun init() {
        hallOfFameAggAdapter =
            HallOfFameAggAdapter(requireContext(), this, this, mutableListOf())

        with(binding) {
            rvHallAgg.adapter = hallOfFameAggAdapter

            val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            divider.setDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.line_divider
                )!!
            )
            binding.rvHallAgg.addItemDecoration(divider)

            // 막다른 곳에서 스크롤할 때 삐딱하게 스크롤하면 좌우로 넘어감 방지
            rvHallAgg.preventUnwantedHorizontalScroll()
        }
    }

    private fun setSegmentGroupView() {
        val includedBinding = binding.inSegmentGroup

        val chartCode = if (mainViewModel.isMaleGender.value?.peekContent() == true) {
            hallOfFameViewModel.getMaleChartCodes()
        } else {
            hallOfFameViewModel.getFemaleChartCodes()
        }

        chartCode.forEachIndexed { index, chartCodeInfo ->
            val inflater = LayoutInflater.from(context)
            val segmentedButton = inflater.inflate(
                R.layout.item_hall_of_fame_segment,
                includedBinding.sbgGroup,
                false
            ) as SegmentedButton

            segmentedButton.text = chartCodeInfo.name
            segmentedButton.setOnClickListener {
                if (index == currentSegmentIndex) return@setOnClickListener

                currentSegmentIndex = index
                val isMale = mainViewModel.isMaleGender.value?.peekContent() ?: true

                hallOfFameViewModel.getAggregatedRanking(
                    requireActivity(),
                    hallOfFameViewModel.getChartCode(index, isMale),
                )
            }

            includedBinding.sbgGroup.addView(segmentedButton)
        }
    }

    override fun onPause() {
        super.onPause()

        // 움짤 멈추기
        if (Const.USE_ANIMATED_PROFILE) {
            stopExoPlayer(playerView1)
            stopExoPlayer(playerView2)
            stopExoPlayer(playerView3)
        }
    }

    fun changeGender(isMale: Boolean) {
        hallOfFameViewModel.getAggregatedRanking(
            requireActivity(),
            hallOfFameViewModel.getChartCode(currentSegmentIndex, isMale)
        )
    }

    //누적순위 변화 화면으로 이동
    override fun onItemClicked(item: AggregateRankModel?) {
        setLeagueFirebaseEvent(GaAction.HOF_TREND_CHART, "hof_trend")
        hallOfFameViewModel.getIdolInfo(item?.idolId ?: return)
    }

    private fun applyAggItems(items: MutableList<AggregateRankModel>) {
        if (items.size == 0) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.tvEmpty.text = requireContext().getString(R.string.label_award_aggregated_no_data)
            binding.rvHallAgg.visibility = View.GONE
            return
        }
        binding.tvEmpty.visibility = View.GONE
        binding.rvHallAgg.visibility = View.VISIBLE
        hallOfFameAggAdapter?.setItems(items)
    }

    private fun setLeagueFirebaseEvent(gaAction: GaAction, screenType: String) {
        if (BuildConfig.CHINA) {
            return
        }
        val category = Util.getPreference(activity, Const.PREF_DEFAULT_CATEGORY)
        val type =
            if (HallOfFameFragment.loaderStatus == HallOfFameFragment.LOADER_AGG_PERSON) "S" else "G"

        setUiActionFirebaseGoogleAnalyticsFragment(
            gaAction.actionValue,
            UtilK.getFirebaseLabel(
                MainTypeCategory.valueOf(category).value,
                MainTypeCategory.valueOf(type).value,
                screenType
            )
        )
    }

    override fun onClick(v: View?) {
        // no-op
    }

    fun scrollToTop() {
        if(!::binding.isInitialized) return
        binding.rvHallAgg.scrollToPosition(0)
    }
}