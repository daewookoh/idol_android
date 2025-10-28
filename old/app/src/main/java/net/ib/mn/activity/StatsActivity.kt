package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.BuildConfig
import net.ib.mn.R
import net.ib.mn.adapter.AwardsStatsAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.awards.StatsAwardsBottomSheetActivity
import net.ib.mn.core.data.model.RecordRoomModel
import net.ib.mn.core.data.repository.AwardsRepositoryImpl
import net.ib.mn.databinding.ActivityStatsBinding
import net.ib.mn.feature.basichistory.BasicHistoryActivity
import net.ib.mn.feature.basichistory.HistoryType
import net.ib.mn.feature.rookiehistory.RookieHistoryActivity
import net.ib.mn.model.AwardStatsModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.GaAction
import net.ib.mn.utils.LocaleUtil
import net.ib.mn.utils.SupportedLanguage
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject

/**
 * Copyright (C) 2024. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author jeeunman manjee.official@gmail.com
 * Description: 기록실 컨테이너 화면
 *
 * */

@AndroidEntryPoint
class StatsActivity : BaseActivity(), OnClickListener, AwardsStatsAdapter.OnClickListener {
    private lateinit var binding: ActivityStatsBinding
    private var awardsStatsAdapter : AwardsStatsAdapter? = null

    private var chartCodes: RecordRoomModel? = null
    @Inject
    lateinit var awardsRepository: AwardsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_stats)
        binding.svStats.applySystemBarInsets()

        val actionbar = supportActionBar
        actionbar!!.setTitle(R.string.menu_stats)

        chartCodes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(PARAM_HISTORY_CHART_CODE, RecordRoomModel::class.java)
        } else {
            intent.getParcelableExtra(PARAM_HISTORY_CHART_CODE)
        }

        if(BuildConfig.CELEB) {
            binding.tvIdol.text = getString(R.string.actor)
            binding.rlRookie.rlStats.visibility = View.GONE
        }
        awardsStatsAdapter = AwardsStatsAdapter(this)
        binding.rvAwards.adapter = awardsStatsAdapter
        getAwardsHistory()

        with(binding){
            rlHighestVotes.rlStats.setOnClickListener(this@StatsActivity)
            rlTop1.rlStats.setOnClickListener(this@StatsActivity)
            rlAngel.rlStats.setOnClickListener(this@StatsActivity)
            rlFairy.rlStats.setOnClickListener(this@StatsActivity)
            rlMiracle.rlStats.setOnClickListener(this@StatsActivity)
            rlTop300.rlStats.setOnClickListener(this@StatsActivity)
            rlTop100.rlStats.setOnClickListener(this@StatsActivity)
            rlRookie.rlStats.setOnClickListener(this@StatsActivity)
        }

        if (LocaleUtil.isExistCurrentLocale(this, SupportedLanguage.BOARD_KIN_QUIZZES_TOP100_LOCALES)) {
            binding.rlTop100.rlStats.visibility = View.VISIBLE
        } else {
            binding.rlTop100.rlStats.visibility = View.GONE
        }
    }
    override fun onItemClickListener(item: AwardStatsModel) {

        val isBottomSheetActivity = item.name == Const.EVENT_YEND
        if (!isBottomSheetActivity) {
            startActivity(AwardStatsActivity.createIntent(this, item))
        } else {
            startActivity(StatsAwardsBottomSheetActivity.createIntent(this, item))
        }
    }

    override fun onClick(v: View) {
        when (v) {
            // TODO 셀럽에 차트코드 적용되면 통합 예정
            binding.rlHighestVotes.root -> {
//                startActivity(HighestVotesActivity.createIntent(this))
                if (BuildConfig.CELEB) {
                    startActivity(HighestVotesActivity.createIntent(this))
                } else {
                    chartCodes?.let {
                        val i = Intent(this, BasicHistoryActivity::class.java)
                        i.putParcelableArrayListExtra(
                            BasicHistoryActivity.PARAM_CHART_CODES,
                            ArrayList(chartCodes!!.votesTop100)
                        )
                        i.putExtra(BasicHistoryActivity.PARAM_CALC_TYPE, HistoryType.MOST_TOP_100)
                        startActivity(i)
                    } ?: return
                }
            }

            binding.rlTop1.root -> {
                if (BuildConfig.CELEB) {
                    startActivity(Top1CountActivity.createIntent(this))
                } else {
                    chartCodes?.let {
                        val i = Intent(this, BasicHistoryActivity::class.java)
                        i.putParcelableArrayListExtra(
                            BasicHistoryActivity.PARAM_CHART_CODES,
                            ArrayList(chartCodes!!.top1Count)
                        )
                        i.putExtra(BasicHistoryActivity.PARAM_CALC_TYPE, HistoryType.COUNT_RANK_1)
                        startActivity(i)
                    } ?: return
                }
            }

            binding.rlAngel.root ->
                startActivity(CharityCountActivity.createIntent(this, ANGEL))

            binding.rlFairy.root ->
                startActivity(CharityCountActivity.createIntent(this, FAIRY))

            binding.rlMiracle.root ->
                startActivity(CharityCountActivity.createIntent(this, MIRACLE))

            binding.rlTop300.root ->
                startActivity(UserVotesTop300Activity.createIntent(this))

            binding.rlTop100.root -> {
                setUiActionFirebaseGoogleAnalyticsActivity(
                    GaAction.QUIZ_RANKING_FEED.actionValue,
                    GaAction.QUIZ_RANKING_FEED.label
                )
                startActivity(IdolQuizRankingActivity.createIntent(this))
            }
            binding.rlRookie.root -> {
                startActivity(Intent(this, RookieHistoryActivity::class.java))
            }
        }
    }

    //어워즈 목록 리스트 오는 Api. on/off 가능
    //서버에서 전부 관리하는 것이 아닌, 앱에서 view 개발한 것에 한하여 on/off 가능
    private fun getAwardsHistory() {
        MainScope().launch {
            awardsRepository.history(
                { response ->
                    val gson1 = IdolGson.getInstance()
                    val listType = object :
                        com.google.gson.reflect.TypeToken<List<AwardStatsModel?>?>() {}.type
                    val awardStatsList: List<AwardStatsModel> =
                        gson1.fromJson(response.optJSONArray("objects")?.toString(), listType)
                    awardsStatsAdapter?.submitList(awardStatsList)
                }, { throwable ->
                }
            )
        }
    }

    companion object {
        const val ANGEL: Int = 0
        const val FAIRY: Int = 1
        const val MIRACLE: Int = 2
        const val PARAM_AWARD_STATS = "ParamAwardStats"
        const val PARAM_AWARD_STATS_CODE = "ParamAwardStatsCode"
        const val PARAM_AWARD_STATS_INDEX = "ParamAwardStatsIndex" // awards/history 응답의 charts 배열에서 몇번째인지
        const val PARAM_HISTORY_CHART_CODE = "paramHistoryChartCode"

        @JvmStatic
        fun createIntent(context: Context): Intent {
            return Intent(context, StatsActivity::class.java)
        }
    }


}
