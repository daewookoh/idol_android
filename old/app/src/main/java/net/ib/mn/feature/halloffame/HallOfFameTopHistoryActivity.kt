package net.ib.mn.feature.halloffame

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.databinding.ActivityHalloffametopBinding
import net.ib.mn.utils.ext.applySystemBarInsets
import net.ib.mn.utils.livedata.SingleEventObserver

/**
 * 명전 - 일일순위 - 날짜별 순위
 */
@AndroidEntryPoint
class HallOfFameTopHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityHalloffametopBinding
    private val viewModel: HallOfFameTopHistoryViewModel by viewModels()
    private lateinit var adapter: HallTopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_halloffametop)
        binding.apply {
            lifecycleOwner = this@HallOfFameTopHistoryActivity
            mainScreen.applySystemBarInsets()
        }


        initUI()
        observedVM()
        getHOFTopData()
    }

    private fun initUI() {
        adapter = HallTopAdapter(arrayListOf())
        binding.rvHalloffameTop.adapter = adapter

        val date = intent.getStringExtra(PARAM_DATE)
        val actionbar = supportActionBar
        val title = date + " " + getString(R.string.title_daily_rank)
        actionbar!!.title = title
    }

    private fun getHOFTopData() {
        val type = intent.getStringExtra(PARAM_TYPE) ?: ""
        val category = intent.getStringExtra(PARAM_CATEGORY) ?: ""
        val dateParam = intent.getStringExtra(PARAM_DATE_PARAM) ?: ""
        val chartCode = intent.getStringExtra(PARAM_CHART_CODE) ?:""

        viewModel.getHallTop(this, type, category, dateParam, chartCode)
    }

    private fun observedVM() = with(viewModel) {
        hofTopList.observe(this@HallOfFameTopHistoryActivity, SingleEventObserver { items ->
            if (items.isEmpty()) {
                binding.rvHalloffameTop.visibility = View.GONE
                binding.tvEmpty.apply {
                    text = getString(R.string.no_data)
                    visibility = View.VISIBLE
                }
            } else {
                adapter.setItems(items)

                binding.rvHalloffameTop.visibility = View.VISIBLE
                binding.tvEmpty.visibility = View.GONE
            }
        })

        errorToast.observe(this@HallOfFameTopHistoryActivity, SingleEventObserver { msg ->
            val toastMsg = msg.ifEmpty {
                getString(R.string.error_abnormal_exception)
            }
            Toast.makeText(this@HallOfFameTopHistoryActivity, toastMsg, Toast.LENGTH_SHORT).show()
        })
    }

    companion object {
        private const val PARAM_TYPE = "type"
        private const val PARAM_DATE = "date"
        private const val PARAM_DATE_PARAM = "date_param"
        private const val PARAM_CATEGORY = "category"
        private const val PARAM_CHART_CODE = "chartCode"

        @JvmStatic
        fun createIntent(
            context: Context,
            type: String,
            date: String,
            dateParam: String,
            category: String
        ): Intent {
            return Intent(context, HallOfFameTopHistoryActivity::class.java).apply {
                putExtra(PARAM_TYPE, type)
                putExtra(PARAM_DATE, date)
                putExtra(PARAM_DATE_PARAM, dateParam)
                putExtra(PARAM_CATEGORY, category)
            }
        }

        @JvmStatic
        fun createIntent(
            context: Context,
            date: String,
            dateParam: String,
            chartCode: String
        ): Intent {
            return Intent(context, HallOfFameTopHistoryActivity::class.java).apply {
                putExtra(PARAM_DATE, date)
                putExtra(PARAM_DATE_PARAM, dateParam)
                putExtra(PARAM_CHART_CODE, chartCode)
            }
        }
    }
}