/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description:
 *
 * */

package net.ib.mn.awards
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.GaonHistoryActivity
import net.ib.mn.activity.StatsActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.databinding.AwardsRankingFragmentBinding
import net.ib.mn.fragment.BaseFragment
import net.ib.mn.model.AwardStatsModel
import net.ib.mn.model.HallModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.UtilK
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.TimeZone
import javax.inject.Inject

/**
 * 2023~ 기록실 어워즈
 */
@AndroidEntryPoint
class Hda2023Fragment : BaseFragment(), HeartDreamAwardAdapter.OnClickListener {

    private lateinit var binding: AwardsRankingFragmentBinding
    private lateinit var models: ArrayList<HallModel>

    private var mRankingAdapter: HeartDreamAwardAdapter? = null
    private var awardStatsModel: AwardStatsModel? = null
    private var awardStatsCode: String? = null
    private var awardStatsIndex: Int = 0
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
        binding = DataBindingUtil.inflate(inflater, R.layout.awards_ranking_fragment, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        init()

        // 어댑터 초기값 세팅
        setAdapter()

        // DB에 저장된 순위 먼저 보여준다
        updateDataWithUI()
    }
    private fun init(){
        if (arguments != null) {
            awardStatsModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arguments?.getSerializable(StatsActivity.PARAM_AWARD_STATS, AwardStatsModel::class.java)
            } else {
                arguments?.getSerializable(StatsActivity.PARAM_AWARD_STATS) as AwardStatsModel
            }
        }
        awardStatsCode = arguments?.getString(StatsActivity.PARAM_AWARD_STATS_CODE)
        awardStatsIndex = arguments?.getInt(StatsActivity.PARAM_AWARD_STATS_INDEX) ?: 0
        models = ArrayList()
    }

    private fun setAdapter() {
        val divider = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.line_divider)!!)

        val title = UtilK.getAwardTitle(awardStatsModel?.resultTitle,
            awardStatsModel?.charts?.get(awardStatsIndex)?.name)
        binding.rvRanking.layoutManager = LinearLayoutManagerWrapper(requireContext(), LinearLayoutManager.VERTICAL, false)

        mRankingAdapter = HeartDreamAwardAdapter(
            requireContext(),
            mGlideRequestManager,
            this,
            models,
            null,
            awardStatsCode,
            getString(R.string.hda_2023),
            awardStatsModel?.name,
            title
        )

        mRankingAdapter?.setHasStableIds(true)
        with(binding) {
            rvRanking.adapter = mRankingAdapter
            rvRanking.addItemDecoration(divider)
            rvRanking.setHasFixedSize(true)
        }
    }


    override fun onItemClickListener(item: HallModel) {
        if(BuildConfig.CELEB){
            val intent = GaonHistoryActivity.createIntent(activity, item.idol)
            intent.putExtra(Const.EXTRA_GAON_EVENT, awardStatsModel?.name)
            intent.putExtra(Const.KEY_CHART_CODE, awardStatsCode)
            intent.putExtra(Const.KEY_SOURCE_APP, item.idol?.sourceApp)
            startActivityForResult(intent, 1)
        }
        else{
            val intent = AwardsHistoryActivity.createIntent(activity, item.idol)
            intent.putExtra(Const.EXTRA_GAON_EVENT, awardStatsModel?.name)
            intent.putExtra(Const.KEY_CHART_CODE, awardStatsCode)
            startActivityForResult(intent, 1)
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
    }

    // 어워드 기간 response에서 받아와서 처리한 후 보내줌
    private fun awardPeriod(startDate: String, endDate: String) {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(context ?: return))
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val formatter2 = SimpleDateFormat("yyyy.MM.dd", LocaleUtil.getAppLocale(context ?: return))
        formatter2.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        mRankingAdapter?.setAwardPeriod(
            String.format(
                getString(R.string.gaon_voting_period),
                formatter.parse(startDate)?.let { formatter2.format(it) },
                formatter.parse(endDate)?.let { formatter2.format(it) },
            ),
        )
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
                    awardPeriod(response.optString("start_date"), response.optString("end_date"))

                    var idols: ArrayList<HallModel> = ArrayList<HallModel>()

                    val gson = IdolGson.getInstance()
                    val listType: Type? = object : TypeToken<List<HallModel>>() {}.type
                    idols = gson.fromJson(objects.toString(), listType)

                    for (i in idols.indices) {
                        val item: HallModel = idols[i]
                        if (i > 0 && idols[i - 1].score == item.score) {
                            item.rank =
                                idols[i - 1].rank
                        } else {
                            item.rank = i
                        }
                    }
                    applyItems(idols)
                }, { throwable ->
                    showEmptyView()
                }
            )
        }
    }

    private fun applyItems(items: ArrayList<HallModel>) {
        mRankingAdapter?.setItems(items)
        mRankingAdapter?.notifyDataSetChanged()

        if (items.isEmpty()) {
            showEmptyView()
        } else {
            hideEmptyView()
        }
    }

    private fun showEmptyView() {
        with(binding) {
            tvLoadData.visibility = View.VISIBLE
            awardsGuide.root.visibility = View.GONE
            rvRanking.visibility = View.GONE
        }
    }

    private fun hideEmptyView() {
        with(binding) {
            tvLoadData.visibility = View.GONE
            awardsGuide.root.visibility = View.GONE
            rvRanking.visibility = View.VISIBLE
        }
    }
}