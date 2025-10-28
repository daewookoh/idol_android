package net.ib.mn.feature.basichistory

import android.os.Build
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.databinding.DataBindingUtil
import dagger.hilt.android.AndroidEntryPoint
import net.ib.mn.R
import net.ib.mn.activity.BaseActivity
import net.ib.mn.databinding.ActivityBasicHistoryBinding
import net.ib.mn.core.data.model.ChartCodeInfo
import net.ib.mn.utils.ext.applySystemBarInsets

@AndroidEntryPoint
class BasicHistoryActivity : BaseActivity() {

    private lateinit var binding: ActivityBasicHistoryBinding
    private val viewModel: BasicHistoryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chartCodes: ArrayList<ChartCodeInfo>? =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(PARAM_CHART_CODES, ChartCodeInfo::class.java)
            } else {
                intent.getParcelableArrayListExtra(PARAM_CHART_CODES)
            }

        val calcType: HistoryType? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(PARAM_CALC_TYPE, HistoryType::class.java)
        } else {
            intent.getSerializableExtra(PARAM_CALC_TYPE) as? HistoryType
        }

        val title = calcType?.let {
            getString(it.titleRes)
        } ?: ""

        supportActionBar?.let {
            it.title = title
        }

        binding = DataBindingUtil.setContentView(this, R.layout.activity_basic_history)
        binding.lifecycleOwner = this
        binding.flContainer.applySystemBarInsets()

        chartCodes?.let {
            binding.cvBasicHistory.setContent {
                MaterialTheme {
                    BasicHistoryScreen(
                        historyType = calcType?: HistoryType.MOST_TOP_100,
                        chartCodes = chartCodes,
                        viewModel = viewModel
                    )
                }
            }
        } ?: finish()
    }

    companion object {
        const val PARAM_CHART_CODES = "paramChartCodes"
        const val PARAM_CALC_TYPE = "paramCalcType"
    }
}