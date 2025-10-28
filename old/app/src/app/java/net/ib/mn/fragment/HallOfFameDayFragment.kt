package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.SavedStateHandle
import androidx.recyclerview.widget.DividerItemDecoration
import com.addisonelliott.segmentedbutton.SegmentedButton
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.adapter.HallOfFameDayAdapter
import net.ib.mn.databinding.FragmentHalloffameDayBinding
import net.ib.mn.feature.halloffame.HallOfFameTopHistoryActivity
import net.ib.mn.model.HallHistoryModel
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.MainTypeCategory
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.UtilK
import net.ib.mn.utils.ext.preventUnwantedHorizontalScroll
import net.ib.mn.utils.livedata.SingleEventObserver
import net.ib.mn.viewmodel.HallOfFameViewModel
import net.ib.mn.viewmodel.MainViewModel
import java.text.SimpleDateFormat

/**
 * Copyright 2023-01-12,목,15:28. ExodusEnt Corp. All rights reserved.
 *
 * @author Kim Min Gue <mingue0605@myloveidol.com>
 * Description: 명예전당 개인/그룹 일일 Fragment
 *
 **/
class HallOfFameDayFragment : BaseFragment(), HallOfFameDayAdapter.OnClickListener,
    View.OnClickListener {

    private lateinit var binding: FragmentHalloffameDayBinding

    private var historyParam: String? = null    //서버에 보내는 날짜
    private var historyArray: List<HallHistoryModel>? = null
    private var hallOfFameDayAdapter: HallOfFameDayAdapter? = null
    private var tagArrayList = listOf<String?>() //서버에서 내려준 날짜 기간 저장하는 arrayList
    private var sortByRecentList: MutableList<HallModel> = mutableListOf() //최신순 정렬된 아이템 리스트

    private var currentSegmentIndex = 0
    private var currentPosition = 0                     //현재 내가 바라보고 있는 달 위치 (tagArrayList 위치)

    private val hallOfFameViewModel: HallOfFameViewModel by viewModels({ requireParentFragment() })
    private val mainViewModel: MainViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_halloffame_day, container, false)
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
        hallOfFameViewModel.getHofDayData(
            requireActivity(),
            hallOfFameViewModel.getChartCode(
                0,
                mainViewModel.isMaleGender.value?.peekContent() ?: true
            ),
            null
        )
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

            segmentedButton.text = chartCodeInfo.name // 텍스트 수정 등
            segmentedButton.setOnClickListener {
                if (index == currentSegmentIndex) return@setOnClickListener

                currentSegmentIndex = index
                historyParam = tagArrayList[currentPosition]
                val isMale = mainViewModel.isMaleGender.value?.peekContent() ?: true
                hallOfFameViewModel.getHofDayData(
                    requireActivity(),
                    hallOfFameViewModel.getChartCode(index, isMale),
                    historyParam
                )
            }

            includedBinding.sbgGroup.addView(segmentedButton)
        }
    }

    private fun getDataFromVM() {

        hallOfFameViewModel.errorToast.observe(
            viewLifecycleOwner,
            SingleEventObserver { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            })

        hallOfFameViewModel.dayHofList.observe(viewLifecycleOwner) { separatedList ->

            if (separatedList.isEmpty()) {
                if (currentPosition == 0) {
                    binding.tvEmpty.text = getString(R.string.label_rank_daily_no_data)
                } else {
                    binding.tvEmpty.text = getString(R.string.label_rank_daily_m_no_data)
                }
                binding.tvEmpty.visibility = View.VISIBLE
                hallOfFameDayAdapter?.clear()
                return@observe
            }

            binding.tvEmpty.visibility = View.GONE

            hallOfFameDayAdapter?.setItems(separatedList.toMutableList())
        }

        hallOfFameViewModel.tagList.observe(viewLifecycleOwner) {
            this.tagArrayList = it
        }

        hallOfFameViewModel.historyList.observe(viewLifecycleOwner) {
            this.historyArray = it
        }

        hallOfFameViewModel.setPrevNextVisibility.observe(viewLifecycleOwner, SingleEventObserver {
            prevNextVisibility()
        })
    }

    private fun init() {

        hallOfFameDayAdapter = HallOfFameDayAdapter(
            requireActivity(),
            this,
            this,
            sortByRecentList,
            mGlideRequestManager
        )
        hallOfFameDayAdapter?.setHasStableIds(true)

        with(binding) {
            rvHallDay.adapter = hallOfFameDayAdapter

            val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            divider.setDrawable(
                ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.line_divider
                )!!
            )
            binding.rvHallDay.addItemDecoration(divider)

            ivPrev.setOnClickListener(this@HallOfFameDayFragment)
            ivNext.setOnClickListener(this@HallOfFameDayFragment)

            binding.rvHallDay.preventUnwantedHorizontalScroll()
        }
    }

    fun changeGender(isMale: Boolean) {
        val param = if (tagArrayList.isNotEmpty() && currentPosition in tagArrayList.indices) {
            tagArrayList[currentPosition]
        } else {
            null
        }
        hallOfFameViewModel.getHofDayData(
            requireActivity(),
            hallOfFameViewModel.getChartCode(currentSegmentIndex, isMale),
            param
        )
    }

    //이미지 클릭했을 때 배너 처리
    override fun onPhotoClicked(item: HallModel?, position: Int) {
        binding.rvHallDay.postDelayed({
            // get clicked item view
            val targetView = binding.rvHallDay.findViewHolderForAdapterPosition(position)?.itemView
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
                binding.rvHallDay.getLocationInWindow(location)
                val listviewTop = location[1]
                val listviewBottom = listviewTop + binding.rvHallDay.height
                // check if target bottom is under listview's bottom
                if (targetBottom > listviewBottom) {
                    binding.rvHallDay.smoothScrollBy(
                        targetBottom - listviewBottom + viewHeight,
                        200
                    )
                }
            }
        }, 300)
    }

    //명예전당 화면 이동
    override fun onItemClicked(item: HallModel) {
        val format = SimpleDateFormat("yyyy.MM.dd", LocaleUtil.getAppLocale(requireContext()))
        val formatParam = SimpleDateFormat("yyyy-MM-dd", LocaleUtil.getAppLocale(requireContext()))

        val date: String = format.format(item.createdAt)
        val dateParam: String = formatParam.format(item.createdAt)

        setLeagueFirebaseEvent(GaAction.HOF_DAILY_HISTORY, "hof_history")

        val isMale = mainViewModel.isMaleGender.value?.peekContent() ?: true
        startActivity(
            HallOfFameTopHistoryActivity.createIntent(
                requireContext(),
                date,
                dateParam,
                hallOfFameViewModel.getChartCode(currentSegmentIndex, isMale)
            )
        )
    }

    override fun onClick(view: View) {
        when (view) {
            binding.ivPrev -> {
                if (currentPosition < tagArrayList.size - 1) {
                    currentPosition += 1
                    historyParam = tagArrayList[currentPosition]
                    val isMale = mainViewModel.isMaleGender.value?.peekContent() ?: true
                    hallOfFameViewModel.getHofDayData(
                        requireActivity(),
                        hallOfFameViewModel.getChartCode(currentSegmentIndex, isMale),
                        historyParam
                    )
                }
            }

            binding.ivNext -> {
                if (currentPosition != 0) { //현재 바라보는 달력이 최근 30일이 아닐때만 작동하도록
                    currentPosition -= 1
                    historyParam = tagArrayList[currentPosition]
                    val isMale = mainViewModel.isMaleGender.value?.peekContent() ?: true
                    hallOfFameViewModel.getHofDayData(
                        requireActivity(),
                        hallOfFameViewModel.getChartCode(currentSegmentIndex, isMale),
                        historyParam
                    )
                }
            }
        }
    }

    //전, 후 버튼 상태 처리
    private fun prevNextVisibility() {
        binding.ivPrev.visibility = View.VISIBLE
        binding.ivNext.visibility = View.VISIBLE
        if (currentPosition == 0) { //다음 눌렀을 때 최근 30일이라면,
            with(binding) {
                tvYear.text = getString(R.string.recent)
                tvMonth.text = getString(R.string.thirty_days)
                ivNext.visibility = View.GONE
            }
            return
        }
        if (currentPosition == tagArrayList.size - 1) { //이전 화살표 가려줌.
            binding.ivPrev.visibility = View.GONE
        }
        binding.tvYear.text = historyArray!![currentPosition - 1].historyYear
        binding.tvMonth.text = historyArray!![currentPosition - 1].historyMonth
    }

    private fun setLeagueFirebaseEvent(gaAction: GaAction, screenType: String) {
        if (BuildConfig.CHINA) {
            return
        }
        val category = Util.getPreference(activity, Const.PREF_DEFAULT_CATEGORY)
        val type =
            if (HallOfFameFragment.loaderStatus == HallOfFameFragment.LOADER_PERSON) "S" else "G"

        setUiActionFirebaseGoogleAnalyticsFragment(
            gaAction.actionValue,
            UtilK.getFirebaseLabel(
                MainTypeCategory.valueOf(category).value,
                MainTypeCategory.valueOf(type).value,
                screenType
            )
        )
    }

    fun scrollToTop() {
        if(!::binding.isInitialized) return
        binding.rvHallDay.scrollToPosition(0)
    }
}