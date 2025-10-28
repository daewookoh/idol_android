/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 기록실 gaon2016, gaon2017 Fagment
 *
 * */

package net.ib.mn.fragment

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.GaonHistoryActivity
import net.ib.mn.activity.StatsActivity
import net.ib.mn.adapter.GaonStatsAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.databinding.GaonRankingFragmentBinding
import net.ib.mn.model.AwardStatsModel
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.UtilK
import java.lang.reflect.Type
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import javax.inject.Inject

/**
 * 기록실 가온2016, 가온2017
 */
@AndroidEntryPoint
class GaonStatsFragment : BaseFragment(), GaonStatsAdapter.OnClickListener {

    private lateinit var binding: GaonRankingFragmentBinding
    private lateinit var mItems: ArrayList<HallModel>

    private var mRankingAdapter: GaonStatsAdapter? = null

    private var awardStatsModel: AwardStatsModel? = null
    private var awardStatsCode: String? = null
    private var awardStatsIndex: Int = 0

    private lateinit var startDate: Date
    private lateinit var endDate: Date
    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mGlideRequestManager = Glide.with(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.gaon_ranking_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        // 어댑터 초기값 세팅
        setAdapter()

        updateDataWithUI()
    }
    private fun init() {
        if (arguments != null) {
            awardStatsModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getSerializable(StatsActivity.PARAM_AWARD_STATS, AwardStatsModel::class.java)
            } else {
                arguments?.getSerializable(StatsActivity.PARAM_AWARD_STATS) as AwardStatsModel
            }
        }
        awardStatsCode = arguments?.getString(StatsActivity.PARAM_AWARD_STATS_CODE)
        awardStatsIndex = arguments?.getInt(StatsActivity.PARAM_AWARD_STATS_INDEX) ?: 0
        mItems = ArrayList()
    }

    private fun setAdapter() {
        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        val title = UtilK.getAwardTitle(awardStatsModel?.resultTitle,
            awardStatsModel?.charts?.get(awardStatsIndex)?.name)
        mRankingAdapter = GaonStatsAdapter(
            requireContext(),
            mItems,
            awardStatsCode,
            title,
            mGlideRequestManager,
            this,
        )

        mRankingAdapter?.setHasStableIds(true)
        with(binding) {
            rvRanking.adapter = mRankingAdapter
            rvRanking.addItemDecoration(divider)
            rvRanking.setHasFixedSize(true)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    fun updateDataWithUI() {
        MainScope().launch {
            trendsRepository.awardRank(
                awardStatsCode,
                awardStatsModel?.name,
                { response ->
                    if (!response.optBoolean("success")) {
                        showEmptyView()
                        return@awardRank
                    }
                    val objects = response.getJSONArray("data")
                    if (objects.length() == 0) {
                        showEmptyView()
                        return@awardRank
                    }

                    val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(context ?: return@awardRank))

                    startDate = formatter.parse(response.getString("start_date")) as Date
                    endDate = formatter.parse(response.getString("end_date")) as Date

                    val gson = IdolGson.getInstance()
                    val listType: Type? = object : TypeToken<List<HallModel>>() {}.type
                    mItems = gson.fromJson(objects.toString(), listType)

                    for (i in mItems.indices) {
                        val item: HallModel = mItems[i]
                        if (i > 0 && mItems[i - 1].score == item.score) {
                            item.rank =
                                mItems[i - 1].rank
                        } else {
                            item.rank = i
                        }
                    }

                    val hallModel = HallModel()
                    mItems.add(0, hallModel)
                    applyItems(mItems)
                }, { throwable ->
                    showEmptyView()
                }
            )
        }
    }

    override fun onItemClickListener(item: HallModel) {
        val intent = GaonHistoryActivity.createIntent(activity, item.idol)
        intent.putExtra(Const.EXTRA_GAON_EVENT, awardStatsModel?.name)
        intent.putExtra(Const.KEY_CHART_CODE, awardStatsCode)
        startActivity(intent)
    }

    private fun applyItems(items: ArrayList<HallModel>) {
        setDate()
        mRankingAdapter?.setItems(items)
        hideEmptyView()
    }

    // 집계기간 표시. 서버 응답의 start_date ~ 집계중 ? 어제 날짜 : end_date
    private fun setDate() {
        val start: String
        val end: String

        val today = Date()

        val f = DateFormat.getDateInstance(DateFormat.SHORT, LocaleUtil.getAppLocale(context ?: return))
        start = f.format(startDate)

        // today가 endDate보다 뒤쪽 날이면 endDate를 표시
        if (today > endDate) {
            end = f.format(endDate)
        } else {
            val cal: Calendar = GregorianCalendar()
            cal.time = today
            cal.add(Calendar.DATE, -1)
            val yesterday = cal.time
            end = f.format(yesterday)
        }
        mRankingAdapter?.setDate(start + "~" + end + " " + requireContext().resources.getString(R.string.label_header_result))
    }

    private fun showEmptyView() {
        with(binding) {
            tvEmpty.visibility = View.VISIBLE
            rvRanking.visibility = View.GONE
        }
    }

    private fun hideEmptyView() {
        with(binding) {
            tvEmpty.visibility = View.GONE
            rvRanking.visibility = View.VISIBLE
        }
    }
}