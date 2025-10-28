/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 명예전당 개인/그룹 누적 Fragment
 *
 * */

package net.ib.mn.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.HallOfFameAggHistoryActivity
import net.ib.mn.adapter.HallOfFameAggAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.databinding.FragmentHalloffameAggBinding
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.DateUtil
import net.ib.mn.utils.Toast
import javax.inject.Inject

@AndroidEntryPoint
class HallOfFameAggFragment : BaseFragment(), HallOfFameAggAdapter.OnClickListener {

    private lateinit var binding: FragmentHalloffameAggBinding
    private lateinit var items: ArrayList<HallModel>

    private var hallOfFameAggAdapter: HallOfFameAggAdapter? = null

    // 처음 화면 그리기 위한 데이터 세팅용 변수 -> 처음 이후에는 부모 뷰에서 제어함
    private lateinit var initialData: Triple<String?, String?, String?>

    private lateinit var mAggSheetFragment: BottomSheetFragment

    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            initialData =
                Triple(it.getString(ARG_TYPE), it.getString(ARG_CATEGORY), it.getString(ARG_NAME))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_halloffame_agg, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        init()
    }

    private fun init() {
        items = ArrayList()
        hallOfFameAggAdapter = HallOfFameAggAdapter(requireContext(), this, items)

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

            aggTypeFilter.setOnClickListener {
                showAggSheet()
            }
        }

        loadAggResources(initialData.first, initialData.second, initialData.third)
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

    // 셀럽
    override fun filterClicked() {
        showAggSheet()
    }

    // 누적순위 변화 화면으로 이동
    override fun onItemClicked(item: HallModel?) {
        startActivity(HallOfFameAggHistoryActivity.createIntent(context, item?.idol))
    }

    // 누적순위 데이터 불러오는 api
    fun loadAggResources(type: String?, category: String?, typeName: String?) {
        MainScope().launch {
            trendsRepository.rank(
                type,
                category,
                null,
                null,
                { response ->
                    if (!response.optBoolean("success")) {
                        return@rank
                    }
                    val array = response.getJSONArray("data")
                    val gson = IdolGson.getInstance(true)
                    items.clear()
                    for (i in 0 until array.length()) {
                        items.add(
                            gson.fromJson(
                                array.getJSONObject(i).toString(),
                                HallModel::class.java,
                            ),
                        )
                    }
                    activity?.runOnUiThread {
                        applyAggItems(suddenRanking(items), typeName, type)
                    }
                }, { throwable ->
                    Toast.makeText(
                        activity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            )
        }
    }

    private fun suddenRanking(items: ArrayList<HallModel>): ArrayList<HallModel> {
        // diff 크기로  내림 차순으로 정렬한다.
        // index 0이 가장  높은 diff를 가지고 있다.
        val listSortByDiff: ArrayList<HallModel> = ArrayList(items)

        listSortByDiff.sortWith(
            Comparator { lhs, rhs ->
                when {
                    lhs!!.difference > rhs!!.difference -> -1
                    lhs.difference < rhs.difference -> 1
                    else -> {
                        lhs.rank.compareTo(rhs.rank)
                    }
                }
            },
        )
        // 가장 높은 diff의   hallmodel 값을 넣어준다.
        var topDifferenceId: HallModel? = null

        // diff 크기로  내림 차순 정렬후, 동점자 처리
        for (i in listSortByDiff.indices) {
            val item = listSortByDiff[i]
            if (item.status.equals("increase", ignoreCase = true)) {
                // 가장 높은 diff 의 아이디가 아직 없는 경우
                if (topDifferenceId == null) {
                    // 현재 item이 가장 높은 diff 아이템임
                    item.topOneDifferenceId = item.id // 1등 diff 넣어줌
                    topDifferenceId = item // 가장 높은 diff 임을 설정 함.
                } else { // 가장 높은 diff 의 아이디가 잇을 경우

                    // 해당  diff의  값이랑  현재 item diff값  비교해서  같으면  1등 diff로  설정해줌.
                    if (item.difference == topDifferenceId.difference) {
                        item.topOneDifferenceId = item.id // 가장  높은 diff를 넣어줌
                    } else {
                        break
                    }
                }
                // 그다음  diff 가  현재  diff와 다르면  더이상 동점자가 안나옴으로  for 문 중지
                if (listSortByDiff[i + 1].difference != item.difference) {
                    break
                }
            }
        }

        val temp: ArrayList<HallModel> = ArrayList(items)
        for (i in temp.indices) {
            val item = temp[i]
            // 동점자 처리
            if (i > 0 && temp[i - 1].score == item.score) {
                item.rank = temp[i - 1].rank
            } else {
                item.rank = i
            }
        }
        return temp
    }

    private fun applyAggItems(items: ArrayList<HallModel>, typeName: String?, type: String?) {
        if (items.size == 0) {
            binding.apply {
                clEmptyRanking.visibility = View.VISIBLE
                tvAggTypeFilter.text = typeName
                tvPeriod.text = DateUtil.getHallOfFameDateString()
                rvHallAgg.visibility = View.GONE
                tvEmpty.visibility = View.GONE
            }
            return
        }
        binding.tvEmpty.visibility = View.GONE
        binding.clEmptyRanking.visibility = View.GONE
        binding.rvHallAgg.visibility = View.VISIBLE
        hallOfFameAggAdapter?.setItems(items, typeName, type)
    }

    private fun showAggSheet() {
        mAggSheetFragment = BottomSheetFragment.newInstance(
            BottomSheetFragment.FLAG_HALL_OF_FAME_TYPE_FILTER,
            HallOfFameFragment.LOADER_AGG
        )
        val tag = "filter"
        val oldTag = requireActivity().supportFragmentManager.findFragmentByTag(tag)
        if (oldTag == null) {
            mAggSheetFragment.show(requireActivity().supportFragmentManager, tag)
        }
    }

    fun scrollToTop() {
        if(!::binding.isInitialized) return
        binding.rvHallAgg.scrollToPosition(0)
    }

    companion object {
        private const val ARG_CATEGORY = "category"
        private const val ARG_TYPE = "type"
        private const val ARG_NAME = "name"

        fun newInstance(type: String?, category: String?, name: String): HallOfFameAggFragment {
            val fragment = HallOfFameAggFragment()
            val args = Bundle()
            args.apply {
                putString(ARG_TYPE, type)
                putString(ARG_CATEGORY, category)
                putString(ARG_NAME, name)
            }
            fragment.arguments = args
            return fragment
        }
    }
}