/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 명예전당 개인/그룹 일일 Fragment
 *
 * */

package net.ib.mn.fragment


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.HallOfFameDayAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.HofsRepository
import net.ib.mn.databinding.FragmentHalloffameDayBinding
import net.ib.mn.feature.halloffame.HallOfFameTopHistoryActivity
import net.ib.mn.model.HallHistoryModel
import net.ib.mn.model.HallModel
import net.ib.mn.utils.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class HallOfFameDayFragment : BaseFragment(), HallOfFameDayAdapter.OnClickListener, View.OnClickListener {

    private lateinit var binding: FragmentHalloffameDayBinding
    private lateinit var simpleDateFormat: SimpleDateFormat

    private var historyParam: String? = null // 서버에 보내는 날짜
    private var type: String? = null
    private var category: String? = null
    private var historyArray: ArrayList<HallHistoryModel>? = null
    private var hallOfFameDayAdapter: HallOfFameDayAdapter? = null
    private var tagArrayList = ArrayList<String?>() // 서버에서 내려준 날짜 기간 저장하는 arrayList
    private var sortByRecentList: MutableList<HallModel> = mutableListOf() // 최신순 정렬된 아이템 리스트
    private var sortByHeartList: MutableList<HallModel> = mutableListOf() // 하트순 정렬된 아이템 리스트
    private var typeName: String? = null

    var currentPosition = 0 // 현재 내가 바라보고 있는 달 위치 (tagArrayList 위치)
    var isHeartFilter: Boolean = false // 바텀시트 최신순 or 하트순 체크하는 값

    private lateinit var mAggSheetFragment: BottomSheetFragment

    @Inject
    lateinit var hofsRepository: HofsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_halloffame_day, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        hallOfFameDayAdapter = HallOfFameDayAdapter(requireContext(), this, this, sortByRecentList, mGlideRequestManager)
        hallOfFameDayAdapter?.setHasStableIds(true)

        with(binding) {
            rvHallDay.adapter = hallOfFameDayAdapter

            val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
            divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)
            binding.rvHallDay.addItemDecoration(divider)

            ivPrev.setOnClickListener(this@HallOfFameDayFragment)
            ivNext.setOnClickListener(this@HallOfFameDayFragment)
            tvDailyTypeFilter.setOnClickListener(this@HallOfFameDayFragment)
        }

        simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()) // 서버에서 리그 시작 날짜 yyyy.M.d (2022.1.1) 형식으로 줌
    }

    fun loadResources(type: String?, category: String?, historyParam: String?, typeName: String?) {
        this.typeName = typeName
        this.type = type
        this.category = category

        prevNextVisibility()

        // 애돌과 다르게 타입을 통일시켜 사용하고 있어 종합이 아닐 경우, typeName이 null이 아니기 때문에 추가. null 체크를 안하면 종합일 경우에 빈값으로 보이는 문제가 생김
        if (!typeName.isNullOrEmpty()) {
            binding.tvDailyTypeFilter.text = this.typeName
        }
        // 애돌과 다르게 타입을 통일시켜 사용하고 있어 누적, 일일 왔다갔다 할 때 historyParam을 Null로 보내고 있어 날짜가 초기화되는 현상을 막고자 추가
        if (!historyParam.isNullOrEmpty()) {
            this.historyParam = historyParam
        }

        lifecycleScope.launch {
            hofsRepository.get(
                code = null,
                type = type,
                category = category,
                historyParam = (this@HallOfFameDayFragment).historyParam,
                listener = { obj ->
                    val items: ArrayList<HallModel> = ArrayList()

                    if (!obj.optBoolean("success")) {
                        val responseMsg = ErrorControl.parseError(activity, obj)
                        Toast.makeText(context, responseMsg, Toast.LENGTH_SHORT).show()
                        return@get
                    }
                    val array = obj.getJSONArray("objects")
                    val gson = IdolGson.getInstance(true)
                    for (i in 0 until array.length()) {
                        items.add(gson.fromJson(array.getJSONObject(i).toString(), HallModel::class.java))
                    }
                    items.reverse()
                    sortByRecentList.clear()
                    sortByRecentList.addAll(items)

                    sortByHeartList.clear()
                    sortByHeartList.addAll(sortByRecentList)
                    sortByHeartList.sortWith(
                        Comparator { lhs, rhs ->
                            when {
                                lhs!!.heart > rhs!!.heart -> -1
                                lhs.heart < rhs.heart -> 1
                                else -> {
                                    lhs.rank.compareTo(rhs.rank)
                                }
                            }
                        },
                    )
                    for (i in 0 until sortByHeartList.size) {
                        sortByHeartList[i].rank = i
                    }

                    val historyJosnArray = obj.getJSONArray("history")
                    // historyParam이 없을 때에만 historyArray를 갱신. 이렇게 안하면 다른달 것 볼 때 월 목록이 이상해짐.
                    if ((this@HallOfFameDayFragment).historyParam == null) {
                        historyArray = ArrayList<HallHistoryModel>()
                        for (i in 0 until historyJosnArray.length()) {
                            historyArray?.add(gson.fromJson(historyJosnArray.getJSONObject(i).toString(), HallHistoryModel::class.java))
                        }
                        tagArrayList.clear()
                        historyArray!!.reverse()
                        tagArrayList.add(null)
                        for (i in historyArray!!.indices) {
                            // 서버에서 내려준 날짜 기간
                            val tag = (
                                "&" + historyArray!![i].historyParam +
                                    "&" + historyArray!![i].nextHistoryParam
                                )
                            tagArrayList.add(tag)
                        }
                    }

                    applyItems(sortByRecentList, isHeartFilter)
                },
                errorListener = { throwable ->
                    Toast.makeText(
                        activity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    // 뷰 처리
    fun applyItems(items: MutableList<HallModel>, isHeartFilter: Boolean) {
        activity?.runOnUiThread {
            if (items.size == 0) {
                if (currentPosition == 0) {
                    binding.tvEmpty.text = getString(R.string.label_rank_daily_no_data)
                }else {
                    binding.tvEmpty.text = getString(R.string.label_rank_daily_m_no_data)
                }
                binding.tvEmpty.visibility = View.VISIBLE
                hallOfFameDayAdapter?.clear()
                return@runOnUiThread
            }
            binding.tvEmpty.visibility = View.GONE

            val sheet = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_HALL_OF_FAME_FILTER)
            binding.llFilter.setOnClickListener {
                val tag = "filter"
                val oldFrag = requireActivity().supportFragmentManager.findFragmentByTag(tag)
                if (oldFrag == null) {
                    sheet.show(requireActivity().supportFragmentManager, tag)
                }
            }
            if (isHeartFilter) {
                filterByHeart()
                return@runOnUiThread
            }
            filterByLatest()
        }
    }

    // 최신순 정렬
    fun filterByLatest() {
        setUiActionFirebaseGoogleAnalyticsFragment(
            GaAction.HOF_ORDER_TIME.actionValue,
            GaAction.HOF_ORDER_TIME.label,
        )
        isHeartFilter = false
        binding.tvFilter.text = getString(R.string.freeboard_order_newest)
        hallOfFameDayAdapter?.setItems(sortByRecentList)
    }

    // 하트순 정렬
    fun filterByHeart() {
        setUiActionFirebaseGoogleAnalyticsFragment(
            GaAction.HOF_ORDER_HEART.actionValue,
            GaAction.HOF_ORDER_HEART.label,
        )
        isHeartFilter = true
        binding.tvFilter.text = getString(R.string.order_by_heart)
        hallOfFameDayAdapter?.bannerClear()
        hallOfFameDayAdapter?.setItems(sortByHeartList)
    }

    // 이미지 클릭했을 때 배너 처리
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
                    binding.rvHallDay.smoothScrollBy(targetBottom - listviewBottom + viewHeight, 200)
                }
            }
        }, 300)
    }

    // 명예전당 화면 이동
    override fun onItemClicked(item: HallModel) {
        val format = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
        val formatParam = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val date: String = format.format(item.createdAt)
        val dateParam: String = formatParam.format(item.createdAt)

        startActivity(HallOfFameTopHistoryActivity.createIntent(requireContext(), type ?: "", date, dateParam, category ?: ""))
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.ivPrev.id -> {
                if (currentPosition < tagArrayList.size - 1) {
                    currentPosition += 1
                    historyParam = tagArrayList[currentPosition]
                    loadResources(type, category, historyParam, typeName)
                }
            }
            binding.ivNext.id -> {
                if (currentPosition != 0) { // 현재 바라보는 달력이 최근 30일이 아닐때만 작동하도록
                    currentPosition -= 1
                    historyParam = tagArrayList[currentPosition]
                    loadResources(type, category, historyParam, typeName)
                }
            }
            binding.tvDailyTypeFilter.id -> {
                mAggSheetFragment = BottomSheetFragment.newInstance(BottomSheetFragment.FLAG_HALL_OF_FAME_TYPE_FILTER, HallOfFameFragment.LOADER_DAY)
                val tag = "filter"
                val oldTag = requireActivity().supportFragmentManager.findFragmentByTag(tag)
                if (oldTag == null) {
                    mAggSheetFragment.show(requireActivity().supportFragmentManager, tag)
                }
            }
        }
    }

    // 전, 후 버튼 상태 처리
    private fun prevNextVisibility() {
        binding.ivPrev.visibility = View.VISIBLE
        binding.ivNext.visibility = View.VISIBLE
        if (currentPosition == 0) { // 다음 눌렀을 때 최근 30일이라면,
            with(binding) {
                tvYear.text = getString(R.string.recent)
                tvMonth.text = getString(R.string.thirty_days)
                ivNext.visibility = View.GONE
            }
            return
        }
        if (currentPosition == tagArrayList.size - 1) { // 이전 화살표 가려줌.
            binding.ivPrev.visibility = View.GONE
        }
        binding.tvYear.text = historyArray!![currentPosition - 1].historyYear
        binding.tvMonth.text = historyArray!![currentPosition - 1].historyMonth
    }

    fun scrollToTop() {
        if(!::binding.isInitialized) return
        binding.rvHallDay.scrollToPosition(0)
    }
}