package net.ib.mn.presentation.main.ranking.idol_subpage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.ib.mn.domain.model.ApiResult
import net.ib.mn.domain.repository.RankingRepository
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * 명예전당 - 일일 순위 ViewModel
 *
 * hofs/ API를 호출하여 일일 순위 데이터를 가져옵니다.
 */
@HiltViewModel(assistedFactory = HallOfFameRankingSecondSubDailyPageViewModel.Factory::class)
class HallOfFameRankingSecondSubDailyPageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @Assisted private val exoTabSwitchType: Int,
    private val rankingRepository: RankingRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String, exoTabSwitchType: Int): HallOfFameRankingSecondSubDailyPageViewModel
    }

    private val _jsonData = MutableStateFlow<String>("")
    val jsonData: StateFlow<String> = _jsonData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // History 관련 state
    private val _historyYear = MutableStateFlow<String?>(null)
    val historyYear: StateFlow<String?> = _historyYear.asStateFlow()

    private val _historyMonth = MutableStateFlow<String?>(null)
    val historyMonth: StateFlow<String?> = _historyMonth.asStateFlow()

    private val _showPrevButton = MutableStateFlow(true)
    val showPrevButton: StateFlow<Boolean> = _showPrevButton.asStateFlow()

    private val _showNextButton = MutableStateFlow(false)
    val showNextButton: StateFlow<Boolean> = _showNextButton.asStateFlow()

    private var historyList = mutableListOf<HistoryItem>()
    private var currentPosition = 0

    init {
        android.util.Log.d("HoF_Daily_VM", "========================================")
        android.util.Log.d("HoF_Daily_VM", "📦 ViewModel initialized")
        android.util.Log.d("HoF_Daily_VM", "  - chartCode: $chartCode")
        android.util.Log.d("HoF_Daily_VM", "  - exoTabSwitchType: $exoTabSwitchType")
        android.util.Log.d("HoF_Daily_VM", "========================================")

        loadData()
    }

    fun loadData(newChartCode: String? = null, historyParam: String? = null) {
        val codeToUse = newChartCode ?: chartCode

        viewModelScope.launch {
            android.util.Log.d("HoF_Daily_VM", "🔵 Loading 일일 data for chartCode=$codeToUse, historyParam=$historyParam")

            rankingRepository.getHofs(codeToUse, historyParam).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d("HoF_Daily_VM", "⏳ Loading...")
                        _isLoading.value = true
                        _error.value = null
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d("HoF_Daily_VM", "✅ Success: received JSON")
                        _isLoading.value = false
                        _error.value = null
                        _jsonData.value = result.data

                        // Parse history only when historyParam is null (initial load)
                        if (historyParam == null) {
                            parseHistory(result.data)
                        }

                        updatePrevNextVisibility()

                        android.util.Log.d("HoF_Daily_VM", "JSON length: ${result.data.length}")
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("HoF_Daily_VM", "❌ Error: ${result.message}")
                        _isLoading.value = false
                        _error.value = result.message ?: "Unknown error"
                    }
                }
            }
        }
    }

    private fun parseHistory(jsonString: String) {
        try {
            val jsonObject = JSONObject(jsonString)
            val historyArray = jsonObject.optJSONArray("history") ?: return

            historyList.clear()
            for (i in 0 until historyArray.length()) {
                val historyObj = historyArray.getJSONObject(i)
                val historyYear = historyObj.optString("history_year", "")
                val historyMonthRaw = historyObj.optString("history_month", "")

                // API에서 "11" 같은 숫자로 오면 "11월" (한국어) 또는 "Nov" (영어)로 변환
                val historyMonth = formatHistoryMonth(historyMonthRaw)

                android.util.Log.d("HoF_Daily_VM", "History[$i] - Year: '$historyYear', Month raw: '$historyMonthRaw', formatted: '$historyMonth'")

                historyList.add(
                    HistoryItem(
                        historyYear = historyYear,
                        historyMonth = historyMonth,
                        historyParam = historyObj.optString("history_param", ""),
                        nextHistoryParam = historyObj.optString("next_history_param", "")
                    )
                )
            }

            historyList.reverse()
            android.util.Log.d("HoF_Daily_VM", "Parsed ${historyList.size} history items")
        } catch (e: Exception) {
            android.util.Log.e("HoF_Daily_VM", "Error parsing history", e)
        }
    }

    /**
     * history_month 값을 포맷팅 (Old 프로젝트의 HallHistoryModel.historyMonth와 동일)
     * "11" -> "11월" (한국어) 또는 "Nov" (영어)
     */
    private fun formatHistoryMonth(monthString: String): String {
        if (monthString.isEmpty()) return monthString

        return try {
            val stringToDate = SimpleDateFormat("MM", Locale.getDefault())
            val dateToString = SimpleDateFormat("MMM", Locale.getDefault())
            val date = stringToDate.parse(monthString)
            date?.let { dateToString.format(it) } ?: monthString
        } catch (e: Exception) {
            android.util.Log.e("HoF_Daily_VM", "Error formatting month: $monthString", e)
            monthString
        }
    }

    private fun updatePrevNextVisibility() {
        if (currentPosition == 0) {
            _historyYear.value = null
            _historyMonth.value = null
            _showNextButton.value = false
        } else {
            val item = historyList.getOrNull(currentPosition - 1)
            _historyYear.value = item?.historyYear ?: ""
            _historyMonth.value = item?.historyMonth ?: ""
            _showNextButton.value = true

            android.util.Log.d("HoF_Daily_VM", "updatePrevNextVisibility - Year: '${item?.historyYear}', Month: '${item?.historyMonth}'")
        }

        _showPrevButton.value = currentPosition < historyList.size
    }

    fun onPrevClicked(currentChartCode: String) {
        if (currentPosition < historyList.size) {
            currentPosition += 1
            val historyParam = if (currentPosition > 0 && currentPosition <= historyList.size) {
                val item = historyList[currentPosition - 1]
                "${item.historyParam}&${item.nextHistoryParam}"
            } else {
                null
            }
            loadData(currentChartCode, historyParam)
        }
    }

    fun onNextClicked(currentChartCode: String) {
        if (currentPosition != 0) {
            currentPosition -= 1
            val historyParam = if (currentPosition > 0 && currentPosition <= historyList.size) {
                val item = historyList[currentPosition - 1]
                "${item.historyParam}&${item.nextHistoryParam}"
            } else {
                null
            }
            loadData(currentChartCode, historyParam)
        }
    }

    data class HistoryItem(
        val historyYear: String,
        val historyMonth: String,
        val historyParam: String,
        val nextHistoryParam: String
    )
}
