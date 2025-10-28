/**
 * Copyright 2023-07-4,화,17:6. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 명예전당 일일, 누적 부모 Fragment
 *
 **/

package net.ib.mn.fragment

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import net.ib.mn.R
import net.ib.mn.databinding.FragmentHalloffameBinding
import net.ib.mn.activity.LevelHeartGuideActivity
import net.ib.mn.core.data.model.TypeListModel
import net.ib.mn.utils.*
import net.ib.mn.utils.ext.getFontColor
import net.ib.mn.utils.ext.getUiColor

class HallOfFameFragment : BaseFragment(), View.OnClickListener, OnScrollToTopListener {

    private lateinit var binding: FragmentHalloffameBinding
    private lateinit var hallOfFameAggFragment: HallOfFameAggFragment
    private lateinit var hallOfFameDayFragment: HallOfFameDayFragment

    private var currentType: String? = null
    private var currentCategory: String? = null
    private var typeName: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_halloffame, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        binding.btnAggregatedGuide.setOnClickListener(this)
        binding.tabbtnAggregated.setOnClickListener(this)
        binding.tabbtnDaily.setOnClickListener(this)
        binding.tabbtnAggregated.isSelected = true
        binding.tabbtnAggregatedUnderbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.main))
        createFragment()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.btnAggregatedGuide.id -> {
                setUiActionFirebaseGoogleAnalyticsFragment(
                    GaAction.HELP_HISTORY.actionValue,
                    GaAction.HELP_HISTORY.label,
                )
                startActivity(Intent(activity, LevelHeartGuideActivity::class.java))
            }
            binding.tabbtnAggregated.id -> {
                if (binding.tabbtnAggregated.isSelected) {
                    return
                }
                binding.btnAggregatedGuide.visibility = View.VISIBLE
                binding.clHallAggregated.visibility = View.VISIBLE
                binding.btnAggregatedGuide.visibility = View.VISIBLE
                binding.clHallRealtime.visibility = View.GONE
                changeFilterOrTab(true, LOADER_AGG)
                hallOfFameAggFragment.loadAggResources(currentType, currentCategory, typeName)
            }
            binding.tabbtnDaily.id -> {
                if (binding.tabbtnDaily.isSelected) {
                    return
                }
                binding.btnAggregatedGuide.visibility = View.GONE
                binding.clHallAggregated.visibility = View.GONE
                binding.btnAggregatedGuide.visibility = View.GONE
                binding.clHallRealtime.visibility = View.VISIBLE
                changeFilterOrTab(true, LOADER_DAY)
                hallOfFameDayFragment.loadResources(currentType, currentCategory, null, typeName)
            }
        }
    }

    private fun createFragment() {
        val fragmentManager = childFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()

        val firstTypeModel = UtilK.getTypeListArray(requireContext()).first()
        val category = if (firstTypeModel.type == Const.ENTERTAINER) {
            null
        } else if (firstTypeModel.isFemale) {
            Const.CATEGORY_FEMALE
        } else {
            Const.CATEGORY_MALE
        }
        val settingModel = Util.setGenderTypeLIst(listOf(firstTypeModel), requireContext()).find { it.isFemale == firstTypeModel.isFemale }

        currentType = firstTypeModel.type
        currentCategory = category
        typeName = settingModel?.name

        hallOfFameAggFragment = HallOfFameAggFragment.newInstance(firstTypeModel.type, category, settingModel?.name ?: "")
        hallOfFameDayFragment = HallOfFameDayFragment()

        if (fragmentManager.findFragmentByTag("HallAggregated")?.isAdded != true || !::hallOfFameAggFragment.isInitialized) {
            fragmentTransaction.add(
                R.id.rl_hall_aggregated,
                hallOfFameAggFragment,
                "HallAggregated",
            )
        }

        if (fragmentManager.findFragmentByTag("HallRealtime")?.isAdded != true || !::hallOfFameDayFragment.isInitialized) {
            fragmentTransaction.add(
                R.id.rl_hall_realtime,
                hallOfFameDayFragment,
                "HallRealtime",
            )
        }
        fragmentTransaction.commitAllowingStateLoss() // 크래시 발생하여 commitAllowingStateLoss()로 변경. 저장하는 state가 없으니 이래도 될듯.
    }

    //일일 순위에서 하트순, 투표순 바텀시트 클릭 했을 경우
    fun filterClick(isHeart : Boolean){
        if(isHeart){
            hallOfFameDayFragment.filterByHeart()
        }else{
            hallOfFameDayFragment.filterByLatest()
        }
    }

    //타입 변경 또는 탭 변경시 처리
    private fun changeFilterOrTab(isTabChanged: Boolean, id: Int){
        val typeList: TypeListModel
        when(id){
            LOADER_AGG -> {
                typeList = UtilK.getTypeList(requireContext(), currentType + currentCategory)
                binding.tabbtnAggregated.isSelected = true
                binding.tabbtnDaily.isSelected = false
                binding.tabbtnDaily.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dimmed))
                binding.tabbtnDailyUnderbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background))

                if (currentType == null) {
                    binding.tabbtnAggregated.setTextColor(ContextCompat.getColor(requireContext(), R.color.main))
                    binding.tabbtnAggregatedUnderbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.main))
                    return
                }
                binding.tabbtnAggregated.setTextColor(Color.parseColor(typeList.getFontColor(requireContext())))
                binding.tabbtnAggregatedUnderbar.setBackgroundColor(Color.parseColor(typeList.getFontColor(requireContext())))

            }
            LOADER_DAY -> {
                typeList = UtilK.getTypeList(requireContext(), currentType + currentCategory)
                binding.tabbtnAggregated.isSelected = false
                binding.tabbtnDaily.isSelected = true
                binding.tabbtnAggregated.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_dimmed))
                binding.tabbtnAggregatedUnderbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.background))
                if (currentType == null) {
                    binding.tabbtnDaily.setTextColor(ContextCompat.getColor(requireContext(), R.color.main))
                    binding.tabbtnDailyUnderbar.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.main))
                    return
                }
                binding.tabbtnDaily.setTextColor(Color.parseColor(typeList.getFontColor(requireContext())))
                binding.tabbtnDailyUnderbar.setBackgroundColor(Color.parseColor(typeList.getFontColor(requireContext())))
            }
        }
    }

    // 누적, 실시간 둘다 타입을 일치하도록 사용하기 때문에 부모 프래그먼트에서 관리하도록 처리
    fun filterByType(id: Int, type: String?, name: String) {
        typeName = name

        currentType = type
        currentCategory = null
        // type이 AM, AF, SM, SF인 경우에 한해 처리 (AG가 생겼음...)
        arrayOf("AM", "AF", "SM", "SF").forEach {
            if (type == it) {
                currentType = type[0].toString()
                currentCategory = type[1].toString()
            }
        }

        if (id == LOADER_AGG) {
            hallOfFameAggFragment.loadAggResources(currentType, currentCategory, typeName)
        } else {
            hallOfFameDayFragment.loadResources(currentType, currentCategory, null, typeName)
        }

        changeFilterOrTab(false, id)
    }

    // onVisibility 안불려서 사용
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)

        // 해당 프래그먼트 없을경우 대비.
        if (!hidden && (childFragmentManager.findFragmentByTag("HallAggregated") == null || childFragmentManager.findFragmentByTag("HallRealtime") == null)) {
            createFragment()
        }
    }

    override fun onScrollToTop() {
        if (!::hallOfFameAggFragment.isInitialized || !::hallOfFameDayFragment.isInitialized) {
            return
        }
        hallOfFameAggFragment.scrollToTop()
        hallOfFameDayFragment.scrollToTop()
    }

    companion object {
        const val LOADER_AGG = 0 // 누적
        const val LOADER_DAY = 1 // 일일
        
        const val ARG_MALE_CHART_CODE = "maleChartCode"
        const val ARG_FEMALE_CHART_CODE = "femaleChartCode"
    }
}