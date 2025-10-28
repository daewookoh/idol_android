/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author __Jung Sang Min__ <jnugg0819@myloveidol.com>
 * Description: 어워즈 바텀시트 필터 전용 액티비티.
 *
 * */

package net.ib.mn.awards

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.activity.GaonHistoryActivity
import net.ib.mn.activity.StatsActivity
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.core.model.AwardChartsModel
import net.ib.mn.databinding.StatsAwardsRankingFragmentBinding
import net.ib.mn.dialog.BottomSheetRcyDialogFragment
import net.ib.mn.model.AwardStatsModel
import net.ib.mn.model.HallModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.LinearLayoutManagerWrapper
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import java.lang.reflect.Type
import java.text.SimpleDateFormat
import java.util.TimeZone
import javax.inject.Inject

/**
 * 기록실 전용 어워즈 결과 화면
 * 진행중인 어워즈 화면이 영향을 주지 않도록 분리함
 */
@AndroidEntryPoint
class StatsAwardsBottomSheetActivity : BaseActivity(), BestChoice2023Adapter.OnClickListener {

    private lateinit var binding: StatsAwardsRankingFragmentBinding

    private var awardStatsModel: AwardStatsModel? = null
    private var awardStatsCode: String? = null

    private var rankingAdapter: BestChoice2023Adapter? = null

    private var requestChartCodeModel: AwardChartsModel? = null
    lateinit var mGlideRequestManager: RequestManager
    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState != null) {
            requestChartCodeModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                savedInstanceState.getSerializable(
                    CHART_MODEL,
                    AwardChartsModel::class.java,
                )
            } else {
                (
                    {
                        savedInstanceState.getSerializable(CHART_MODEL)
                    }
                    ) as AwardChartsModel?
            }
        }

        binding =
            DataBindingUtil.setContentView(this, R.layout.stats_awards_ranking_fragment)
        binding.clRankingView.applySystemBarInsets()

        initSet()
        setAdapter()
        updateDataWithUI()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable(CHART_MODEL, requestChartCodeModel)
    }

    private fun setAdapter() {
        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.line_divider)!!)

        rankingAdapter = BestChoice2023Adapter(
            glideRequestManager = mGlideRequestManager,
            awardStatsModel,
            this,
        ) { item: IdolModel? -> onItemClicked(item) }

        binding.rvRanking.layoutManager =
            LinearLayoutManagerWrapper(this, LinearLayoutManager.VERTICAL, false)
        binding.rvRanking.apply {
            itemAnimator = null
        }

        rankingAdapter?.setHasStableIds(true)

        with(binding) {
            rvRanking.adapter = rankingAdapter
            rvRanking.addItemDecoration(divider)
            rvRanking.setHasFixedSize(true)
        }
    }

    private fun initSet() {
        mGlideRequestManager = Glide.with(this)

        if (intent != null) {
            awardStatsModel = intent.extras?.getSerializable(
                StatsActivity.PARAM_AWARD_STATS,
            ) as AwardStatsModel?
            awardStatsCode = awardStatsModel?.name
        }

        requestChartCodeModel = awardStatsModel?.charts?.get(0)
        supportActionBar?.title = awardStatsModel?.title
    }

    // 어워드 기간 response에서 받아와서 처리한 후 보내줌
    private fun awardPeriod(startDate: String, endDate: String) {
        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", LocaleUtil.getAppLocale(this))
        formatter.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        val formatter2 = SimpleDateFormat("yyyy.MM.dd", LocaleUtil.getAppLocale(this))
        formatter2.timeZone = TimeZone.getTimeZone("Asia/Seoul")

        rankingAdapter?.setAwardPeriod(
            String.format(
                getString(R.string.gaon_voting_period),
                formatter.parse(startDate)?.let { formatter2.format(it) },
                formatter.parse(endDate)?.let { formatter2.format(it) },
            ),
        )
    }

    private fun updateDataWithUI() {
        MainScope().launch {
            trendsRepository.awardRank(
                requestChartCodeModel?.code,
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

                    idols[0].chartName = requestChartCodeModel?.name

                    applyItems(idols)
                }, { throwable ->
                    showEmptyView()
                }

            )
        }
    }

    private fun applyItems(items: ArrayList<HallModel>) {
        Util.closeProgress()

        if (items.isEmpty()) {
            showEmptyView()
        } else {
            hideEmptyView()
        }

        rankingAdapter?.submitList(items)
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

    override fun onItemClicked(item: IdolModel?) {
        if (BuildConfig.CELEB) {
            val intent = GaonHistoryActivity.createIntent(this, item)
            intent.putExtra(Const.EXTRA_GAON_EVENT, awardStatsCode)
            intent.putExtra(Const.KEY_CHART_CODE, requestChartCodeModel?.code)
            intent.putExtra(Const.KEY_SOURCE_APP, item?.sourceApp)
            startActivityForResult(intent, 1)
        } else {
            val intent = AwardsHistoryActivity.createIntent(this, item)
            intent.putExtra(Const.EXTRA_GAON_EVENT, awardStatsCode)
            intent.putExtra(Const.KEY_CHART_CODE, requestChartCodeModel?.code)
            startActivityForResult(intent, 1)
        }
    }

    override fun onFinalResultBottomSheetClicked() {
        showAwardsBottomSheetFilter()
    }

    private fun showAwardsBottomSheetFilter() {
        if (awardStatsModel == null) {
            return
        }

        val bottomSheetFragment = BottomSheetRcyDialogFragment.getInstance(
            awardStatsModel?.charts ?: return,
            onClickConfirm = { chartModel ->
                Util.showProgress(this)
                requestChartCodeModel = chartModel
                updateDataWithUI()
            },
            onClickDismiss = {
                binding.inTopAwards.ivTopArrowDown.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.btn_arrow_down_gray
                    )
                )
            },
        )
        val tag = "awards_filter"

        val oldFrag = supportFragmentManager.findFragmentByTag(tag)
        if (oldFrag == null) {
            bottomSheetFragment.show(supportFragmentManager, tag)
            binding.inTopAwards.ivTopArrowDown.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.btn_arrow_up_gray))
        }
    }

    companion object {
        const val CHART_MODEL = "chart_model"

        fun createIntent(
            context: Context?,
            awardStatsModel: AwardStatsModel,
        ): Intent {
            val intent = Intent(context, StatsAwardsBottomSheetActivity::class.java)
            intent.putExtra(StatsActivity.PARAM_AWARD_STATS, awardStatsModel)
            return intent
        }
    }
}