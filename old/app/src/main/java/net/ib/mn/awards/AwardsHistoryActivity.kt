/**
 * Copyright (C) 2023. ExodusEnt Corp. All rights reserved.
 * You must have prior written permission to read this file.
 * @author Kim min gyu <mingue0605@myloveidol.com>
 * Description: 어워즈 누적, 어워즈 최종결과에서 가수 클릭 시 가지는 순위 차트 화면
 *
 * */

package net.ib.mn.awards

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.bumptech.glide.RequestManager
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.adapter.HallAggHistoryLeagueAdapter
import net.ib.mn.addon.IdolGson
import net.ib.mn.core.data.repository.TrendsRepositoryImpl
import net.ib.mn.databinding.ActivityHistoryBinding
import net.ib.mn.model.HallAggHistoryModel
import net.ib.mn.model.IdolModel
import net.ib.mn.utils.Const
import net.ib.mn.utils.Toast
import net.ib.mn.utils.Util
import net.ib.mn.utils.ext.applySystemBarInsets
import javax.inject.Inject
import kotlin.math.roundToInt

/**
 * 2023~ 어워즈 기록실-누적순위변화
 */
@AndroidEntryPoint
class AwardsHistoryActivity : BaseActivity(), HallAggHistoryLeagueAdapter.OnClickListener {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var mGlideRequestManager: RequestManager
    private var mItems: ArrayList<HallAggHistoryModel>? = null
    private var awardsHistoryAdapter: HallAggHistoryLeagueAdapter? = null

    private var mIdolModel: IdolModel? = null

    private var chartCode: String? = null

    var event: String? = null
    @Inject
    lateinit var trendsRepository: TrendsRepositoryImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_history)
        binding.clContainer.applySystemBarInsets()

        init()
    }

    private fun init() {

        val mIntent = intent
        chartCode = mIntent.getStringExtra(Const.KEY_CHART_CODE)
        mIdolModel = mIntent.getSerializableExtra("idol") as IdolModel?
        event = mIntent.getStringExtra(Const.EXTRA_GAON_EVENT)
        setCommunityTitle2(mIdolModel, getString(R.string.title_rank_history))

        mGlideRequestManager = Glide.with(this)
        mItems = ArrayList()

        awardsHistoryAdapter = HallAggHistoryLeagueAdapter(
            this,
            this,
            mIdolModel?.sourceApp
        )
        binding.rvAggHistory.adapter = awardsHistoryAdapter

        idolRecentRankingAward()
    }

    //어워즈 땐 사용 안함. 돌려쓰고있어서 override 해야 함
    override fun onItemClickListener(item: HallAggHistoryModel) {
    }

    private fun idolRecentRankingAward() {
        MainScope().launch {
            trendsRepository.awardRecent(
                type = null,
                category = null,
                event = event,
                idolId = mIdolModel?.getId(),
                chartCode = chartCode,
                sourceApp = mIdolModel?.sourceApp,
                listener = { response ->
                    Util.closeProgress()
                    var items: MutableList<HallAggHistoryModel> = ArrayList()
                    try {
                        items.add(HallAggHistoryModel())
                        val gson = IdolGson.getInstance(true)
                        val listType = object : TypeToken<ArrayList<HallAggHistoryModel>>() {}.type
                        items = gson.fromJson(response.getJSONArray("objects").toString(), listType)

                        var totalHeart = 0L

                        items.forEach {
                            totalHeart += it.heart
                        }

                        // 반올림 적용을 위해  double로 캐스팅해서  평균값을 내어  Math.round로  반올림을 적용한다.
                        val averageHeart: Long =
                            ((totalHeart.toDouble() / items.size).roundToInt()).toLong()
                        awardsHistoryAdapter?.updateAverageHeart(averageHeart)

                        if (items.isNullOrEmpty()) {
                            binding.rvAggHistory.visibility = View.GONE
                            binding.tvEmpty.visibility = View.VISIBLE
                            binding.tvEmpty.text =
                                this@AwardsHistoryActivity.getString(R.string.no_data)
                        } else {
                            binding.rvAggHistory.visibility = View.VISIBLE
                            binding.tvEmpty.visibility = View.GONE
                        }
                        items.add(0, HallAggHistoryModel()) // 첫번째 인덱스에는 가짜 데이터 삽입(listAdapter에서 currentPosition 안먹는 문제때문)
                        awardsHistoryAdapter?.submitList(items)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, errorListener = { throwable ->
                    Util.closeProgress()
                    Toast.makeText(
                        this@AwardsHistoryActivity,
                        R.string.error_abnormal_exception,
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            )
        }
    }

    companion object {
        fun createIntent(context: Context?, idol: IdolModel?): Intent {
            val intent = Intent(context, AwardsHistoryActivity::class.java)
            intent.putExtra("idol", idol as Parcelable?)
            return intent
        }
    }
}