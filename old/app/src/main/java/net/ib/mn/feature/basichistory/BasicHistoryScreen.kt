package net.ib.mn.feature.basichistory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.ib.mn.R
import net.ib.mn.feature.basichistory.component.BasicHistoryItem
import net.ib.mn.feature.basichistory.component.BasicHistoryTop3Item
import net.ib.mn.feature.common.BottomTab
import net.ib.mn.core.data.model.ChartCodeInfo
import net.ib.mn.utils.LocaleUtil.getAppLocale
import java.text.DateFormat

@Composable
fun BasicHistoryScreen(
    historyType: HistoryType,
    chartCodes: ArrayList<ChartCodeInfo>,
    viewModel: BasicHistoryViewModel,
) {
    var isInit = false
    val context = LocalContext.current
    var currentIndex by remember { mutableIntStateOf(0) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = isInit) {
        if (isInit) return@LaunchedEffect
        isInit = true

        if (historyType == HistoryType.MOST_TOP_100) {
            viewModel.getHighestTicket(context, chartCodes)
        } else if (historyType == HistoryType.COUNT_RANK_1) {
            viewModel.getTop1Count(context, chartCodes)
        }
    }

    when (uiState) {
        is BasicHistoryUiState.Loading -> {}
        is BasicHistoryUiState.Success -> {
            val successState = uiState as BasicHistoryUiState.Success
            val data = successState.dataMap[chartCodes[currentIndex].code] ?: arrayListOf()

            val dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM, getAppLocale(context))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        colorResource(id = R.color.background_100)
                    )
            ) {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            colorResource(id = R.color.background_100)
                        )
                ) {
                    items(data.size, key = { data[it].id }) { index ->
                        if (index < 3) {
                            BasicHistoryTop3Item(
                                historyType.historyCalcType,
                                data[index],
                                dateFormat
                            )
                        } else {
                            BasicHistoryItem(historyType.historyCalcType, data[index], dateFormat)
                        }
                    }
                }
                Row {
                    chartCodes.forEachIndexed { index, chartCodeInfo ->
                        BottomTab(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { currentIndex = index },
                            isSelect = currentIndex == index,
                            tabTitle = chartCodeInfo.name
                        )
                    }
                }
            }
        }

        is BasicHistoryUiState.Error -> {}
    }
}