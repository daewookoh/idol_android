package net.ib.mn.fragment

import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.activity.LevelHeartGuideActivity
import net.ib.mn.databinding.FragmentHalloffameBinding
import net.ib.mn.core.data.model.ChartCodeInfo
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.OnScrollToTopListener
import net.ib.mn.utils.Util
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.HallOfFameViewModel
import net.ib.mn.viewmodel.MainViewModel

/**
 * Copyright 2023-01-4,수,17:6. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 명예전당 일일, 누적 부모 Fragment
 *
 *
 **/
// ⚠️⚠️⚠️⚠️⚠️ 셀럽은 별도 파일로 존재하므로 항상 같이 수정해야 함 !! XML 파일도 별도로 있음!! ⚠️⚠️⚠️⚠️⚠️

@AndroidEntryPoint
class HallOfFameFragment : BaseFragment(), View.OnClickListener, OnScrollToTopListener {
    private val mainViewModel: MainViewModel by activityViewModels()

    private lateinit var binding: FragmentHalloffameBinding
    private lateinit var hallOfFameAggFragment: HallOfFameAggFragment
    private lateinit var hallOfFameDayFragment: HallOfFameDayFragment

    private val hallOfFameViewModel: HallOfFameViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_halloffame, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val maleChartCodes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableArrayList(
                    NewRankingFragment.ARG_MALE_CHART_CODE,
                    ChartCodeInfo::class.java
                ) ?: return
            } else {
                it.getParcelableArrayList(NewRankingFragment.ARG_MALE_CHART_CODE) ?: return
            }

            val feMaleChartCodes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                it.getParcelableArrayList(
                    NewRankingFragment.ARG_FEMALE_CHART_CODE,
                    ChartCodeInfo::class.java
                ) ?: return
            } else {
                it.getParcelableArrayList(NewRankingFragment.ARG_FEMALE_CHART_CODE) ?: return
            }

            hallOfFameViewModel.setChartCodes(maleChartCodes, feMaleChartCodes)
        }

        init()
        observedVm()
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter()

        filter.addAction(Const.REFRESH)
        if (Const.USE_ANIMATED_PROFILE) {
            filter.addAction(Const.PLAYER_START_RENDERING)
        }
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loaderStatus = LOADER_AGG_PERSON    // 뷰 사라졌을 경우 바라보고 있는 값 초기화
    }

    private fun init() {
        createFragment()
        binding.btnDaily.setOnClickListener(this)
        binding.btnAggregated.setOnClickListener(this)
        binding.btnAggregated.isSelected = true
    }

    private fun observedVm() {
        mainViewModel.changeHallOfGender.observe(viewLifecycleOwner, SingleEventObserver {
            val isMale =
                Util.getPreference(requireContext(), Const.PREF_DEFAULT_CATEGORY) == Const.TYPE_MALE
            hallOfFameAggFragment.changeGender(isMale)
            hallOfFameDayFragment.changeGender(isMale)
        })
    }

    override fun onVisibilityChanged(isVisible: Boolean) {
        super.onVisibilityChanged(isVisible)

        //해당 프래그먼트 없을경우 대비.
        if (childFragmentManager.findFragmentByTag("HallRealtime") == null ||
            childFragmentManager.findFragmentByTag("HallAggregated") == null
        ) {
            createFragment()
        }
    }

    override fun onClick(v: View?) {
        when (v) {
            binding.btnAggregated -> {
                if (!binding.btnAggregated.isSelected) {
                    binding.btnAggregated.isSelected = true
                    binding.btnDaily.isSelected = false
                    binding.btnAggregated.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_default
                        )
                    )
                    binding.btnDaily.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_dimmed
                        )
                    )
                    binding.clHallRealtime.visibility = View.GONE
                    binding.clHallAggregated.visibility = View.VISIBLE
                }
            }

            binding.btnDaily -> {
                if (!binding.btnDaily.isSelected) {
                    binding.btnAggregated.isSelected = false
                    binding.btnDaily.isSelected = true
                    binding.btnAggregated.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_dimmed
                        )
                    )
                    binding.btnDaily.setTextColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.text_default
                        )
                    )

                    binding.clHallRealtime.visibility = View.VISIBLE
                    binding.clHallAggregated.visibility = View.GONE
                }
            }
        }
    }

    private fun createFragment() {
        val tagAgg = "HallAggregated"
        val tagDay = "HallRealtime"
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val aggFragment = childFragmentManager.findFragmentByTag(tagAgg)
        if (aggFragment == null) {
            hallOfFameAggFragment = HallOfFameAggFragment()
            fragmentTransaction.replace(
                R.id.rl_hall_aggregated,
                hallOfFameAggFragment,
                tagAgg
            )
        } else {
            hallOfFameAggFragment = aggFragment as HallOfFameAggFragment
        }

        val dayFragment = childFragmentManager.findFragmentByTag(tagDay)
        if (dayFragment == null) {
            hallOfFameDayFragment = HallOfFameDayFragment()
            fragmentTransaction.replace(
                R.id.rl_hall_realtime,
                hallOfFameDayFragment,
                tagDay
            )
        } else {
            hallOfFameDayFragment = dayFragment as HallOfFameDayFragment
        }

        fragmentTransaction.commitAllowingStateLoss()// 크래시 발생하여 commitAllowingStateLoss()로 변경. 저장하는 state가 없으니 이래도 될듯.
    }

    override fun onScrollToTop() {
        hallOfFameAggFragment.scrollToTop()
        hallOfFameDayFragment.scrollToTop()
    }

    // FIXME 필요 없는 코드인데 애돌 셀럽 합쳐놓은거 영향으로 남겨둬야함
    fun filterClick(isHeart: Boolean) {}

    companion object {
        const val ARG_MALE_CHART_CODE = "maleChartCode"
        const val ARG_FEMALE_CHART_CODE = "femaleChartCode"

        var loaderStatus = 0        //현재 바라보는 곳
        const val LOADER_AGG_PERSON = 0   //개인 누적
        const val LOADER_PERSON = 2       //개인 일일
    }
}