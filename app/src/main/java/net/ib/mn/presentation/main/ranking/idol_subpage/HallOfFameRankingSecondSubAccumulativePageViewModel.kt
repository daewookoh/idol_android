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

/**
 * 명예전당 - 30일 누적 순위 ViewModel
 *
 * charts/ranks/ API를 호출하여 30일 누적 순위 데이터를 가져옵니다.
 */
@HiltViewModel(assistedFactory = HallOfFameRankingSecondSubAccumulativePageViewModel.Factory::class)
class HallOfFameRankingSecondSubAccumulativePageViewModel @AssistedInject constructor(
    @Assisted private val chartCode: String,
    @Assisted private val exoTabSwitchType: Int,
    private val rankingRepository: RankingRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(chartCode: String, exoTabSwitchType: Int): HallOfFameRankingSecondSubAccumulativePageViewModel
    }

    private val _jsonData = MutableStateFlow<String>("")
    val jsonData: StateFlow<String> = _jsonData.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        android.util.Log.d("HoF_Accum_VM", "========================================")
        android.util.Log.d("HoF_Accum_VM", "📦 ViewModel initialized")
        android.util.Log.d("HoF_Accum_VM", "  - chartCode: $chartCode")
        android.util.Log.d("HoF_Accum_VM", "  - exoTabSwitchType: $exoTabSwitchType")
        android.util.Log.d("HoF_Accum_VM", "========================================")

        loadData()
    }

    fun loadData(newChartCode: String? = null) {
        val codeToUse = newChartCode ?: chartCode

        viewModelScope.launch {
            android.util.Log.d("HoF_Accum_VM", "🔵 Loading 30일 누적 data for chartCode=$codeToUse, exoTabSwitchType=$exoTabSwitchType")

            rankingRepository.getChartRanks(codeToUse).collect { result ->
                when (result) {
                    is ApiResult.Loading -> {
                        android.util.Log.d("HoF_Accum_VM", "⏳ Loading...")
                        _isLoading.value = true
                        _error.value = null
                    }
                    is ApiResult.Success -> {
                        android.util.Log.d("HoF_Accum_VM", "✅ Success: received ${result.data.size} items")
                        _isLoading.value = false
                        _error.value = null

                        // Convert to JSON string
                        val jsonString = com.google.gson.Gson().toJson(result.data)
                        _jsonData.value = jsonString

                        android.util.Log.d("HoF_Accum_VM", "JSON length: ${jsonString.length}")
                    }
                    is ApiResult.Error -> {
                        android.util.Log.e("HoF_Accum_VM", "❌ Error: ${result.message}")
                        _isLoading.value = false
                        _error.value = result.message ?: "Unknown error"
                    }
                }
            }
        }
    }
}
