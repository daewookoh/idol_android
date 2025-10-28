/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 커뮤니티 헤더
 *
 * */

package net.ib.mn.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.adapter.HallAggHistoryLeagueAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.databinding.ActivityHalloffameAggHistoryBinding
import net.ib.mn.feature.halloffame.HallOfFameTopHistoryActivity
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Toast.Companion.makeText
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * 명예전당 - 누적순위변화
 */
@AndroidEntryPoint
class HallOfFameAggHistoryLeagueActivity : BaseActivity(), HallAggHistoryLeagueAdapter.OnClickListener {

    private lateinit var binding : ActivityHalloffameAggHistoryBinding

    private var mAdapter: HallAggHistoryLeagueAdapter? = null
    private var mIdolModel: IdolModel? = null
    private var chartCode: String = ""
    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_halloffame_agg_history)
        binding.clContainer.applySystemBarInsets()

        init()
    }

    private fun init(){
        val mIntent = intent
        mIdolModel = mIntent.getSerializableExtra(PARAM_IDOL) as IdolModel?
        chartCode = intent.getStringExtra(PARAM_CHART_CODE) ?: ""

        try {
            if(mIdolModel!=null){
                setCommunityTitle2(mIdolModel, getString(R.string.title_rank_history))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 차트
        mAdapter = HallAggHistoryLeagueAdapter(this, this, null)
        binding.rvHallAggHistory.adapter = mAdapter

        val divider = DividerItemDecoration(this, DividerItemDecoration.VERTICAL)
        divider.setDrawable(ContextCompat.getDrawable(this, R.drawable.line_divider)!!)
        binding.rvHallAggHistory.addItemDecoration(divider)

        getTrendsRecent()
    }

    override fun onItemClickListener(item: HallAggHistoryModel) {

        if (item.refdate == null) {
            return
        }

        startActivity(
            HallOfFameTopHistoryActivity.createIntent(
                this,
                item.getRefdate(this),
                item.refdate,
                chartCode
            )
        )
    }

    private fun getTrendsRecent(){
        Util.showProgress(this)

        MainScope().launch {
            trendsRepository.recent(
                mIdolModel!!.getId(),
                chartCode,
                null,
                { response ->
                    Util.closeProgress()
                    var items: MutableList<HallAggHistoryModel> = ArrayList()
                    try {
                        items.add(HallAggHistoryModel())
                        val gson = IdolGson.getInstance(true)
                        val listType = object : TypeToken<ArrayList<HallAggHistoryModel>>() {}.type
                        items = gson.fromJson(response.getJSONArray("objects").toString(), listType)
                        chartCode = response.getString("chart_code")

                        var totalHeart = 0L

                        items.forEach {
                            totalHeart += it.heart
                        }

                        //반올림 적용을 위해  double로 캐스팅해서  평균값을 내어  Math.round로  반올림을 적용한다.
                        val averageHeart: Long =
                            ((totalHeart.toDouble() / items.size).roundToInt()).toLong()
                        mAdapter?.updateAverageHeart(averageHeart)

                        items.add(0, HallAggHistoryModel()) //첫번째 인덱스에는 가짜 데이터 삽입(listAdapter에서 currentPosition 안먹는 문제때문)

                        if (items.isEmpty()) {
                            binding.mainScreen.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE
                        } else {
                            binding.mainScreen.visibility = View.VISIBLE
                            binding.tvEmpty.visibility = View.GONE

                            mAdapter?.submitList(items)
                        }
                    } catch (_: Exception) {
                    }
                },
                { throwable ->
                    Util.closeProgress()
                    makeText(this@HallOfFameAggHistoryLeagueActivity, R.string.error_abnormal_exception, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    companion object {
        private const val PARAM_IDOL = "idol"
        private const val PARAM_CHART_CODE = "chartCode"

        fun createIntent(context: Context?, idol: IdolModel?): Intent {
            val intent = Intent(context, HallOfFameAggHistoryLeagueActivity::class.java)
            intent.putExtra(PARAM_IDOL, idol as Parcelable?)
            return intent
        }

        fun createIntent(context: Context?, idol: IdolModel?, chartCode: String): Intent {
            val intent = Intent(context, HallOfFameAggHistoryLeagueActivity::class.java)
            intent.putExtra(PARAM_IDOL, idol as Parcelable?)
            intent.putExtra(PARAM_CHART_CODE, chartCode)
            return intent
        }
    }
}

